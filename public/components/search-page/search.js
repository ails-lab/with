define(['bridget', 'knockout', 'text!./search.html', 'masonry', 'imagesloaded', 'app'], function (bridget, ko, template, masonry, imagesLoaded, app) {

	 $.bridget( 'masonry', masonry );
	 var transDuration='0.4s';
	 var isFirefox = typeof InstallTrigger !== 'undefined';   // Firefox 1.0+
	 if(isFirefox){transDuration=0;}
	 var $container = $('#columns');




	 ko.bindingHandlers.masonry = { init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
	    	var $element = $(element);
	    	    $element.masonry( {itemSelector: '.masonryitem',gutter:15,isFitWidth: true,transitionDuration:transDuration});

			    ko.utils.domNodeDisposal.addDisposeCallback(element, function() {

			        $element.masonry("destroy");
			    });

	    }
	    };


    ko.bindingHandlers.foreachmasonry = { init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
    	 return ko.bindingHandlers.foreach.init(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext);

    },
    update: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {

        var $el = $(element);
        var value = ko.utils.unwrapObservable(valueAccessor());

        var key = "masonry_added";

        var newValue = function () {
            return {
                data: value,
                afterAdd: function (ele, index, data) {
                    if (ko.utils.domData.get(element, key)) {

                    	if (ele.nodeType === 1) { // Element type
                    		//$(ele).css('opacity','0');
                    		$(ele).hide();


                    		$(element).imagesLoaded(function () {

                    			if (!($(element).data('masonry'))){
        							$(element).masonry({
        							    itemSelector: '.masonryitem',
        							    gutter:15,isFitWidth: true,transitionDuration:transDuration
        							  });
        						}
                    			$(element).masonry('appended', $(ele)).masonry( 'layout');
        						$(ele).fadeIn();

        					});
        				}


                   }
                }
            };
        };



        ko.bindingHandlers.foreach.update(element, newValue, allBindingsAccessor, viewModel, bindingContext);

        //if we have not previously marked this as initialized and there are currently items in the array, then cache on the element that it has been initialized
       if (!ko.utils.domData.get(element, key) && value.length) {
            ko.utils.domData.set(element, key, true);
        }

        return { controlsDescendantBindings: true };
    }
    };



	function Record(data) {
		var self = this;
	    self.recordId = "";
		self.title = "";
		self.description="";
		self.thumb = "";
		self.fullres="";
		self.view_url="";
		self.source="";
		self.creator="";
		self.provider="";
		self.rights="";
		self.url="";
		self.externalId = "";
		self.isLiked = ko.pureComputed(function () {
			return app.isLiked(self.externalId);
		});
		self.load = function(data) {
			if(data.title==undefined){
				self.title="No title";
			}else{self.title=data.title;}
			self.url="#item/"+data.recordId;
			self.view_url=data.view_url;
			self.thumb=data.thumb;
			//self.imageThumb(data.thumb);
			self.fullres=data.fullres;
			self.description=data.description;
			self.source=data.source;
			self.creator=data.creator;
			self.provider=data.provider;
			self.rights=data.rights;
			self.recordId=data.recordId;
			self.externalId=data.externalId;
		};

		self.cachedThumbnail = ko.pureComputed(function() {
			
			   if(self.thumb){
				if (self.thumb.indexOf('/') === 0) {
					return self.thumb;
				} else {
					var newurl='url=' + encodeURIComponent(self.thumb)+'&';
					return '/cache/byUrl?'+newurl+'Xauth2='+ sign(newurl);
				}}
			   else{
				   return "images/no_image.jpg";
			   }
			});
		self.sourceCredits = ko.pureComputed(function() {
			 switch(self.source) {
			    case "DPLA":
			    	return "dp.la";
			    case "Europeana":
			    	return "europeana.eu";
			    case "NLA":
			    	return "nla.gov.au";
			    case "DigitalNZ":
			    	return "digitalnz.org";
			    case "EFashion":
			    	return "europeanafashion.eu";
			    case "YouTube": 
			    	return "youtube.com";
			    case "Mint":
			    	return "mint";
			    case "Rijksmuseum":
					return "www.rijksmuseum.nl";
			    default: return "";
			 }
			});

		self.displayTitle = ko.pureComputed(function() {
			var distitle="";
			distitle=self.title;
			if(self.creator!==undefined && self.creator.length>0)
				distitle+=", by "+self.creator;
			if(self.provider!==undefined && self.provider.length>0 && self.provider!=self.creator)
				distitle+=", "+self.provider;
			return distitle;
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
		var $request;
		self.filterselect=ko.observable(false);
		self.filterselection=ko.observableArray([]);

		self.route = params.route;
		self.term = ko.observable("");
		self.sourceview=ko.observable(false);
		self.sources= ko.observableArray([ "Europeana", "DPLA","DigitalNZ","Mint", "Rijksmuseum", "DDB", "EFashion"]);
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
	    self.filters=ko.observableArray([]);

		self.noResults = ko.computed(function() {
			return (!self.searching() && self.results().length == 0 && self.currentTerm() != "");
		})

		self.toggleSourceview = function () { self.sourceview(!self.sourceview());
		if(self.sourceview()==false){
			$('.withsearch-content').css({'overflow-x': 'hidden'});
			$container.imagesLoaded(function () {


    				$container.masonry({
					    itemSelector: '.masonryitem',
					    gutter:15,isFitWidth: true
					  });
    				$container.masonry( 'layout' );

			});

		  }
		else{
			$('.withsearch-content').css({'overflow-x': 'auto'});
		  }

		};

		self.reset = function() {
			if($request!==undefined)$request.abort();
			self.term("");
			self.currentTerm = ko.observable("");
			self.page(1);
			self.pageSize(20);
			self.previous(-1);
			self.next(-1);
			self.mixresults([]);
			self.results([]);
			self.searching(false);
			ko.dataFor(searchfacets).initFacets();
			if ($container.data('masonry')){
			 $container.masonry( 'remove', $container.find('.masonryitem') );
			 }
		}

		self._search = function(facetinit,facetrecacl) {
         if(facetinit){self.filterselection.removeAll();}
		 $(".withsearch-input").devbridgeAutocomplete("hide");
		 self.currentTerm($(".withsearch-input").val());
		 if(self.searching()==false && self.currentTerm()!=""){
			self.searching(true);
			$request=$.ajax({
				"url": "/api/advancedsearch",
				"method": "post",
				"contentType": "application/json",
				"data": JSON.stringify({
					searchTerm: self.currentTerm(),
					page: self.page(),
					pageSize:self.pageSize(),
				    source:self.sources(),
				    filters:self.filterselection()
				}),
				"success": function(reply) {

					self.previous(self.page()-1);
					var moreitems=false;
                    var data=reply.responces;

					var filters=reply.filters;
                    if(facetinit || facetrecacl){
                    self.filters.removeAll();
                    self.filters().push.apply(self.filters(),filters);}

					for(var i in data) {
						source=data[i].source;
						//count should be working in api but it's not, use item length until fixed
						if(data[i].items!=null && data[i].items.length==self.pageSize() && moreitems==false){
							moreitems=true;
						}
						var items = [];
						for(var j in data[i].items){
						 var result = data[i].items[j];

						 if(result !=null ){
						 var record = new Record({
							recordId: result.recordId || result.id,
							thumb: result.thumb!=null && result.thumb[0]!=null  && result.thumb[0]!="null" ? result.thumb[0]:"",
							fullres: result.fullresolution,
							title: result.title!=null && result.title[0]!=null? result.title[0].value:"",
							view_url: result.url.fromSourceAPI,
							creator: result.creator!==undefined && result.creator!==null && result.creator[0]!==undefined? result.creator[0].value : "",
							provider: result.dataProvider!=undefined && result.dataProvider!==null && result.dataProvider[0]!==undefined? result.dataProvider[0].value : "",
							rights: result.rights!==undefined && result.rights!==null && result.rights[0]!==undefined? result.rights[0].value : "",
							externalId: result.externalId,
							source: source
						  });
						  items.push(record);

						  self.mixresults.push(record);
						 }
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
								self.results.splice(k,1,new SourceCategory({
									source:inCat.source,
									items:inCat.items,
									consoleurl:inCat.consoleurl
								}))
								break;
							}

						  }
						if(srcCat.items.length>0 && (!found || self.results().length==0)){
							self.results.push(srcCat);
						}

					}
			    if(moreitems){
							self.next(self.page()+1);
						}else{
							self.next(-1);
						}
				},
				"complete":function(reply){
					 self.searching(false);

					if(facetinit)
					  ko.dataFor(searchfacets).initFacets();
					else if(facetrecacl){
						ko.dataFor(searchfacets).recalcFacets();
					}

				}
			});

		 }
		};


		 self.filtersearch = function() {
    	  if($request!==undefined)$request.abort();
    	   self.filterselect(true);

			self.results.removeAll();
			self.mixresults.removeAll();
			self.page(1);
			self.next(1);
			self.previous(0);
			self.currentTerm(self.term());
			self.searching(false);
			if ($container.data('masonry')){
		  	     $container.masonry( 'remove', $container.find('.masonryitem') );
			}else{
				$container.masonry( {itemSelector: '.masonryitem',gutter:15,isFitWidth: true,transitionDuration:transDuration});

			}

			self._search(false,true);



		};

		self.search = function(facetinit,facetrecacl) {
			if($request!==undefined)$request.abort();

			self.results.removeAll();
			self.mixresults.removeAll();
			self.page(1);
			self.next(1);
			self.previous(0);
			self.currentTerm(self.term());
			self.searching(false);
			if ($container.data('masonry')){
		  	     $container.masonry( 'remove', $container.find('.masonryitem') );
			}else{
				$container.masonry( {itemSelector: '.masonryitem',gutter:15,isFitWidth: true,transitionDuration:transDuration});

			}
			self.filterselect(false);

			self._search(facetinit,facetrecacl);

		};

		self.recordSelect= function (e){

			itemShow(e);

		}

        self.columnRecordSelect= function (e){

			itemShow(e);

		}


		self.searchNext = function() {
		if(self.next()>0){
			self.page(self.next());
			self._search(false,false);}
		};

		self.searchPrevious = function() {
			self.page(self.previous());
			self._search(false,false);
		};
		self.defaultSource=function(item){
			item.thumb='//placehold.it/300?text=image+missing';

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
				self.dispose();

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

       self.likeRecord = function (rec) {
			app.likeItem(rec, function (status) {
				if (status) {
					// $('#' + id).addClass('active');
				} else {
					// $('#' + id).removeClass('active');
				}
			});
		};


  }


	SearchModel.prototype.dispose = function() {
		console.log("dispose function called");

	}

  return { viewModel: SearchModel, template: template };
});
