define(['knockout', 'text!./item.html', 'app','smoke'], function (ko, template, app) {

	self.disqusLoaded=ko.observable(false);
    helper_thumb = "";
	
	function Record(data,showMeta, isRelated) {
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
		
		
		self.Xannotations = ko.observableArray([]);
		self.annotationsKeys = ko.observableArray([]);		
		self.referenceMap = ko.observableArray([]);
		 
		
		self.nextItemToAnnotate = ko.observable({});
		self.annotations = ko.observableArray([]);
		self.myAnnotations = ko.pureComputed(function () {
			return self.annotations.filter(function(i) {
				for (j = 0, len = i.annotators.length; j < len; j++) { 
					if (i.annotators[j].withCreator == app.currentUser._id()) {
						return true;
					}
				}
				return false;
		   	});
		});
		self.otherAnnotations = ko.pureComputed(function () {
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
			//$('#mediaplayer').remove();
			if(data.title==undefined){
				self.title="No title";
			}else{self.title=data.title;}
			self.view_url=data.view_url;
			self.thumb=data.thumb;
			if ( data.fullres && data.fullres.length > 0 ) {
				self.fullres(data.fullres);
			} else {
				self.fullres(self.thumb);
			}
			self.mediatype=data.mediatype;
			self.description=data.description;
			self.source=data.source;
			self.creator=data.creator;
			self.provider=data.provider;
			self.dataProvider=data.dataProvider;
			self.dataProvider_uri=data.dataProvider_uri;
			self.rights=data.rights;
			
			self.externalId=data.externalId;
			self.likes=data.likes;
			self.collected=data.collected;
			self.collectedIn=data.collectedIn;
			self.data(data.data);
			if(data.dbId){
				 self.recordId=data.dbId;
				 self.loc(location.href.replace(location.hash,"")+"#item/"+self.recordId);
				}
			self.facebook='https://www.facebook.com/sharer/sharer.php?u='+encodeURIComponent(self.loc());
			self.twitter='https://twitter.com/share?url='+encodeURIComponent(self.loc())+'&text='+encodeURIComponent(self.title+" on "+window.location.host)+'"';
			self.mail="mailto:?subject="+self.title+"&body="+encodeURIComponent(self.loc());
			var likeval=app.isLiked(self.externalId);
			self.isLike(likeval);
			self.nextItemToAnnotate(data.nextItemToAnnotate);
			self.annotations(data.annotations);
			self.loading(false);
			//if (data.fullrestype != null) {
			if (data.view_url.indexOf('archives_items_') > -1) {
				var id = data.view_url.split("_")[2];
				$('#mediadiv').html('<div><iframe id="mediaplayer" src="http://archives.crem-cnrs.fr/archives/items/'+id+'/player/346x130/"height="250px scrolling="no"" width="361px"></iframe></div>');
			} else {
			if (data.mediatype != null) {
				//if (data.fullrestype == "VIDEO") {
				if (data.mediatype == "VIDEO") {
					self.vtype = "MEDIA";
					if(!isRelated) {
						$('#mediadiv').append('<video id="mediaplayer" autoplay="true" controls width="576" height="324"><source src="' + self.fullres() + '" type="video/mp4">Your browser does not support HTML5</video>');
					}
				//} else if (data.fullrestype == "AUDIO") {
				} else if (data.mediatype == "AUDIO") {
					self.vtype = "MEDIA";
					if(!isRelated) {
						$('#mediadiv').append('<div><audio id="mediaplayer" autoplay="true" controls width="576" height="324"><source src="' + self.fullres() + '" type="audio/mpeg">Your browser does not support HTML5</audio></div>');
					}
				}
			}
			
			if (data.fullrestype == null || (data.fullrestype != null && data.fullrestype == "IMAGE")) {
				var img = new Image();
				img.onload = function() {
					$('#mediadiv').html("<svg style='height:100%;width:100%;max-height:" + img.height + "px; max-width:" + img.width + "px;' viewBox='0 0 " + img.width + " " + img.height + "' preserveAspectRatio='xMidYMin meet' version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'>" +
						                    "<image width='" + img.width + "' height='" + img.height + "' xlink:href='" + self.fullres() + "'/>" +
			         		  	        "</svg>");
					self.vtype = "IMAGE";
				};
				img.src = self.fullres();

			} 			
			
			helper_thumb = self.calcOnErrorThumbnail();
		};

		var Annotations = function(data) {
			var selfx = this;
			
			selfx.field = ko.observable();
			selfx.values = ko.observableArray([]);
			
			ko.mapping.fromJS(data, {}, selfx);
		}

		self.annotationIndex = [];
		self.annotatedTexts = ko.observableArray([]);
		
		self.annotate = function(){
			if(self.Xannotations().length==0) {
				self.loading(true);
		           $.ajax({
						type    : "get",
						url     : "/annotate/" + self.recordId,
						contentType: "application/json",
						dataType: "json",
						success : function(result) {
							self.parseAnnotations(result);
							self.loading(false);
						},
						error   : function(request, status, error) {
							self.loading(false);
							
						}
		           });
			}
		}
		
		self.getAnnotationLabel = function (data) {
			if (data.label == undefined) {
				return data.vocabulary + " : " + data.uri;
			} else {
				return data.vocabulary + " : " + data.label;
			}
		}

		var fieldMap = { label: "title"};
		
		self.parseAnnotations = function(data){
			
			var anns = data.annotations;
			
			var annotations = new Object();
			
			for (x in anns) {
				var location = anns[x].target.selector.value;
				var property = location.substring(2,location.indexOf("&t="));
				if (fieldMap[property] != undefined) {
					property = fieldMap[property];
				}
				
				if (annotations[property] == undefined) {
					annotations[property] = [];
				}
				
				for (z in anns[x].body) {
					if (z == "@id") {
						annotations[property].push(anns[x].body[z]);
					}
				}
			}
			
			for (property in annotations) {
				self.Xannotations.push(new Annotations({ field:property, values: annotations[property] }));
			}
			
			var properties = [];
			var positionsArray = [];
			
			var count = 0;
			
			for (x in anns) {
				var uri = "";

				for (z in anns[x].body) {
					if (z == "@id") {
						uri = anns[x].body[z];
					}
				}
				
				var label = anns[x].body.label;
				var vocabulary = anns[x].body.vocabulary;
				
				var found = false; 
				for (var i = 0; i < self.annotationsKeys().length; i++) {
					if (self.annotationsKeys()[i].uri == uri) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					self.annotationsKeys.push({uri: uri, label: label, vocabulary: vocabulary, id: "ann-key-" + count});

					self.annotationIndex[uri] = count++;
				}

				var location = anns[x].target.selector.value;
				var property = location.substring(2,location.indexOf("&t="));
				if (fieldMap[property] != undefined) {
					property = fieldMap[property];
				}

				var position = location.substring(location.indexOf("&t=") + 3);
				var sp = parseInt(position.substring(0, position.indexOf(",")));
				var ep = parseInt(position.substring(position.indexOf(",") + 1));
				
				found = false;
				for (var i = 0; i < properties.length; i++) {
					if (properties[i] == property) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					properties.push(property);
				}
				
				if (positionsArray[property] == undefined) {
					positionsArray[property] = [];
				}
				
				positionsArray[property].push({uri:uri, pos:sp, type:0, index:self.annotationIndex[uri]});
				positionsArray[property].push({uri:uri, pos:ep, type:1, index:self.annotationIndex[uri]});
			}
			
			self.annotationsKeys.sort(function(a, b) {
				if (a.vocabulary < b.vocabulary) {
					return -1;
				} else if (a.vocabulary > b.vocabulary) {
					return 1;
				} else {
					if (a.label < b.label) {
						return -1;
					} else if (a.label < b.label) {
						return 1;
					} else {
						return 0;
					}
				}
			});
			
			for (v in positionsArray) {
				positionsArray[v].sort(function(a, b) {
					if (a.pos < b.pos) {
						return -1;
					} else if (a.pos > b.pos) {
						return 1;
					} else {
						if (a.type > b.type) {
							return -1;
						} else if (a.type < b.type) {
							return 1;
						} else {
							return 0;
						}
					}
				});
			}
			
			
			for (v in properties) {
				var ppos = -1;
				var text = this[properties[v]];
				var rtext = "";
				var current = [];
				var array = positionsArray[properties[v]];
				var i;
				
				for (i = 0; i < array.length;) {
					var s = i;
					var e = i;
					while (e < array.length - 1) {
						if (array[e + 1].pos == array[s].pos) {
							e++;
						} else {
							break;
						}
					}
					
					var close = false;
					var t;
					for (t = s; t <= e; t++) {
						if (array[t].type == 0) {
							if (current.length > 0) {
								close = true;
							}
							current.push(array[t].uri);
						} else if (array[t].type == 1) {
							for (var j = 0; j < current.length; j++) {
								if (current[j] == array[t].uri) {
									close = true;
									current.splice(j, 1);
								}
							}
						}
					}
					
					if (ppos == -1) {
						rtext += text.substring(0, array[s].pos)
					} else {
						rtext += text.substring(ppos, array[s].pos)
					}
					
					ppos = array[s].pos;

					if (close) {
						rtext += "</span>";
					}

					if (current.length > 0) {
						rtext += "<span class='";
						for (var j = 0; j < current.length; j++) {
							if (j > 0) {
								rtext += " ";
							}
							rtext += "ann-value-" + self.annotationIndex[current[j]];
						}
						
						rtext += "'>";
					}
					
					i = e + 1;
				}
				
				rtext += text.substring(ppos);
				
				$("#ann-" + properties[v]).html(rtext);
			}
			
			
		};
		
		var selectedAnnotations = [];
		
		self.annShow = function(uri) {
			$(".ann-resource").removeClass("tag-selection");
			
			$("#ann-key-" + self.annotationIndex[uri]).addClass("tag-selection");
			
			for (v in selectedAnnotations) {
				$(".ann-value-" + selectedAnnotations[v]).removeClass("tag-selection");
			}
			selectedAnnotations.push(self.annotationIndex[uri]);
			
			$(".ann-value-" + self.annotationIndex[uri]).addClass("tag-selection");
		};

		self.hierarchy = ko.observable('');
		
		self.showHierarchy = function(uri) {
			if (self.hierarchy.length == 0) {
				$.ajax({
					type    : "get",
					url     : "/thesaurus/getTerm?uri=" + uri,
					contentType: "application/json",
					success : function(result) {
						var s = "";
						
						var broader = result.semantic.broaderTransitive;
						if (broader != null) {
							for (c in broader) {
								
								var z = broader[c].prefLabel.default;
								if (s.length > 0) {
									z += ", ";
								}
								s = z + s;
							}
						}
						
						self.hierarchy(s);
						
						alert(s);
					},
					
					error   : function(request, status, error) {
						$.smkAlert({
							text: 'An error has occured',
							type: 'danger',
							permanent: true
						});
					}
				});
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
					pageSize:20,
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
						pageSize:20,
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

		
		
		if(data != undefined) { 
			if(isRelated) self.load(data, true); 
			else self.load(data);
		} ;
	}
	
	

	function ItemViewModel(params) {
		var self = this;
		setTimeout(function(){ WITHApp.init(); }, 300);  
		self.route = params.route;
		var thumb = "";
		self.from=window.location.href;	
		var thumb = "";
		self.record = ko.observable(new Record());
		self.loggedUser=ko.pureComputed(function(){
			if(app.isLogged())return true;
			else return false;
		});
		self.detailsEnabled =  ko.observable(false);
		self.id = ko.observable(params.id);
		
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
							annotations: backendRecord.annotations
				  };
			 return record;
		};
		
		itemShow = function (e,showMeta) {
			data = ko.toJS(e);
			self.record(new Record(data, showMeta));
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
			$('.nav-tabs a[href="#information"]').tab('show');
			$(".mediathumb > img").attr("src","");
			$('body').css('overflow','hidden');
			adjustHeight();
			WITHApp.tabAction();
		};

		self.close = function () {
			//self.record(new Record());
			$('body').css('overflow','visible');
			$( '.itemview' ).fadeOut();
			var vid = document.getElementById("mediaplayer");
			 if (vid != null) {
				 vid.parentNode.removeChild(vid);
			}
			 $('#mediadiv').html("");
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
		
		self.goToCollection = function(collection) {
			if (collection.isExhibition) {
				window.location = '#exhibition-edit/'+ collection.dbId;		
		
			}
			
			else {
		
				window.location.href = 'index.html#collectionview/' + collection.dbId;		
			}	
			
			if (isOpen){
				//toggleSearch(event,'');
			}
			self.close();
		};
		
		
			self.likeRecord = function (rec,event) {
        	event.preventDefault();
        	var $heart=$(event.target);
        	if(!app.currentUser._id()) {
    			window.location.href = "/assets/index.html#login";
    			return;
    		}
        	app.likeItem(rec, function (status) {
				if (status) {
					$heart.addClass('redheart');
					if($( '[class*="'+rec.externalId+'"]' ) || $( '[class*="'+rec.recordId+'"]')){
					//if($( "." + rec.externalId ) || $( "." + rec.recordId)){
						$( '[class*="'+rec.externalId+'"]' ).find("a.fa-heart").css("color","#ec5a62");
						$( '[class*="'+rec.recordId+'"]').find("a.fa-heart").css("color","#ec5a62");
					}	
				} else {
				    $heart.removeClass('redheart');
					
				    if($( '[class*="'+rec.externalId+'"]' ) || $( '[class=*"'+rec.recordId+'"]' )){
							
						$( '[class*="'+rec.externalId+'"]').find("a.fa-heart").css("color","");
						$( '[class*="'+rec.recordId+'"]' ).find("a.fa-heart").css("color","");
						
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
				"data": "format=noContent",
				"method": "get",
				"contentType": "application/json",
				"success": function (result) {
					var record = new Record(formatRecord(result));
					self.record(record);
					self.open();
					self.addDisqus();
				},
				error: function (xhr, textStatus, errorThrown) {
					
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
		
		$('#itemTabs a').click(function (e) {
		     $(this).tab('show');
		   })
		
	}
	
	
	return {
		viewModel: ItemViewModel,
		template: template
	};
});
