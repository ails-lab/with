define(['knockout', 'text!./item.html', 'app', 'knockout-else', 'smoke'], function (ko, template, app, KnockoutElse) {

    self.disqusLoaded=ko.observable(false);
    helper_thumb = "";
    

	function Record(data, isRelated) {
		var self = this;
	    self.recordId = "-1";
		self.title = "";
		self.description="";
		self.thumb = "";
		self.fullres=ko.observable('');
		self.view_url=ko.observable('');
		self.source=ko.observable("");
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
		self.vtype = "IMAGE";
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
		self.nextItemToAnnotate = ko.observable(null);
		self.annotations = ko.observableArray([]);
		self.myAnnotations = ko.computed(function () {
			return self.annotations.filter(function(i) {
				for (j = 0, len = i.annotators.length; j < len; j++) { 
					if (i.annotators[j].withCreator == app.currentUser._id()) {
						return true;
					}
				}
				return false;
		    });
		});
		self.otherAnnotations = ko.computed(function () {
			return self.annotations.filter(function(i) {
				var my = false;
				for (j = 0, len = i.annotators.length; j < len; j++) { 
					if (i.annotators[j].withCreator == app.currentUser._id()) {
						my = true;
					}
				}
				return (!my);
		    });
		});
		self.isLiked = ko.pureComputed(function () {
			return app.isLiked(self.externalId);
		});
		self.isLoaded = ko.observable(false);

		self.load = function(data, isRelated) {
			if(data.title==undefined){
				self.title="No title";
			}else{self.title=data.title;}
			self.view_url(data.view_url);
			self.thumb=data.thumb;
			if ( data.fullres && data.fullres.length > 0 ) {
				self.fullres(data.fullres);
			} else {
				self.fullres(self.calcThumbnail());
			}
			self.mediatype=data.mediatype;
			self.description=data.description;
			self.source(data.source);
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
			if (data.nextItemToAnnotate !== undefined)
				self.nextItemToAnnotate(data.nextItemToAnnotate);
			if (data.annotations !== undefined)
				self.annotations(data.annotations);
			self.loading(false);
			$("audio").trigger("pause");
			var vid = document.getElementById("mediaplayer");
			if (vid != null) {
				vid.parentNode.removeChild(vid);
			}
			$('#mediathumbid').show();
			if (data.view_url.indexOf('archives_items_') > -1) {
				var id = data.view_url.split("_")[2];
				$('#mediathumbid').hide();
				$('#mediadiv').append('<iframe id="mediaplayer" src="http://archives.crem-cnrs.fr/archives/items/'+id+'/player/346x130/"height="250px scrolling="no"" width="361px"></iframe>');
			} else {
			if (data.mediatype != null) {
				if (data.mediatype == "VIDEO") {		
					self.vtype = "MEDIA";
					if (!isRelated) {
						$('#mediadiv').append('<video id="mediaplayer" autoplay="true" controls width="576" height="324"><source src="' + self.fullres() + '" type="video/mp4">Your browser does not support HTML5</video>');
					}
				} else if (data.mediatype == "AUDIO") {
					self.vtype = "MEDIA";
					if (!isRelated) {
						$('#mediadiv').append('<audio id="mediaplayer" autoplay="true" controls width="576" height="324"><source src="' + self.fullres() + '" type="audio/mpeg">Your browser does not support HTML5</audio>');
					}
				}
			}
			}
			console.log(helper_thumb);
			helper_thumb = self.calcOnErrorThumbnail();
			if(self.source() && self.source()=="Europeana" && self.recordId != '-1'){
				$("span.pnd-resource").show();
				$("div.pnd-resource").show();
				var pundit_url=self.view_url().replace('http://www.europeana.eu/portal/record/','http://data.europeana.eu/item/');
				pundit_url=pundit_url.replace('.html','');
				$("span.pnd-resource").attr('about',pundit_url);
				$("div.pnd-resource").attr('about',pundit_url);
				dispatchDocumentEvent('Pundit.loadAnnotations');
				dispatchDocumentEvent('Pundit.forceCompileButton');				
			}
			else{$("span.pnd-resource").hide();
				$("div.pnd-resource").hide();
				}
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
				    source:[self.source()],
				    filters:[]
				}),
				success : function(result) {
					data=result.responses[0]!=undefined  &&  result.responses[0].items.culturalCHO !=undefined? result.responses[0].items.culturalCHO :null;
					var items=[];
					if(data!=null) {
						for (var i in data) {
							var result = data[i];
							 if(result !=null){
								var record = new Record(formatRecord(result), true);
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
					    source:[self.source()],
					    filters:[]
					}),
					success : function(result) {
						data=result.responses[0]!=undefined &&  result.responses[0].items.culturalCHO !=undefined? result.responses[0].items.culturalCHO :null;
						var items=[];
						if(data!=null) {
							for (var i in data) {
								var result = data[i];
								 if(result !=null){
									var record = new Record(formatRecord(result), true);
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
			   if(self.thumb && self.thumb.indexOf("empty")==-1){
					return self.thumb;
				}
			   else{
				   return "img/ui/ic-noimage.png";
			   }
			});
		
		self.calcOnErrorThumbnail = ko.pureComputed(function() {


			   if(self.thumb && self.thumb.indexOf('.pdf') == -1){
					return self.thumb;
				}
			   else{
				   return "img/content/thumb-empty.png";
			   }
			});
		
		self.sourceCredits = ko.pureComputed(function() {
			 switch(self.source()) {
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

		
		
		if (data != undefined) { 
			if(isRelated) self.load(data, true); 
			else self.load(data);
		};
	}

	function ItemViewModel(params) {
		var self = this;
		//document.body.setAttribute("data-page","item");
		setTimeout(function(){ WITHApp.init(); }, 300);
		KnockoutElse.init([spec = {}]);
		self.batchItemsAnnotated = [];
		self.batchAnnotationCount = 0;
		self.route = params.route;
		self.from=window.location.href;
		var thumb = "";
		self.loggedUser=ko.pureComputed(function(){
			if(app.isLogged())return true;
			else return false;
		});
		self.record = ko.observable(new Record());
		self.id = ko.observable(params.id);
		self.indexInBatch = ko.observable(1);
		document.addEventListener("Pundit.saveAnnotation", function(event) {
			var annotationId = event.detail;
			$.ajax({
		    	url: "http://thepund.it:8083/annotationserver/api/open/annotations/"+annotationId,
		    	method: "GET",
		    	contentType    : "application/json",
		    	beforeSend : function (request) { request.setRequestHeader("Accept", "application/json"); },
		    	success : function(punditAnnotation) {
		    		//save annotation, get with response and update annotations array
		    		// Map pundit annotation JSON to WITH annotation JSON
		    		var withAnnotation = {
		    			'generator': 'pundit',
		    			'motivation': 'Tagging',
		    			'target' : {
		    				'recordId': self.record().recordId,
		    				'withURI': '/record/'+ self.record().recordId,
		    			},
		    			'body' : {
		    				'uriType': [ 'http://www.mimo-db.eu/InstrumentsKeywords' ],
		    				'uriVocabulary': 'MIMO' 
		    			}
		    		};
		    		if (self.record().recordId == "-1")
		    			return;
		    		var annotationUri = punditAnnotation.metadata['http://purl.org/pundit/as/annotation/'+annotationId];
		    		var serializedAt = annotationUri['http://www.openannotation.org/ns/serializedAt'];
		    		withAnnotation.generated = serializedAt[0].value;
		    		var graph = punditAnnotation.graph['http://purl.org/pundit/as/graph/body-'+annotationId];
		    		var externalId = Object.keys(graph)[0];
		    		var annotationInfo = graph[externalId];
		    		var tagTypes = Object.keys(annotationInfo);
		    		for (i in tagTypes) {
			    		var tagType = tagTypes[i];
			    		var uriInfo = annotationInfo[tagType];
			    		for (j in uriInfo) {
			    			var uri = uriInfo[j].value;
			    			var itemsInfo = punditAnnotation.items[uri];
				    		var labelInfo = itemsInfo['http://www.w3.org/2000/01/rdf-schema#label'];
				    		var label = labelInfo[0].value;
				    		withAnnotation['target'].externalId = externalId;
				    		withAnnotation['body'].uri = uri;
				    		withAnnotation['body'].tagType = tagType;
				    		withAnnotation['body'].label = {'default' : [ label ]};
				    		console.log(JSON.stringify(withAnnotation));
				    		$.ajax({
				    			url : '/record/annotation',
						    	method : "POST",
						    	contentType : "application/json",
						    	data     : JSON.stringify(withAnnotation),
								success : function(result) {
									//check if duplicate
									var index1 = self.arrayFirstIndexOf(ko.toJS(self.record().annotations), function (annotation) {
										return annotation.dbId === result.dbId;
									});
									if (index1 >= 0)
										self.record().annotations.splice(index1, 1);
									self.record().annotations.push(result);
									self.batchAnnotationCount++;
									self.recordSimple = ko.toJS(self.record);
									self.recordSimple.nextItemToAnnotate = null;
									var index = self.arrayFirstIndexOf(self.batchItemsAnnotated, function (item) {
										return item.recordId === self.recordSimple.recordId;
									});
									if (index >= 0) 
										self.batchItemsAnnotated.splice(index, 1);
									self.batchItemsAnnotated.push(self.recordSimple);
									updateRecordAnnotations(self.record().recordId, self.record().annotations());
								}
				    		});
			    		}
		    		}
			}
		   });
		});
		
		self.arrayFirstIndexOf = function (array, predicate) {
			for (var i = 0, j = array.length; i < j; i++) {
				if (predicate.call(undefined, array[i])) {
					return i;
				}
			}
			return -1;
		};
		
		self.nextItem = function() {
			$("audio").trigger("pause");
			var vid = document.getElementById("mediaplayer");
			if (vid != null) {
				vid.parentNode.removeChild(vid);
			}
			formattedNextRecord = formatRecord(self.record().nextItemToAnnotate());
			self.indexInBatch(self.indexInBatch()+1);
			dispatchDocumentEvent('Pundit.hide');
			itemShow(formattedNextRecord);
		};
		
		self.endBatch = function() {
			$("audio").trigger("pause");
			var vid = document.getElementById("mediaplayer");
			if (vid != null) {
				vid.parentNode.removeChild(vid);
			}
			self.close();
			showEndOfAnnotations(self.batchItemsAnnotated, self.batchAnnotationCount);
		};
		
		formatRecord =  function(backendRecord) {
			var admindata=backendRecord.administrative;
			var descdata=backendRecord.descriptiveData;
			var media=backendRecord.media;
			var provenance=backendRecord.provenance;
			var usage=backendRecord.usage;
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
			 var record = {
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
							dbId:backendRecord.dbId,
							likes: usage.likes,
							collected: usage.collected,
							collectedIn:backendRecord.collectedIn,
							fullrestype: media[0] != null && media[0].Original != null
								&& media[0].Original.type != "null" ? media[0].Original.type : "",
							nextItemToAnnotate: backendRecord.nextItemToAnnotate,
							annotations: backendRecord.annotations,
							data: backendRecord
				  };
			 return record;
		};
		
		itemShow = function (e) {
			data = ko.toJS(e);
			$('.nav-tabs a[href="#information"]').tab('show');
			$(".mediathumb > img").attr("src","");
			$("span.pnd-resource").attr('about','');
			self.record(new Record(data));
			self.open();
			if(self.record().recordId!="-1"){
				self.addDisqus();
			}
		};

		self.open = function () {
			if (window.location.href.indexOf('#item')>0) {
				document.body.setAttribute("data-page","media");		
			}
			//document.body.setAttribute("data-page","item");
			$( '.itemview' ).fadeIn();
			$('body').css('overflow','hidden');
			adjustHeight();
			
		};

		self.close = function () {
			$("audio").trigger("pause");
			var vid = document.getElementById("mediaplayer");
			if (vid != null) {
				vid.parentNode.removeChild(vid);
			}
			//self.record(new Record());
			dispatchDocumentEvent('Pundit.hide');
			$('body').css('overflow','visible');
			$( '.itemview' ).fadeOut();
			self.indexInBatch(1);
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
					var record = new Record(formatRecord(result));
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
		
		self.deleteAnnotation = function(annotation) {
    		$.ajax({
    			url : '/annotation/'+annotation.dbId,
		    	method : "DELETE",
				success : function(result) {
					self.record().annotations.remove(annotation);
					self.batchAnnotationCount--;
					if (result !== "") {
						self.record().annotations.push(result);
					}
					updateRecordAnnotations(self.record().recordId, self.record().annotations());
				}
    		});
		};
		

       self.loginFromItem = function (data,event) {
       		event.preventDefault();
       		$("#loginPopup").addClass("open");
       		$("#loginPopup").on("loginEvent", function(event) {
       			itemShow(self.record());
       		});
		}
		
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
		
		self.annotate=function(){
			
			window.open('http://euspndwidget.netseven.it/index.php?id='+self.record().externalId, self.record().externalId, 'top=10, left=10, width=900, height=600, status=no, menubar=no, toolbar=no scrollbars=no');
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
