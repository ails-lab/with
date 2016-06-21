define(['bridget','knockout', 'text!./main-content.html','isotope','imagesloaded','app','jquery.sticky'], function(bridget,ko, template,Isotope,imagesLoaded,app,sticky) {
	
	
	$.bridget('isotope', Isotope);
	
	//self.loading=ko.observable(false);
		
	ko.bindingHandlers.homeisotope = {
					init: app.initOrUpdate('init'),
					update: app.initOrUpdate('update')
				};
	
	
	// method to initialize filter stick plugin
	// dependency sticker
	// moved to here from plugin.js, no need to track scroll on the rest of the pages
	var initFilterStick = function(){

		// log
		//logger( 'info','plugins.js / initSticky' );
 
		if ( $( '.filter' ).length !== 0 ){

			// init sticky
			$( '.filter' ).sticky({
				topSpacing: 74
			});

			// on scroll
			$( window ).on( 'scroll touchmove', function(){

				// var
				if( $( "body" ).attr( 'data-page' )=== 'home' ) {
				var offset = $( '.partners' ).offset();
				if(offset){
					topPos = offset.top - 74;
	
					// set class
					if ( $( document ).scrollTop() >= topPos ){
	
						$( '.filter' ).addClass('unstick');
	
					} else {
	
						$( '.filter' ).removeClass('unstick');
	
					}
				}}
			});
		
	}};
	
	
	
	function Collection(data) {
		var self=this;
		
		var mapping = {
				create: function(options) {
			    	var self=this;
			        // use extend instead of map to avoid observables
			    	
			    	self=$.extend(self, options.data);
			    	
			    	self.title=findByLang(self.descriptiveData.label);
			    	self.thumbnail = ko.computed(function() {
			          if(self.media && self.media[0] && self.media[0].Thumbnail){
			        	var data=self.media[0].Thumbnail.url;
			        	 if(data){
			 				return data;}
			 			  else{
			 				   return "img/ui/ic-noimage.png";
			 			   }
			        	}
			        	return "img/ui/ic-noimage.png";
			        });

			        self.type=ko.computed(function() {
			        	if(self.resourceType){
			        		if (self.resourceType.indexOf("Collection")!=-1)
			        		  return "COLLECTION";
			        		else if (self.resourceType.indexOf("Space")!=-1)
			        			return "SPACE";
			        	    else return "EXHIBITION";
			        	}else return "";
			        });
			        
			        self.css=ko.computed(function() {
			        	if(self.resourceType){
			        		if (self.resourceType.indexOf("Collection")!=-1)
			        		  return "item collection";
			        		else if (self.resourceType.indexOf("Space")!=-1)
			        			return "item space";
			        		else return "item exhibition";
			        	}
			        	else{return "item collection";}
			        });
			        
			        self.url=ko.computed(function() {
			        	if(self.resourceType){
			        		if (self.resourceType.indexOf("Collection")>-1)
				    		  return 'index.html#collectionview/'+ self.dbId;
			        		else if (self.resourceType.indexOf("Space")>-1){
			        			return self.administrative.isShownAt;
			        		}
				    		else{
				    			return 'index.html#exhibitionview/'+ self.dbId;
				    		}
			        	}else return "";
			        });
			        self.owner=ko.computed(function(){
			        	if(self.withCreatorInfo){
			        		return self.withCreatorInfo.username;
			        	}
			        	return "";
			        });
			        
			        return self;
			     }
			  
		};
		
		
		var recmapping={
				'dbId': {
					key: function(data) {
			            return ko.utils.unwrapObservable(data.dbId);
			        }
				 }};
		self.isLoaded = ko.observable(false);
		self.records=ko.mapping.fromJS([], recmapping);
		
		
		self.data = ko.mapping.fromJS({"dbID":"","administrative":"","descriptiveData":""}, mapping);
		
		self.load = function(data) {
			self.data=ko.mapping.fromJS(data, mapping);
			
		};

		self.loadRecords= function(offset,count){
			//loading(true);
			var promise=self.getCollectionRecords(0,30);
			 $.when(promise).done(function(responseRecords) {
				 ko.mapping.fromJS(responseRecords.records,recmapping,self.records);
				 //loading(false);
				 
			 });
		}
		
		self.more= function(){
			var offset=self.records().length;
			self.loadRecords(offset,30);
		}
		
		self.getCollectionRecords = function (offset,count) {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/"+self.data.dbId+"/list",
				processData: false,
				data: "start="+offset+"&count="+count,
			}).success (function(){
			});
		};
		
		if(data != undefined){ 
			self.load(data);
			
		}
	}
	
	
  function MainContentModel(params) {
	  this.route = params.route;
	  var self = this;
	  
	  $("div[role='main']").toggleClass( "homepage", true );
	  self.featuredExhibition=ko.observable(null);	
	  self.featuredCollection=ko.observable(null);
	  self.homecollections=ko.observableArray();
	  self.totalCollections=ko.observable(0);
	  self.totalExhibitions=ko.observable(0);
	  self.spaces=ko.observableArray();
	  self.collections=ko.observableArray();
	  self.exhibitions=ko.observableArray();
	  self.all=ko.observableArray();
	  self.morespaces=ko.observable(true);
	  self.fetchitemnum=10;
	  self.nocollections=ko.observable(false);
	  self.noexhibitions=ko.observable(false);
	  self.nospaces=ko.observable(false);
	  self.loadingex=ko.observable(false);
	  self.loadingcoll=ko.observable(false);
	  self.loadingspaces=ko.observable(false);
	  var $container = $("#homemasonry").isotope({
			itemSelector: '.item',
			transitionDuration: transDuration,
			masonry: {
				columnWidth		: '.sizer',
				percentPosition	: true
			}
		});
	  
	  
	
	  self.buttontext=ko.computed(function() {
		  if(self.homecollections().length>0 && self.noexhibitions()==true && self.nocollections()==true && self.nospaces()==true){
			  $(".loadmore").text("no more results");
		  }
		  
	  });
	  
	  self.revealItems = function (data) {
		  if((data.length==0 || data.length<self.fetchitemnum)){ self.nocollections(true);}
		  var items = [];
			for (var i in data) {
				var c=new Collection(
							data[i]				
				);
				
				items.push(c);
				self.homecollections().push(c);
				self.collections.push(c);
			}
			
			self.homecollections.valueHasMutated();self.loadingcoll(false);
			return items;
		};
		
		
		self.revealExItems = function (data) {
			  if((data.length==0 || data.length<self.fetchitemnum)){self.noexhibitions(true);}
				var items=[];
				for (var i in data) {
					var c=new Collection(
								data[i]				
					);
					items.push(c);
					self.homecollections().push(c);
					self.exhibitions.push(c);
				}
				self.homecollections.valueHasMutated();self.loadingex(false);
				return items;
			};
	  
		self.revealSpaceItems = function (data) {
			var groups;
			if (typeof data.groups === 'object') {
				groups = data.groups;
			} else {
				groups = data;
			}
			if(groups.length==0 || groups.length<self.fetchitemnum){self.morespaces(false);self.nospaces(true);}
			var items = [];
				for (var i in groups) {
					var page=groups[i].page;
					var thumb=null;
					var url="";
					
					if(page && page.cover){
						thumb=page.cover.Thumbnail;
						url=page.url;
					}
					
					var spacetocollection={resourceType: 'Space', administrative:{isShownAt:url}, descriptiveData:{label:{default:[groups[i].friendlyName]}},media:[{Thumbnail:{url: thumb}}]};
					var c=new Collection(
								spacetocollection				
					);
					
					items.push(c);
					self.homecollections().push(c);
					self.spaces.push(c);
				}
				self.homecollections.valueHasMutated();self.loadingspaces(false);
				return items;
			};	
		
	  self.loadAll = function () {
		  //this should replaced with get space collections + exhibitions
		  
		  self.loadingcoll(true);
		  self.loadingex(true);
		  self.loadingspaces(true);
		  var promiseCollections = self.getSpaceCollections();
		  var promiseExhibitions=self.getSpaceExhibitions();
		  var promiseSpaces=self.getSpaces();
		  WITHApp.initTooltip();
		 
		  $.when(promiseCollections).done(function(responseCollections) {
			        //self.totalCollections(responseCollections.totalCollections);
			        //self.totalExhibitions(responseCollections.totalExhibitions);
				    var items=self.revealItems(responseCollections['collectionsOrExhibitions']);
				    if (items.length > 0) {
						var $newitems = getItems(items);
						isotopeImagesReveal($newitems);
					}
				    
				    initFilterStick();
				    WITHApp.initIsotope();
				    var selector=$("ul.nav").find("li.active").attr('data-filter');
				    $( settings.mSelector ).isotope({ filter: selector });
				   
					
			});
		 
		  $.when(promiseExhibitions).done(function(response) {
		        //self.totalCollections(response.totalCollections);
		        //self.totalExhibitions(response.totalExhibitions);
			    var items=self.revealExItems(response['collectionsOrExhibitions']);
			    if (items.length > 0) {
					var $newitems = getItems(items);
					isotopeImagesReveal($newitems);
				}
			    initFilterStick();
			    WITHApp.initIsotope();
			    var selector=$("ul.nav").find("li.active").attr('data-filter');
			    $( settings.mSelector ).isotope({ filter: selector });
			   
				
		});
		  $.when(promiseSpaces).done(function(response) {
		        //self.totalSpaces(response);
			    var items=self.revealSpaceItems(response);
			    if (items.length > 0) {
					var $newitems = getItems(items);
					isotopeImagesReveal($newitems);
				}
			    var selector=$("ul.nav").find("li.active").attr('data-filter');
			    $( settings.mSelector ).isotope({ filter: selector });
			    
				
		  });
		  
		  
		  var promise2 = self.getFeatured("56cd993275fe2461e089a8a5");
          $.when(promise2).done(function (data) {
        	  
        	 
        	  self.featuredExhibition(new Collection(data));
        	  $("div.featured-box.exhibition").find("div.featured-hero > img").attr("src",self.featuredExhibition().data.thumbnail());    
        	 
        	  
          });
          var promise3 = self.getFeatured("56cd85e375fe2461e0868723");
          $.when(promise3).done(function (data) {
        	  
        	 
        	  self.featuredCollection(new Collection(data));
        	 
        	  WITHApp.initCharacterLimiter();
          });
          

		 
		};
		
		self.getSpaces = function () {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/group/list",
				processData: false,
				data: "offset=0&count="+self.fetchitemnum,
			}).success (function(){
			});
		};
		
		self.getSpaceCollections = function () {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/listPublic",
				processData: false,
				data: "offset=0&count="+self.fetchitemnum+"&collectionHits=true&isExhibition=false",
			}).success (function(){
			});
		};
		
		
		self.getSpaceExhibitions = function () {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/listPublic",
				processData: false,
				data: "offset=0&count="+self.fetchitemnum+"&collectionHits=true&isExhibition=true",
			}).success (function(){
			});
		};
		
		
         self.getFeatured=function(id) {
			
			/*call must change to get featured exhibition for space*/
	        return $.ajax({
	            type: "GET",
	            url: "/collection/"+id,
	            success: function () {

	            }
	        });
	    };
		
      
		
	    
		self.loadNext = function () {
		  if (!self.loadingcoll() && !self.loadingex() && !self.loadingspaces()) {
			self.loadingcoll(true);
			self.loadingex(true);
			self.loadingspaces(true);
			var promise1=self.moreSpaces();
			var promise2=self.moreCollections();
			var promise3=self.moreExhibitions();
			$.when(promise1,promise2,promise3).done(function(data1,data2,data3){
				var items1=self.revealSpaceItems(data1[0]);
				if (items1.length > 0) {
					var $newitems = getItems(items1);
					isotopeImagesReveal($newitems);
				}
				var items2=self.revealItems(data2[0]['collectionsOrExhibitions']);
				if (items2.length > 0) {
					var $newitems = getItems(items2);
					isotopeImagesReveal($newitems);
				}
				var items3=self.revealExItems(data3[0]['collectionsOrExhibitions']);
				if (items3.length > 0) {
					var $newitems = getItems(items3);
					isotopeImagesReveal($newitems);
				}
				
			})
			
			
			}
		};
		
		self.moreSpaces = function(){
			
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/group/list",
				processData: false,
				data: "count="+self.fetchitemnum+"&offset=" + self.spaces().length,
			}).success (function(){
			});
				
			
		}

		self.moreCollections = function () {
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/listPublic",
				processData: false,
				data: "count="+self.fetchitemnum+"&offset=" + self.collections().length+"&isExhibition=false",
			}).success (function(){
			});
			
		};

		self.moreExhibitions = function () {
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/listPublic",
				processData: false,
				data: "count="+self.fetchitemnum+"&offset=" + self.exhibitions().length+"&isExhibition=true",
			}).success (function(){
			});
			
		};
		
	  self.loadCollectionOrExhibition = function(item) {
		  if (item.isExhibition) {
			  window.location = 'index.html#exhibitionview/'+ item.id;
			  
		  }
		  else {
			  window.location = 'index.html#collectionview/' + item.id;
		  }
		  return false;
	  };
		
      self.loadAll();	  

		
	  
	  self.filter=function(data, event) {
		       if(self.loadingcoll()==false && self.loadingex()==false && self.loadingspaces()==false){
		  			  var selector = event.currentTarget.attributes.getNamedItem("data-filter").value;
					  $(event.currentTarget).siblings().removeClass("active");
					  $(event.currentTarget).addClass("active");
					  $( settings.mSelector ).isotope({ filter: selector });
					 /* if(selector!="*"){
						  $(".loadmore").hide();
						  
					  }
					  else{ $(".loadmore").show();}*/
					  return false;}
				}
					  
	 
	  function getItem(collection) {
			var tile = '<div class="' + collection.data.css() + '"> <div class="wrap">';

			if (collection.data.type()==='COLLECTION' || collection.data.type()==='EXHIBITION') {
				var entryCount;
				if (collection.data.administrative.entryCount === 1) {
					entryCount = collection.data.administrative.entryCount+' item';
				} else {
					entryCount = collection.data.administrative.entryCount+' items';
				}
				tile += '<a href="#" onclick="loadUrl(\'' + collection.data.url() + '\',event)">' +
				'<div class="thumb"><img src="' + collection.data.thumbnail() + '">'+'<div class="counter">'+entryCount+'</div>'+'</div>' +
				'<div class="info"><span class="type">' + collection.data.type() + '</span><h1 class="title">' +
				collection.data.title + '</h1><span class="owner">'+collection.data.owner()+'</span></div>' +
				'</a></div></div>';
			} else {
			tile += '<a href="#" onclick="loadUrl(\'' + collection.data.url() + '\',event)">' +
				'<div class="thumb"><img src="' + collection.data.thumbnail() + '"></div>' +
				'<div class="info"><span class="type">' + collection.data.type() + '</span><h1 class="title">' +
				collection.data.title + '</h1><span class="owner">'+collection.data.owner()+'</span></div>' +
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

		

		isotopeImagesReveal = function ($items) {
			var $container=$("#homemasonry");
			var iso = $container.data('isotope');
			if(!iso){
				var $container = $("#homemasonry").isotope({
					itemSelector: '.item',
					transitionDuration: transDuration,
					masonry: {
						columnWidth		: '.sizer',
						percentPosition	: true
					}
				});
				iso= $container.data('isotope');
			}
			var itemSelector = ".item";

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
				else{console.log("leaving");
					$.error("iso gone");
				}
			});

			return this;
		};
	  
		loadUrl = function (data,event) {
			event.preventDefault();
			window.location.href = data;

			return false;
		};
	
  }
  
 
 
  return { viewModel: MainContentModel, template: template };
});