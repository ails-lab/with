define(['bridget','knockout', 'text!./provider.html','isotope','imagesloaded','app','smoke'], function (bridget,ko, template,Isotope,imagesLoaded,app) {

	$.bridget('isotope', Isotope);

	ko.bindingHandlers.profileisotope = {
		init: app.initOrUpdate('init'),
		update: app.initOrUpdate('update')
	};


	function Collection(data) {
		var self = this;

		var mapping = {
			create: function (options) {
				var self = this;
				// use extend instead of map to avoid observables

				self = $.extend(self, options.data);

				self.title = findByLang(self.descriptiveData.label);
				self.description = findByLang(self.descriptiveData.description);
				self.thumbnail = ko.computed(function () {
					if (self.media && self.media[0] && self.media[0].Thumbnail) {
						var data = self.media[0].Thumbnail.url;
						if (data) {
							return data;
						} else {
							return "img/ui/ic-noimage.png";
						}
					}

					return "img/ui/ic-noimage.png";
				});

				self.type = ko.computed(function () {
					if (self.resourceType) {
						if (self.resourceType.indexOf("SimpleCollection") != -1) {
							return "COLLECTION";
						} else if (self.resourceType.indexOf("Org") != -1) {
							return "ORGANIZATION";
						} else {
							return "EXHIBITION";
						}
					} else {
						return "";
					}
				});

				self.css = ko.computed(function () {
					if (self.resourceType) {
						if (self.resourceType.indexOf("SimpleCollection") != -1) {
							return "item collection";
						} else if (self.resourceType.indexOf("Org") != -1) {
							return "item organization";
						} else {
							return "item exhibition";
						}
					}else {
						return "item exhibition";
					}
				});

				self.url = ko.computed(function () {
					if (self.resourceType) {
						if (self.resourceType.indexOf("SimpleCollection") != -1) {
							return 'index.html#collectionview/' + self.dbId;
						} else if (self.resourceType.indexOf("Org") != -1) {
							return self.administrative.isShownAt;
						} else {
							return 'index.html#exhibitionview/' + self.dbId;
						}
					} else {
						return "";
					}
				});

				self.owner = ko.computed(function () {
					if (self.withCreatorInfo) {
						return self.withCreatorInfo.username;
					}
				});

				self.itemCount = ko.computed(function () {
					if (self.administrative) {

						if (self.administrative.entryCount === 1) {
							return "1 Item";
						} else {
							//console.log(self.administrative);
							//return self.administrative.entryCount + " Items";
							if(self.resourceType.indexOf("Org") == -1)
								return self.administrative.entryCount + " Items";
							else 
								return self.administrative.colCount + " Collections";
						}
					}
				});

				return self;
			}
		};

		var recmapping = {
			'dbId': {
				key: function (data) {
					return ko.utils.unwrapObservable(data.dbId);
				}
			}
		};

		self.isLoaded = ko.observable(false);
		self.records = ko.mapping.fromJS([], recmapping);

		self.data = ko.mapping.fromJS({"dbID": "", "administrative": "", "descriptiveData": ""}, mapping);

		self.load = function (data) {
			self.data = ko.mapping.fromJS(data, mapping);
		};

		self.loadRecords = function (offset,count) {
			loading(true);
			var promise = self.getCollectionRecords(0, 30);
			$.when(promise).done(function (responseRecords) {
				ko.mapping.fromJS(responseRecords.records, recmapping, self.records);
				loading(false);
			});
		};

		self.more = function () {
			var offset = self.records().length;
			self.loadRecords(offset, 30);
		};

		self.getCollectionRecords = function (offset,count) {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/" + self.data.dbId + "/list",
				processData: false,
				data: "start=" + offset + "&count=" + count
			}).success (function () {
			});
		};

		if (data != undefined) {
			self.load(data);
		}
	}

	function ProviderModel(params) {
		this.route = params.route;
		var self = this;
		document.body.setAttribute("data-page", "profile");
		setTimeout(function () {
			WITHApp.init();
		}, 600);
		self.id = ko.observable(params.id);

		/*---*/
		self.loading = ko.observable(false);
		self.loadingOrgs = ko.observable(false);
		self.collections = ko.observableArray();
		self.address = ko.observable('');
		self.description = ko.observable('');
		self.count = ko.observable(params.count);
		self.hash = window.location.hash;
		self.name = ko.observable('');
		self.coords = ko.observable(false);
		self.url = ko.observable(false);
		self.logo = ko.observable(false);
		self.hero = ko.observable(false);
		self.username = ko.observable(false);
		self.totalCollections = ko.observable(0);
		self.totalExhibitions = ko.observable(0);
		self.totalOrganizations = ko.observable(0);
		var $container = $("#orggrid").isotope({
			itemSelector: '.item',
			transitionDuration: transDuration,
			masonry: {
				columnWidth		: '.sizer',
				percentPosition	: true
			}
		});

		if (params.count) {
			self.count = ko.observable(params.count);
		} else {
			self.count = ko.observable(20);
			sessionStorage.removeItem("groupview" + self.id());
		}

		self.revealItems = function (data) {
			if (data.length === 0) {
				self.loading(false);
			}

			var items = [];
			for (var i in data) {
				var result = new Collection(data[i]);
				items.push(result);
			}

			self.collections.push.apply(self.collections, items);
			var offset = self.collections().length;
			var new_url = "";
			if (window.location.hash.indexOf("organization") == 1) {
				if (window.location.hash.indexOf("count") == -1) {
					new_url = window.location.pathname + window.location.hash + "/count/" + offset;
				} else {
					temphash = window.location.hash.substring(0, window.location.hash.lastIndexOf("/"));
					new_url = window.location.pathname + temphash + "/" + offset;
				}
			}

			history.replaceState('', '', new_url);
			return items;
		};

		self.loadAll = function () {
			var promise = self.getProviderData();
			self.loading(true);
			$.when(promise).done(function (data) {
				self.description(data.about);
				self.username(data.username);
				self.name(data.friendlyName != null ? data.friendlyName : data.username);
				if (data.page) {
					if (data.page.coordinates && data.page.coordinates.latitude && data.page.coordinates.longitude) {
						self.coords("https://www.google.com/maps/embed/v1/place?q=" + data.page.coordinates.latitude + "," + data.page.coordinates.longitude + "&key=AIzaSyAN0om9mFmy1QN6Wf54tXAowK4eT0ZUPrU");
					}

					if (data.page.address) {
						self.address(data.page.address);
					}

					if (data.page.city && data.page.country) {
						self.address(self.address() + " " + data.page.city + " " + data.page.country);
					}

					if (data.page.url && data.page.url.startsWith("http://")) {
						self.url(data.page.url);
					} else {
						self.url('http://' + data.page.url);
					}

					self.logo(data.avatar && data.avatar.Square ? data.avatar.Square : 'img/ui/profile-placeholder.png');
					self.hero(data.page.cover && data.page.cover.Original ? 'url(' + data.page.cover.Original + ')' : null);
				}

				var promise2 = self.getProfileCollections();
				$.when(promise2).done(function (data) {
					self.totalCollections(data['totalCollections']);
					self.totalExhibitions(data['totalExhibitions']);
					var items = self.revealItems(data['collectionsOrExhibitions']);

					if (items.length > 0) {
						var $newitems = getItems(items);
						providerIsotopeImagesReveal($container, $newitems);
					}

					self.loading(false);
				});
			});
		};

///////////////////////
		
		self.getDescendantOrganizations = function() {
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/group/descendantOrganizations/" + self.id(),
				data: "direct=false&collectionHits=true",
			}).success(function () {});
		}
		
		if(true) {
			var promise3 = self.getDescendantOrganizations();
			$.when(promise3).done(function (data) {
				if (data.length == 0) {
					loadingOrgs(false); return;
				}
				var items = [];
				for (var i in data) {
					var thumb;
					if(data[i].avatar)
						thumb = window.location.origin + data[i].avatar.Thumbnail;
					else
						thumb=null;
					var orgtocollection={resourceType: 'Org', administrative:{isShownAt: data[i].page.url, colCount: data[i].totalCollections}, 
							descriptiveData:{label:{default:[data[i].friendlyName]}},
							media:[{Thumbnail:{url: thumb}}]};
					var o = new Collection(orgtocollection);
					self.collections().push(o);
					items.push(o);
				}
				self.collections.valueHasMutated();
				self.totalOrganizations(data.length);
				
				if (items.length > 0) {
					var $newitems = getItems(items);
					providerIsotopeImagesReveal($container, $newitems);
				}
				
				self.loadingOrgs(false);
			});
			
		}
		

		self.getProfileCollections = function () {
			//call should be replaced with collection/list?isPublic=true&offset=0&count=20&isExhibition=false&directlyAccessedByGroupName=[{"orgName":self.username(), "access":"READ"}]
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				//TODO:add parent project filter
				data: "offset=0&count=" + self.count() + "&directlyAccessedByUserOrGroup=" + JSON.stringify([{group: self.username(),rights: "WRITE"}])
				//data: "offset=0&count="+self.count()+"&collectionHits=true&directlyAccessedByGroupName="+JSON.stringify([{group:self.username(),rights:"READ"},{group:WITHApp.projectName,rights:"READ"}]),
			}).success (function () {
			});
		};


		self.getProviderData = function () {
			//call should be replaced with self.id() for now use hardcoded
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/group/" + self.id(),
				processData: false

			}).success (function () {
			});
		};

		self.loadNext = function () {
			self.moreCollections();
		};

		self.moreCollections = function () {
			if (self.loading === true) {
				setTimeout(self.moreCollections(), 300);
			}
			if (self.loading() === false) {
				self.loading(true);
				var offset = self.collections().length + 1;

				$.ajax({
					url: '/collection/list',
					data: "count=20&offset=" + offset + "&directlyAccessedByUserOrGroup=" + JSON.stringify([{group: self.username(),rights: "WRITE"}]),
					method: "get",
					contentType: "application/json",
					success: function (data) {
						var items = self.revealItems(data['collectionsOrExhibitions']);

						if (items.length > 0) {
							var $newitems = getItems(items);
							providerIsotopeImagesReveal($container, $newitems);
						}
						self.loading(false);
					},
					error: function (result) {
						self.loading(false);
						$.smkAlert({ text: 'An error has occured', type: 'danger', permanent: true });
					}
				});
			}
		};

		loadUrl = function (data,event) {
			event.preventDefault();
			var scrollPosition = $(window).scrollTop();
			sessionStorage.setItem("groupview" + self.id(), scrollPosition);
			window.location.href = data;

			return false;
		};

		function getItem(collection) {
			var tile = '<div class="' + collection.data.css() + '"> <div class="wrap">';

			if (collection.data.resourceType.indexOf("Org") != -1) {
				tile += '<a href="#" onclick="loadUrl(\'' + collection.data.url() + '\',event)">' +
				'<div class="thumb"><img src="' + collection.data.thumbnail() + '"><div class="counter">' + collection.data.itemCount() + '</div></div>' +
				'<div class="info"><span class="type">' + collection.data.type() + '</span><h1 class="title">' +
				collection.data.title + '</h1><p class="text">' + collection.data.description + '</p></div>' +
				'</a></div></div>';
			} else {
				tile += '<a href="#" onclick="loadUrl(\'' + collection.data.url() + '\',event)">' +
				'<div class="thumb"><img src="' + collection.data.thumbnail() + '"><div class="counter">' + collection.data.itemCount() + '</div></div>' +
				'<div class="info"><span class="type">' + collection.data.type() + '</span><h1 class="title">' +
				collection.data.title + '</h1><p class="text">' + collection.data.description + '</p></div>' +
				'</a></div></div>';
			}

			return tile;
		}

		function getItems(data) {
			var items = '';
			for (var i in data) {
				items += getItem(data[i]);
			}

			return $(items);
		}

		self.loadAll();

		providerIsotopeImagesReveal = function ($container,$items) {
			var iso = $container.data('isotope');
			var itemSelector = iso.options.itemSelector;

			// append to container
			$container.append($items);
			// hide by default
			$items.hide();
			$items.imagesLoaded().progress(function (imgLoad, image) {
				// get item
				var $item = $(image.img).parents(itemSelector);
				// un-hide item
				$item.show();
				if(iso)
					  iso.appended($item);
					else{
						$.error("iso gone");
					}
				
				// $container.isotope("layout");
				var scrollpos = sessionStorage.getItem("groupview" + self.id());
				if (scrollpos && $("#orggrid").height() > scrollpos) {
					$(window).scrollTop(scrollpos);
					sessionStorage.removeItem("groupview" + self.id());
				} else if (scrollpos != null && $("#orggrid").height() < scrollpos) {
					$(window).scrollTop($("#orggrid").height());
				}
			});

			return this;
		};

		self.filter = function (data, event) {
			var selector = event.currentTarget.attributes.getNamedItem("data-filter").value;
			$(event.currentTarget).siblings().removeClass("active");
			$(event.currentTarget).addClass("active");
			$(settings.mSelector).isotope({ filter: selector });

			return false;
		};
	}

	return {
		viewModel: ProviderModel,
		template: template
	};
});
