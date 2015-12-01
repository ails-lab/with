define(['bridget', 'knockout', 'text!./collection-view.html', 'isotope', 'imagesloaded', 'app', 'smoke'], function (bridget, ko, template, Isotope, imagesLoaded, app) {

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
		self.rights="";
		self.dataprovider="";
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
			self.dataprovider=data.dataprovider;
			self.source=data.source;
			self.creator=data.creator;
			self.provider=data.provider;
			self.rights=data.rights;
			self.recordId=data.id;
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
			if(self.provider!==undefined && self.provider.length>0 && self.provider!=self.creator)
				distitle+=", "+self.provider;
			return distitle;
		});

		if(data != undefined) self.load(data);
		
	}

	function CViewModel(params) {
		document.body.setAttribute("data-page","collection");
		
		var self = this;

		var $container = $(".grid");
		self.route = params.route;
		var counter = 1;
		self.collname = ko.observable('');
		self.access = ko.observable("READ");
		self.id = ko.observable(params.id);
		self.creator = ko.observable('');
		self.ownerId = ko.observable(-1);
		self.itemCount = ko.observable(0);
		self.citems = ko.observableArray();

		self.description = ko.observable('');
		self.selectedRecord = ko.observable(false);

		self.loading = ko.observable(false);

		self.next = ko.observable(-1);
		self.desc = ko.showMoreLess('');
		self.showAPICalls = ko.observable(false);
		self.revealItems = function (data) {
			if(data.length==0){ self.loading(false);}
			else{
				for (var i in data) {
					var result = data[i];
					var record = new Record({
						id: result.dbId,
						thumb: result.thumbnailUrl,
						description: result.description!=null ? result.description : result.title,
						title: result.title,
						view_url: result.sourceUrl,
						creator: result.creator,
						provider: result.provider,
						dataprovider: result.dataProvider,
						source: result.source,
						rights: result.rights,
						externalId: result.externalId
					});
					self.citems().push(record);
				}
				self.citems.valueHasMutated();}
			
		};

		self.loadCollection = function (id) {
			self.loading(true);
			
			$.ajax({
				"url": "/collection/" + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					self.collname(data.title);
					self.desc(data.description);
					self.creator(data.creator);
					self.ownerId(data.ownerId);
					self.itemCount(data.itemCount);
					self.access(data.access);
					self.revealItems(data.firstEntries);
				},
				error: function (xhr, textStatus, errorThrown) {
					self.loading(false);
					$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
				}
			});
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
			self.moreItems();
		};

		self.moreItems = function () {
			if (self.loading === true) {
				setTimeout(self.moreItems(), 300);
			}
			if (self.loading() === false) {
				self.loading(true);
				var offset = self.citems().length;
				$.ajax({
					"url": "/collection/" + self.id() + "/list?count=40&start=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						self.revealItems(data.records);
						
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
		};
		self.recordSelect= function (e){
        	$( '.itemview' ).fadeIn();
			itemShow(e);

		}

		

		self.addCollectionRecord = function (e) {
			self.citems.push(e);
		};

		self.removeRecord = function (e) {
			$.smkConfirm({text:'Are you sure you want to permanently remove this item?', accept: 'Delete', cancel: 'Cancel'}, function (ee) {
				if (ee) {
					$.ajax({
						url: '/collection/' + self.id() + '/removeRecord?recId=' + e,
						type: 'DELETE',
						contentType: "application/json",
						data: JSON.stringify(e),
						success: function (data, textStatus, xhr) {
							//find item index to see if it is first item
							var index=ko.utils.arrayIndexOf(self.citems(),e);
							console.log("index:"+index);

							self.citems.remove(e);
							if ($("#" + e)) {
								$container.masonry( 'remove', $("#" + e)).masonry( 'layout');
							}

							self.itemCount(self.itemCount() - 1);
							$.smkAlert({text:'Item removed from the collection', type:'success'});

						},
						error: function (xhr, textStatus, errorThrown) {
							$.smkAlert({text:'An error has occured', type:'danger', time: 10});
						}
					});
				} else {

				}
			});
		};

		self.likeRecord = function (rec) {
			
			app.likeItem(rec, function (status) {
				if (status) {
					$('#' + rec.recordId).addClass('active');
				} else {
					$('#' + rec.recordId).removeClass('active');
				}
			});
		};

		

		self.uploadItem = function() {
			app.showPopup('image-upload', { collectionId: self.id() });
		};

		
		
		 self.getAPIUrlCollection = function() {
				var url   = window.location.href.split("assets")[0];
				var collectionCall = url + "collection/" + self.id();
				return collectionCall;
		}
		 
		 self.getAPIUrlRecords = function() {
				var url   = window.location.href.split("assets")[0];
				var recordsCall = url + "collection/" + self.id()+"/list?start=0&count=20&format=default";
				return recordsCall;
		}
		 
		self.presentAPICalls = function() {
			 if (self.showAPICalls())
				 self.showAPICalls(false);
			 else
				 self.showAPICalls(true);
		}

	}

	return {
		viewModel: CViewModel,
		template: template
	};
});
