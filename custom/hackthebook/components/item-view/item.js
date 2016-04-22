define(['knockout', 'text!./item.html', 'app','smoke'], function (ko, template, app) {

	self.disqusLoaded=ko.observable(false);
	

	function Record(data) {
		var self = this;
	    self.recordId = "-1";
		self.title = "";
		self.description="";
		self.thumb = "";
		self.fullres=ko.observable('');
		self.view_url="";
		self.source="";
		self.creator="";
		self.provider="";
		self.dataProvider="";
		self.dataProvider_uri="";
		self.rights="";
		self.url="";
		self.externalId = "";
		self.mediatype="";
		self.likes=0;
		self.collected=0;
		self.data=ko.observable('');
		self.collectedIn =  [];
		self.isLike=ko.observable(false);
		
		self.related =  ko.observableArray([]);
		self.similar =  ko.observableArray([]);
		self.facebook='';
		self.twitter='';
		self.mail='';
		self.forrelated=ko.observable("").extend({ uppercase: true });
		self.relatedlabel='';
		self.loc=ko.observable('');
		self.similarsearch=false;
		self.relatedsearch=false;
		self.loading=ko.observable(false);
		self.pinterest=function() {
		    var url = encodeURIComponent(self.loc());
		    var media = encodeURIComponent(self.fullres());
		    var desc = encodeURIComponent(self.title+" on "+window.location.host);
		    window.open("//www.pinterest.com/pin/create/button/"+
		    "?url="+url+
		    "&media="+media+
		    "&description="+desc,'','height=500,width=750');
		    return false;
		};
		 
		self.isLiked = ko.pureComputed(function () {
			return app.isLiked(self.externalId);
		});
		self.isLoaded = ko.observable(false);
		
		self.load = function(data) {
			if(data.title==undefined){
				self.title="No title";
			}else{self.title=data.title;}
			self.view_url=data.view_url;
			self.thumb=data.thumb;
			if ( data.fullres && data.fullres.length > 0 ) {
				self.fullres(data.fullres);
			} else {
				self.fullres(self.calcThumbnail());
			}
			self.mediatype=data.mediatype;
			self.description=data.description;
			self.source=data.source;
			self.creator=data.creator;
			self.provider=data.provider;
			self.dataProvider=data.dataProvider;
			self.dataProvider_uri=data.dataProvider_uri;
			self.rights=data.rights;
			if(data.dbId){
			 self.recordId=data.dbId;
			 self.loc(location.href.replace(location.hash,"")+"#item/"+self.recordId);
			}
			self.externalId=data.externalId;
			self.likes=data.likes;
			self.collected=data.collected;
			self.collectedIn=data.collectedIn;
			self.data(data.data);
			self.facebook='https://www.facebook.com/sharer/sharer.php?u='+encodeURIComponent(self.loc());
			self.twitter='https://twitter.com/share?url='+encodeURIComponent(self.loc())+'&text='+encodeURIComponent(self.title+" on "+window.location.host)+'"';
			self.mail="mailto:?subject="+self.title+"&body="+encodeURIComponent(self.loc());
			var likeval=app.isLiked(self.externalId);
			self.isLike(likeval);
			self.loading(false);
		 
		};

		self.findsimilar=function(){
		  if(self.related().length==0 && self.relatedsearch==false){
			self.relatedsearch=true;  
			self.creator.length>0? self.forrelated(self.creator.toUpperCase()) : self.forrelated(self.provider.toUpperCase());
            self.relatedlabel=self.creator.length>0? "CREATOR" : "PROVIDER";
            if(self.forrelated().length>0){
            	self.loading(true);
           $.ajax({
				type    : "post",
				url     : "/api/advancedsearch",
				contentType: "application/json",
				data     : JSON.stringify({
					searchTerm: self.forrelated(),
					page: 1,
					pageSize:10,
				    source:[self.source],
				    filters:[]
				}),
				success : function(result) {
					data=result.responses[0]!=undefined  &&  result.responses[0].items.culturalCHO !=undefined? result.responses[0].items.culturalCHO :null;
					var items=[];
					if(data!=null) {
						for (var i in data) {
							var result = data[i];
							 if(result !=null){
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
											data: result
								  });
						        if(record.thumb && record.thumb.length>0 && record.externalId!=self.externalId)
							       items.push(record);
							}
							 if(items.length>3){break;}
						}	
					self.related().push.apply(self.related(),items);
					self.related.valueHasMutated();}
					self.loading(false);
				},
				error   : function(request, status, error) {
					self.loading(false);
					
				}
			});
            }
			}
		  if(self.similar().length==0 && self.similarsearch==false){
				self.similarsearch=true;  
				
				self.loading(true);
	           $.ajax({
					type    : "post",
					url     : "/api/advancedsearch",
					contentType: "application/json",
					data     : JSON.stringify({
						searchTerm: self.title,
						page: 1,
						pageSize:10,
					    source:[self.source],
					    filters:[]
					}),
					success : function(result) {
						data=result.responses[0]!=undefined &&  result.responses[0].items.culturalCHO !=undefined? result.responses[0].items.culturalCHO :null;
						var items=[];
						if(data!=null) {
							for (var i in data) {
								var result = data[i];
								 if(result !=null){
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
												data: result
									  });
							        if(record.thumb && record.thumb.length>0 && record.externalId!=self.externalId)
								       items.push(record);
								}
								 if(items.length>3){break;}
							}	
						self.similar().push.apply(self.similar(),items);
						self.similar.valueHasMutated();}
						self.loading(false);
					},
					error   : function(request, status, error) {
						self.loading(false);
						
					}
				});
	            
				}
		}

		self.doLike=function(){
			self.isLike(true);
		}
		
		self.calcThumbnail = ko.pureComputed(function() {


			   if(self.thumb){
					return self.thumb;
				}
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

	function ItemViewModel(params) {
		var self = this;
		document.body.setAttribute("data-page","item");
		setTimeout(function(){ WITHApp.init(); }, 300);   
		self.route = params.route;
		self.from=window.location.href;	
		var thumb = "";
		self.loggedUser=ko.pureComputed(function(){
			if(app.isLogged())return true;
			else return false;
		});
		self.record = ko.observable(new Record());
		self.id = ko.observable(params.id);
		itemShow = function (e) {
			data = ko.toJS(e);
			$('.nav-tabs a[href="#information"]').tab('show');
			$(".mediathumb > img").attr("src","");
			
			self.open();
			self.record(new Record(data
					));
			if(self.record().recordId!="-1"){
				self.addDisqus();
			}
			
		};

		self.open = function () {
			if (window.location.href.indexOf('#item')>0) {
				document.body.setAttribute("data-page","media");	
				
			}
			document.body.setAttribute("data-page","item");
			
			//e.preventDefault();
			$( '.itemview' ).fadeIn();
			//$('[role="main"]').addClass('itemopen');
			//$("div[role='main']").addClass("itemopen");
			$('body').css('overflow','hidden');
			adjustHeight();
			
		};

		self.close = function () {
			//self.record(new Record());
			$('body').css('overflow','visible');
			$( '.itemview' ).fadeOut();
			
		};

		self.changeSource = function (item) {
			item.record().fullres(item.record().calcThumbnail());
		};

		self.collect = function (item) {
				if (!isLogged()) {
				showLoginPopup(self.record());
			} else {
				collectionShow(self.record());
			}
		};

		self.recordSelect = function (e,flag) {
			itemShow(e,flag);
		};
		
		self.likeRecord = function (rec,event) {
        	event.preventDefault();
        	var $star=$(event.target.parentNode).parent();
			app.likeItem(rec, function (status) {
				if (status) {
					$star.addClass('active');
					if($( '[id="'+rec.externalId+'"]' ) || $( '[id="'+rec.recordId+'"]' )){
						$( '[id="'+rec.externalId+'"]' ).find("span.star").addClass('active');
						$( '[id="'+rec.recordId+'"]' ).find("span.star").addClass('active');}
						
				} else {
					$star.removeClass('active');
					if($( '[id="'+rec.externalId+'"]' ) || $( '[id="'+rec.recordId+'"]' )){
						$( '[id="'+rec.externalId+'"]' ).find("span.star").removeClass('active');
						$( '[id="'+rec.recordId+'"]' ).find("span.star").removeClass('active');
					}
				}
			});
		};
		
		self.collect = function (rec,event) {
				event.preventDefault();
				collectionShow(rec);
		};
		
		
	
		
		self.loadItem = function () {
			$.ajax({
				"url": "/record/" + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (result) {
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
						            thumb: media!=null &&  media[0] !=null  && media[0].Thumbnail!=null  && media[0].Thumbnail.url!="null" ? media[0].Thumbnail.url:"img/content/thumb-empty.png",
								    fullres: media!=null &&  media[0] !=null && media[0].Original!=null  && media[0].Original.url!="null"  ? media[0].Original.url : "",
									title: findByLang(descdata.label),
									description: findByLang(descdata.description),
									view_url: findProvenanceValues(provenance,"source_uri"),
									creator: findByLang(descdata.dccreator),
									dataProvider: findProvenanceValues(provenance,"dataProvider"),
									dataProvider_uri: findProvenanceValues(provenance,"dataProvider_uri"),
									provider: findProvenanceValues(provenance,"provider"),
									mediatype: mediatype, 
									rights: rights,
									externalId: admindata.externalId,
									source: source,
									dbId:result.dbId,
									likes: usage.likes,
									collected: usage.collected,
									collectedIn:result.collectedIn,
									data: result
						  });
					self.record(record);
					$('.nav-tabs a[href="#information"]').tab('show');
					self.open();
					self.addDisqus();
					$( '.itemview' ).fadeIn();
					
				},
				error: function (xhr, textStatus, errorThrown) {
					self.open();
					$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
				}
			});
		};
		
		self.addDisqus= function(){
			$("#disqus_thread").hide();
			if(disqusLoaded()==false){
		        var disqus_shortname = 'withculture';
		        var disqus_identifier = self.record().recordId;
		        var disqus_url = location.href.replace(location.hash,"")+"#!item/"+self.record().recordId;
		       

		        /* * * DON'T EDIT BELOW THIS LINE * * */
		        (function() {
		            var dsq = document.createElement('script'); dsq.type = 'text/javascript'; dsq.async = true;
		            dsq.src = 'http://' + disqus_shortname + '.disqus.com/embed.js';
		            (document.getElementsByTagName('head')[0] || document.getElementsByTagName('body')[0]).appendChild(dsq);
		        })();
		        disqusLoaded(true);
		        
			}
			    setTimeout(function(){
			    	DISQUS.reset({
			        reload: true,
			        config: function () {
			            this.page.identifier = self.record().recordId;
			            this.page.url =  location.href.replace(location.hash,"")+"#!item/"+self.record().recordId;
			            this.page.title = self.record().title;
			            this.language = "en";
			        }
			    	
			    });
			    	$("#disqus_thread").show();
			    }, 2000);
		    
			
		}
		
		
		if(self.id()!=undefined){
			
			self.loadItem();
		}
		
		function adjustHeight() {

			// vars 
			var wHeight = $( window ).height(),
				wWidth = $( window ).width(),
				itemHeight = wHeight - 70;

			// check
			if( wWidth >= 1200 ) {

				// set height
				$( '.itemopen .itemview' ).css({
					height : itemHeight+"px"
				});
			}
		}
		
	}
	
	
	return {
		viewModel: ItemViewModel,
		template: template
	};
});
