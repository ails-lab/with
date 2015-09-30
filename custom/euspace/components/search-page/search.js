define(['bridget', 'knockout', 'text!./search.html', 'isotope', 'imagesloaded', 'app','inputtags'], function (bridget, ko, template, Isotope, imagesLoaded, app,tagsinput) {

	$.bridget('isotope', Isotope);
	
	 function initOrUpdate(method) {
			return function (element, valueAccessor, allBindings, viewModel, bindingContext) {
				function isotopeAppend(ele) {
					if (ele.nodeType === 1) { // Element type
						$(element).imagesLoaded(function () {
							$(element).isotope('appended', ele).isotope('layout');
						});
					}
				}

				function attachCallback(valueAccessor) {
					return function() {
						return {
							data: valueAccessor(),
							afterAdd: isotopeAppend,
						};
					};
				}

				var data = ko.utils.unwrapObservable(valueAccessor());
				//extend foreach binding
				ko.bindingHandlers.foreach[method](element,
					 attachCallback(valueAccessor), // attach 'afterAdd' callback
					 allBindings, viewModel, bindingContext);

				if (method === 'init') {
					$(element).isotope({
						itemSelector: '.media',
						transitionDuration: transDuration,
						masonry: {
							columnWidth		: '.sizer',
							percentPosition	: true
						}
					});

					ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
						$(element).isotope("destroy");
					});
					
				} else {
					 console.log("updating isotope...");
				}
			};
		}
		
		ko.bindingHandlers.scroll = {
				updating: true,

				init: function (element, valueAccessor, allBindingsAccessor) {
					var self = this;
					self.updating = true;
					ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
						$(window).off("scroll.ko.scrollHandler");
						self.updating = false;
					});
				},

				update: function (element, valueAccessor, allBindingsAccessor) {
					var props = allBindingsAccessor().scrollOptions;
					var offset = props.offset ? props.offset : "0";
					var loadFunc = props.loadFunc;
					var load = ko.utils.unwrapObservable(valueAccessor());
					var self = this;

					if (load) {
						$(window).on("scroll.ko.scrollHandler", function () {
							if ($(window).scrollTop() >= $(document).height() - $(window).height() - 300) {
								if (self.updating) {
									loadFunc();
									self.updating = false;
								}
							} else {
								self.updating = true;
							}
							 if ($(window).scrollTop() > 100) {
									$('.scroll-top-wrapper').addClass('show');
								} else {
									$('.scroll-top-wrapper').removeClass('show');
								}
						});
					} else {
						element.style.display = "none";
						$(window).off("scroll.ko.scrollHandler");
						self.updating = false;
					}
				}
			};


		ko.bindingHandlers.searchIsotope = {
				init: initOrUpdate('init'),
				update: initOrUpdate('update')
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
		self.cachedThumbnail="";
		self.isLike=ko.observable(false);
		 self.isLoaded = ko.observable(false);
		 
		
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
			
		   if(!self.thumb){
		   
					   self.thumb="img/content/thumb-empty.png";
			 }
		};

		
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
		self.items=ko.observableArray();

		self.loading=ko.observable(true);
		self.load=function(data){
			self.source=data.source;
			self.consoleurl=data.consoleurl;
			self.items.push.apply(self.items, data.items);
			self.loading(false);
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
		document.body.setAttribute("data-page","search");	
		var $container = $('.grid');
		var $request;
		self.filterselect=ko.observable(false);
		self.filterselection=ko.observableArray([]);
		self.route = params.route;
		self.term = ko.observable("");
		self.sourceview=ko.observable(true);
		self.sources= ko.observableArray([ "Europeana", "DPLA","DigitalNZ","WITHin", "Rijksmuseum"]);
		self.mixresults=ko.observableArray();
		
		self.results = ko.observableArray([]);
		self.selectedRecord=ko.observable(false);
		//self.results.extend({ rateLimit: 50 });
		self.searching = ko.observable(false);
		self.scrolled= function(data, event) {
	        var elem = event.target;
	        if (elem.scrollTop > (elem.scrollHeight - elem.offsetHeight-100)) {
	        	self.searchSource(data);
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

		self.toggleSourceview = function (data,event) { 
			if($(event.currentTarget).find("a").attr('data-view')=='column'){
				self.sourceview(true);
				window.EUSpaceUI.initSearch();
				
			}
			else{
				
				self.sourceview(false);
				$container.isotope({
					itemSelector: '.media',
					transitionDuration: transDuration,
					masonry: {
						columnWidth		: '.sizer',
						percentPosition	: true
					}
				  });
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
			
		}

		self._search = function(facetinit,facetrecacl) {
		 if(facetinit){self.filterselection.removeAll();}
		 $('#facet_tags').tagsinput({
		        allowDuplicates: false,
		          itemValue: 'id',  // this will be used to set id of tag
		          itemText: 'label' // this will be used to set text of tag
		      });
		 $(".searchinput").devbridgeAutocomplete("hide");
		 self.currentTerm($(".searchinput").val());
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
						items=self.revealItems(data[i].items);
						
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
						var idx=k;
						if(self.results().length>0)
						  for(var k in self.results()){
							var inCat=self.results()[k];
							idx=k;
							if(inCat.source==srcCat.source){
								found=true;
								self.results()[idx].items.push.apply(self.results()[idx].items,items);
								self.results()[idx].loading(false);
								
								break;
							}

						  }
						if(srcCat.items().length>0 && (!found || self.results().length==0)){
							srcCat.loading(false);
							self.results.push(srcCat);
						}

					}
					window.EUSpaceUI.initSearch();
					if(moreitems){
						self.next(self.page()+1);

					}else{
						self.next(-1);
					}
				},
				"complete":function(reply){
					 self.searching(false);
					if(self.next()!=-1){
						if(facetinit)
						  ko.dataFor(searchfacets).initFacets();
						else if(facetrecacl){
							ko.dataFor(searchfacets).recalcFacets();
						}
					}
					
				}
			});

		 }
		};

		
	  self.searchSource = function(sdata){
		  if(self.searching()==false && self.currentTerm()!="" && Math.floor(sdata.items().length/self.pageSize())+1!=1){
		    	
				self.searching(true);
				 var inCat=null;
                 var idx=0;
					if(self.results().length>0)
						  for(var k in self.results()){
							inCat=self.results()[k];
							if(inCat.source==sdata.source){
								found=true;
								inCat.loading(true);
								idx=k;
								break;
							}

						  }
				$request=$.ajax({
					"url": "/api/advancedsearch",
					"method": "post",
					"contentType": "application/json",
					"data": JSON.stringify({
						searchTerm: self.currentTerm(),
						page: Math.floor(sdata.items().length/self.pageSize())+1,
						pageSize:self.pageSize(),
					    source:[sdata.source],
					    filters:self.filterselection()
					}),
					"success": function(reply) {
						var data=reply.responces;
	                   
						
	                    for(var i in data) {
						  source=data[i].source;
							//count should be working in api but it's not, use item length until fixed
						  if(data[i].items!=null && source==sdata.source){
							var items = [];
							for(var j in data[i].items){
							 var result = data[i].items[j];

							 if(result !=null ){
								 //&& result.title[0]!=null && result.title[0].value!="[Untitled]" && result.thumb!=null && result.thumb[0]!=null  && result.thumb[0]!="null" && result.thumb[0]!=""){
							 var record = new Record({
								recordId: result.recordId || result.id,
								thumb: result.thumb!=null && result.thumb[0]!=null  && result.thumb[0]!="null" ? result.thumb[0]:"",
								fullres: result.fullresolution!=null ? result.fullresolution : "",
								title: result.title!=null? result.title:"",
								view_url: result.url.fromSourceAPI,
								creator: result.creator!==undefined && result.creator!==null? result.creator : "",
								provider: result.dataProvider!=undefined && result.dataProvider!==null ? result.dataProvider: "",
								rights: result.rights!==undefined && result.rights!==null ? result.rights : "",
								externalId: result.externalId,
								source: source
							  });
							   items.push(record);
							   self.mixresults().push(record);
							
							 }
							}
							if(items.length>0){
								self.results()[idx].items.push.apply(self.results()[idx].items,items);
								self.results()[idx].loading(false);
								self.mixresults.valueHasMutated();
								
							}
							}
							

						}
	                    inCat.loading(false);

						
					},
					"complete":function(reply){
						 self.searching(false);
						
						
					}
				});

			 }
	  }	
		

      self.filtersearch = function() {
    	  if($request!==undefined)$request.abort();
    	   self.filterselect(true);

			self.results.removeAll();
			self.mixresults.removeAll();
			self.page(1);
			self.next(-1);
			self.previous(0);
			self.currentTerm(self.term());
			self.searching(false);
			
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
			
			self.filterselect(false);
			self._search(facetinit,facetrecacl);
			
		};

		self.recordSelect= function (e){
			var selrecord = ko.utils.arrayFirst(self.mixresults(), function(record) {
				   return record.recordId === e;
				});
			$( '.itemview' ).fadeIn();
			itemShow(selrecord);

		}

        self.columnRecordSelect= function (e){
        	$( '.itemview' ).fadeIn();
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
			item.thumb='images/no_image.jpg';
	     }

	  var withsearch = $( '#withsearchid' );
	  var selectedSources = ["YouTube", "Europeana"];
	  var withinput =$("input.searchinput");
	  var limit = 3;
	  $(".searchinput").devbridgeAutocomplete({
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
		    	 if(self.searching()){ $(".searchinput").devbridgeAutocomplete("hide");}
		    	 else{
			    	 $(".autocomplete-suggestions").addClass("autocomplete-suggestions-extra");
			    	 $(".autocomplete-suggestion").addClass("autocomplete-suggestion-extra");
			    	 for (var i in suggestions) {
			    		 var category = suggestions[i].data.category;
			    		 var s = $(".autocomplete-suggestion").get(i);
			    	 }
		    	 }
		     },
			 formatResult: function(suggestion, currentValue) {
				var s = '<strong>' + currentValue + '</strong>';
				s    += suggestion.value.substring(currentValue.length);
				s    += ' <span class="label pull-right">' + suggestion.data.category + '</span>';
				return s;
			 }
	 });

	 

		self.revealItems = function (data) {
			var items=[];
			for (var i in data) {
				var result = data[i];
				 if(result !=null ){
						
			        var record = new Record({
						recordId: result.recordId || result.id,
						thumb: result.thumb!=null && result.thumb[0]!=null  && result.thumb[0]!="null" ? result.thumb[0]:"",
						fullres: result.fullresolution!=null ? result.fullresolution : "",
						title: result.title!=null? result.title:"",
						view_url: result.url.fromSourceAPI,
						creator: result.creator!==undefined && result.creator!==null? result.creator : "",
						provider: result.dataProvider!=undefined && result.dataProvider!==null ? result.dataProvider: "",
						rights: result.rights!==undefined && result.rights!==null ? result.rights : "",
						externalId: result.externalId,
						source: source
					  });
				  items.push(record);
				 self.mixresults().push(record);}
			}
			self.mixresults.valueHasMutated();
			return items;
		};
		
	
	  
		$(document).on("keypress", ".searchinput", function(e) {
		     if (e.which == 13) {
		         self.search(true,true);
		     }
		});
        
        self.goToTop= function () {
        	if($request!==undefined)$request.abort();
        	
        	$("html, body").animate({
                scrollTop: 0
            }, 600);
        }

		self.likeRecord = function (id) {
			var rec = ko.utils.arrayFirst(self.mixresults(), function (record) {
				return record.externalId=== id;
			});

			app.likeItem(rec, function (status) {
				if (status) {
					$('#' + id).addClass('active');
				} else {
					$('#' + id).removeClass('active');
				}
			});
		};

      


  }

  return { viewModel: SearchModel, template: template };
});
