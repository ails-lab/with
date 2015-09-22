define(['bridget', 'knockout', 'text!./search.html', 'masonry', 'imagesloaded', 'app','inputtags'], function (bridget, ko, template, masonry, imagesLoaded, app,tagsinput) {

	 $.bridget( 'masonry', masonry );
	 var transDuration='0.4s';
	 var isFirefox = typeof InstallTrigger !== 'undefined';   // Firefox 1.0+
	 if(isFirefox){transDuration=0;}

    ko.bindingHandlers.masonry = { init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
    	var $element = $(element);
    	    $element.masonry( {itemSelector: '.masonryitem',gutter:15,isFitWidth: true,transitionDuration:transDuration});

		    ko.utils.domNodeDisposal.addDisposeCallback(element, function() {

		        $element.masonry("destroy");
		    });

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
		self.cachedThumbnail="";
		self.isLike=ko.observable(false);
		
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
		   
					   self.thumb="images/no_image.jpg";
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


		self.load=function(data){
			self.source=data.source;
			self.consoleurl=data.consoleurl;
			self.items.push.apply(self.items, data.items);
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
		var $container = $('#columns');
		var $request;
		self.filterselect=ko.observable(false);
		self.filterselection=ko.observableArray([]);
		self.route = params.route;
		self.term = ko.observable("");
		self.sourceview=ko.observable(false);
		self.sources= ko.observableArray([ "Europeana", "DPLA","DigitalNZ","Mint", "Rijksmuseum"]);
		self.mixresults=ko.observableArray([]);
		
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

		self.toggleSourceview = function () { self.sourceview(!self.sourceview());
		if(self.sourceview()==false){
			$('.withsearch-content').css({'overflow-x': 'hidden'});
			$container.masonry({
			    itemSelector: '.masonryitem',
			    gutter:15,isFitWidth: true,transitionDuration:transDuration
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
						 items.push(record);}
						}
						if(items.length>0){
							 var $newitems=getItems(items);
						     self.mixresults.push.apply(self.mixresults, items);

						     self.masonryImagesReveal( $newitems,$container );

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
						if(srcCat.items().length>0 && (!found || self.results().length==0)){
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
		  if(self.searching()==false && self.currentTerm()!=""){
			  console.log("searching source");
	        	
				self.searching(true);
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
	                    var inCat=null;
	                    var idx=0;
						if(self.results().length>0)
							  for(var k in self.results()){
								inCat=self.results()[k];
								if(inCat.source==sdata.source){
									found=true;
									idx=k;
									break;
								}

							  }
						
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
							
							 }
							}
							if(items.length>0){
								self.results()[idx].items.push.apply(self.results()[idx].items,items);
								
							}
							}
							

						}

						
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
			var selrecord = ko.utils.arrayFirst(self.mixresults(), function(record) {
				   return record.recordId === e;
				});
			itemShow(selrecord);

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

	  self.masonryImagesReveal = function( $items,$container ) {
		  $items.hide();
		  $container.append( $items );
		  if (!($container.data('masonry'))){

				$container.masonry( {itemSelector: '.masonryitem',gutter:15,isFitWidth: true,transitionDuration:transDuration});

			}
		  $items.imagesLoaded().progress( function( imgLoad, image ) {

		    var $item = $( image.img ).parents(".masonryitem" );
		    ko.applyBindings(self, $item[ 0 ] );
		    $item.show();
		    $container.masonry( 'appended', $item, true ).masonry( 'layout', $item );

		  });/*.always(
				  self.searching(false)
				  );
         */

		};


	  
		$(document).on("keypress", ".searchinput", function(e) {
		     if (e.which == 13) {
		         self.search(true,true);
		     }
		});
        
        self.goToTop= function () {
        	if($request!==undefined)$request.abort();
        	verticalOffset = typeof(verticalOffset) != 'undefined' ? verticalOffset : 0;
        	element = $("#withsearchid");
        	offset = element.offset();
        	offsetTop = offset.top;
        	element.animate({scrollTop: offsetTop}, 100, 'linear');
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

        function getItem(record) {
			var figure='<figure class="masonryitem">';
			

			figure+='<span class="star" data-bind="css: { active: '+record.isLike()+'}" id='+record.externalId+'>'+
						'<span class="fa-stack fa-fw" data-bind="event: { click: function() { likeRecord(\'' + record.externalId + '\'); } }">'
						+'<i class="fa fa-heart fa-stack-1x"></i><i class="fa fa-heart-o fa-stack-1x fa-inverse"></i>'
						+'</span></span>'
			+ '<a data-bind="event: { click: function() { recordSelect(\''+record.recordId+'\')}}"><img onError="this.src=\'images/no_image.jpg\'" src="'+record.thumb+'" width="211"/></a><figcaption>'+record.displayTitle()+'</figcaption>'
			+'<div class="sourceCredits"><a href="'+record.view_url+'" target="_new">'+record.sourceCredits()+'</a></figure>';

			return figure;
		}

          function getItems(data) {
        	  var items = '';
        	  for ( i in data) {
        	    items += getItem(data[i]);
        	  }
        	  // return jQuery object
        	  return $( items );
        	}



  }

  return { viewModel: SearchModel, template: template };
});
