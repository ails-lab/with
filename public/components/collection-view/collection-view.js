define(['bridget','knockout', 'text!./collection-view.html','masonry','imagesloaded'], function(bridget,ko, template, masonry,imagesLoaded) {

	 $.bridget( 'masonry', masonry );	
	 
	 ko.bindingHandlers.masonry = { init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
	    	var $element = $(element);
	    	    $element.masonry( {itemSelector: '.masonryitem',gutter: 10,isInitLayout: false});
			
			    ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
			       
			        $element.masonry("destroy");
			    });

	    },
	    update: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
	    	
	    	var $element = $(element),
	    	list = ko.utils.unwrapObservable(allBindingsAccessor().foreach)
	    	masonry = ko.utils.unwrapObservable(valueAccessor())
	    	if (!list.length){
				
				return;
			}
	    	
	        
	    	imagesLoaded( $element, function() {
	    		if (!($element.data('masonry'))){
	        		
	        		 $element.masonry( {itemSelector: '.masonryitem',gutter: 10,isInitLayout: false});
	        			
	        	}
	    		$('#columns > figure').each(function () {
	 				
	 	 		    $(this).animate({ opacity: 1 });
	 			});
	    		
	    		$element.masonry( 'reloadItems' );
	 			$element.masonry( 'layout' );
	 			
	    		
	 			
	 		 });
			 
	      }
	    };
	 
	 function Citem(data) {
			var self = this;
			self.id = ko.observable(false);
			self.title = ko.observable(false);
			self.description=ko.observable(false);
			self.thumb = ko.observable(false);
			self.fullres=ko.observable(false);
			self.view_url=ko.observable(false);
			self.source=ko.observable(false);
			self.creator=ko.observable("");
			self.provider=ko.observable("");
			self.url=ko.observable("");
			self.id=ko.observable("");
			self.scrolled= function(data, event) {
		        var elem = event.target;
		        if (elem.scrollTop > (elem.scrollHeight - elem.offsetHeight - 200)) {
		        	self.loadNext();
		        }
		    },
			self.load = function(data) {
				if(data.title==undefined){
					self.title("No title");
				}else{self.title(data.title);}
				self.url("#item/"+data.id);
				self.view_url(data.view_url);
				self.thumb(data.thumb);
				self.fullres(data.fullres);
				self.description(data.description);
				self.source(data.source);
				self.creator(data.creator);
				self.provider(data.provider);
				self.id(data.id);
			};

			self.displayTitle = ko.computed(function() {
				if(self.title != undefined) return self.title;
				else if(self.description != undefined) return self.description;
				else return "- No title -";
			});

			if(data != undefined) self.load(data);
		}
	 
	 
	
   function CViewModel(params) {
	  var self = this;

	  self.route = params.route;
	  
	  self.collname=ko.observable('');
	  self.id=ko.observable(params.id);
	  
	  self.citems = ko.observableArray([]);
  
	  
	  self.selectedRecord=ko.observable(false);
		
	  self.loading = ko.observable(false);
	  
	  
		
		self.previous = ko.observable(-1);
		self.page = ko.observable(1);
		self.pageSize=ko.observable(20);
		self.next = ko.observable(-1);
	    
	  
	  self.loadCollection=function(id){
		  self.page(1);
		  self.next(1);
		  self.previous(0);
		  self.loading(true);
		  self.citems([]);
		  $.ajax({
				"url": "/collection/"+self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function(data) {
					console.log(data);
					self.loading(false);
					self.collname(data.title);
						var items = [];
					for(var i in data.firstEntries){
					 var result = data.firstEntries[i];
					 
					 
					 var record = new Citem({
						id: result.dbId,
						thumb: result.thumbnailUrl,
						title: result.title,
						view_url: result.sourceUrl,
						creator: result.creator!==undefined && result.creator!==null && result.creator[0]!==undefined? result.creator[0].value : "",
						provider: result.dataProvider!=undefined && result.dataProvider!==null && result.dataProvider[0]!==undefined? result.dataProvider[0].value : "",
						source: result.source
					  });
					 items.push(record);}
					
					
					
					self.citems.push.apply(self.citems, items);
					
				},
				
				"error":function(result) {
					self.loading(false);
				//	$("#myModal").find("h4").html("An error occured");
				//	$("#myModal").find("div.modal-body").html(result.statusText);
			       
					 
			     }});
	  }
	  
	  self.loadCollection();
	
	  
	  self.loadNext = function() {
			if(self.next()>0){	
				self.page(self.next());
				self.moreItems();}
			};

			
	 self.moreItems=function(){
		 if(collectionitems.length()==20){
			 
		 }
	 }		

	 
	 self.recordSelect= function (e){
			console.log(e);
			itemShow(e);
			
		}
	 
	 window.onscroll = function(ev) {
		    if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight) {
		    	//self.searchNext();
		    	console.log("searching for more");
		    }
		};
  }



  return { viewModel: CViewModel, template: template };
});
