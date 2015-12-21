define(['bridget', 'knockout', 'text!./collection-view.html', 'isotope', 'imagesloaded', 'app','smoke'], function (bridget, ko, template, Isotope, imagesLoaded,app) {

	$.bridget('isotope', Isotope);
		
		
		ko.bindingHandlers.collectionIsotope = {
				init: app.initOrUpdate('init'),
				update: app.initOrUpdate('update')
			};

	ko.showMoreLess = function (initialText) {

		var observable = ko.observable(initialText);
		observable.limit = ko.observable(100);
		observable.showAll = ko.observable(false);
		observable.showButton = ko.computed(function () {
			return observable().length > observable.limit();
		});
		observable.toggleShowAll = function () {
			observable.showAll(!observable.showAll());
		};
		observable.display = ko.computed(function () {
			if (observable.showAll() || !observable.showButton()) {
				return observable();
			}
			return observable().slice(0, observable.limit());
		}, observable);
		return observable;
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
		self.dataProvider="";
		self.rights="";
		self.url="";
		self.externalId = "";
		self.isLoaded = ko.observable(false);
		self.isLiked = ko.pureComputed(function () {
			return app.isLiked(self.externalId);
		}); 
		self.load = function(data) {
			if(data.title==undefined){
				self.title="No title";
			}else{self.title=data.title;}
			self.url="#item/"+data.id;
			self.view_url=data.view_url;
			self.thumb=data.thumb;
			self.fullres=data.fullres;
			self.description=data.description;
			self.source=data.source;
			self.creator=data.creator;
			self.provider=data.provider;
			self.dataProvider=data.dataProvider;
			self.rights=data.rights;
			self.recordId=data.id;
			self.externalId=data.externalId;
			
		};

		self.cachedThumbnail = ko.pureComputed(function() {
			
		   if(self.thumb && self.thumb!="img/content/thumb-empty.png"){
			if (self.thumb.indexOf('/') === 0) {
				return self.thumb;
			} else {
				var newurl='url=' + encodeURIComponent(self.thumb)+'&';
				return '/cache/byUrl?'+newurl+'Xauth2='+ sign(newurl);
			}}
		   else{
			   return "img/content/thumb-empty.png";
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
			    case "YouTube": {
			    	return "youtube.com";
			    }
			    case "Mint":
			    	return "mint";
			    case "WITHin":
			    	return "WITHin";
			    default: return "";
			 }
			});

		self.displayTitle = ko.pureComputed(function() {
			var distitle="";
			distitle=self.title;
			if(self.creator!==undefined && self.creator.length>0)
				distitle+=", by "+self.creator;
			
			return distitle;
		});

		if(data != undefined) self.load(data);
		
	}

	
	function CViewModel(params) {
		document.body.setAttribute("data-page","collection");
		
		   
		var self = this;

		var $container = $(".grid").isotope({
			itemSelector: '.media',
			transitionDuration: transDuration,
			masonry: {
				columnWidth		: '.sizer',
				percentPosition	: true
			
			}
		});
		
		self.route = params.route;
		var counter = 1;
		self.hash=window.location.hash;
		self.collname = ko.observable('');
		self.access = ko.observable("READ");
		self.id = ko.observable(params.id);
		self.count=ko.observable(params.count);
		self.owner = ko.observable('');
		self.ownerId = ko.observable(-1);
		self.itemCount = ko.observable(0);
		self.citems = ko.observableArray();
		self.description = ko.observable('');
		self.selectedRecord = ko.observable(false);

		self.loading = ko.observable(false);
        self.loggedUser=app.isLogged();
		self.next = ko.observable(-1);
		self.desc = ko.showMoreLess('');
	

		self.loadCollection = function (id) {
			// check if a state property exists and write back the HTML cache
		    
			self.loading(true);
			$.ajax({
				"url": "/collection/" + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					self.collname(data.title);
					self.desc(data.description);
					self.owner(data.creator);
					self.ownerId(data.ownerId);
					self.itemCount(data.itemCount);
					self.access(data.access);
					
					if(self.count() && self.count()>0){
						$.ajax({
							"url": "/collection/" + self.id() + "/list?count="+self.count()+"&start=0",
							"method": "get",
							"contentType": "application/json",
							"success": function (data) {
								var items=self.revealItems(data.records);
								
								if(items.length>0){
									 var $newitems=getItems(items);
								     
									 isotopeImagesReveal($container, $newitems );

									}
								

								
								self.loading(false);
							},
							"error": function (result) {
								self.loading(false);
							}
						});
						
					}
					else{
					
						var items=self.revealItems(data.firstEntries);
						
						if(items.length>0){
							 var $newitems=getItems(items);
							if(sessionStorage.getItem("collection-viewscroll"+self.id())){
								 sessionStorage.removeItem("collection-viewscroll"+self.id());
							 }
							 isotopeImagesReveal( $container,$newitems );
								
							}
						self.loading(false);
					}
				},
				error: function (xhr, textStatus, errorThrown) {
					self.loading(false);
					$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
				}
			});
		     
		};

		
		self.revealItems = function (data) {
			var items=[];
			for (var i in data) {
				var result = data[i];
				 if(result !=null ){
					 var record = new Record({
							id: result.dbId,
							description: result.description,
							thumb: result.thumbnailUrl,
							dataProvider: result.dataProvider,
							title: result.title,
							view_url: result.sourceUrl,
							creator: result.creator,
							provider: result.provider,
							source: result.source,
							rights: result.rights,
							externalId: result.externalId
						});	
					 
			      
				  items.push(record);
				 }
			}
			self.citems.push.apply(self.citems, items);
			var offset = self.citems().length;
			var new_url="";
			if(window.location.hash.indexOf("collectionview")==1){
				if(window.location.hash.indexOf("count")==-1)
			      new_url=window.location.pathname+window.location.hash+"/count/"+offset;
			    else{
				 temphash=window.location.hash.substring(0,window.location.hash.lastIndexOf("/"));
				 new_url=window.location.pathname+temphash+"/"+offset;
			}}
			history.replaceState('', '', new_url);
			return items;
		};
		
		self.loadCollection();
		self.isOwner = ko.pureComputed(function () {
			if (app.currentUser._id() == self.ownerId()) {
				return true;
			} else {
				return false;
			}
		});

		self.loadNext = function () {
			console.log("more items ");
			self.moreItems();
		};

		self.moreItems = function () {
			if (self.loading === true) {
				setTimeout(self.moreItems(), 1000);
			}
			if (self.loading() === false) {
				self.loading(true);
				
				var offset = self.citems().length;
				$.ajax({
					"url": "/collection/" + self.id() + "/list?count=40&start=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						var items=self.revealItems(data.records);
						
						if(items.length>0){
							 var $newitems=getItems(items);
						     
							 isotopeImagesReveal( $container,$newitems );
							
							}
						self.loading(false);
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
		};

		

	
		likeRecord = function (id,event) {
        	event.preventDefault();
			var rec = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.recordId=== id;
			});

			app.likeItem(rec, function (status) {
				if (status) {
					$('#' + id).find("span.star").addClass('active');
				} else {
					$('#' + id).find("span.star").removeClass('active');
				}
			});
		};
		
		collect = function (id,event) {
			event.preventDefault();
			var rec = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.recordId=== id;
			});
			console.log(rec);
			
			collectionShow(rec);
		};
		
		
		function getItem(record) {
			 var tile= '<div class="item media" id="'+record.recordId+'"> <div class="wrap">';
			 if(isLogged()){
				    if(record.isLiked()){
				    	 tile+='<span class="star active" id="'+record.externalId+'">';
				    }
				    else{tile+='<span class="star">';}
				    if(record.externalId){
				    tile+='<span class="fa-stack fa-fw" onclick="likeRecord(\'' + record.recordId + '\',event);" title="add to favorites">'
						+'<i class="fa fa-heart fa-stack-1x"></i><i class="fa fa-heart-o fa-stack-1x fa-inverse"></i>'
						+'</span>';}
						tile+='<span class="collect" title="collect" onclick="collect(\'' + record.recordId + '\',event)"><i class="fa fa-download fa-stack-1x fa-inverse"></i></span></span>';
					}
			 else{
				 tile+='<span class="star" id="'+record.externalId+'" style="display:none">'
				 if(record.externalId){
					 tile+='<span class="fa-stack fa-fw" onclick="likeRecord(\'' + record.recordId + '\',event);" title="add to favorites">'
					    +'<i class="fa fa-heart fa-stack-1x"></i><i class="fa fa-heart-o fa-stack-1x fa-inverse"></i>'
					    +'</span>';}
					tile+='<span class="collect" title="collect" onclick="collect(\'' + record.recordId + '\',event)" style="display:none"><i class="fa fa-download fa-stack-1x fa-inverse"></i></span></span>';
					
			 }
                    tile+='<a href="#" onclick="recordSelect(\''+record.recordId+'\',event)">'
                     +'<div class="thumb"><img src="'+record.cachedThumbnail()+'" onError="this.src=\'img/content/thumb-empty.png\'"></div>'
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
       
       
       recordSelect = function (data,event) {
       	
       	event.preventDefault();
			var selrecord = ko.utils.arrayFirst(self.citems(), function(record) {
				   return record.recordId === data;
				});
			itemShow(selrecord);
			return false;

		}

       self.refresh=function(){
    	   self.loading(true);
    	   var items=self.citems();
			
			if(items.length>0){
				 var $newitems=getItems(items);
			     
				 isotopeImagesReveal( $container,$newitems );

				}
			self.loading(false);
    	   
       }
       
       isotopeImagesReveal = function( $container,$items ) {
 		  var iso = $container.data('isotope');
 		  var itemSelector = iso.options.itemSelector;
 		  
 		  // append to container
 		 $container.append( $items );
 		// hide by default
 		  $items.hide();
 		  $items.imagesLoaded().progress( function( imgLoad, image ) {
 		    // get item
 		    var $item = $( image.img ).parents( itemSelector );
 		    // un-hide item
 		    $item.show();
 		    iso.appended( $item );
 		    var scrollpos=sessionStorage.getItem("collection-viewscroll"+self.id());
 			if(scrollpos && $(".grid").height()>scrollpos){
 		    	 $(window).scrollTop(scrollpos);
 		    	 //sessionStorage.removeItem("collection-viewscroll"+$(".grid").attr("id"));
 		    }else if(scrollpos && $(".grid").height()<scrollpos){
 		    	$(window).scrollTop($(".grid").height());	
 		    }
 		    
 		  });
 		  
 		  return this;
 		};
       
      
    			
	}

	return {
		viewModel: CViewModel,
		template: template
	};
});
