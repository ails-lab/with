define(['bridget','knockout', 'text!./main-content.html','isotope','imagesloaded','app'], function(bridget,ko, template,Isotope,imagesLoaded,app) {
	
	
	$.bridget('isotope', Isotope);
		
	ko.bindingHandlers.homeisotope = {
					init: app.initOrUpdate('init'),
					update: app.initOrUpdate('update')
				};
	
					
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
	 	
	  self.revealItems = function (data) {
		  if(data.length==0){ self.loading(false);}
			
			for (var i in data) {
				var c=new Collection(
						data[i]
						);
				self.homecollections().push(c);
			}
			self.homecollections.valueHasMutated();
		};
		
	  
	  self.loadAll = function () {
		  //this should replaced with get space collections + exhibitions
		   self.loading(true);
		 
		  var promiseCollections = self.getSpaceCollections();
		  $.when(promiseCollections).done(function(responseCollections) {
			        self.totalCollections(responseCollections.totalCollections);
			        self.totalExhibitions(responseCollections.totalExhibitions);
				    self.revealItems(responseCollections['collectionsOrExhibitions']);
					
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
				data: "offset=0&count=20&collectionHits=true&directlyAccessedByGroupName="+JSON.stringify([{group:WITHApp.projectName,rights:"READ"}]),
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
					"url": "/collection/list?count=20&offset=" + offset + "&directlyAccessedByGroupName="+JSON.stringify([{group:WITHApp.projectName,rights:"READ"}]),
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						self.revealItems(data['collectionsOrExhibitions']);
						
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
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
		  			  var selector = event.currentTarget.attributes.getNamedItem("data-filter").value;
					  $(event.currentTarget).siblings().removeClass("active");
					  $(event.currentTarget).addClass("active");
					  $( settings.mSelector ).isotope({ filter: selector });
					  return false;
				}
					  
	 

	  
	
  }
  
 
 
  return { viewModel: MainContentModel, template: template };
});