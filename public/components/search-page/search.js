define(['bridget','knockout', 'text!./search.html','masonry','imagesloaded'], function(bridget,ko, template,masonry,imagesLoaded) {

	 $.bridget( 'masonry', masonry );





    ko.bindingHandlers.masonry = { init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
    	var $element = $(element);
    	    $element.masonry( {itemSelector: '.masonryitem',gutter: 10,isInitLayout: false});

		    ko.utils.domNodeDisposal.addDisposeCallback(element, function() {

		        $element.masonry("destroy");
		    });

    }
    };



	function Record(data) {
		var self = this;
		self.recordId = ko.observable("");
		self.title = ko.observable(false);
		self.description=ko.observable(false);
		self.thumb = ko.observable("//placehold.it/200x200");
		self.fullres=ko.observable(false);
		self.view_url=ko.observable(false);
		self.source=ko.observable(false);
		self.creator=ko.observable("");
		self.provider=ko.observable("");
		self.rights=ko.observable("");
		self.url=ko.observable("");
		//self.id=ko.observable("");
		self.load = function(data) {
			if(data.title==undefined){
				self.title("No title");
			}else{self.title(data.title);}
			self.url("#item/"+data.recordId);
			self.view_url(data.view_url);
			self.thumb(data.thumb);
			self.fullres(data.fullres);
			self.description(data.description);
			self.source(data.source);
			self.creator(data.creator);
			self.provider(data.provider);
			self.rights(data.rights);
			self.recordId(data.recordId);
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
		self.consoleurl=ko.observable("");
		self.items=ko.observableArray([]);


		self.load=function(data){
			self.source=data.source;
			self.consoleurl=data.consoleurl;
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
		self.sources= ko.observableArray([ "Europeana","DPLA","YouTube","DigitalNZ","NLA","Mint"]);
		self.mixresults=ko.observableArray([]);
		self.results = ko.observableArray([]);
		self.selectedRecord=ko.observable(false);
		//self.results.extend({ rateLimit: 50 });

		self.searching = ko.observable(false);
		self.scrolled= function(data, event) {
	        var elem = event.target;
	        if (elem.scrollTop > (elem.scrollHeight - elem.offsetHeight - 300)) {
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

		self.toggleSourceview = function () { self.sourceview(!self.sourceview());
		if(self.sourceview()==false){
			$('.withsearch-content').css({'overflow-x': 'hidden'});
			self.searching(true);
			imagesLoaded( '#columns', function() {
				  $('#columns').masonry( 'reloadItems' );
	    		  $('#columns').masonry( 'layout' );
	    		  $('#columns > figure').each(function () {

	    			  $(this).animate({ opacity: 1 });
	    		  	});
	    		  self.searching(false);


	    		  });
		  }
		else{
			$('.withsearch-content').css({'overflow-x': 'auto'});
		  }

		};

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
			var isFirefox = typeof InstallTrigger !== 'undefined';   // Firefox 1.0+
			var isSafari = Object.prototype.toString.call(window.HTMLElement).indexOf('Constructor') > 0;
			  
		   if(self.page()==1 && (isFirefox || isSafari)){self.sourceview(true);}
			
		 $(".withsearch-input").devbridgeAutocomplete("hide");
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
					var moreitems=false;

					for(var i in data) {
						source=data[i].source;
						//count should be working in api but it's not, use item length until fixed
						if(data[i].items!=null && data[i].items.length==self.pageSize() && moreitems==false){
							moreitems=true;
						}
						var items = [];
						for(var j in data[i].items){
						 var result = data[i].items[j];

						 if(result !=null && result.title[0]!=null && result.title[0].value!="[Untitled]" && result.thumb!=null && result.thumb[0]!=null  && result.thumb[0]!="null" && result.thumb[0]!=""){
						 var record = new Record({
							recordId: result.recordId || result.id,
							thumb: result.thumb[0],
							fullres: result.fullresolution,
							title: result.title[0].value,
							view_url: result.url.fromSourceAPI,
							creator: result.creator!==undefined && result.creator!==null && result.creator[0]!==undefined? result.creator[0].value : "",
							provider: result.dataProvider!=undefined && result.dataProvider!==null && result.dataProvider[0]!==undefined? result.dataProvider[0].value : "",
							rights: result.rights!==undefined && result.rights!==null && result.rights[0]!==undefined? result.rights[0].value : "",

							source: source
						  });
						 items.push(record);}
						}
						if(items.length>0){
							self.mixresults.push.apply(self.mixresults, items);
							}
						api_console="";
						if(source=="Europeana"){
							api_console="http://labs.europeana.eu/api/console/?function=search&query="+self.term();
							}
						else if(source=="DPLA"){
							api_console="http://api.dp.la/";
						}
						else if(source=="NLA"){
							api_console="http://api.trove.nla.gov.au/";
						}
						else if(source=="DigitalNZ"){
							api_console="http://api.digitalnz.org/"
						}
						else{api_console="http://www.europeanafashion.eu/api/search/"+self.term();}
						var srcCat=new SourceCategory({
							source:source,
							items:items,
							consoleurl:api_console
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
									items:inCat.items,
									consoleurl:inCat.consoleurl
								}));
								break;
							}

						  }
						if(srcCat.items.length>0 && (!found || self.results().length==0)){
							self.results.push(srcCat);
						}

					}
					
					
				
				if(self.sourceview()==false){	
					imagesLoaded( '#columns', function() {
							  $('#columns').masonry( 'reloadItems' );
				    		  $('#columns').masonry( 'layout' );
				    		  $('#columns > figure').each(function () {
	
				    			  $(this).animate({ opacity: 1 });
				    		  	});
				    		  
				    		  self.searching(false);
				    	});
				}	
				else{ self.searching(false);}
					

						if(moreitems){
							self.next(self.page()+1);
							
						}else{
							self.next(-1);
						}
				}
			});
			console.log(self.term());
		 }
		};


		self.search = function() {
			self.results([]);
			self.mixresults([]);
			self.page(1);
			self.next(1);
			self.previous(0);
			self.currentTerm(self.term());
			self.searching(false);
			self._search();
		};

		self.recordSelect= function (e){
			console.log(e);
			itemShow(e);

		}

		self.searchNext = function() {
		if(self.next()>0){
			self.page(self.next());
			self._search();}
		};

		self.searchPrevious = function() {
			self.page(self.previous());
			self._search();
		};

		self.defaultSource=function(item){
			item.thumb('images/no_image.jpg');

	    }

	  var withsearch = $( '#withsearchid' );
	  var selectedSources = ["YouTube", "Europeana"];
	  var withinput =$("input.withsearch-input");
	  var limit = 3;
	  $(".withsearch-input").devbridgeAutocomplete({
	   		 minChars: 3,
	   		 //lookupLimit: 10,
	   		 //default type GET
	   		 //type: "POST",
	   		 serviceUrl: "/api/autocompleteExt",
	   		 //paramName: default is "query"
	   		 paramName: "term",
	   		 params: {
	   			 source: selectedSources,
	   			 limit: limit
	   		 },
	   		 ajaxSettings: {
	   			 traditional: true,
	   			dataType: "json"
	   		 },
	   		 transformResult: function(response) {
	   			var result = [];
	   			for (var i in response) {
	   				var suggestions  = response[i].suggestions;
	   				$.merge(result, suggestions);
	   			}
	   			return {"suggestions": result};
	   		 },
	   		 //groupBy: "category",
	   		 //width: "600",
	   		 orientation: "auto",
		     onSearchComplete: function(query, suggestions) {
		    	 $(".autocomplete-suggestions").addClass("autocomplete-suggestions-extra");
		    	 $(".autocomplete-suggestion").addClass("autocomplete-suggestion-extra");
		    	 for (var i in suggestions) {
		    		 var category = suggestions[i].data.category;
		    		 var s = $(".autocomplete-suggestion").get(i);
		    		
		    	 }
		    	
		     },
			 formatResult: function(suggestion, currentValue) {
				var s = '<strong>' + currentValue + '</strong>';
				s    += suggestion.value.substring(currentValue.length);
				s    += ' <span class="label pull-right">' + suggestion.data.category + '</span>';

				return s;
			 }

	 });

	  ctrlClose =$("span.withsearch-close");
	  isOpen = false;
		// show/hide search area
      toggleSearch = function(evt,char) {
			// return if open and the input gets focused
			if(  evt === 'focus' && isOpen ) return false;

			if( isOpen ) {
				$('[id^="modal"]').removeClass('md-show').css('display', 'none');
		    	$("#myModal").modal('hide');
		    	$("body").removeClass("modal-open");


				$("body").removeClass("noscroll");
				withsearch.removeClass("open");
				withinput.blur();

			}
			else {
				var isOpera = !!window.opera || navigator.userAgent.indexOf(' OPR/') >= 0;
			    // Opera 8.0+ (UA detection to detect Blink/v8-powered Opera)
			    var isFirefox = typeof InstallTrigger !== 'undefined';   // Firefox 1.0+

			    /* put no scroll to body to eliminate double vertical scrollbars*/
			    $("body").addClass("noscroll");
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
