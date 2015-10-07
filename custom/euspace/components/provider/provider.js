define(['bridget','knockout', 'text!./provider.html','isotope','imagesloaded','app','smoke'], function(bridget,ko, template,Isotope,imagesLoaded,app) {
	
	
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


			ko.bindingHandlers.profileisotope = {
					init: initOrUpdate('init'),
					update: initOrUpdate('update')
				};
	
					
	
			
	 function Collection(data){
		 var self = this;


		  self.collname='';
		  self.id=-1;
		  self.url='';
		  self.owner='';
		  self.ownerId=-1;
		  self.itemCount=0;
		  self.thumbnail='img/content/thumb-empty.png';
		  self.description='';
		  self.isLoaded = ko.observable(false);
		  self.isExhibition=false;
		  self.itemcss="item ";
		  self.type="COLLECTION";
		  self.load=function(data){
			  if(data.title==undefined){
					self.collname="No title";
				}else{self.collname=data.title;}
				self.id=data.dbId;
				
				self.url="#collectionview/"+self.id;
				
				self.description=data.description;
				if(data.firstEntries.length>0){
					self.thumbnail=data.firstEntries[0].thumbnailUrl;
				}
				self.isExhibition=data.isExhibition;
				if(self.isExhibition){
					self.itemcss+="exhibition";
					self.type="EXHIBITION";
				}
				else{self.itemcss+="collection";}
				if(data.owner!==undefined){
						self.owner=data.owner;
					}

			  
		  }
		  
		 
		  if(data != undefined) self.load(data);
		   
		  
	}
	
  function ProviderModel(params) {
	  this.route = params.route;
	  var self = this;
	  document.body.setAttribute("data-page","profile");
	  self.id = ko.observable(params.id);	

	  /*---*/
	  self.loading = ko.observable(false);
	  self.exhibitloaded=ko.observable(false);
	  self.collections=ko.observableArray();
	  self.address=ko.observable('');
	  self.description=ko.observable('');
	  self.name=ko.observable('');
	  self.coords=ko.observable(false);
	  self.url=ko.observable(false);
	  self.logo=ko.observable(false);
	  self.hero=ko.observable(false);
	  self.username=ko.observable(false);
	  self.totalCollections=ko.observable(0);
	  self.totalExhibitions=ko.observable(0);
    
	  self.revealItems = function (data) {
			for (var i in data) {
				var c=new Collection(
						data[i]
						);
				self.collections().push(c);
			}
			self.collections.valueHasMutated();
		};
		
	  
	  self.loadAll = function () {
		  var promise=self.getProviderData();
		  
		  $.when(promise).done(function (data) {
        	  
	            self.description(data.about);
	            self.username(data.username);
	            self.name(data.friendlyName !=null ? data.friendlyName : data.username);
		        if(data.page){
			          if(data.page.coordinates && data.page.coordinates.latitude && data.page.coordinates.longitude)
			          self.coords("https://www.google.com/maps/embed/v1/place?q="+data.page.coordinates.latitude+","+data.page.coordinates.longitude+"&key=AIzaSyAN0om9mFmy1QN6Wf54tXAowK4eT0ZUPrU");
			          
			          if(data.page.address)self.address(data.page.address);
			          if(data.page.city && data.page.country)
			          self.address(self.address()+" "+data.page.city+ " "+ data.page.country);
				      self.url(data.page.url);  
			          self.logo(data.page.coverThumbnail ? window.location.origin+'/media/' + data.page.coverThumbnail : '');
			          self.hero(data.page.coverImage ? data.page.coverImage : null); 
			          if(self.hero()){
			        	  $(".profilebar > .wrap").css('background-image', 'url('+window.location.origin+'/media/' + self.hero()+')');
			          }
			      }
		          var promise2 = self.getProfileCollections();
				  $.when(promise2).done(function(data) {
					       self.totalCollections(data['totalCollections']);  
					       self.totalExhibitions(data['totalExhibitions']);  
					       self.revealItems(data['collectionsOrExhibitions']);
					       window.EUSpaceUI.initProfile();
					});
          });
		  
		 
          
		  
		};
		
		
		self.getProfileCollections = function () {
			//call should be replaced with collection/list?isPublic=true&offset=0&count=20&isExhibition=false&directlyAccessedByGroupName=[{"orgName":self.username(), "access":"READ"}]
			
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				data: "access=read&offset=0&count=20&collectionHits=true"
			}).success (function(){
			});
		};
		

		self.getProviderData = function () {
			//call should be replaced with self.id() for now use hardcoded 
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/group/"+self.id(),
				processData: false,
				
			}).success (function(){
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
				//replace with collection/list?isPublic=true&offset=0&count=20&isExhibition=false&directlyAccessedByGroupName=[{"orgName":self.username(), "access":"READ"}]
				var offset = self.collections().length+1;
				$.ajax({
					"url": "/collection/list?access=read&count=20&offset=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						self.revealItems(data['collectionsOrExhibitions']);
					    self.loading(false);
					},
					"error": function (result) {
						self.loading(false);
						$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
					}
				});
			}
		};
		
		
		
      self.loadAll();	  

      self.loadCollectionOrExhibition = function(item) {
		  if (item.isExhibition) {
			  window.location = 'index.html#exhibitionview/'+ item.id;
		  }
		  else {
			  window.location = 'index.html#collectionview/' + item.id;
		  }
	  };	
	  
	  self.filter=function(data, event) {
		  			  var selector = event.currentTarget.attributes.getNamedItem("data-filter").value;
					  $(event.currentTarget).siblings().removeClass("active");
					  $(event.currentTarget).addClass("active");
					  $( settings.mSelector ).isotope({ filter: selector });
					  return false;
				}
					  
	 

	  
	
  }
  
 
 
  return { viewModel: ProviderModel, template: template };
});