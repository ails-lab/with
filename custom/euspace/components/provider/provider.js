define(['bridget','knockout', 'text!./provider.html','isotope','imagesloaded','app','smoke'], function(bridget,ko, template,Isotope,imagesLoaded,app) {
	
	
		$.bridget('isotope', Isotope);
		

		
		ko.bindingHandlers.profileisotope = {
					init: app.initOrUpdate('init'),
					update: app.initOrUpdate('update')
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
	  self.count=ko.observable(params.count);
	  self.hash=window.location.hash;	
	  self.name=ko.observable('');
	  self.coords=ko.observable(false);
	  self.url=ko.observable(false);
	  self.logo=ko.observable(false);
	  self.hero=ko.observable(false);
	  self.username=ko.observable(false);
	  self.totalCollections=ko.observable(0);
	  self.totalExhibitions=ko.observable(0);
	  var $container = $(".grid").isotope({
			itemSelector: '.media',
			transitionDuration: transDuration,
			masonry: {
				columnWidth		: '.sizer',
				percentPosition	: true
			
			}
		});
    
	  self.revealItems = function (data) {
		   if(data.length==0){ self.loading(false);}
			
			for (var i in data) {
				var c=new Collection(
						data[i]
						);
				self.collections().push(c);
			}
			self.collections.valueHasMutated();
		};
		
		self.revealItems = function (data) {
			if(data.length==0){ self.loading(false);}
			var items=[];
			for (var i in data) {
				var result = new Collection(data[i]);
				items.push(result);
				 
			}
			self.collections.push.apply(self.collections, items);
			var offset = self.collections().length;
			var new_url="";
			if(window.location.hash.indexOf("provider")==1){
				if(window.location.hash.indexOf("count")==-1)
			      new_url=window.location.pathname+window.location.hash+"/count/"+offset;
			    else{
				 temphash=window.location.hash.substring(0,window.location.hash.lastIndexOf("/"));
				 new_url=window.location.pathname+temphash+"/"+offset;
			}}
			history.replaceState('', '', new_url);
			return items;
		};
		
	  
	  self.loadAll = function () {
		  var promise=self.getProviderData();
		  self.loading(true);
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
		        if(self.count()==0 && sessionStorage.getItem("providerview"+self.id())){
					 sessionStorage.removeItem("providerview"+self.id());
				 }
		          var promise2 = self.getProfileCollections();
				  $.when(promise2).done(function(data) {
					       self.totalCollections(data['totalCollections']);  
					       self.totalExhibitions(data['totalExhibitions']);  
					       var items=self.revealItems(data['collectionsOrExhibitions']);
							
							if(items.length>0){
								 var $newitems=getItems(items);
							     
								 providerIsotopeImagesReveal( $container,$newitems );
								
								}
							self.loading(false);
					       
					       
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
				//TODO:add parent project filter
				data: "offset=0&count="+self.count()+"&collectionHits=true&directlyAccessedByGroupName="+JSON.stringify([{group:self.username(),rights:"READ"},{group:WITHApp.projectName,rights:"READ"}]),
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
				var offset = self.collections().length+1;
				$.ajax({
					"url": '/collection/list',
					data: "count=20&offset=" + offset + "&directlyAccessedByGroupName="+JSON.stringify([{group:self.username(),rights:"READ"},{group:WITHApp.projectName,rights:"READ"}]),
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						var items=self.revealItems(data['collectionsOrExhibitions']);
						
						if(items.length>0){
							 var $newitems=getItems(items);
						     
							 providerIsotopeImagesReveal( $container,$newitems );
							
							}
						self.loading(false);
					   
					},
					"error": function (result) {
						self.loading(false);
						$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
					}
				});
			}
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

    
      providerIsotopeImagesReveal = function( $container,$items ) {
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
		    $container.isotope("layout");
		    var scrollpos=sessionStorage.getItem("providerview"+self.id());
		   if(scrollpos && $(".grid").height()>scrollpos){
 		    	 $(window).scrollTop(scrollpos);
 		    	 sessionStorage.removeItem("providerview"+self.id());
 		    }else if(scrollpos!=null && $(".grid").height()<scrollpos){
 		    	$(window).scrollTop($(".grid").height());	
 		    	
 		    }
			
		    
		  });
		  
		  return this;
		};
	
	  
	  loadCollectionOrExhibition = function(data,event) {
		  event.preventDefault();
		  var scrollPosition = $(window).scrollTop();
	      sessionStorage.setItem("providerview"+self.id(), scrollPosition);
	      console.log("setting scrollpos to :"+scrollPosition);
			var item = ko.utils.arrayFirst(self.collections(), function(coll) {
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