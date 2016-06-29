define(['bridget', 'knockout', 'text!./search.html', 'isotope', 'imagesloaded', 'app','inputtags'], function (bridget, ko, template, Isotope, imagesLoaded, app,tagsinput) {

	$.bridget('isotope', Isotope);
	
	$.fn.isotopeImagesReveal = function( $items ) {
		  var iso = this.data('isotope');
		  var itemSelector = iso.options.itemSelector;
		  
		  // append to container
		  this.append( $items );
		// hide by default
		  $items.hide();
		  $items.imagesLoaded().progress( function( imgLoad, image ) {
		    // get item
		    var $item = $( image.img ).parents( itemSelector );
		    // un-hide item
		    $item.show();
		    if(iso)
				  iso.appended($item);
				else{
					$.error("iso gone");
				}
		    
		    
		  });
		  
		  return this;
		};
	 
		function Record(data) {
			var self = this;
			self.annotations = [];
		    self.dbId = "";
			self.title = "";
			self.description="";
			self.thumb = "";
			self.fullres="";
			self.view_url="";
			self.source="";
			self.creator="";
			self.provider="";
			self.dataProvider="";
			self.dataProvider_uri="";
			self.rights="";
			self.mediatype="";
			self.url="";
			self.externalId = "";
			self.likes=0;
			self.collected=0;
			self.collectedIn=[];
			self.data=ko.observable("");
			self.isLike=ko.observable(false);
			self.isLiked = ko.pureComputed(function () {
				return app.isLiked(self.externalId);
			});
			self.isLoaded = ko.observable(false);
			
			self.load = function(data) {
				if(data.title==undefined){
					self.title="No title";
				}else{self.title=data.title;}
				//self.url="#item/"+data.recordId;
				self.view_url=data.view_url;
				self.thumb=data.thumb;
				self.mediatype=data.mediatype;
				self.fullres=data.fullres;
				self.description=data.description;
				self.source=data.source;
				self.creator=data.creator;
				self.provider=data.provider;
				self.dataProvider=data.dataProvider;
				self.dataProvider_uri=data.dataProvider_uri;
				self.rights=data.rights;
				self.dbId=data.dbId;
				self.externalId=data.externalId;
				self.likes=data.likes;
				self.collected=data.collected;
				self.collectedIn=data.collectedIn;
				self.data(data.data);
				var likeval=app.isLiked(self.externalId);
			    self.isLike(likeval);
			    if(!self.thumb){
			    	 self.thumb="img/content/thumb-empty.png";
			    }
			    self.annotations = data.annotations;
			};

			self.doLike=function(){
				self.isLike(true);
			}
			
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
				    case "The British Library":
				    	return "www.bl.uk";
				    case "Mint":
				    	return "mint";
				    case "Rijksmuseum":
						return "www.rijksmuseum.nl";
				    case "DDB":
				        return "deutsche-digitale-bibliothek.de";
				    default: return "";
				 }
				});

			self.displayTitle = ko.pureComputed(function() {
				var distitle="";
				distitle=self.title;
				if(self.creator && self.creator.length>0)
					distitle+=", by "+self.creator;
				if(self.dataProvider && self.dataProvider.length>0 && self.dataProvider!=self.creator)
					distitle+=", "+self.dataProvider;
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
		setTimeout(function(){ WITHApp.init(); }, 300);
		var $container = $("#searchgrid").isotope({
			itemSelector: '.media',
			masonry: {
				columnWidth		: '.sizer',
				percentPosition	: true
			}
		});
		var $request;
		self.filterselect=ko.observable(false);
		self.filterselection=ko.observableArray([]);
		self.route = params.route;
		self.term = ko.observable("");
		self.sourceview=ko.observable(true);
		//self.sources= ko.observableArray([ "Europeana", "WITHin"]);
		self.sources= ko.observableArray([ "WITHinASpace"]);
		self.mixresults=ko.observableArray();
		self.selectedSource=ko.observable(self.sources()[0]);
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
		
		self.columnChanged=function(){
			$(".column").removeClass("selected");
			$("#"+self.selectedSource()+"_result").addClass("selected");
		}  
		
		self.noResults = ko.computed(function() {
			return (!self.searching() && self.results().length == 0 && self.mixresults().length == 0 && self.currentTerm() != "");
		})

		self.toggleSourceview = function (data,event) { 
			if($(event.currentTarget).find("a").attr('data-view')=='column' && self.sourceview()==false){
				self.sourceview(true);
				WITHApp.initSearchColumnAdjustment();	
			}
			else{
				
				self.sourceview(false);
				$("#gridlist").show();
			   $container.isotope({
					itemSelector: '.media',
					masonry: {
						columnWidth		: '.sizer',
						percentPosition	: true
					}
				});
					
			}
			
			$( '.searchbar .view li').removeClass( 'active' );
			$(event.currentTarget).addClass( 'active' );


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
			if ($container.data('isotope')){
				 $container.isotope( 'remove', $(".media"));}
			$container.isotope({
						itemSelector: '.media',
						masonry: {
							columnWidth		: '.sizer',
							percentPosition	: true
						}
					});
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
                  
                    var data=reply.responses;
                    var facets=[];
                    for(var f in reply.filters){
                    	facet=reply.filters[f];
                    	facet.filterID=facet.filterID.replace(/\./g, '_');
			            facets.push(facet);
                    }
                    if(facetinit || facetrecacl){
	                    self.filters.removeAll();
	                    
	                    self.filters().push.apply(self.filters(),facets);}
                    
                    
                 	for(var i in data) {
						source=data[i].source;
						//count should be working in api but it's not, use item length until fixed
						if(data[i].items!=null && data[i].items.culturalCHO!=null && data[i].items.culturalCHO.length==self.pageSize() && moreitems==false){
							moreitems=true;
						}
						var items = [];
						
						items=self.revealItems(data[i].items.culturalCHO);
						
						
						if(items.length>0){
							 var $newitems=getItems(items);
						     $container.isotope({
									itemSelector: '.media',
									masonry: {
										columnWidth		: '.sizer',
										percentPosition	: true
									}
								});
							 $container.isotopeImagesReveal( $newitems );

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
							if(self.sourceview())
							WITHApp.initSearchColumnAdjustment();
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
					self.sourceview(false);
					$("#gridlist").show();
				   $container.isotope({
						itemSelector: '.media',
						masonry: {
							columnWidth		: '.sizer',
							percentPosition	: true
						}
					});
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
						var data=reply.responses;
	                    for(var i in data) {
						  source=data[i].source;
						  
					      //count should be working in api but it's not, use item length until fixed
						  if(data[i].items!=null && data[i].items.culturalCHO!=null && source==sdata.source){
							var items = [];
							for(var j in data[i].items.culturalCHO){
							 var result = data[i].items.culturalCHO[j];
							 if(result !=null ){
								var admindata=result.administrative;
								var descdata=result.descriptiveData;
								var media=result.media;
								var provenance=result.provenance;
								var usage=result.usage;
								var rights=null;
								if(media){
								 if(media[0].Original){
									 rights=findResOrLit(media[0].Original.originalRights);
								 }else if(media[0].Thumbnail){
									 rights=findResOrLit(media[0].Thumbnail.originalRights);
								 }}
								
								var source=findProvenanceValues(provenance,"source");
								
								if(source=="Rijksmuseum" && media){
									media[0].Thumbnail=media[0].Original;
								}
								var mediatype="";
								if(media &&  media[0]){
									if(media[0].Original && media[0].Original.type){
										mediatype=media[0].Original.type;
									}else if(media[0].Thumbnail && media[0].Thumbnail.type){
										mediatype=media[0].Thumbnail.type;
									}
								}
						        var record = new Record({
									//recordId: result.recordId || result.id,
									thumb: media!=null &&  media[0] !=null  && media[0].Thumbnail!=null  && media[0].Thumbnail.url!="null" ? media[0].Thumbnail.url:"img/content/thumb-empty.png",
									fullres: media!=null &&  media[0] !=null && media[0].Original!=null  && media[0].Original.url!="null"  ? media[0].Original.url : "",
									title: findByLang(descdata.label),
									description: findByLang(descdata.description),
									view_url: findProvenanceValues(provenance,"source_uri"),
									creator: findByLang(descdata.dccreator),
									dataProvider: findProvenanceValues(provenance,"dataProvider"),
									dataProvider_uri: findProvenanceValues(provenance,"dataProvider_uri"),
									provider: findProvenanceValues(provenance,"provider"),
									rights: rights,
									mediatype: mediatype,
									externalId: admindata.externalId,
									source: source,
									likes: usage.likes,
									collected: usage.collected,
									collectedIn:result.collectedIn,
									data: result,
									annotations: result.annotations,
									dbId: result.dbId
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
			if ($container.data('isotope')){
				 $container.isotope( 'remove', $(".media"));}
				
			$container.isotope({
						itemSelector: '.media',
						masonry: {
							columnWidth		: '.sizer',
							percentPosition	: true
						}
					});

				
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
			if ($container.data('isotope')){
				 $container.isotope( 'remove', $(".media"));
			}	
			
			$container.isotope({
						itemSelector: '.media',
						masonry: {
							columnWidth		: '.sizer',
							percentPosition	: true
						}
					});
			self.page(1);
			self.next(1);
			self.previous(0);
			self.currentTerm(self.term());
			self.searching(false);
			
			self.filterselect(false);
			self._search(facetinit,facetrecacl);
			
		};

		
        self.columnRecordSelect= function (e){
        	$( '.itemview' ).fadeIn();
			itemShow(e);
			return false;
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
					var admindata=result.administrative;
					var descdata=result.descriptiveData;
					var media=result.media;
					var provenance=result.provenance;
					var usage=result.usage;
					var rights=null;
					if(media){
						
						 if(media[0].Original){
							 rights=findResOrLit(media[0].Original.originalRights);
						 }else{
							 rights=findResOrLit(media[0].Thumbnail.originalRights);
						 }
					}

					var source=findProvenanceValues(provenance,"source");
					
					if(source=="Rijksmuseum" && media){
						media[0].Thumbnail=media[0].Original;
					}
					var mediatype="";
					if(media &&  media[0]){
						if(media[0].Original && media[0].Original.type){
							mediatype=media[0].Original.type;
						}else if(media[0].Thumbnail && media[0].Thumbnail.type){
							mediatype=media[0].Thumbnail.type;
						}
					}
			        var record = new Record({
						thumb: media!=null &&  media[0] !=null  && media[0].Thumbnail!=null  && media[0].Thumbnail.url!="null" ? media[0].Thumbnail.url:"img/content/thumb-empty.png",
						fullres: media!=null &&  media[0] !=null && media[0].Original!=null  && media[0].Original.url!="null"  ? media[0].Original.url : "",
						title: findByLang(descdata.label),
						description: findByLang(descdata.description),
						view_url: findProvenanceValues(provenance,"source_uri"),
						creator: findByLang(descdata.dccreator),
						dataProvider: findProvenanceValues(provenance,"dataProvider"),
						dataProvider_uri: findProvenanceValues(provenance,"dataProvider_uri"),
						provider: findProvenanceValues(provenance,"provider"),
						rights: rights,
						mediatype: mediatype,
						externalId: admindata.externalId,
						source: source,
						likes: usage.likes,
						collected: usage.collected,
						collectedIn:result.collectedIn,
						annotations: result.annotations,
						data: result,
						dbId: result.dbId
					  });
				  items.push(record);
				 }
			}
			self.mixresults.push.apply(self.mixresults, items);
			return items;
		};
		
	
	  
        self.goToTop= function () {
        	if($request!==undefined)$request.abort();
        	
        	$("html, body").animate({
                scrollTop: 0
            }, 600);
        }

        recordSelect = function (data,event) {
        	event.preventDefault();
			var selrecord = ko.utils.arrayFirst(self.mixresults(), function(record) {
				   return record.externalId === data;
				});
			itemShow(selrecord);
			return false;

		}
        
        likeRecord = function (id,event) {
        	event.preventDefault();
			var rec = ko.utils.arrayFirst(self.mixresults(), function (record) {
				return record.externalId=== id;
			});

			app.likeItem(rec, function (status) {
				if (status) {
					$( '[id="'+id+'"]' ).find("span.star").addClass('active');
				} else {
					$( '[id="'+id+'"]').find("span.star").removeClass('active');
				}
			});
		};
		
		collect = function (id,event) {
			event.preventDefault();
			var rec = ko.utils.arrayFirst(self.mixresults(), function (record) {
				return record.externalId=== id;
			});
			
			collectionShow(rec);
		};

 
        function getItem(record) {
			 var tile= '<div class="item media" id="'+record.externalId+'"> <div class="wrap">';
			 if(isLogged()){
				    if(record.isLike()){
				    	 tile+='<span class="star active">';
				    }
				    else{tile+='<span class="star">';}
				    if(record.externalId){
				    tile+='<span class="fa-stack fa-fw" onclick="likeRecord(\'' + record.externalId + '\',event);" title="add to favorites">'
						+'<i class="fa fa-heart fa-stack-1x"></i><i class="fa fa-heart-o fa-stack-1x fa-inverse"></i>'
						+'</span>';}
						tile+='<span class="collect" title="collect" onclick="collect(\'' + record.externalId + '\',event)"><i class="fa fa-download fa-stack-1x fa-inverse"></i></span></span>';
					}
			 else{
				 tile+='<span class="star" style="display:none">'
				 if(record.externalId){
					 tile+='<span class="fa-stack fa-fw" onclick="likeRecord(\'' + record.externalId + '\',event);" title="add to favorites">'
					    +'<i class="fa fa-heart fa-stack-1x"></i><i class="fa fa-heart-o fa-stack-1x fa-inverse"></i>'
					    +'</span>';}
					tile+='<span class="collect" title="collect" onclick="collect(\'' + record.externalId + '\',event)" style="display:none"><i class="fa fa-download fa-stack-1x fa-inverse"></i></span></span>';
					
			 }
                     tile+='<a href="#" onclick="recordSelect(\''+record.externalId+'\',event)">'
                      +'<div class="thumb"><img src="'+record.thumb+'" onError="this.src=\'img/content/thumb-empty.png\'"></div>'
                      +' <div class="info"><h1 class="title">'+record.displayTitle()+'</h1><span class="owner">'+ record.dataProvider+'</span></div>'
                      +'<span class="rights">'+record.sourceCredits()+'</span>'
                     +'</a></div> </div>';
			return tile;
			
		}
		
        function getItems(data) {
      	  var items = '';
      	  for ( i in data) {
      	    items += getItem(data[i]);
      	  }
      	  return $( items );
      	}
      
        $("#withsearchid").keyup(function(event){
            if(event.keyCode == 13){
            	self.search(true,true);
            }
        });
      
        
        /*take care of special characters in id of item*/
        function jq( myid ) {
        	 
            myid=myid.replace("\\g", "/");
        	return "#" + myid.replace( /(:|\.|\[|\]|,)/g, "\\$1" );
         
        }


  }

  return { viewModel: SearchModel, template: template };
});
