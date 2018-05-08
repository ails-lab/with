define(['knockout', 'text!./import-collections.html'], function(ko, template) {

	ko.bindingHandlers.selectbody = {
		    init: function(element, valueAccessor) {
		        // Start visible/invisible according to initial value
		        var e = valueAccessor();
		        $(element).name = e.filterName;
		        console.log( e.filterName );
		    },
		    update: function(element, valueAccessor) {
		    	var e = valueAccessor();
		        $(element).name = e.filterName;
		        console.log( e.filterName );
		    }
		};

	
	function Query() {
  	    var self = this;
  	    self.searchTerm = ko.observable("");
  	    self.page = ko.observable("1");
  	    self.pageSize = ko.observable("20");
	  	self.collectionName = ko.observable("");
	  	self.limit = ko.observable("-1");
  	    self.toJson = function (){
  	    	return {  
  	    		collectionName : self.collectionName(),
  	    		query: {
  	    		searchTerm : self.searchTerm(),
  	    		page : self.page(),
  	    		pageSize : self.pageSize()
  	    	}
  	    	}
  	    }
  	}
	
	function CollectionName() {
  	    var self = this;
  	    self.collectionName = ko.observable("");
  	    self.limit = ko.observable("-1");
	    
  	}
  	
	
	
  function TestSearchModel(params) {
	  
	  
    // This viewmodel doesn't do anything except pass through the 'route' parameter to the view.
    // You could remove this viewmodel entirely, and define 'nav-bar' as a template-only component.
    // But in most apps, you'll want some viewmodel logic to determine what navigation options appear.
	  	self = this;
	  	self.im = new CollectionName();
	  	self.q = new Query();
	  	//self.q.searchTerm =  ko.observable("");
	  	//self.q.page =  ko.observable("1");
	  	//self.q.pageSize =  ko.observable("20");
	  	
	  	
	  	self.responses = ko.observableArray([]);
	  	self.filters = ko.observableArray([]);
	  	
	  	
	  	self.importf = function() {
	        //alert(self.q.searchTerm());
	        
	        var json1 = JSON.stringify(self.q.toJson());
	    	//var json1 = q;
	    	console.log( self.im.collectionName());
	    	console.log( json1 );
	    	$.ajax({
	    				"url": "/collection/importEuropeanaCollection?id="+self.im.collectionName()+"&limit="+self.im.limit(),
	    				"method": "GET",
	    				"success": function( data, textStatus, jQxhr ){
	    					console.log( data );
	    	                alert("ok: " +data);
	                
	            },
	            "error": function( jqXhr, textStatus, errorThrown ){
	            alert(errorThrown);
	                console.log( errorThrown );
	                alert("error: " +errorThrown);

	            }
	      });
	  	
	  	}
	  	self.search = function() {
	        //alert(self.q.searchTerm());
	       
	  		 var json1 = JSON.stringify(self.q.toJson());
	    	//var json1 = q;
	    	console.log( self.q.toJson());
	    	console.log( json1 );
	    	$.ajax({
	    				"url": "/collection/importSearch",
	    				"method": "POST",
	    				"contentType": "application/json",
	    				"data":  json1
	    				,
	    				"success": function( data, textStatus, jQxhr ){
	                self.filters(data.filters);
	                self.responses(data.responses);
	                
	            },
	            "error": function( jqXhr, textStatus, errorThrown ){
	            alert(errorThrown);
	                console.log( errorThrown );
	                alert("error: " +errorThrown);

	            }
	            
	            
	    				});
	    }
   
	     
	    
	    self.route = params.route;
  }

  return { viewModel: TestSearchModel, template: template };
});
