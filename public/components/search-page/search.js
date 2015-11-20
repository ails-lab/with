define(['bridget', 'knockout', 'text!./search.html', 'isotope', 'imagesloaded', 'app'], function (bridget, ko, template, Isotope, imagesLoaded, app) {

	 $.bridget('isotope', Isotope);
	
	 
	ko.bindingHandlers.searchIsotope = {
				init: app.initOrUpdate('init'),
				update: app.initOrUpdate('update')
			};



	function Record(data) {
		var mapping = {
				'externalId': {
					key: function(data) {
			            return ko.utils.unwrapObservable(data.extrnalId);
			        }
				},
			    'include': ["title", "thumbnailUrl"],
				'observe': ["title"],
				'thumbnailUrl': {
			        create: function(options) {
			        	setDefaultValue(options.data.thumbnailUrl, "images/no_image.jpg");
			        }
			    }
		};
		
		setDefaultValue =  function(fieldValue, defaultValue)  {
			if (fieldValue == null || fieldValue == undefined || fieldValue == "")
				return defaultValue;
			else 
				return fieldValue;
		};
		
		self.recordData = ko.mapping.fromJS({title: "No title", "thumbnailUrl": "images/no_image.jpg", "rights":"", "provider":"",
			"creator":"", "importedFrom":""}, {});
		
		self.isLike = ko.observable(false);

		self.load = function(data) {
			ko.mapping.fromJS(data, mapping, self.recordData);
			var likeval=app.isLiked(self.recordData.externalId);
			self.isLike(likeval);
			/*recordId: result.recordId || result.id,
			thumb: result.thumb!=null && result.thumb[0]!=null  && result.thumb[0]!="null" ? result.thumb[0]:"",
			fullres: result.fullresolution,
			title: result.title!=null ? result.title : "",
			view_url: result.url.fromSourceAPI,
			description: result.description,
			creator: result.creator!==undefined && result.creator!==null? result.creator : "",
			provider: result.dataProvider!=undefined && result.dataProvider!==null ? result.dataProvider: "",
			rights: result.rights!==undefined && result.rights!==null ? result.rights : "",
			externalId: result.externalId,
			source: result.comesFrom!=null ? result.comesFrom : source*/
		};

		self.doLike=function(){
			isLike(true);
		}
		
		self.sourceCredits = ko.pureComputed(function() {
			switch(self.recordData.importedFrom.providerName) {
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
			distitle=self.recordData.title;
			var creator = self.recordData.creator;
			if (creator!==undefined && creator.length>0)
				distitle+=", by "+ creator;
			var provider = self.recordData.provenanceChain[0];
			if(provider==undefined && provider.length>0 && provider!=creator)
				distitle+=", "+ provider;
			return distitle;
		});
		
		if(data != undefined) 
			self.load(data);
	}

	function SourceCategory(data) {
		var self = this;
		self.source = ko.observable("");
		self.consoleurl=ko.observable("");
		self.items=ko.observableArray([]);

		self.load=function(data){
			self.source=data.source;
			self.items=data.items;
			if(source=="Europeana"){
				self.consoleurl="http://labs.europeana.eu/api/console/?function=search&query="+self.term();
				}
			else if(source=="DPLA"){
				self.consoleurl="http://api.dp.la/";
			}
			else if(source=="NLA"){
				self.consoleurl="http://api.trove.nla.gov.au/";
			}
			else if(source=="DigitalNZ"){
				self.consoleurl="http://api.digitalnz.org/"
			}
			else if (source=="EFashion"){
				self.consoleurl="http://www.europeanafashion.eu/api/search/"+self.term();
			}
			else if (source=="Rijksmuseum") {
				self.consoleurl="https://www.rijksmuseum.nl/en/api";
			}
			else
				self.consoleurl="";
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
		self.sources= ko.observableArray([ "Europeana", "DPLA","DigitalNZ", "Rijksmuseum", "WITHin", "The British Library"]);
		self.mixresults=ko.observableArray([]);
		
		self.results = ko.observableArray([]);
		self.selectedRecord=ko.observable(false);
		//self.results.extend({ rateLimit: 50 });
		self.searching = ko.observable(false);
		self.scrolled= function(data, event) {
	        var elem = event.target;
	       /* if (elem.scrollTop > (elem.scrollHeight - elem.offsetHeight)) {
	        	console.log("searching next");
	        	self.searchNext();
	        }*/
	        if($(elem).scrollTop()+ $(elem).innerHeight()>=$(elem)[0].scrollHeight){
					self.searchNext();
				}
	        if ($(elem).scrollTop() > 100) {
				$('.scroll-top-wrapper').addClass('show');
			} else {
				$('.scroll-top-wrapper').removeClass('show');
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
			$container.isotope({
				itemSelector: '.item',
				transitionDuration: transDuration,
				masonry: {
					columnWidth		: '.sizer',
					percentPosition	: true
				}
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
			/*if ($container.data('isotope')){
			 $container.isotope( 'remove', $container.find('.item') );
			 }*/
		}
		app.getEditableCollections();//TODO: update the storages for the collect it select- fix properly
		self._search = function(facetinit,facetrecacl) {
		 if(facetinit){self.filterselection.removeAll();}
		 $(".withsearch-input").devbridgeAutocomplete("hide");
		 self.currentTerm($(".withsearch-input").val());
		 var directlyAccessedByGroupName = [{"groupName":"a", "rights": "OWN"}];
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
								var record = new Record(result);
								items.push(record);}
						}
						if(items.length>0){
							 var $newitems=getItems(items);
						     self.mixresults.push.apply(self.mixresults, items);
						     self.masonryImagesReveal($newitems,$container);

							}
						var srcCat=new SourceCategory({
							source:source,
							items:items
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
				   return record.recordData.externalId === e;
				});
			itemShow(selrecord,true);
		}

		

        self.columnRecordSelect= function (e){

			itemShow(e,true);

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
						source: result.comesFrom!=null ? result.comesFrom : source
					  });
					  
					 
				  items.push(record);
				 self.mixresults().push(record);}
			}
			self.mixresults.valueHasMutated();
			return items;
		};


	  ctrlClose =$("span.withsearch-close");
	  isOpen = false;
		// show/hide search area
      toggleSearch = function(evt,char) {
			// return if open and the input gets focused
			if(  evt === 'focus' && isOpen ) return false;

			if( isOpen ) {
				$('[id^="modal"]').removeClass('md-show').css('display', 'none');
		    	$("#myModal").modal('hide');
		    	$("#myModal").find("h4").html("");
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
        
        
        
        
        
        self.goToTop= function () {
        	if($request!==undefined)$request.abort();
        	verticalOffset = typeof(verticalOffset) != 'undefined' ? verticalOffset : 0;
        	element = $("#withsearchid");
        	offset = element.offset();
        	offsetTop = offset.top;
        	element.animate({scrollTop: offsetTop}, 100, 'linear');
        }

       self.likeRecord = function (rec) {
			app.likeItem(rec, function (status) {
				if (status) {
					$('#' + rec.recordId).addClass('active');
				} else {
					$('#' + rec.recordId).removeClass('active');
				}
			});
		};

<<<<<<< HEAD
        function getItem(record) {
			var figure='<figure class="masonryitem">';
			
			console.log(JSON.stringify(recordData));
			figure+='<span class="star" data-bind="css: { active: '+isLike+'},visible:isLogged" id='+recordData.externalId+'>'+
						'<span class="fa-stack fa-fw" data-bind="event: { click: function() { likeRecord(\'' + recordData.externalId + '\'); } }">'
						+'<i class="fa fa-heart fa-stack-1x"></i><i class="fa fa-heart-o fa-stack-1x fa-inverse"></i>'
						+'</span></span>'
			+ '<a data-bind="event: { click: function() { recordSelect(\''+recordData.externalId+'\')}}"><img onError="this.src=\'images/no_image.jpg\'" src="'+recordData.thumbnailUrl+'" width="211"/></a><figcaption>'+displayTitle()+'</figcaption>'
			+'<div class="sourceCredits"><a href="'+recordData.isShownAt+'" target="_new">'+sourceCredits()+'</a></figure>';

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
