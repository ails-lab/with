define(['bootstrap', 'knockout', 'text!./myfavorites.html', 'knockout-else', 'app', 'isotope', 'imagesloaded', 'bridget', 'smoke'], function (bootstrap, ko, template, KnockoutElse, app, Isotope, imagesLoaded) {

	$.bridget('isotope', Isotope);
	var transDuration = '0.4s';
	var isFirefox = typeof InstallTrigger !== 'undefined'; // Firefox 1.0+
	if (isFirefox) {
		transDuration = 0;
	}

	var mapping = {
		create: function(options) {
			var vm = ko.mapping.fromJS(options.data);

			vm.cachedThumbnail = ko.pureComputed(function() {
				if (vm.thumbnailUrl().indexOf('/') === 0) {
					return vm.thumbnailUrl();
				} else {
					return '/cache/byUrl?url=' + encodeURIComponent(vm.thumbnailUrl());
				}
			});

			vm.cachedThumbnail = ko.pureComputed(function() {
				
				   if(vm.thumbnailUrl()){
					if (vm.thumbnailUrl().indexOf('/') === 0) {
						return self.thumb;
					} else {
						var newurl='url=' + encodeURIComponent(vm.thumbnailUrl())+'&';
						return '/cache/byUrl?'+newurl+'Xauth2='+ sign(newurl);
					}}
				   else{
					   return "images/no_image.jpg";
				   }
				});
			vm.displayTitle = ko.pureComputed(function() {
				var distitle = "";
				distitle = '<b>' + vm.title() + '</b>';
				if (vm.creator !== undefined && vm.creator().length > 0) {
					distitle += ", by " + vm.creator();

					if (vm.provider !== undefined && vm.provider().length > 0 && vm.provider() != vm.creator()) {
						distitle += ", " + vm.provider();
					}
				}
				else {
					if (vm.provider !== undefined && vm.provider().length > 0) {
						distitle += ", " + vm.provider();
					}
				}

				return distitle;
			});

			vm.url = ko.pureComputed(function() {
				return '#item/' + vm.dbId();
			});

			vm.sourceCredits = ko.pureComputed(function () {
				switch (vm.source()) {
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

			vm.isLoaded = ko.observable(false);
			if (vm.sourceUrl === undefined) { vm.sourceUrl = ko.observable(); } // Uploaded items have no source url

			return vm;
		}
	};

	function MyFavoritesModel(params) {
		var self = this;
		self.loading = ko.observable(false);
		self.checkLogged=function(){
			if(isLogged()==false){
		
			window.location='#login';
			return;
		  }
		}
		
		self.checkLogged();
		self.items = ko.observableArray();

		self.loadFavorites = function () {
			$.ajax({
				"url": "/collection/favoriteCollection",
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					self.revealItems(data.firstEntries);
				},
				error: function (xhr, textStatus, errorThrown) {
					self.loading(false);
					$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
				}
			});
		};

		self.revealItems = function (data) {
			var itm = ko.mapping.fromJS(data, mapping);
			for (var i in itm()) {
				self.items().push(itm()[i]);
			}
			self.items.valueHasMutated();
		};

		self.loadNext = function () {
			self.moreItems();
		};

		self.moreItems = function () {
			if (self.loading === true) {
				setTimeout(self.moreItems(), 300);
			}
			if (self.loading() === false) {
				self.loading(true);
				var offset = self.items().length;
				$.ajax({
					"url": "/collection/" + app.currentUser.favoritesId() + "/list?count=20&start=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						console.log(data.entryCount);
						self.revealItems(data.records);
						self.loading(false);
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
		};
		
		self.recordSelect = function (e) {
			e.view_url=e.sourceUrl;
			e.thumb=e.cachedThumbnail;
			
			itemShow(e);
		};

		self.loadFavorites();
	}

	function initOrUpdate(method) {
		return function (element, valueAccessor, allBindings, viewModel, bindingContext) {
			function isotopeAppend(ele) {
				// runs isotope animation
				// console.log('nodeType ' + ele.nodeType);
				// nodeType 3 is Text (the empty space before and after div.item)
				// nodeType 1 is element
				if (ele.nodeType === 1) { // Element type
					// console.log("appended isotope");
					$(element).imagesLoaded(function () {
						$(element).isotope('appended', ele); //.isotope('layout');
					});
				}
			}

			function attachCallback(valueAccessor) {
				return function() {
					return {
						data: valueAccessor(),
						afterAdd: isotopeAppend,
					};
				};
			}

			// extend foreach binding
			ko.bindingHandlers.foreach[method](element,
				 attachCallback(valueAccessor), // attach 'afterAdd' callback
				 allBindings, viewModel, bindingContext);

			if (method === 'init') {
				$(element).isotope({
					itemSelector: '.masonry-item',
					transitionDuration: transDuration,
					masonry: {
						columnWidth: 211,
						gutter: 15,
						isFitWidth: true
					}
				});

				ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
					$(element).isotope("destroy");
				});
			} else {
				// console.log("updating...");
			}
		};
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

	ko.bindingHandlers.isotope = {
		init: initOrUpdate('init'),
		update: initOrUpdate('update')
	};

	return {
		viewModel: MyFavoritesModel,
		template: template
	};
});
