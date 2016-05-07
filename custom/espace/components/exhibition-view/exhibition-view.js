define(['knockout', 'text!./exhibition-view.html', 'app', 'magnific-popup', 'slick'], function (ko, template, app, magnificPopup, slick) {

	ko.bindingHandlers.backgroundImage = {
		update: function (element, valueAccessor, allBindingsAccessor, viewModel, context) {
			ko.bindingHandlers.style.update(element,
				function () {
					return {
						backgroundImage: "url('" + viewModel.backgroundImgWithUrl() + "')"
					};
				});
		}
	};

	function Record(data) {
		var self = this;
		self.recordId = "";
		self.title = "";
		self.description = "";
		self.thumb = "";
		self.fullres = ko.observable("");
		self.view_url = "";
		self.source = "";
		self.creator = "";
		self.provider = "";
		self.rights = "";
		self.url = "";
		self.externalId = "";
		self.isLoaded = ko.observable(false);

		self.load = function (options) {
			var admindata = options.administrative;
			var descdata = options.descriptiveData;
			var media = options.media;
			var provenance = options.provenance;
			var usage = options.usage;

			if (descdata) {
				self.title = findByLang(descdata.label);
				self.description = findByLang(descdata.description);
				self.rights = findResOrLit(descdata.metadataRights);
                if (options.withCreator != null) {
				    self.creator = options.withCreatorInfo.username;
                }
			}

			self.dbId = options.dbId;
			if (provenance) {
				self.view_url = findProvenanceValues(provenance, "source_uri");
				self.dataProvider = findProvenanceValues(provenance, "dataProvider");
				self.provider = findProvenanceValues(provenance, "provider");
				self.source = findProvenanceValues(provenance, "source");
			}
			self.externalId = admindata.externalid;
			if (usage) {
				self.likes = usage.likes;
				self.collected = usage.collected;
				self.collectedIn = usage.collectedIn;
			}

			self.thumb = media[0] != null && media[0].Thumbnail != null && media[0].Thumbnail.url != "null" ? media[0].Thumbnail.url : "img/content/thumb-empty.png";
			//self.fullres = media[0] != null && media[0].Original != null && media[0].Original.url != "null" ? media[0].Original.url : null,
			if(media[0] != null && media[0].Original != null && media[0].Original.url != "null"){
				self.fullres(media[0].Original.url);
			}
		    self.isLoaded = ko.observable(false);
			if(self.fullres()==null || self.fullres().length==0){
				self.fullres(self.thumb);
			}
		};

		
		if (data != undefined) self.load(data);
	}

	function EViewModel(params) {
		document.body.setAttribute("data-page", "exhibition");
		$("div[role='main']").toggleClass("homepage", false);

		var self = this;

		var $container = $(".grid");
		self.route = params.route;
		var counter = 1;
		self.exhName = ko.observable('');
		//self.access = ko.observable([]);
		self.id = ko.observable(params.id);
		self.owner = ko.observable('');
		self.ownerId = ko.observable(-1);
		self.entryCount = ko.observable(0);
		self.exhItems = ko.observableArray();
		self.desc = ko.observable('');
		self.loading = ko.observable(false);
		self.showCarousel = ko.observable(false);
		self.backgroundImgWithUrl = ko.observable('');
		
		self.initCarousel = function () {
			WITHApp.initCarousel();
			WITHApp.initExpandExhibitionText();
			WITHApp.initImageZoom();
		};

		self.revealItems = function (data) {
			for (var i in data) {
				var result = data[i];
                var record = new Record(result);
				record.annotation = '';
				if (result.contextData !== undefined && result.contextData !== null && 
						result.contextData.body != undefined 
						&& result.contextData.body != null
						&& ! $.isEmptyObject(result.contextData.body)) {
					
					record.annotation = result.contextData.body.text.default;
					record.videoUrl = result.contextData.body.videoUrl;
//					for (var j in result.contextData) {
//						if (result.contextData[j].target.collectionId == self.id() && result.contextData[j].target.position == i) {
//							record.annotation = result.contextData[j].body.text.default;
//							record.videoUrl = result.contextData[j].body.videoUrl;
//						}
//					}
				}
				var styleId = self.exhItems().length % 5 || 0;
				var styleIdMapping = {
					0: 1,
					1: 1,
					2: 2,
					3: 2,
					4: 3
				};
				styleId = styleIdMapping[styleId];
				record.css = 'item style' + styleId; //0, 1, 2, 3, 4 -> 2 x style1, 2 x style2 , 1 x style3
				self.exhItems().push(record);
			}
			console.log(self.exhItems()[0]);
			self.exhItems.valueHasMutated();
			setTimeout(function () {
				self.initCarousel();
			}, 1000);
		};

		self.loadExhibition = function (id) {
			self.loading(true);
			$.ajax({
				"url": "/collection/" + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					/*if user not logged in and not public redirect*/
					if (data.administrative.access.isPublic == false) {
						if (isLogged() == false) {
							window.location = '#home';
							$.smkAlert({
									text: 'This exhibition is private',
									type: 'danger',
									permanent: true
								});
							return;
						}
					}
					var adminData = data.administrative;
					var descData = data.descriptiveData;
					self.exhName(findByLang(data.descriptiveData.label));
					self.desc(findByLang(data.descriptiveData.description));
					self.owner(data.withCreatorInfo.username);
					self.ownerId(data.administrative.withCreator);
					self.entryCount(data.administrative.entryCount);
					var backgroundImg = data.descriptiveData.backgroundImg;
					//self.access(adminData.access);
					if (self.entryCount() && self.entryCount() > 0) {
						$.ajax({
							"url": "/collection/" + self.id() + "/list?count="+self.entryCount()+"&start=0",
							"method": "get",
							"contentType": "application/json",
							"success": function (data) {
								var items = self.revealItems(data.records);
								if (backgroundImg == null || backgroundImg.Original ==null || 
										backgroundImg.Original.withUrl == null || 
										backgroundImg.Original.withUrl == "") {
									self.backgroundImgWithUrl(self.exhItems()[0].fullres());
								} 						
								else {
									if (backgroundImg.Original.withUrl.indexOf("/media") == 0) {
										self.backgroundImgWithUrl(window.location.origin + backgroundImg.Original.withUrl);
									} else {
										self.backgroundImgWithUrl(self.backgroundImg.Original.withUrl);
									}
								}
								self.loading(false);
							},
							"error": function (result) {
								self.loading(false);
								$.smkAlert({
									text: 'An error has occured',
									type: 'danger',
									permanent: true
								});
							}
						});
					}
					self.showCarousel(true);
					self.loading(false);
				},
				error: function (xhr, textStatus, errorThrown) {
					self.loading(false);
					$.smkAlert({
						text: 'An error has occured',
						type: 'danger',
						permanent: true
					});
				}
			});
		};
		self.loadExhibition();

		self.loadNext = function () {
			self.moreItems();
		};

		self.moreItems = function () {
			if (self.loading === true) {
				setTimeout(self.moreItems(), 300);
			}
			if (self.loading() === false) {
				self.loading(true);
				var offset = self.exhItems().length;
				$.ajax({
					"url": "/collection/" + self.id() + "/list?count=10&start=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						self.revealItems(data.records);
						self.loading(false);
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
		};
	}

	return {
		viewModel: EViewModel,
		template: template
	};
});