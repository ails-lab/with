define(['bootstrap', 'knockout', 'text!./myannotations.html', 'knockout-else','app', 'moment', 'isotope', 'knockout-validation', 'easypiechart'], function (bootstrap, ko, template, KnockoutElse, app, moment, Isotope) {
	
	
	function Record(data) {
		var self = this;

		self.annotations = [];
		self.title = "";
		self.description = "";
		self.thumb = "";
		self.fullres = "";
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
			
			
			self.annotations = options.annotations;

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

			
			
			if(self.source=="Rijksmuseum" && media){
				media[0].Thumbnail=media[0].Original;
			}
			self.thumb = media[0] != null && media[0].Thumbnail != null && media[0].Thumbnail.withUrl != "null" ? media[0].Thumbnail.withUrl : null;
			self.fullres = media[0] != null && media[0].Original != null && media[0].Original.url != "null" ? media[0].Original.url : null;
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
	
	
	
	function MyAnnotationsModel(params) {
		KnockoutElse.init([spec = {}]);
		var self = this;
		self.route = params.route;
		self.loading = ko.observable(false);
		self.citems = ko.observableArray();
		self.access = ko.observable("READ");
		self.isFavorites = ko.observable(false);
		//self.id = ko.observable(params.id);
		self.id = ko.observable(app.currentUser._id())
		self.$container = $(".grid#" + self.id()).isotope({
			itemSelector: '.item',
			transitionDuration: transDuration,
			masonry: {
				columnWidth: '.sizer',
				percentPosition: true
			}
		});
		self.fetchitemnum = 10;
		self.annotationCount = ko.observable();
		self.annotatedRecordCount = ko.observable(5);
		
		self.annotationRecords = ko.observable();
		self.annotationGoal = ko.observable();
		self.annotationPercentage = ko.observable();
		//self.myAnnotatios = ko.mapping.fromJS([], mapping);
		
		self.img = ko.observable("img/ui/rookie.png");
		self.badgeName = ko.observable('Rookie');

		
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
		
		self.hideMessage = function () {
			$("section.message").toggle();
		};
		
		function getItem(record) {
			var tile = '<div class="item ' + record.dbId + '"><div class="wrap"><a href="#"  onclick="recordSelect(\'' + record.dbId + '\',event)">' +
			'<div class="thumb"><img style="width:100%" src="' + record.thumbnail() + '" onError="this.src=\'img/ui/ic-noimage.png\'"/><div class="counter">' + record.annotations.length + '  Annotations</div></div>';
			tile += '<div class="info"><h2 class="title truncate">' + record.title + '</h2></a>';
			
			var distitle = "";
			if (record.creator && record.creator.length > 0) {
				distitle = "by " + record.creator;
			}
			else if (record.dataProvider && record.dataProvider.length > 0 && record.dataProvider != record.creator) {
				distitle = record.dataProvider;
			}
			tile+='<span class="source">'+distitle+'</source><a href="' + record.view_url + '" target="_new" class="links">' + record.sourceCredits() + '</a></div>';
			tile += '<div class="action-group"><div class="wrap">';

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
		
		self.revealItems = function (data) {
			if ((data.length === 0 || data.length < self.fetchitemnum)) {
				self.loading(false);
				$(".loadmore").text("no more results");
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
		
		
		self.percentageCall = function() {			
			$.ajax({
				"url": '/record/annotationPercentage?groupId=56e13d2e75fe2450755e553a' + '&goal=1000',
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					self.annotationRecords(data.annotatedRecords);
					self.annotationGoal(data.goal);
					self.annotationPercentage(data.annotatedRecordsPercentage);
					WITHApp.initChart(self.annotationPercentage());
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
		
		
		self.percentage = function() {
		
			var promise = self.percentageCall();
			$.when(promise).done(function (data) {
				self.loading(false);
			});
		}
		
		self.loadAnnotations = function (id) {
			self.loading(true);

			$.ajax({
				"url": '/user/annotations',
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					self.annotationCount(data.annotationCount);
					if(data.annotationCount >= 10 && data.annotationCount < 25) {
						self.img('img/ui/ic-badge-bronze.png');
						self.badgeName('Bronze');
					} else if(data.annotationCount >= 25 &&  data.annotationCount < 50) {
						self.img('img/ui/ic-badge-silver.png');
						self.badgeName('Silver');
					}
					else if(data.annotationCount >= 50){
						self.img('img/ui/ic-badge-gold');
						self.badgeName('Golden');
					}
					self.annotatedRecordCount(data.annotatedRecordsCount);
					var items = self.revealItems(data.records);
					if (items.length > 0) {
						var $newitems = getItems(items);

						self.isotopeImagesReveal(self.$container, $newitems);
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
		};
		WITHApp.initTooltip();
		self.loadAnnotations();
		self.percentage();
		
	
		self.loadNext = function () {
						
			if (self.loading() === false) {
				self.loading(true);

				var promise = self.moreItems();
				$.when(promise).done(function (data) {
					var items = self.revealItems(data.records);
					if (items.length > 0) {
						var $newitems = getItems(items);

						self.isotopeImagesReveal(self.$container, $newitems);

					}
					self.loading(false);
				});
			}
		};
		
		self.moreItems = function () {
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				url: "/user/annotations",
				processData: false,
				data: "count=" + self.fetchitemnum + "&offset=" + self.citems().length
			}).success (function () {
			});

		};
		
	}
	
	return {viewModel: MyAnnotationsModel, template: template};
});