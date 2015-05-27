define(['bridget','knockout', 'text!./main-content.html','masonry','imagesloaded','app'], function(bridget,ko, template,masonry,imagesLoaded,app) {

	 $.bridget( 'masonry', masonry );



	 ko.bindingHandlers.masonrypc = { init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
	    	var $element = $(element);
	    	console.log($element);
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

	        		 $element.masonry( {itemSelector: '.masonryitem',gutter: 5,isInitLayout: false,isFitWidth: true});

	        	}
	    		$element.masonry( 'reloadItems' );
	 			$element.masonry( 'layout' );


	    		$('#publiccolumns > figure').each(function () {

	 	 		    $(this).animate({ opacity: 1 });
	 			});



	 		 });

	      }
	    };
	 
	 function Collection(data){
		 var self = this;


		  self.collname=ko.observable('');
		  self.id=ko.observable(-1);
		  self.url=ko.observable("");
		  self.owner=ko.observable('');
		  self.ownerId=ko.observable(-1);
		  self.itemCount=ko.observable(0);
		  self.thumbnail=ko.observable('images/no_image.jpg');
		  self.citems = ko.observableArray([]);
		  self.description=ko.observable('');
		  self.load=function(data){
			  if(data.title==undefined){
					self.collname("No title");
				}else{self.collname(data.title);}
				self.id(data.dbId);
				
				self.url("#collectionview/"+self.id());
				
				self.description(data.description);
				if(data.firstEntries.length>0){
					self.thumbnail(data.firstEntries[0].thumbnailUrl);
				}
				if(data.owner!==undefined){
						self.owner(data.owner);
					}

			  
		  }
		  if(data != undefined) self.load(data);
		   
		  
	}
	
  function MainContentModel(params) {
	  this.route = params.route;
	  var self = this;


	  self.collections=ko.observableArray([]);
	  if(sessionStorage.getItem('PublicCollections')!==null){
		  var c_items = JSON.parse(sessionStorage.getItem("PublicCollections"));
		  var citems=[];
			for(i in c_items){
				var c=new Collection(
						{title:c_items[i].title,
						 dbId:c_items[i].dbId,
						 firstEntries:c_items[i].firstEntries,
						 owner:c_items[i].owner}
						);
				citems.push(c);
				
				
			}
			
		  self.collections.push.apply(self.collections,citems);
	  }else{
	  var promise = app.getPublicCollections();
	  $.when(promise).done(function() {
			if (sessionStorage.getItem('PublicCollections') !== null){
				var c_items = JSON.parse(sessionStorage.getItem("PublicCollections"));
			  var citems=[];
				for(i in c_items){
					var c=new Collection(
							{title:c_items[i].title,
							 dbId:c_items[i].dbId,
							 firstEntries:c_items[i].firstEntries,
							 owner:c_items[i].owner}
							);
					citems.push(c);
					
					
				}
				
			  self.collections.push.apply(self.collections,citems);}
			
		});
	  }
	  /*self.loadCollections=function(){
			$.ajax({
				type        : "GET",
				contentType : "application/json",
				dataType    : "json",
				url         : "/collection/list",
				processData : false,
				data        : "access=read&offset=0&count=20"}).done(
				function(data, text) {
					console.log(data);
					var citems=[];
					for(i in data){
						var c=new Collection(
								{title:data[i].title,
								 id:data[i].dbId,
								 firstEntries:data[i].firstEntries,
								 owner:data[i].owner}
								);
						citems.push(c);
						
						
					}
					
					self.collections.push.apply(self.collections,citems);
					
				}).fail(function(request, status, error) {

					var err = JSON.parse(request.responseText);
					console.log(err);
				}
			);
			
		}
	  //self.loadCollections();*/
		
	  
	  $('.containermason').each(function(index,listitem){
		 
		  var url="";
		  if($(this).find('div.bigsquare').attr('id')=='omeka'){
			  url='http://digitalgallery.promoter.it/';
		  }
		  
		 
		  $(this).masonry({
			  columnWidth: '.littlesquare',
			  itemSelector: '.square',
			  gutter: 1
			});
		  if(url.length==0)
			$(this).append("<span class='withsearch-view'><i class='fa fa-arrow-circle-right' title='see more'></i></span>");
		  else{
			  $(this).append("<span class='withsearch-view'>"+"<a href=\'"+url+"\' target='_blank'><i class='fa fa-arrow-circle-right' title='see more'></i></a></span>");
		  }	
			
		  
	  });
	  
	
  }
 
  return { viewModel: MainContentModel, template: template };
});