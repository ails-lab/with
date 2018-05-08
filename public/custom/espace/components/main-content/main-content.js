define(['bridget','knockout', 'text!./main-content.html','isotope','imagesloaded','app'], function(bridget,ko, template,Isotope,imagesLoaded,app) {
	
	
	$.bridget('isotope', Isotope);
		
	
	self.loading=ko.observable(false);
	
					
		
	function Collection(data) {
		var self=this;
		
		var mapping = {
				create: function(options) {
			    	var self=this;
			        // use extend instead of map to avoid observables
			    	
			    	self=$.extend(self, options.data);
			    	
			    	self.title=findByLang(self.descriptiveData.label);
			    	self.thumbnail = ko.computed(function() {
			          if(self.media && self.media[0]){
			        	  
			              var data;
			        	  if(self.media[0].Thumbnail && self.media[0].Thumbnail.url){
			        	      data=self.media[0].Thumbnail.url;
			        	      return data;}
			        	  else if(self.media[0].Thumbnail && self.media[0].Thumbnail.withUrl){
			        		 data=self.media[0].Thumbnail.withUrl;
			        		 return data;
			        	  }
			 			  else{
			 				   return "img/content/thumb-empty.png";
			 			   }
			        	}
			        	return "img/content/thumb-empty.png";
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
			        	}else return "item collection";
			        });
			        
			        self.url=ko.computed(function() {
			        	if(self.resourceType){
			        		if (self.resourceType.indexOf("Collection")==-1)
				    		  return 'index.html#exhibitionview/'+ self.dbId;
			        		else if (self.resourceType.indexOf("Space")>-1){
			        			return self.administrative.isShownAt;
			        		}
				    		else{
				    			return 'index.html#collectionview/'+ self.dbId;
				    		}
			        	}else return "";
			        });
			        self.owner=ko.computed(function(){
			        	if(self.withCreatorInfo){
			        		return self.withCreatorInfo.username;
			        	}
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
			loading(true);
			var promise=self.getCollectionRecords(0,30);
			 $.when(promise).done(function(responseRecords) {
				 ko.mapping.fromJS(responseRecords.records,recmapping,self.records);
				 loading(false);
				 
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
	  document.body.setAttribute("data-page","home");
	  setTimeout(function(){ WITHApp.init(); }, 300);
	 
	 
	  self.hash=window.location.hash;
	  
	  self.exhibitloaded=ko.observable(false);
	  self.featuredExhibition=ko.observable(null);	
	  self.homecollections=ko.observableArray();
	  self.totalCollections=ko.observable(0);
	  self.totalExhibitions=ko.observable(0);
	  self.collections=ko.observableArray();
	  self.fetchitemnum=20;
	  
	  var $container = $(".grid").isotope({
			itemSelector: '.item',
			transitionDuration: transDuration,
			masonry: {
				columnWidth		: '.sizer',
				percentPosition	: true
			
			}
		});	
	  
	  
	  self.loadAll = function () {
		  //this should replaced with get space collections + exhibitions
		 loading(true);
		 var count=40;
		 if(sessionStorage.getItem("homemasonrycount")){
			 count=sessionStorage.getItem("homemasonrycount");
		 }
		  var promiseCollections = self.getSpaceCollections(count);
		  $.when(promiseCollections).done(function(responseCollections) {
			        self.totalCollections(responseCollections.totalCollections);
			        self.totalExhibitions(responseCollections.totalExhibitions);
			        var items=self.revealItems(responseCollections['collectionsOrExhibitions']);
			       
					if(items.length>0){
						 var $newitems=getItems(items);
					     
						 homeisotopeImagesReveal( $container,$newitems );
						
						}
				    loading(false);
				   // self.revealItems(responseCollections['collectionsOrExhibitions']);
					
			});
		  var promise2 = self.getFeaturedExhibition("570f9af34c74795dd10e3014");
          $.when(promise2).done(function (data) {
        	  
        	  self.featuredExhibition(new Collection(data));
        	  $("#featuredExhibit").css('background-image','url('+self.featuredExhibition().data.thumbnail()+')');    
        	  self.exhibitloaded(true);
        	  WITHApp.initCharacterLimiter();
          });
          
		  
		};
		//TODO:get project from backend. Update hard-coded group name parameter in list collections call.
		
		self.getSpaceCollections = function () {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				data: "offset=0&count="+self.fetchitemnum+"&collectionHits=true&directlyAccessedByUserOrGroup="+JSON.stringify([{group:WITHApp.projectName,rights:"READ"}]),
				//data: "offset=0&count="+self.fetchitemnum+"&collectionHits=true&isPublic=true",
			}).success (function(){
			});
		};
		
	
		
         self.getFeaturedExhibition=function(id) {
			
			/*call must change to get featured exhibition for space*/
	        return $.ajax({
	            type: "GET",
	            url: "/collection/"+id,
	            success: function () {

	            }
	        });
	    };
	    
	    self.loadNext = function () {
			  if (loading() === false) {
				 loading(true);
				
				var promise=self.moreCollections();
				$.when(promise).done(function(data){
					var items=self.revealItems(data['collectionsOrExhibitions']);
					if(items.length>0){
						 var $newitems=getItems(items);
						 homeisotopeImagesReveal( $container,$newitems );
						
						}
					loading(false);
				})
				
				
				}
			};
		
			self.moreCollections = function () {
				return $.ajax({
					type: "GET",
					contentType: "application/json",
					dataType: "json",
					url: "/collection/list",
					processData: false,
					//data: "isPublic=true&count="+self.fetchitemnum+"&offset=" + self.homecollections().length,
					data: "count="+self.fetchitemnum+"&offset=" + self.homecollections().length+"&directlyAccessedByUserOrGroup="+JSON.stringify([{group:WITHApp.projectName,rights:"READ"}]),
					
				}).success (function(){
				});
				
			};

	  
	  
	  loadUrl = function(data,event) {
		 
		  event.preventDefault();
		  var scrollPosition = $(window).scrollTop();
	      sessionStorage.setItem("homemasonryscroll", scrollPosition);
	     
		  window.location.href = data;
		  
		  return false;
	  };
	  
	  self.revealItems = function (data) {
		var items=[];
			
		  
		  if(data.length==0){ loading(false);}
			
			for (var i in data) {
				var c=new Collection(
						data[i]
						);
				items.push(c);
				self.homecollections().push(c);
				
			
			}
			self.homecollections.valueHasMutated();
			sessionStorage.setItem("homemasonrycount", self.homecollections().length);
				
			return items;
			
		};
		
		
		function getItem(collection) {
			
			  var tile= '<div class="'+collection.data.css()+'"> <div class="wrap">';
			
                   tile+='<a href="#" onclick="loadUrl(\''+collection.data.url()+'\',event)">'
                    +'<div class="thumb"><img src="'+collection.data.thumbnail()+'" onerror="this.src=\'img/content/thumb-empty.png\'"></div>'
                    +' <div class="info"><span class="type">'+collection.data.type()+'</span><h1 class="title">'+collection.data.title+'</h1><span class="owner">'+ collection.data.owner()+'</span></div>'
                    +'</a></div></div>';
			return tile;
			
		}
		
      function getItems(data) {
    	  var items = '';
    	  for ( i in data) {
    	    items += getItem(data[i]);
    	  }
    	  return $( items );
    	}
      
	  
      self.loadAll();	  

		
      homeisotopeImagesReveal = function( $container,$items ) {
		  var iso = $container.data('isotope');
		  var itemSelector = iso.options.itemSelector;
		  
		  // append to container
		  $container.append( $items );
		// hide by default
		  $items.hide();
		  $items.imagesLoaded().progress( function( imgLoad, image ) {
		    // get item
		    var $item = $( image.img ).parents( itemSelector );
		    // un-hide item
		    $item.show();
		    iso.appended( $item );
		    $container.isotope( 'layout' );
		    var scrollpos=sessionStorage.getItem("homemasonryscroll");
			
		    
		    
		  }).always(function(){
			 var scrollpos=sessionStorage.getItem("homemasonryscroll");
				/*
			  if(scrollpos && $("#homemasonry").height()<scrollpos)
			   setTimeout(function(){window.scrollTop(scrollpos);sessionStorage.removeItem("homemasonryscroll");},300);
			  */
			  if(scrollpos!=null && $(".grid").height()>scrollpos){
				  console.log("scrolling to "+scrollpos);
			    	 $(window).scrollTop(scrollpos);
			    	 sessionStorage.removeItem("homemasonryscroll");
			    }else if(scrollpos!=null && $(".grid").height()<scrollpos){
			    	 console.log("scrolling to grid height");
			    	$(window).scrollTop($(".grid").height());	
			    	if(scrollpos!=null && $(".grid").height()>scrollpos)
			    		sessionStorage.removeItem("homemasonryscroll");
			    }
		  });
		  
		  
		  return $container;
		};

	  
	  self.filter=function(data, event) {
		  			  var selector = event.currentTarget.attributes.getNamedItem("data-filter").value;
					  $(event.currentTarget).siblings().removeClass("active");
					  $(event.currentTarget).addClass("active");
					  $( settings.mSelector ).isotope({ filter: selector });
					  return false;
				}
					  
	 
	
  }
  
 
 
  return { viewModel: MainContentModel, template: template };
});
