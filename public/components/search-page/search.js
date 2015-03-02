define(['bridget','knockout', 'text!./search.html','masonry','imagesloaded'], function(bridget,ko, template,masonry,imagesLoaded) {
	
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
    
	
	function Record(data) {
		var self = this;
		self.id = ko.observable(false);
		self.title = ko.observable(false);
		self.description=ko.observable(false);
		self.thumb = ko.observable(false);
		self.fullres=ko.observable(false);
		self.view_url=ko.observable(false);
		self.source=ko.observable(false);
		self.load = function(data) {
			if(data.title==undefined){
				self.title("No title");
			}else{self.title(data.title);}
			
			self.view_url(data.view_url);
			self.thumb(data.thumb);
			self.fullres(data.fullres);
			self.description(data.description);
			self.source(data.source);
			
		};

		self.displayTitle = ko.computed(function() {
			if(self.title != undefined) return self.title;
			else if(self.description != undefined) return self.description;
			else return "- No title -";
		});

		if(data != undefined) self.load(data);
	}

	function SourceCategory(data) {
		var self = this;
		self.source = ko.observable("");
		self.items=ko.observableArray([]);
		
		
		self.load=function(data){
			self.source=data.source;
			
			self.items=data.items;
		};
		
		
		self.addItem = function(c) {            
	           self.items.push(new Record(c));
	        };    
	                 
	            
		
		self.append =function(newitems){
			self.items.push.apply(self.items, newitems);
			};
		
		if(data != undefined) self.load(data);
	}
	
	
	function SearchModel(params) {
		var self = this;

		self.route = params.route;
		self.term = ko.observable("");
		self.sourceview=ko.observable(false);
		self.sources= ko.observableArray([]);
		self.mixresults=ko.observableArray([]);
		self.results = ko.observableArray([]);
		self.selectedRecord=ko.observable(false);
		//self.results.extend({ rateLimit: 50 });
		
		self.searching = ko.observable(false);
		self.scrolled= function(data, event) {
	        var elem = event.target;
	        if (elem.scrollTop > (elem.scrollHeight - elem.offsetHeight - 200)) {
	        	self.searchNext();
	        }
	    },
		self.currentTerm = ko.observable("");
		self.previous = ko.observable(-1);
		self.page = ko.observable(1);
		self.pageSize=ko.observable(20);
		self.next = ko.observable(-1);

		self.noResults = ko.computed(function() {
			return (!self.searching() && self.results().length == 0 && self.currentTerm() != "");
		})
		
		self.toggleSourceview = function () { self.sourceview(!self.sourceview())};
		
		self.reset = function() {
			
			self.term("");
			self.currentTerm = ko.observable("");
			self.page(1);
			self.pageSize(20);
			self.previous(-1);
			self.next(-1);
			self.mixresults([]);
			self.results([]);
			self.searching(false);
		}

		self._search = function() {
		 if(self.searching()==false && self.currentTerm()!=""){
			self.searching(true);
			$.ajax({
				"url": "/api/search",
				"method": "post",
				"contentType": "application/json",
				"data": JSON.stringify({
					searchTerm: self.currentTerm(),
					page: self.page(),
					pageSize:self.pageSize(),
				    source:self.sources()
				}),
				"success": function(data) {
					
					self.previous(self.page()-1);
					self.next(self.page()+1);
					for(var i in data) {
						source=data[i].source;
						var items = [];
						for(var j in data[i].items){
						 var result = data[i].items[j];
						 console.log("source:"+source);
						 if(result !=null && result.thumb!=null && result.thumb[0]!=null  && result.thumb[0]!="null"){
						 var record = new Record({
							id: result.id,
							thumb: result.thumb[0],
							fullres: result.fullresolution,
							title: result.title[0].value,
							view_url: result.url.fromSourceAPI,
							source: source
						  });
						 items.push(record);}
						}
						if(items.length>0){
							
							self.mixresults.push.apply(self.mixresults, items);
							
						}
						var srcCat=new SourceCategory({
							source:source,
							items:items
						});
						var found=false;
						if(self.results().length>0)
						  for(var k in self.results()){
							var inCat=self.results()[k];
							if(inCat.source==srcCat.source){
								found=true;
								inCat.append(srcCat.items);
								self.results.replace(self.results()[k],new SourceCategory({
									source:inCat.source,
									items:inCat.items
								}));
								break;
							}
							
						  }
						if(srcCat.items.length>0 && (!found || self.results().length==0)){
							self.results.push(srcCat);
						}
						
					}
						self.searching(false);
				}
			});
			console.log(self.term());
		 }
		};

		
		self.search = function() {
			
	      
			self.results([]);
			self.mixresults([]);
			self.page(1);
			self.currentTerm(self.term());
			self.searching(false);
			self._search();
		};

		self.recordSelect= function (e){
			console.log(e);
			itemShow(e);
			
		}
		
		self.searchNext = function() {
			self.page(self.next());
			self._search();
		};

		self.searchPrevious = function() {
			self.page(self.previous());
			self._search();
		};

      

	  var withsearch = $( '#withsearchid' );
	  var withinput =$("input.withsearch-input");
	  ctrlClose =$("span.withsearch-close");
	  isOpen = false;
		// show/hide search area
     toggleSearch = function(evt,char) {
			// return if open and the input gets focused
			if(  evt === 'focus' && isOpen ) return false;

			if( isOpen ) {
				withsearch.removeClass("open");
				withinput.blur();
			}
			else {
				var isOpera = !!window.opera || navigator.userAgent.indexOf(' OPR/') >= 0;
			    // Opera 8.0+ (UA detection to detect Blink/v8-powered Opera)
			    var isFirefox = typeof InstallTrigger !== 'undefined';   // Firefox 1.0+

				withsearch.addClass('open');
				withinput.focus();
				if(isOpera || isFirefox)
				withinput.val(char);
				
			}
			isOpen = !isOpen;
		};

		
	    $(document).keyup(function(e) {

  		  if (e.keyCode == 27 && isOpen ) { 
  			self.reset();
  			toggleSearch(e,'');
  			  
  			  }   // esc
  		});
        ctrlClose.on('click',function(event){
    		self.reset();
    		toggleSearch(event,'');
    		}
    	);
        
       	 
  }
 
  return { viewModel: SearchModel, template: template };
});
