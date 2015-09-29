define(['bridget', 'knockout', 'text!./collection-view.html', 'isotope', 'imagesloaded', 'app','smoke'], function (bridget, ko, template, Isotope, imagesLoaded,app) {

	$.bridget('isotope', Isotope);
	

	 function initOrUpdate(method) {
			return function (element, valueAccessor, allBindings, viewModel, bindingContext) {
				function isotopeAppend(ele) {
					if (ele.nodeType === 1) { // Element type
						$(element).imagesLoaded(function () {
							$(element).isotope('appended', ele).isotope('layout');
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

				var data = ko.utils.unwrapObservable(valueAccessor());
				//extend foreach binding
				ko.bindingHandlers.foreach[method](element,
					 attachCallback(valueAccessor), // attach 'afterAdd' callback
					 allBindings, viewModel, bindingContext);

				if (method === 'init') {
					$(element).isotope({
						itemSelector: '.item',
						transitionDuration: transDuration,
						masonry: {
							columnWidth		: '.sizer',
							percentPosition	: true
						}
					});

					ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
						$(element).isotope("destroy");
					});
					
				} else {
					 console.log("updating isotope...");
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


		ko.bindingHandlers.collectionIsotope = {
				init: initOrUpdate('init'),
				update: initOrUpdate('update')
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
	    self.recordId = "";
		self.title = "";
		self.description="";
		self.thumb = "";
		self.fullres="";
		self.view_url="";
		self.source="";
		self.creator="";
		self.provider="";
		self.rights="";
		self.url="";
		self.externalId = "";
		 self.isLoaded = ko.observable(false);
		 
		self.load = function(data) {
			if(data.title==undefined){
				self.title="No title";
			}else{self.title=data.title;}
			self.url="#item/"+data.id;
			self.view_url=data.view_url;
			self.thumb=data.thumb;
			self.fullres=data.fullres;
			self.description=data.description;
			self.source=data.source;
			self.creator=data.creator;
			self.provider=data.provider;
			self.rights=data.rights;
			self.recordId=data.id;
			self.externalId=data.externalId;
		};

		self.cachedThumbnail = ko.pureComputed(function() {
			
		   if(self.thumb){
			if (self.thumb.indexOf('/') === 0) {
				return self.thumb;
			} else {
				var newurl='url=' + encodeURIComponent(self.thumb)+'&';
				return '/cache/byUrl?'+newurl+'Xauth2='+ sign(newurl);
			}}
		   else{
			   return "img/content/thumb-empty.png";
		   }
		});
		self.sourceCredits = ko.pureComputed(function() {
			 switch(self.source) {
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
			    case "YouTube": {
			    	return "youtube.com";
			    }
			    case "WITHin":
			    	return "WITHin";
			    default: return "";
			 }
			});

		self.displayTitle = ko.pureComputed(function() {
			var distitle="";
			distitle=self.title;
			if(self.creator!==undefined && self.creator.length>0)
				distitle+=", by "+self.creator;
			if(self.provider!==undefined && self.provider.length>0 && self.provider!=self.creator)
				distitle+=", "+self.provider;
			return distitle;
		});

		if(data != undefined) self.load(data);
		
	}

	function CViewModel(params) {
		document.body.setAttribute("data-page","collection");
		var self = this;

		var $container = $(".grid");
		self.route = params.route;
		var counter = 1;
		self.collname = ko.observable('');
		self.access = ko.observable("READ");
		self.id = ko.observable(params.id);
		self.owner = ko.observable('');
		self.ownerId = ko.observable(-1);
		self.itemCount = ko.observable(0);
		self.citems = ko.observableArray();

		
	   
		self.description = ko.observable('');
		self.selectedRecord = ko.observable(false);

		self.loading = ko.observable(false);

		self.next = ko.observable(-1);
		self.desc = ko.showMoreLess('');
		
		self.revealItems = function (data) {
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
				
				self.citems().push(record);
			}
			self.citems.valueHasMutated();
		};
		
	

		self.loadCollection = function (id) {
			self.loading(true);
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
					self.loading(false);
					window.EUSpaceUI.initTooltip();
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
					"url": "/collection/" + self.id() + "/list?count=40&start=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						console.log(data.itemCount);
						self.revealItems(data.records);
						self.loading(false);
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
		};

		

		self.recordSelect= function (e){
        	$( '.itemview' ).fadeIn();
			itemShow(e);

		}
	
		

		
	}

	return {
		viewModel: CViewModel,
		template: template
	};
});
