define(['bridget', 'knockout', 'text!./collection-view.html', 'isotope', 'imagesloaded', 'app', 'smoke'], function (bridget, ko, template, Isotope, imagesLoaded, app) {

	$.bridget('isotope', Isotope);
	self.loading = ko.observable(false);

	ko.bindingHandlers.collectionIsotope = {
		init: app.initOrUpdate('init'),
		update: app.initOrUpdate('update')
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

		self.title = "";
		self.description = "";
		self.thumb = "";
		self.fullres = "";
		self.view_url = "";
		self.source = "";
		self.creator = "";
		self.provider = "";
		self.dataProvider = "";
		self.dataProvider_uri = "";
		self.rights = "";
		self.url = "";
		self.externalId = "";
		self.thumbnail = "";
		self.likes = 0;
		self.collected = 0;
		self.collectedIn = [];
		self.position=0;
		self.dbId = "";
		self.data = ko.observable('');
		self.thumbnail = ko.pureComputed(function () {

			if (self.thumb) {
				return self.thumb;
			} else {
				return "img/content/thumb-empty.png";
			}
		});
		
		self.fullresolution = ko.pureComputed(function () {

			if (self.fullres) {
				return self.fullres;

			} else {
				return self.thumbnail();
			}
		});

		self.sourceCredits = ko.pureComputed(function () {
			switch (self.source) {
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
			case "The British Library":
				return "www.bl.uk";
			case "Mint":
				return "mint";
			case "Rijksmuseum":
				return "www.rijksmuseum.nl";
			case "DDB":
				return "deutsche-digitale-bibliothek.de";
			default:
				return "";
			}
		});

		self.displayTitle = ko.pureComputed(function () {
			var distitle = "";
			distitle = self.title;
			if (self.creator && self.creator.length > 0)
				distitle += ", by " + self.creator;
			if (self.dataProvider && self.dataProvider.length > 0 && self.dataProvider != self.creator)
				distitle += ", " + self.dataProvider;
			return distitle;
		});

		self.isLiked = ko.pureComputed(function () {
			return app.isLiked(self.externalId);
		});

		self.load = function (options) {
			var admindata = options.administrative;
			var descdata = options.descriptiveData;
			var media = options.media;
			var provenance = options.provenance;
			var usage = options.usage;

			if (descdata) {
				self.title = findByLang(descdata.label);
				self.description = findByLang(descdata.description);
				self.rights = findResOrLit(descdata.metadataRights);
				self.creator = findByLang(descdata.dccreator);
			}

			self.dbId = options.dbId;
			if (provenance) {
				self.view_url = findProvenanceValues(provenance, "source_uri");
				self.dataProvider = findProvenanceValues(provenance, "dataProvider");
				self.provider = findProvenanceValues(provenance, "provider");
				self.source = findProvenanceValues(provenance, "source");
			}

			self.externalId = admindata.externalId;
			self.collectedIn = options.collectedIn;
			if (usage) {
				self.likes = usage.likes;
				self.collected = usage.collected;
			}

			self.thumb = media[0] != null && media[0].Thumbnail != null && media[0].Thumbnail.withUrl != "null" ? media[0].Thumbnail.url : null;
			self.fullres = media[0] != null && media[0].Original != null && media[0].Original.url != "null" ? media[0].Original.url : null;
			self.data(options);
			self.isLoaded = ko.observable(false);
			/*to calculate position find how many of these are already in citems
			 * 
			 */
			/*var foundrec=ko.utils.arrayFilter(ko.dataFor(withcollection).citems(), function(item) {
	            return item.dbId==self.dbId;
			});
			var positions=ko.utils.arrayFilter(self.collectedIn, function(item) {
	            return item.collectionId==ko.dataFor(withcollection).id();
			});
			if(positions && positions[foundrec.length])
			 self.position=positions[foundrec.length].position;*/
			
		};

		if (data !== undefined) self.load(data);
	}

	function CViewModel(params) {
		document.body.setAttribute("data-page", "collection");
		var self = this;
		self.route = params.route;
		var counter = 1;
		self.title = ko.observable('');
		self.description = ko.observable('');
		self.access = ko.observable("READ");
		self.id = ko.observable(params.id);
		self.creator = ko.observable('');
		self.ownerId = ko.observable(-1);
		self.entryCount = ko.observable(0);
		self.citems = ko.observableArray();
		self.selectedRecord = ko.observable(false);
		self.loggedUser = app.isLogged();
		self.rightsmap = ko.mapping.fromJS([]);
		self.isFavorites = ko.observable(false);

		if (params.type === 'favorites') {
			self.isFavorites(true);
			self.id(currentUser.favoritesId());
		}

		if (params.count) {
			self.count = ko.observable(params.count);
		}
		else {
			self.count = ko.observable(10);
			sessionStorage.removeItem("collection-viewscroll" + self.id());
		}

		self.next = ko.observable(-1);
		self.desc = ko.showMoreLess('');
		self.showAPICalls = ko.observable(false);
		self.$container = $(".grid#" + self.id()).isotope({
			itemSelector: '.media',
			transitionDuration: transDuration,
			masonry: {
				columnWidth: '.sizer',
				percentPosition: true
			}
		});

		self.revealItems = function (data) {

			var items = [];
			for (var i in data) {
				var result = data[i];
				if (result != null) {
					var record = new Record(result);

					items.push(record);
					self.citems.push(record);
				}
			}
			//self.citems.push.apply(self.citems, items);
			var offset = self.citems().length;
			var new_url = "";
			if (window.location.hash.indexOf("collectionview") == 1) {
				if (window.location.hash.indexOf("count") == -1)
					new_url = window.location.pathname + window.location.hash + "/count/" + offset;
				else {
					temphash = window.location.hash.substring(0, window.location.hash.lastIndexOf("/"));
					new_url = window.location.pathname + temphash + "/" + offset;
				}
			}
			history.replaceState('', '', new_url);

			return items;
		};

		self.loadCollection = function (id) {
			loading(true);

			$.ajax({
				"url": '/collection/' + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					if (data.administrative.isPublic === false) {
						if (isLogged() === false) {
							window.location = '#login';
							return;
						}
					}
					if (data.descriptiveData) {
						self.title(findByLang(data.descriptiveData.label));

						self.description(findByLang(data.descriptiveData.description));
					}

					self.entryCount(data.administrative.entryCount);
					self.creator = ko.observable("");
					if (data.administrative.withCreator) {
						ko.computed(function () {
							var params = {};
							$.getJSON('/user/' + data.administrative.withCreator, params, self.creator);
						}, this.username);
					}

					self.desc(self.description());
					if (data.administrative.access) {
						ko.mapping.fromJS(data.administrative.access.acl, self.rightsmap);

						var rightsrec = ko.utils.arrayFirst(self.rightsmap(), function (right) {
							return right.user() === currentUser._id();
						});
						if (rightsrec) {
							self.access(rightsrec.level());
						}
					}
					if (self.count() && self.count() > 0) {
						$.ajax({
							"url": "/collection/" + self.id() + "/list?count=" + self.count() + "&start=0",
							"method": "get",
							"contentType": "application/json",
							"success": function (data) {

								var items = self.revealItems(data.records);
								if (items.length > 0) {
									var $newitems = getItems(items);

									self.isotopeImagesReveal(self.$container, $newitems);
								}
								loading(false);
							},
							"error": function (result) {
								loading(false);
								$.smkAlert({
									text: 'An error has occured',
									type: 'danger',
									permanent: true
								});
							}
						});
					} else {
						loading(false);
					}
				},
				error: function (xhr, textStatus, errorThrown) {
					loading(false);
					if (xhr.status == "403") {
						window.location = '#login';
						return;
					}

					$.smkAlert({
						text: 'An error has occured',
						type: 'danger',
						permanent: true
					});
				}
			});
		};

		self.loadCollection();
		self.isOwner = ko.pureComputed(function () {
			if (app.currentUser._id() == self.withCreator) {
				return true;
			} else {
				return false;
			}
		});

		self.loadNext = function () {
			self.moreItems();
		};

		self.moreItems = function () {
			if (loading === true) {
				setTimeout(self.moreItems(), 300);
			}
			if (loading() === false) {
				loading(true);
				var offset = self.citems().length;
				$.ajax({
					"url": "/collection/" + self.id() + "/list?count=10&start=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						var items = self.revealItems(data.records);

						if (items.length > 0) {
							var $newitems = getItems(items);

							self.isotopeImagesReveal(self.$container, $newitems);

						}
						loading(false);

					},
					"error": function (result) {
						loading(false);
					}
				});
			}
		};

		self.addCollectionRecord = function (e) {
			self.citems.push(e);
		};

		removeRecord = function (id, event) {
			var $elem=$(event.target).parents(".item");
			$.smkConfirm({
				text: 'Are you sure you want to permanently remove this item?',
				accept: 'Delete',
				cancel: 'Cancel'
			}, function (ee) {
				if (ee) {
					$.ajax({
						url: '/collection/' + self.id() + '/removeRecord?' + $.param({
							"recId": id,
							"all": false
						}),
						type: 'DELETE',
						contentType: "application/json",
						data: JSON.stringify({
							recId: id,
							all: false
						}),
						success: function (data, textStatus, xhr) {
							var foundrec=ko.utils.arrayFirst(self.citems(), function(item) {
					            return item.dbId==id;
							});
							self.citems.remove(foundrec);
							if ($elem) {
								self.$container.isotope('remove', $elem).isotope('layout');
							}

							self.reloadEntryCount();
							$.smkAlert({
								text: 'Item removed from the collection',
								type: 'success'
							});

						},
						error: function (xhr, textStatus, errorThrown) {
							$.smkAlert({
								text: 'An error has occured',
								type: 'danger',
								time: 10
							});
						}
					});
				} else {
					// Empty
				}
			});
		};

		self.uploadItem = function () {
			app.showPopup('image-upload', {
				collectionId: self.id()
			});
		};

		self.getAPIUrlCollection = function () {
			var url = window.location.href.split("assets")[0];
			var collectionCall = url + "collection/" + self.id();
			return collectionCall;
		};

		self.getAPIUrlRecords = function () {
			var url = window.location.href.split("assets")[0];
			var recordsCall = url + "collection/" + self.id() + "/list?start=0&count=20&format=default";
			return recordsCall;
		};

		self.presentAPICalls = function () {
			if (self.showAPICalls())
				self.showAPICalls(false);
			else
				self.showAPICalls(true);
		};

		likeRecord = function (id, event) {
			event.preventDefault();
			var rec = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.dbId === id;
			});

			app.likeItem(rec, function (status) {
				if (status) {
					$("div." + id).find(".star").addClass('active');

				} else {
					$('div.' + id).find(".star").removeClass('active');
				}
			});
		};

		collect = function (id, event) {
			event.preventDefault();
			var rec = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.dbId === id;
			});

			collectionShow(rec);
		};

		function getItem(record) {

			var tile = '<div class="item media ' + record.dbId + '"><div class="wrap">';

			if (isLogged()) {
				if (record.isLiked()) {
					tile += '<div class="star active">';
				} else {
					tile += '<div class="star">';
				}
				if (self.access() == "WRITE" || self.access() == "OWN")
					tile += ' <span class="collect" title="remove"><i class="fa fa-trash-o fa-inverse" onclick="removeRecord(\'' + record.dbId + '\',event)"></i></span>';

				if (!self.isFavorites()) { // Don't show the favorites icon when in favorites
					if (record.externalId) {
						tile += '<span class="fa fa-fw" onclick="likeRecord(\'' + record.dbId + '\',event);" title="add to favorites">' + '<i class="fa fa-heart fa-stack-1x"></i><i class="fa fa-heart-o fa-stack-1x fa-inverse"></i>' + '</span>';
					}
				}

				tile += '<span class="delete" title="collect" onclick="collect(\'' + record.dbId + '\',event)"><i class="fa fa-download fa-stack-1x fa-inverse"></i></span></div>';

			}

			tile += '<a href="#" data-view="inline"  onclick="recordSelect(\'' + record.dbId + '\',event)">' + '<div class="thumb"><img src="' + record.thumbnail() + '" onError="this.src=\'img/content/thumb-empty.png\'"></div>' + ' <div class="info"><h1 class="title">' + record.displayTitle() + '</h1><span class="owner">' + record.dataProvider + '</span></div></a>' + "<div class='sourceCredits'><a href='" + record.view_url + "' target='_new'>" + record.sourceCredits() + "</a></div></div></div>";

			return tile;
		}

		function getItems(data) {
			var items = '';
			for (i in data) {
				items += getItem(data[i]);
			}
			return $(items);
		}

		recordSelect = function (data, event) {

			event.preventDefault();
			var selrecord = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.dbId === data;
			});
			itemShow(selrecord);
			return false;
		};

		self.reloadEntryCount = function () {
			$.ajax({
				"url": "/collection/" + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {

					self.entryCount(data.administrative.entryCount);

				}
			});
		};

		self.isotopeImagesReveal = function ($container, $items) {
			self.$container = $(".grid#" + self.id());
			var iso = self.$container.data('isotope');
			var itemSelector = null;
			if (iso)
				itemSelector = iso.options.itemSelector;
			else {
				self.$container = $(".grid#" + self.id()).isotope({
					itemSelector: '.media',
					transitionDuration: transDuration,
					masonry: {
						columnWidth: '.sizer',
						percentPosition: true

					}
				});
				iso = self.$container.data('isotope');
				itemSelector = iso.options.itemSelector;
			}
			// append to container
			self.$container.append($items);
			// hide by default
			$items.hide();
			$items.imagesLoaded().progress(function (imgLoad, image) {
				// get item
				var $item = $(image.img).parents(itemSelector);
				// un-hide item
				$item.show();
				iso.appended($item);
				var scrollpos = sessionStorage.getItem("collection-viewscroll" + self.id());
				if (scrollpos && $(".grid#" + self.id()).height() > scrollpos) {
					$(window).scrollTop(scrollpos);
					sessionStorage.removeItem("collection-viewscroll" + self.id());
				} else if (scrollpos && $(".grid#" + self.id()).height() < scrollpos) {
					$(window).scrollTop($(".grid#" + self.id()).height());

				}

			}).always(function () {

				var scrollpos = sessionStorage.getItem("collection-viewscroll" + self.id());
				if (scrollpos && $(".grid#" + self.id()).height() > scrollpos) {
					$(window).scrollTop(scrollpos);
					sessionStorage.removeItem("collection-viewscroll" + self.id());
				} else if (scrollpos && $(".grid#" + self.id()).height() < scrollpos) {
					$(window).scrollTop($(".grid#" + self.id()).height());
					if (scrollpos != null && $(".grid#" + self.id()).height() > scrollpos)
						sessionStorage.removeItem("collection-viewscroll" + self.id());
				}

			});

			return this;
		};
	}

	return {
		viewModel: CViewModel,
		template: template
	};
});