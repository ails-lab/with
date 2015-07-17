define(['bridget', 'knockout', 'text!./collection-view.html', 'masonry', 'imagesloaded', 'app', 'smoke'], function (bridget, ko, template, masonry, imagesLoaded, app) {

	$.bridget('masonry', masonry);
	var transDuration = '0.4s';
	var isFirefox = typeof InstallTrigger !== 'undefined'; // Firefox 1.0+
	if (isFirefox) {
		transDuration = 0;
	}

	ko.bindingHandlers.scroll = {
		updating: true,

		init: function (element, valueAccessor, allBindingsAccessor) {
			var self = this;
			self.updating = true;
			ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
				$(window).off("scroll.ko.scrollHandler");
				self.updating = false;
			});
		},

		update: function (element, valueAccessor, allBindingsAccessor) {
			var props = allBindingsAccessor().scrollOptions;
			var offset = props.offset ? props.offset : "0";
			var loadFunc = props.loadFunc;
			var load = ko.utils.unwrapObservable(valueAccessor());
			var self = this;

			if (load) {
				$(window).on("scroll.ko.scrollHandler", function () {
					if ($(window).scrollTop() >= $(document).height() - $(window).height() - 300) {
						if (self.updating) {
							loadFunc();
							self.updating = false;
						}
					} else {
						self.updating = true;
					}
				});
			} else {
				element.style.display = "none";
				$(window).off("scroll.ko.scrollHandler");
				self.updating = false;
			}
		}
	};

	ko.bindingHandlers.masonrycoll = {
		init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
			var $element = $(element);
			$element.masonry({
				itemSelector: '.masonryitem',
				gutter: 15,
				isFitWidth: true,
				transitionDuration: transDuration
			});

			ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
				$element.masonry("destroy");
			});
		}
	};

	ko.showMoreLess = function (initialText) {

		var observable = ko.observable(initialText);
		observable.limit = ko.observable(100);
		observable.showAll = ko.observable(false);
		observable.showButton = ko.computed(function () {
			return observable().length > observable.limit();
		});
		observable.toggleShowAll = function () {
			observable.showAll(!observable.showAll());
		};
		observable.display = ko.computed(function () {
			if (observable.showAll() || !observable.showButton()) {
				return observable();
			}
			return observable().slice(0, observable.limit());
		}, observable);
		return observable;
	};

	function Record(data) {
		var self = this;

		self.recordId = ko.observable("");
		self.title = ko.observable(false);
		self.description = ko.observable(false);
		self.thumb = ko.observable(false);
		self.fullres = ko.observable(false);
		self.view_url = ko.observable(false);
		self.source = ko.observable(false);
		self.creator = ko.observable("");
		self.provider = ko.observable("");
		self.url = ko.observable("");
		self.rights = ko.observable("");
		self.externalId = ko.observable("");
		self.isLiked = ko.computed(function () {
			return app.isLiked(self.externalId());
		});

		self.load = function (data) {
			if (data.title === undefined) {
				self.title("No title");
			} else {
				self.title(data.title);
			}
			self.url("#item/" + data.id);
			self.view_url(data.view_url);
			self.thumb(data.thumb);
			self.fullres(data.fullres);
			self.description(data.description);
			self.source(data.source);
			self.creator(data.creator);
			self.provider(data.provider);
			self.recordId(data.id);
			self.rights(data.rights);
			self.externalId(data.externalId);
		};

		self.sourceCredits = ko.pureComputed(function () {
			switch (self.source()) {
			case "DPLA":
				return "dp.la";
			case "Europeana":
				return "europeana.eu";
			case "NLA":
				return "nla.gov.au";
			case "DigitalNZ":
				return "digitalnz.org";
			case "EFashion":
				return "europeanafashion.eu";
			case "YouTube":
				return "youtube.com";
			case "Mint":
				return "mint";
			default:
				return "";
			}
		});

		self.displayTitle = ko.pureComputed(function () {
			var distitle = "";
			distitle = '<b>' + self.title() + '</b>';
			if (self.creator() !== undefined && self.creator().length > 0)
				distitle += ", by " + self.creator();
			if (self.provider() !== undefined && self.provider().length > 0 && self.provider() != self.creator())
				distitle += ", " + self.provider();
			return distitle;
		});

		if (data !== undefined) self.load(data);
	}

	function CViewModel(params) {
		var self = this;

		var $container = $("#collcolumns");
		self.route = params.route;
		var counter = 1;
		self.collname = ko.observable('');
		self.access = ko.observable("READ");
		self.id = ko.observable(params.id);
		self.owner = ko.observable('');
		self.ownerId = ko.observable(-1);
		self.itemCount = ko.observable(0);
		self.citems = ko.observableArray([]);

		self.description = ko.observable('');
		self.selectedRecord = ko.observable(false);

		self.loading = ko.observable(false);

		self.next = ko.observable(-1);
		self.desc = ko.showMoreLess('');

		self.loadCollection = function (id) {
			self.loading(true);
			self.citems([]);
			$container.empty();
			$.ajax({
				"url": "/collection/" + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					self.collname(data.title);
					self.desc(data.description);
					self.owner(data.owner);
					self.ownerId(data.ownerId);
					self.itemCount(data.itemCount);
					self.access(data.access);
					self.revealItems(data.firstEntries);
				},
				error: function (xhr, textStatus, errorThrown) {
					self.loading(false);
					$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
				}
			});
		};

		self.loadCollection();
		self.isOwner = ko.pureComputed(function () {
			if (app.currentUser._id() == self.ownerId()) {
				return true;
			} else {
				return false;
			}
		});

		self.loadNext = function () {
			self.moreItems();
		};

		self.moreItems = function () {
			if (self.loading === true) {
				setTimeout(self.moreItems(), 300);
			}
			if (self.loading() === false) {
				self.loading(true);
				var offset = self.citems().length;
				$.ajax({
					"url": "/collection/" + self.id() + "/list?count=20&start=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						console.log(data.itemCount);
						self.revealItems(data.records);
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
		};

		self.masonryImagesReveal = function ($items, $container) {
			$items.hide();
			$container.append($items);
			if (!($container.data('masonry'))) {

				$container.masonry({
					itemSelector: '.masonryitem',
					gutter: 15,
					isFitWidth: true,
					transitionDuration: transDuration
				});
			}

			$items.imagesLoaded().progress(function (imgLoad, image) {
				counter++;
				var $item = $(image.img).parents(".masonryitem");
				ko.applyBindings(self, $item[0]);
				$item.show();
				$container.masonry('appended', $item, true).masonry('layout', $item);

			}).always(function () {
				self.loading(false);
			});
		};

		self.recordSelect = function (e) {
			var selrecord = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.recordId() === e;
			});
			itemShow(selrecord);
		};

		self.addCollectionRecord = function (e) {
			self.citems.push(e);
		};

		self.removeRecord = function (e) {
			$.smkConfirm({text:'Are you sure you want to permanently remove this item?', accept: 'Delete', cancel: 'Cancel'}, function (ee) {
				if (ee) {
					$.ajax({
						url: '/collection/' + self.id() + '/removeRecord?recId=' + e,
						type: 'DELETE',
						contentType: "application/json",
						data: JSON.stringify(e),
						success: function (data, textStatus, xhr) {
							//find item index to see if it is first item
							var index=ko.utils.arrayIndexOf(self.citems(),e);
							console.log("index:"+index);

							self.citems.remove(e);
							if ($("#" + e)) {
								$container.masonry( 'remove', $("#" + e)).masonry( 'layout');
							}

							self.itemCount(self.itemCount() - 1);
							$.smkAlert({text:'Item removed from the collection', type:'success'});

						},
						error: function (xhr, textStatus, errorThrown) {
							$.smkAlert({text:'An error has occured', type:'danger', time: 10});
						}
					});
				} else {

				}
			});
		};

		self.likeRecord = function (id) {
			var rec = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.externalId() === id;
			});

			app.likeItem(rec, function (status) {
				if (status) {
					$('#' + id).addClass('active');
				} else {
					$('#' + id).removeClass('active');
				}
			});
		};

		function getItem(record,index) {
			var figure = '<figure class="masonryitem" id="' + record.recordId() + '">';
			if (record.isLiked()) {
				figure += '<span class="star active" id="' + record.externalId() + '">';
				figure += '<span class="fa-stack fa-fw" data-bind="event: { click: function() { likeRecord(\'' + record.externalId() + '\'); } }">';
				figure += '<i class="fa fa-heart fa-stack-1x"></i><i class="fa fa-heart-o fa-stack-1x fa-inverse"></i>';
				figure += '</span></span>';
			} else {
				figure += '<span class="star" id="' + record.externalId() + '">';
				figure += '<span class="fa-stack fa-fw" data-bind="event: { click: function() { likeRecord(\'' + record.externalId() + '\'); } }">';
				figure += '<i class="fa fa-heart fa-stack-1x"></i><i class="fa fa-heart-o fa-stack-1x fa-inverse"></i>';
				figure += '</span></span>';
			}

			figure += '<a data-bind="event: { click: function() { recordSelect(\'' + record.recordId() + '\')}}"><img onError="this.src=\'images/no_image.jpg\'" src="' + record.thumb() + '" width="211"/></a><figcaption>' + record.displayTitle() + '</figcaption>'
			+ '<div class="sourceCredits">';

			if (self.access() == "WRITE" || self.access() == "OWN") {
				figure += '<span class="glyphicon glyphicon-trash closeButton" data-bind="event: { click: function(){ removeRecord(\'' + record.recordId() + '\','+index+')}}"></span>';
			}
			figure+='<a href="' + record.view_url() + '" target="_new">' + record.sourceCredits() + '</a></figure>';
			return figure;
		}

		function getItems(data) {
			var items = '';
			for (var i in data) {
				items += getItem(data[i]);
			}
			return $(items);
		}

		self.uploadItem = function() {
			app.showPopup('image-upload', { collectionId: self.id() });
		};

		self.revealItems = function (data) {
			var items = [];
			for (var i in data) {
				var result = data[i];
				var record = new Record({
					id: result.dbId,
					thumb: result.thumbnailUrl,
					title: result.title,
					view_url: result.sourceUrl,
					creator: result.creator,
					provider: result.provider,
					source: result.source,
					rights: result.rights,
					externalId: result.externalId
				});
				items.push(record);
			}
			self.citems.push.apply(self.citems, items);
			var $newitems = getItems(items);

			self.masonryImagesReveal($newitems, $container);
		};
	}

	return {
		viewModel: CViewModel,
		template: template
	};
});
