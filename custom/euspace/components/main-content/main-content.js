define(['bridget','knockout', 'text!./main-content.html','isotope','imagesloaded','app'], function(bridget,ko, template,Isotope,imagesLoaded,app) {
	
	
	$.bridget('isotope', Isotope);
		
	
	
	
					
	function FeaturedExhibit(data){
	  var fe=this;
	  fe.title=ko.observable();
	  fe.description=ko.observable();
	  fe.dbId=ko.observable(-1);
	  fe.thumbs=ko.observableArray();
	  fe.url=ko.observable();
	  
	  fe.load=function(data){
	     fe.title(data.title);
	     fe.dbId(data.dbId);
	     fe.description(data.description);
	     fe.url="#exhibitionview/"+fe.dbId();
		  var i=0;
		  var j=0;
		  
		  while (i<2 && j<data.firstEntries.length){
			  if(data.firstEntries[j].thumbnailUrl){
				  var thumb={url:data.firstEntries[j].thumbnailUrl,title:data.firstEntries[j].title};
				  fe.thumbs.push(thumb);
				  i++;}
			    j++
		  }}
	  if(data != undefined) fe.load(data);
	  
	}		
			
	 function Collection(data){
		 var self = this;


		  self.collname='';
		  self.id=-1;
		  self.url='';
		  self.owner='';
		  self.ownerId=-1;
		  self.itemCount=0;
		  self.thumbnail='';
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
				if(data.firstEntries.length>0 && data.firstEntries[0].thumbnailUrl){
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
		  self.cachedThumbnail = ko.pureComputed(function() {
				
			   if(self.thumbnail && self.thumbnail!="img/content/thumb-empty.png"){
				if (self.thumbnail.indexOf('/') === 0) {
					return self.thumbnail;
				} else {
					var newurl='url=' + encodeURIComponent(self.thumbnail)+'&';
					return '/cache/byUrl?'+newurl+'Xauth2='+ sign(newurl);
				}}
			   else{
				   return "img/content/thumb-empty.png";
			   }
			});
		  if(data != undefined) self.load(data);
		   
		  
	}
	
  function MainContentModel(params) {
	  this.route = params.route;
	  var self = this;
	  document.body.setAttribute("data-page","home");
	  //setTimeout(function(){ WITHApp.init(); }, 300);
	  self.loading = ko.observable(false);
	  self.exhibitloaded=ko.observable(false);
	  self.featured=ko.observable(null);	
	  self.homecollections=ko.observableArray();
	  self.totalCollections=ko.observable(0);
	  self.totalExhibitions=ko.observable(0);
	  self.hash=window.location.hash;
	  var $container = $(".grid").isotope({
			itemSelector: '.media',
			transitionDuration: transDuration,
			masonry: {
				columnWidth		: '.sizer',
				percentPosition	: true
			
			}
		});	
	  
	  
	  self.loadAll = function () {
		  console.log("load all");
		  //this should replaced with get space collections + exhibitions
		   self.loading(true);
		 
		  var promiseCollections = self.getSpaceCollections();
		  $.when(promiseCollections).done(function(responseCollections) {
			        self.totalCollections(responseCollections.totalCollections);
			        self.totalExhibitions(responseCollections.totalExhibitions);
			        var items=self.revealItems(responseCollections['collectionsOrExhibitions']);
					
					if(items.length>0){
						 var $newitems=getItems(items);
					     
						 homeisotopeImagesReveal( $container,$newitems );
						
						}
					self.loading(false);
				   // self.revealItems(responseCollections['collectionsOrExhibitions']);
					
			});
		  var promise2 = self.getFeaturedExhibition(WITHApp.featuredExhibition);
          $.when(promise2).done(function (data) {
        	  
        	  self.featured(new FeaturedExhibit(data));
        	  $("#featuredExhibit").css('background-image','url('+self.featured().thumbs()[0].url+')');    
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
				data: "offset=0&count=40&collectionHits=true&directlyAccessedByGroupName="+JSON.stringify([{group:WITHApp.projectName,rights:"READ"}]),
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
			console.log("main content more collections");
			
			self.moreCollections();
		};

		self.moreCollections = function () {
			if (self.loading === true) {
				setTimeout(self.moreCollections(), 1000);
			}
			if (self.loading() === false) {
				self.loading(true);
				var offset = self.homecollections().length+1;
				$.ajax({
					"url": "/collection/list?count=40&offset=" + offset + "&directlyAccessedByGroupName="+JSON.stringify([{group:WITHApp.projectName,rights:"READ"}]),
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						var items=self.revealItems(data['collectionsOrExhibitions']);
						
						if(items.length>0){
							 var $newitems=getItems(items);
						     
							 homeisotopeImagesReveal( $container,$newitems );
							
							}
						self.loading(false);
						//self.revealItems(data['collectionsOrExhibitions']);
						
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
		};

	  loadCollectionOrExhibition = function(data,event) {
		  event.preventDefault();
		  var scrollPosition = $(window).scrollTop();
	      sessionStorage.setItem("homemasonryscroll", scrollPosition);
	     	var item = ko.utils.arrayFirst(self.homecollections(), function(coll) {
				   return coll.id === data;
				});
		  if (item.isExhibition) {
			  window.location = 'index.html#exhibitionview/'+ item.id;
			  
		  }
		  else {
			  window.location = 'index.html#collectionview/' + item.id+'/count/0';
		  }
		  return false;
	  };
		

	  
	  self.revealItems = function (data) {
		var items=[];
			
		  
		  if(data.length==0){ self.loading(false);}
			
			for (var i in data) {
				var c=new Collection(
						data[i]
						);
				items.push(c);
			
			}
			self.homecollections.push.apply(self.homecollections, items);
			return items;
			
		};
		
		
		function getItem(collection) {
			
	          var tile= '<div class="'+collection.itemcss+'"> <div class="wrap">';
			
                   tile+='<a href="#" onclick="loadCollectionOrExhibition(\''+collection.id+'\',event)">'
                    +'<div class="thumb"><img src="'+collection.cachedThumbnail()+'"></div>'
                    +' <div class="info"><span class="type">'+collection.type+'</span><h1 class="title">'+collection.collname+'</h1><span class="owner">'+ collection.owner+'</span></div>'
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
		    var scrollpos=sessionStorage.getItem("homemasonryscroll");
			
		    if(scrollpos && $(".grid").height()>scrollpos){
		    	 $(window).scrollTop(scrollpos);
		    	 sessionStorage.removeItem("homemasonryscroll");
		    }else if(scrollpos && $(".grid").height()<scrollpos){
		    	$(window).scrollTop($(".grid").height());	
		    	if($(".grid").height()>scrollpos)
		    		sessionStorage.removeItem("homemasonryscroll");
		    }
		    
		  }).always(function(){
			  var scrollpos=sessionStorage.getItem("homemasonryscroll");
				
			  if(scrollpos && $("#homemasonry").height()<scrollpos)
			   setTimeout(function(){window.scrollTo(scrollpos);},300);
			  
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