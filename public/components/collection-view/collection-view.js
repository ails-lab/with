define(['bridget', 'knockout', 'text!./_collection-view.html', 'isotope', 'imagesloaded', 'app', 'inputtags', 'liveFilter', 'smoke','knockout-validation'], function (bridget, ko, template, Isotope, imagesLoaded, app, inputtags, liveFilter) {

	$.bridget('isotope', Isotope);
	self.loading = ko.observable(false);

	ko.bindingHandlers.collectionIsotope = {
		init: app.initOrUpdate('init'),
		update: app.initOrUpdate('update')
	};
	
	ko.validation.init({
		errorElementClass: 'error',
		errorMessageClass: 'errormsg',
		decorateInputElement: true
	});

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

		self.annotations = [];
		self.title = "";
		self.description = "";
		self.thumb = "";
		self.fullres = "";
//		self.fullrestype = "";
		self.view_url = "";
		self.source = "";
		self.creator = "";
		self.provider = "";
		self.dataProvider = "";
		self.dataProvider_uri = "";
		self.rights = "";
		self.mediatype="";
		self.url = "";
		self.externalId = "";
		self.thumbnail = "";
		self.likes = 0;
		self.collected = 0;
		self.collectedIn = [];
		self.position = 0;
		self.dbId = "";
		self.data = ko.observable('');
		self.thumbnail = ko.pureComputed(function () {

			if (self.thumb) {
				return self.thumb;
			} else {
				return "img/ui/ic-noimage.png";
			}
		});

		self.fullresolution = ko.pureComputed(function () {

			if (self.fullres) {
				return self.fullres;

			} else {
				return self.thumbnail();
			}
		});

		self.sourceCredits = ko.pureComputed(function () {
			switch (self.source) {
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
			case "DBPedia":
				return "dbpedia.org";
			default:
				return "";
			}
		});

		self.displayTitle = ko.pureComputed(function () {
			var distitle = "";
			distitle = self.title;
			if (self.creator && self.creator.length > 0) {
				distitle += ", by " + self.creator;
			}
			if (self.dataProvider && self.dataProvider.length > 0 && self.dataProvider != self.creator) {
				distitle += ", " + self.dataProvider;
			}
			return distitle;
		});

		self.isLiked = ko.pureComputed(function () {
			return app.isLiked(self.externalId);
		});

		self.load = function (options) {
			var admindata = options.administrative;
			var descdata = options.descriptiveData;
			var media = options.media;
			var provenance = options.provenance;
			var usage = options.usage;

			if (descdata) {
				self.title = findByLang(descdata.label);
				self.description = findByLang(descdata.description);
				self.creator = findByLang(descdata.dccreator);
			}

			self.dbId = options.dbId;
			if (provenance) {
				self.view_url = findProvenanceValues(provenance, "source_uri");
				self.dataProvider = findProvenanceValues(provenance, "dataProvider");
				self.provider = findProvenanceValues(provenance, "provider");
				self.source = findProvenanceValues(provenance, "source");
			}

			self.externalId = admindata.externalId;
			self.collectedIn = options.collectedIn;
			if (usage) {
				self.likes = usage.likes;
				self.collected = usage.collected;
			}

			self.annotations = options.annotations;			
			
			
			if(self.source=="Rijksmuseum" && media){
				media[0].Thumbnail=media[0].Original;
			}
			self.thumb = media[0] != null && media[0].Thumbnail != null && media[0].Thumbnail.withUrl != "null" ? media[0].Thumbnail.withUrl : null;
			self.fullres = media[0] != null && media[0].Original != null && media[0].Original.url != "null" ? media[0].Original.url : null;
//			self.fullrestype = media[0] != null && media[0].Original != null && media[0].Original.type != "null" ? media[0].Original.type : null;
			
			if(self.fullres){
				self.rights=findResOrLit(media[0].Original.originalRights);
				
			}
			else if (self.thumb){
				self.rights=findResOrLit(media[0].Thumbnail.originalRights);
				
			}
			
			if(media &&  media[0]){
				if(media[0].Original && media[0].Original.type){
					self.mediatype=media[0].Original.type;
				}else if(media[0].Thumbnail && media[0].Thumbnail.type){
					self.mediatype=media[0].Thumbnail.type;
				}
			}
			self.data(options);
			self.isLoaded = ko.observable(true);
			self.fullrestype = media[0] != null && media[0].Original != null 
			&& media[0].Original.type != "null" ? media[0].Original.type : null; 
		};

		if (data !== undefined) {
			self.load(data);
		}
		

	}

	function CViewModel(params) {
		
		var self = this;
		self.route = params.route;
		var counter = 1;
		
		self.indexCount = ko.observable("");
		self.terms = ko.observableArray([]);
		self.keywords = ko.observableArray([]);
		self.keyword = ko.observable('');
		
		self.title = ko.observable('');
		self.titleToEdit = ko.observable('').extend({ required: true });
		self.description = ko.observable('');
		self.descriptionToEdit = ko.observable('');
		self.isPublicEdit = ko.observable(true);
		self.access = ko.observable("READ");
		self.id = ko.observable(params.id);
		self.creator = ko.observable('');
		self.ownerId = ko.observable(-1);
		self.entryCount = ko.observable(0);
		self.citems = ko.observableArray();
		self.selectedRecord = ko.observable(false);
		self.loggedUser = isLogged();
		self.rightsmap = ko.mapping.fromJS([]);
		self.isFavorites = ko.observable(false);
		
		$('#term_tags').tagsinput({
	        allowDuplicates: false,
	          itemValue: 'uri',  
	          itemText: 'label' 
	      });
		
	    $('#term_tags').on('itemRemoved', function(event) {
			for (var i = 0; i < self.terms().length; i++) {
				if (self.terms()[i].uri() === event.item.uri) {
					self.terms.splice(i,1);
					self.reload();
					break;
				}
			}
		});
	    
		self.closeSide = function () {
			$('.action').removeClass('active');
		};

		self.vocabularyIndex = [];
		self.vocabularies = ko.observableArray();
		self.selvocabularies = ko.observableArray();
		self.ner = ko.observableArray();
		
	    self.sourceBind=function(e){
	    	$("ul.list>li").css('display','block');
	    }

	    
		$.ajax({
         	"url": "/thesaurus/listThesauri",
			"method": "get",
			"contentType": "application/json",
        	"success": function (data){
           		for (x in data) {
           			if (data[x].annotator != "null") {
           				if (data[x].type === "thesaurus") {
           					self.vocabularyIndex[data[x].vocabulary] = data[x];
           					self.vocabularies.push(data[x]);
           					self.selvocabularies.push(data[x].vocabulary);
           				} else if (data[x].type === "ner") {
           					self.vocabularyIndex[data[x].vocabulary] = data[x];
           					self.ner.push(data[x]);
           					self.selvocabularies.push(data[x].vocabulary);
           					
           				}
           			}
           		}
        	}
     	});
		
	    self.selectVocabulary = function(data, event){
	    	if(event.target.checked){
	    		var id = $(event.target).attr('value');
	    		
				for (var i = 0; i < self.selvocabularies().length; i++) {
					if (self.selvocabularies()[i] === id) {
						return true;
					}
				}
				
				self.selvocabularies.push(id);
	    		
	    	}
	    	
	    	if(!event.target.checked){
	    		var id = $(event.target).attr('value');
	    		
				for (var i = 0; i < self.selvocabularies().length; i++) {
					if (self.selvocabularies()[i] === id) {
						self.selvocabularies.splice(i,1);
						return true;
					}
				}
	    		
	    	}
	    	
//			for (var i = 0; i < self.selvocabularies().length; i++) {
//				alert(self.selvocabularies()[i]);
//			}
	    	
	    	return true;
	    }

	    self.createAnnotateRequest = function(data) {
			var vocitems = [];
			var neritems = [];
	    	
	    	for (var i = 0; i < data.length; i++) {
	    		var voc = self.vocabularyIndex[data[i]];
	    		
	    		if (voc.type == "thesaurus") {
	    			vocitems.push(data[i]);
	    		} else {
	    			neritems.push(data[i])
	    		}
	    	}
	    	
			return "{ \"vocabulary\": " + JSON.stringify(vocitems) + ", \"ner\": " + JSON.stringify(neritems) +"}";
	    }
	    
		self.annotateCollection = function() {
			$.ajax({
				"url": "/collection/" + self.id() + "/annotate",
				"method": "post",
				"data" : self.createAnnotateRequest(self.selvocabularies()),
				"contentType": "application/json",
				"success": function (data) {
					$.smkAlert({
						text: 'Annotating started',
						type: 'success',
						permanent: false
					});
				},
				"error": function (result) {
					$.smkAlert({
						text: 'An error has occured',
						type: 'danger',
						permanent: true
					});
				}
			});
			$('.action').removeClass('active');
		}

	    self.sourceBind=function(e){
	    	$("ul.list>li").css('display','block');
	    }

		var Term = function(data) {
			var selfx = this;
			
			selfx.uri = ko.observable();
			selfx.label = ko.observable();
			selfx.subterms = ko.observableArray([]);
			
			ko.mapping.fromJS(data, {}, selfx);
		}

		self.onEnter = function(data,event){
		    event.keyCode === 13 && self.addKeyword(data);  
		    return true;
		};
		
		self.addKeyword = function(data) {
			
			for (var i = 0; i < self.terms().length; i++) {
				if (self.terms()[i].uri() == self.keyword()) {
					return;
				}
			}

			self.terms.push(new Term({ uri: self.keyword(), label: self.keyword() }));
    	    $("#term_tags").tagsinput('add',{ uri: self.keyword(), label: self.keyword() });

    	    self.keyword('');
			self.reload();
		};

		var NodeModel = function(data) {
			var selfx = this;
			
			selfx.isExpanded = ko.observable(false);
			selfx.uri = ko.observable();
			selfx.label = ko.observable();
			selfx.children = ko.observableArray();
			selfx.id = ko.observable(); 
			selfx.size = ko.observable();
		 
			selfx.sizelabel = ko.pureComputed(function () {
				return selfx.label + "(" + selfx.size + ")";
			});
			
			selfx.toggleVisibility = function() {
				selfx.isExpanded(!selfx.isExpanded());
			};
		
			ko.mapping.fromJS(data, selfx.mapOptions, selfx);

			selfx.addTerm = function() {
				for (var i = 0; i < self.terms().length; i++) {
					if (self.terms()[i].uri() === data.uri) {
						return;
					}
				}

//				$('#' + selfx.id()).hide();
				self.terms.push(new Term({ uri: data.uri, label: data.label, subterms: selfx.collect() }));
	    	    $("#term_tags").tagsinput('add',{ uri: data.uri, label: data.label });

				self.reload();
			};
			
			selfx.collect = function() {
				var result = [];
				result.push(selfx.uri());
				for (var i in selfx.children()) {
					var cc = selfx.children()[i].collect();
					for (j in cc) {
						result.push(cc[j]);
					}
				}
				return result;
			}
			
			selfx.isSelected =  ko.computed(function() {
				for (var i in self.terms()) {
					if (self.terms()[i].uri() === data.uri) {
						return true;
					}
				}
				
				return false;
			});
		};
		
		NodeModel.prototype.mapOptions = {
				children: {
					create: function(args) {
						return new NodeModel(args.data);
					}
				}
			};
		
		self.loadIndex = function() {
			$.ajax({
				"url": "/collection/" + self.id() + "/facets",
				"method": "post",
				"data" : convertTerms(self.terms()),
				"contentType": "application/json",
				"success": function (data) {
					self.index(new NodeModel(data));
				},
				"error": function (result) {
					$.smkAlert({
						text: 'An error has occured',
						type: 'danger',
						permanent: true
					});
				}
			});
		}

		self.reload = function() {
			loading(true);

			self.index(new NodeModel({ schemes: [] }));
			
			if (self.citems().length > 0) {
				
				self.citems.removeAll();
				
				var $elements = self.$container.isotope('getItemElements')
				self.$container.isotope('remove', $elements).isotope('layout');
//				self.revealItems([]);

			}
			
			self.loadIndex();
			self.loadInit();
		}
		
		self.index = ko.observable(new NodeModel({ schemes: [] }));

		self.fetchitemnum = 20;
		self.isPublic = ko.observable(true);
		self.query = ko.observable('');
		self.validationModel = ko.validatedObservable({
			title: self.titleToEdit
		});
		self.displayTitle = ko.pureComputed(function () {
			return self.isFavorites() ? 'Favorites' : self.title();
		});
		self.itemCount = ko.pureComputed(function () {
			if (self.entryCount() === 1) {
				return self.entryCount() + ' Item';
			} else {
				return self.entryCount() + ' Items';
			}
		});

		if (params.type === 'favorites') {
			self.isFavorites(true);
			self.id(currentUser.favoritesId());
		}

		if (params.count) {
			self.count = ko.observable(params.count);
		} else {
			self.count = ko.observable(20);
			sessionStorage.removeItem("collection-viewscroll" + self.id());
		}

		self.next = ko.observable(-1);
		self.desc = ko.showMoreLess('');
		self.showAPICalls = ko.observable(false);
		self.$container = $(".grid#" + self.id()).isotope({
			itemSelector: '.item',
			transitionDuration: transDuration,
			masonry: {
				columnWidth: '.sizer',
				percentPosition: true
			}
		});
		ko.bindingHandlers.filterItems = {
				update: function (elem, valueAccessor, allBindingsAccessor, viewModel, context) {
					var q = ko.utils.unwrapObservable(valueAccessor());
					self.$container.isotope({ filter : function() {
					    var title = $(this).find('.title').text();
					     return title.toLowerCase().indexOf(q.toLowerCase()) == 0;
					  }
					});
				}
			};
		self.revealItems = function (data) {
			if ((data.length === 0 || data.length < self.fetchitemnum)) {
				loading(false);$(".loadmore").text("no more results");
			} else {
				$(".loadmore").text("Load more");
			}

			var items = [];
			for (var i in data) {
				var result = data[i];
				if (result != null) {
					var record = new Record(result);

					items.push(record);
					self.citems.push(record);
				}
			}
			//self.citems.push.apply(self.citems, items);
			var offset = self.citems().length;
			var new_url = "";
			if (window.location.hash.indexOf("collectionview") == 1) {
				if (window.location.hash.indexOf("count") == -1) {
					new_url = window.location.pathname + window.location.hash + "/count/" + offset;
				} else {
					temphash = window.location.hash.substring(0, window.location.hash.lastIndexOf("/"));
					new_url = window.location.pathname + temphash + "/" + offset;
				}
			}
			history.replaceState('', '', new_url);

			return items;
		};

		self.loadCollection = function (id) {
			loading(true);

			$.ajax({
				"url": '/collection/' + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					if (data.administrative.access.isPublic === false) {
						self.isPublic(false);
						self.isPublicEdit(false);
						if (isLogged() === false) {
							window.location = '#login';
							return;
						}
					}
					if (data.descriptiveData) {
						self.title(findByLang(data.descriptiveData.label));
						self.titleToEdit(findByLang(data.descriptiveData.label));
						self.description(findByLang(data.descriptiveData.description));
						self.descriptionToEdit(findByLang(data.descriptiveData.description));
					}

					self.entryCount(data.administrative.entryCount);
					self.creator = ko.observable("");
					if (data.administrative.withCreator) {
						ko.computed(function () {
							var params = {};
							$.getJSON('/user/' + data.administrative.withCreator, params, self.creator);
						}, this.username);
					}

					self.desc(self.description());
					if (data.administrative.access) {
						ko.mapping.fromJS(data.administrative.access.acl, self.rightsmap);

						var rightsrec = ko.utils.arrayFirst(self.rightsmap(), function (right) {
							return right.user() === currentUser._id();
						});
						if (rightsrec) {
							self.access(rightsrec.level());
						}
					}
					
					self.loadIndex();
					
					if (self.count() && self.count() > 0) {
						self.loadInit();
					} else {
						loading(false);
					}
					
					$("#term_tags").tagsinput('removeAll');
				},
				error: function (xhr, textStatus, errorThrown) {
					loading(false);
					if (xhr.status == "403") {
						window.location = '#login';
						return;
					}

					$.smkAlert({
						text: 'An error has occured',
						type: 'danger',
						permanent: true
					});
				}
			});
		};
		
		self.loadInit = function() {
			if (self.terms().length == 0) {
				$.ajax({
					"url": "/collection/" + self.id() + "/list?count=" + self.count() + "&start=0",
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						self.indexCount("");

						var items = self.revealItems(data.records);
						if (items.length > 0) {
							var $newitems = getItems(items);

							self.isotopeImagesReveal(self.$container, $newitems);
						}
						loading(false);
					},
					"error": function (result) {
						loading(false);
						$.smkAlert({
							text: 'An error has occured',
							type: 'danger',
							permanent: true
						});
					}
				});
			} else {
				$.ajax({
					"url": "/collection/" + self.id() + "/selectionlist?count=" + self.count() + "&start=0",
					"method": "post",
					"contentType": "application/json",
					"data" : convertTerms(self.terms()),
					"success": function (data) {
						self.indexCount("(" + data.entryCount + " matches)");

						var items = self.revealItems(data.records);
						if (items.length > 0) {
							var $newitems = getItems(items);

							self.isotopeImagesReveal(self.$container, $newitems);
						}
						loading(false);
					},
					"error": function (result) {
						loading(false);
						$.smkAlert({
							text: 'An error has occured',
							type: 'danger',
							permanent: true
						});
					}
				});
			}
		}

		
		WITHApp.initTooltip();
		self.loadCollection();
		self.isOwner = ko.pureComputed(function () {
			if (app.currentUser._id() == self.withCreator) {
				return true;
			} else {
				return false;
			}
		});

	

		self.loadNext = function () {
			if (loading() === false) {
				loading(true);

				var promise = self.moreItems();
				$.when(promise).done(function (data) {
					var items = self.revealItems(data.records);
					if (items.length > 0) {
						var $newitems = getItems(items);

						self.isotopeImagesReveal(self.$container, $newitems);

					}
					loading(false);
				});
			}
		};

		self.moreItems = function () {
			if (self.terms().length == 0) {
				return $.ajax({
					type: "GET",
					contentType: "application/json",
					dataType: "json",
					url: "/collection/" + self.id() + "/list",
					processData: false,
					data: "count=" + self.fetchitemnum + "&start=" + self.citems().length
				}).success (function () {
					self.indexCount("");
				});
				} else {
					return $.ajax({
						type: "POST",
						contentType: "application/json",
						dataType: "json",
						url: "/collection/" + self.id() + "/selectionlist?count=" + self.fetchitemnum + "&start=" + self.citems().length,
						processData: false,
						data : convertTerms(self.terms()) 
					}).success (function (data) {
						self.indexCount("(" + data.entryCount + " matches)");
					});
				}
		};

		function convertTerms(terms) {
			var uitems = [];
			var kitems = [];
			
			for (i in terms) {
				var str = terms[i].uri();
				
				if (str.substring(0, 7) === "http://") {
					uitems.push({ top: str, sub: terms[i].subterms() });
				} else {
					kitems.push(str);
				}
			}

			return "{ \"terms\": " + JSON.stringify(uitems) + ", \"keywords\": " + JSON.stringify(kitems) +"}";
		}
		
		self.addCollectionRecord = function (e) {
			self.citems.push(e);
		};

		removeRecord = function (id, event) {
			var $elem = $(event.target).parents(".item");
			$.smkConfirm({
				text: 'Are you sure you want to permanently remove this item?',
				accept: 'Delete',
				cancel: 'Cancel'
			}, function (ee) {
				if (ee) {
					$.ajax({
						url: '/collection/' + self.id() + '/removeRecord?' + $.param({
							"recId": id,
							"all": false
						}),
						type: 'DELETE',
						contentType: "application/json",
						data: JSON.stringify({
							recId: id,
							all: false
						}),
						success: function (data, textStatus, xhr) {
							var foundrec = ko.utils.arrayFirst(self.citems(), function (item) {
								return item.dbId == id;
							});
							self.citems.remove(foundrec);
							if ($elem) {
								self.$container.isotope('remove', $elem).isotope('layout');
							}

							self.reloadEntryCount();
							$.smkAlert({
								text: 'Item removed from the collection',
								type: 'success'
							});
						},
						error: function (xhr, textStatus, errorThrown) {
							$.smkAlert({
								text: 'An error has occured',
								type: 'danger',
								time: 10
							});
						}
					});
				} else {
					// Empty
				}
			});
		};

		self.getAPIUrlCollection = function () {
			var url = window.location.href.split("assets")[0];
			var collectionCall = url + "collection/" + self.id();
			return collectionCall;
		};

		self.getAPIUrlRecords = function () {
			var url = window.location.href.split("assets")[0];
			var recordsCall = url + "collection/" + self.id() + "/list?start=0&count=20&format=default";
			return recordsCall;
		};

		likeRecord = function (id, event) {
			event.preventDefault();
			var rec = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.dbId === id;
			});

			app.likeItem(rec, function (status) {
				if (status) {
					$('[class*="' + id + '"]').find("a.fa-heart").css("color", "#ec5a62");
				} else {
					$('[class*="' + id + '"]').find("a.fa-heart").css("color", "");
				}
			});
		};

		collect = function (id, event) {
			event.preventDefault();
			var rec = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.dbId === id;
			});

			collectionShow(rec);
		};
		
		similar = function (id, event) {
			event.preventDefault();
			var rec = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.dbId === id;
			});

			loading(true);

			self.index(new NodeModel({ schemes: [] }));
			
			if (self.citems().length > 0) {
				
				self.citems.removeAll();
				
				var $elements = self.$container.isotope('getItemElements')
				self.$container.isotope('remove', $elements).isotope('layout');
//				self.revealItems([]);
			}
			
			self.loadIndex();
			self.similarLoadInit(rec.dbId);

		};
		
		self.similarLoadInit = function(rec) {
//			if (self.terms().length == 0) {
				$.ajax({
					"url": "/collection/" + self.id() + "/similarlist?itemid=" + rec + "&count=" + self.count() + "&start=0",
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						self.indexCount("");

						var items = self.revealItems(data.records);
						if (items.length > 0) {
							var $newitems = getItems(items);

							self.isotopeImagesReveal(self.$container, $newitems);
						}
						loading(false);
					},
					"error": function (result) {
						loading(false);
						$.smkAlert({
							text: 'An error has occured',
							type: 'danger',
							permanent: true
						});
					}
				});
