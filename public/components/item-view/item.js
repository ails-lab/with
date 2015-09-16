define(['knockout', 'text!./item.html', 'app'], function (ko, template, app) {

	function Record(data) {
		var self = this;
		self.recordId = ko.observable("");
		self.title = ko.observable(false);
		self.description = ko.observable(false);
		self.thumb = ko.observable(false);
		self.fullres = ko.observable(false);
		self.view_url = ko.observable(false);
		self.source = ko.observable(false);
		self.creator = ko.observable("");
		self.provider = ko.observable("");
		self.rights = ko.observable("");
		self.url = ko.observable("");
		self.id = ko.observable("");
		self.externalId = ko.observable("");
		self.collectedCount = ko.observable("");
		self.collections =  ko.observableArray([]);

		self.cachedThumbnail = ko.pureComputed(function() {


			   if(self.thumb()){
				if (self.thumb().indexOf('//') === 0) {
					return self.thumb();
				} else {
					var newurl='url=' + encodeURIComponent(self.thumb())+'&';
					return '/cache/byUrl?'+newurl+'Xauth2='+ sign(newurl);
				}}
			   else{
				   return "images/no_image.jpg";
			   }
			});
		self.load = function (data) {
			if (data.title == undefined) {
				self.title("No title");
			} else {
				self.title(data.title);
			}

			if (data.id) {
				self.recordId(data.id);
			} else {
				self.recordId(data.recordId);
			}

			self.url("#item/" + self.recordId());
			self.view_url(data.view_url);
			self.thumb(data.thumb);

			if (data.source!="Rijksmuseum" && data.fullres !== undefined && data.fullres != null && data.fullres[0].length > 0 && data.fullres != "null") {
				self.fullres(data.fullres[0]);
			} else {
				self.fullres(self.cachedThumbnail());
			}

			if (data.description == undefined) {
				self.description(data.title);
			} else {
				self.description(data.description);
			}

			if (data.creator !== undefined) {
				self.creator(data.creator);
			}

			if (data.provider !== undefined) {
				self.provider(data.provider);
			}

			if (data.rights !== undefined) {
				self.rights(data.rights);
			}

			self.externalId(data.externalId);
			self.source(data.source);
			
			$.ajax({
				type    : "get",
				url     : "/record/"+self.externalId() +"/mergedCollections",
				success : function(result) {
					self.collectedCount(result.count);
					self.collections(result.collections);
				},
				error   : function(request, status, error) {
					console.log(request);
				}
			});

		};

		self.sourceImage = ko.pureComputed(function () {
			switch (self.source()) {
			case "DPLA":
				return "images/logos/dpla.png";
			case "Europeana":
				return "images/logos/europeana.jpeg";
			case "NLA":
				return "images/logos/nla_logo.png";
			case "DigitalNZ":
				return "images/logos/digitalnz.png";
			case "EFashion":
				return "images/logos/eufashion.png";
			case "YouTube":
				{
					return "images/logos/youtube.jpg";
				}
			case "Mint":
				return "images/logos/mint_logo.png";
			case "Rijksmuseum":
				return "images/logos/Rijksmuseum.png";
			default:
				return "";
			}
		});

		self.sourceCredits = ko.pureComputed(function () {
			switch (self.source()) {
			case "DPLA":
				return "dpla.eu";
			case "Europeana":
				return "europeana.eu";
			case "NLA":
				return "nla.gov.au";
			case "DigitalNZ":
				return "digitalnz.org";
			case "EFashion":
				return "europeanafashion.eu";
			case "YouTube":
				{
					return "youtube.com";
				}
			case "Mint":
				return "mint";
			default:
				return "";
			}
		});

		self.displayTitle = ko.pureComputed(function () {
			var distitle = "";
			distitle = self.title();
			if (self.creator() !== undefined && self.creator().length > 0)
				distitle += ", by " + self.creator();
			if (self.provider() !== undefined && self.provider().length > 0 && self.provider() != self.creator())
				distitle += ", " + self.provider();
			return distitle;
		});

		if (data !== undefined) self.load(data);
	}

	function ItemViewModel(params) {
		var self = this;

		self.route = params.route;
		var thumb = "";
		self.record = ko.observable(new Record());


		itemShow = function (e) {
			data = ko.toJS(e);
			self.record(new Record(data));
			self.open();
		};

		self.open = function () {
			$("body").addClass("modal-open");
			$('#modal-1').css('display', 'block');

			$('#modal-1').addClass('md-show');
		};

		self.close = function () {
			self.record().fullres('');
			$("body").removeClass("modal-open");
			$("#modal-1").find("div[id^='modal-']").removeClass('md-show').css('display', 'none');
			$('#modal-1').removeClass('md-show');
			$('#modal-1').css('display', 'none');
			$("#myModal").modal('hide');
		};

		self.changeSource = function (item) {
			item.record().fullres(item.record().cachedThumbnail());
		};

		self.collect = function (item) {
			if (!isLogged()) {
				showLoginPopup(self.record());
			} else {
				collectionShow(self.record());
			}
		};

		self.recordSelect = function (e) {
			itemShow(e);
		};
	}

	return {
		viewModel: ItemViewModel,
		template: template
	};
});