//			} else {
//				$.ajax({
//					"url": "/collection/" + self.id() + "/indexedlist?count=" + self.count() + "&start=0",
//					"method": "post",
//					"contentType": "application/json",
//					"data" : convertTerms(self.terms()),
//					"success": function (data) {
//						self.indexCount("(" + data.entryCount + " matches)");
//
//						var items = self.revealItems(data.records);
//						if (items.length > 0) {
//							var $newitems = getItems(items);
//
//							self.isotopeImagesReveal(self.$container, $newitems);
//						}
//						loading(false);
//					},
//					"error": function (result) {
//						loading(false);
//						$.smkAlert({
//							text: 'An error has occured',
//							type: 'danger',
//							permanent: true
//						});
//					}
//				});
//			}
		}
		
		self.deleteMyCollection = function () {
			$.smkConfirm({
				text: 'Are you sure you want to permanently remove this collection and all the containing records?',
				accept: 'Delete',
				cancel: 'Cancel'
			}, function (ee) {
				if (ee) {
					$.ajax({
						"url": "/collection/" + self.id(),
						"method": "DELETE",
						success: function (result) {
							$.smkAlert({text: 'Collection removed', type: 'success'});
							
							var thecollection = ko.utils.arrayFirst(currentUser.editables(), function (c) {
								return c.dbId === self.id();
							});
							if(thecollection)
							  currentUser.editables.remove(thecollection);
							window.location.href="/assets/index.html#mycollections";
							loadCounters();
								
						}
					    ,
						error: function (xhr, textStatus, errorThrown) {
							$.smkAlert({
								text: 'An error has occured while removing this collection',
								type: 'danger',
								time: 10
							});
						}
					});
				} else {
					// Empty
				}
			});
		};

		
		

		function getItem(record) {
			var tile = '<div class="item ' + record.dbId + '"><div class="wrap"><a href="#"  onclick="recordSelect(\'' + record.dbId + '\',event)"><div class="thumb"><img style="width:100%" src="' + record.thumbnail() + '" onError="this.src=\'img/ui/ic-noimage.png\'"/></a></div>';
			tile += '<div class="info"><h2 class="title">' + record.title + '</h2>';
			
			var distitle = "";
			if (record.creator && record.creator.length > 0) {
				distitle = "by " + record.creator;
			}
			else if (record.dataProvider && record.dataProvider.length > 0 && record.dataProvider != record.creator) {
				distitle = record.dataProvider;
			}
			tile+='<span class="source">'+distitle+'</source><a href="' + record.view_url + '" target="_new" class="links">' + record.sourceCredits() + '</a></div>';
			tile += '<div class="action-group"><div class="wrap">';
			tile+="<ul>";
			if (isLogged()) {
				if ((self.access() == "WRITE" || self.access() == "OWN")) {
					tile += '<li><a  data-toggle="tooltip" data-placement="top" title="Remove media" class="fa fa-trash-o"  onclick="removeRecord(\'' + record.dbId + '\',event)"></a></li>';
				}
				if(!self.isFavorites()){
				if (record.isLiked()) {
					tile += '<li><a data-toggle="tooltip" data-placement="top" title="Remove from favorites"  onclick="likeRecord(\'' + record.dbId + '\',event);" class="fa fa-heart" style="color: #ec5a62;"></a></li>';
				} else {
					tile += '<li><a  data-toggle="tooltip" data-placement="top" title="Add to favorites" onclick="likeRecord(\'' + record.dbId + '\',event);" class="fa fa-heart"></a></li>';
				}}
				tile += '<li><a data-toggle="tooltip" data-placement="top" title="Collect it" class="fa fa-download collectbutton" onclick="collect(\'' + record.dbId + '\',event);" ></a></li>';

			}
			tile += '<li><a data-toggle="tooltip" data-placement="top" title="Sort by similarity" class="fa fa-sort" onclick="similar(\'' + record.dbId + '\',event);" ></a></li>';
			tile += '</ul>';

			tile += "</div></div></div></div>";
			
			
			
			
			return tile;
		}

		function getItems(data) {
			var items = '';
			for (var i in data) {
				items += getItem(data[i]);
			}
			return $(items);
		}

		recordSelect = function (data, event) {

			event.preventDefault();
			var selrecord = ko.utils.arrayFirst(self.citems(), function (record) {
				return record.dbId === data;
			});
			itemShow(selrecord);
			return false;
		};

		self.reloadEntryCount = function () {
			$.ajax({
				"url": "/collection/" + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {

					self.entryCount(data.administrative.entryCount);

				}
			});
		};

		self.editCollection = function () {
			if (self.validationModel.isValid()) {
			var jsondata = JSON.stringify({
				resourceType: "SimpleCollection",
				descriptiveData: {
					label: {default: [self.titleToEdit()]},
					description: {default: [self.descriptionToEdit()]},
				},
				administrative: { access: {
					isPublic: self.isPublicEdit()},
				}
				
			});
			$.ajax({
				"url": "/collection/" + self.id(),
				"method": "PUT",
				"contentType": "application/json",
				"data": jsondata,
				success: function (result) {

					self.title(self.titleToEdit());
					self.description(self.descriptionToEdit());
					self.desc(self.description());
					self.isPublic(self.isPublicEdit());
					$('.action').removeClass('active');
					$.smkAlert({
						text: 'Collection updated',
						type: 'success'
					});

				},
				error: function (error) {
					$.smkAlert({
						text: 'An error has occured while updating this collection',
						type: 'danger',
						time: 5
					});
				}
			});
		} else {
			self.validationModel.errors.showAllMessages();
		}
		}
		

		self.isotopeImagesReveal = function ($container, $items) {
			self.$container = $(".grid#" + self.id());
			var iso = self.$container.data('isotope');
			var itemSelector = ".item";
			if (iso) {
				itemSelector = iso.options.itemSelector;
			} else {
				self.$container = $(".grid#" + self.id()).isotope({
					itemSelector: '.item',
					transitionDuration: transDuration,
					masonry: {
						columnWidth: '.sizer',
						percentPosition: true

					}
				});
				iso = self.$container.data('isotope');
				//itemSelector = iso.options.itemSelector;
			}
			// append to container
			self.$container.append($items);
			WITHApp.tabAction();

			// hide by default
			$items.hide();
			$items.imagesLoaded().progress(function (imgLoad, image) {
				// get item
				var $item = $(image.img).parents(itemSelector);
				// un-hide item
				$item.show();
				if(iso)
					  iso.appended($item);
					else{
						$.error("iso gone");
					}
				
				self.$container.isotope('layout');
				var scrollpos = sessionStorage.getItem("collection-viewscroll" + self.id());
				if (scrollpos && $(".grid#" + self.id()).height() > scrollpos) {
					$(window).scrollTop(scrollpos);
					sessionStorage.removeItem("collection-viewscroll" + self.id());
				} else if (scrollpos && $(".grid#" + self.id()).height() < scrollpos) {
					$(window).scrollTop($(".grid#" + self.id()).height());

				}

			}).always(function () {
				var scrollpos = sessionStorage.getItem("collection-viewscroll" + self.id());
				if (scrollpos && $(".grid#" + self.id()).height() > scrollpos) {
					$(window).scrollTop(scrollpos);
					sessionStorage.removeItem("collection-viewscroll" + self.id());
				} else if (scrollpos && $(".grid#" + self.id()).height() < scrollpos) {
					$(window).scrollTop($(".grid#" + self.id()).height());
					if (scrollpos != null && $(".grid#" + self.id()).height() > scrollpos) {
						sessionStorage.removeItem("collection-viewscroll" + self.id());
					}
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
