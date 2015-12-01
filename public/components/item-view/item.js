define(['knockout', 'text!./item.html', 'app'], function (ko, template, app) {

	function Record(data,showMeta) {
		var self = this;
		self.recordId = ko.observable("");
		self.title = ko.observable(false);
		self.description = ko.observable(false);
		self.thumb = ko.observable(false);
		self.fullres = ko.observable(false);
		self.view_url = ko.observable(false);
		self.source = ko.observable(false);
		self.creator = ko.observable("");
		self.dataprovider=ko.observable("");
		self.provider = ko.observable("");
		self.rights = ko.observable("");
		self.url = ko.observable("");
		self.id = ko.observable("");
		self.externalId = ko.observable("");
		self.collectedCount = ko.observable("");
		self.liked = ko.observable("");
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
			if (data.title) {
				self.title(data.title);
				
			} else {
				self.title("No title");
			}

			if (data.id) {
				self.recordId(data.id);
			} else {
				self.recordId(data.recordId);
			}

			self.url("#item/" + self.recordId());
			self.view_url(data.view_url);
			self.thumb(data.thumb);

			if (data.source!="Rijksmuseum" && data.fullres && data.fullres[0].length > 0 && data.fullres != "null") {
				self.fullres(data.fullres[0]);
			} else {
				self.fullres(self.cachedThumbnail());
			}

			if (data.description) {
				self.description(data.description);
				
			} else {
				self.description(data.title);
			}

			if (data.creator) {
				self.creator(data.creator);
			}

			if (data.provider) {
				self.provider(data.provider);
			}
			if (data.dataprovider) {
				self.dataprovider(data.dataprovider);
			}
			if (data.rights) {
				self.rights(data.rights);
			}

			self.externalId(data.externalId);
			self.source(data.source);
			
			
			
			if (showMeta){
			$.ajax({
				type    : "get",
				url     : "/record/merged/"+self.externalId(),
				success : function(result) {
					self.collectedCount(result.count);
					self.liked(result.liked);
					self.collections(result.collections);
				},
				error   : function(request, status, error) {
					console.log(request);
				}
			});
			}
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
				return "images/logos/mint_logo.png";
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
			if (self.creator() && self.creator().length > 0)
				distitle += ", by " + self.creator();
			if (self.dataprovider() && self.dataprovider().length > 0 && self.dataprovider() != self.creator())
				distitle += ", " + self.dataprovider();
			return distitle;
		});

		if (data) self.load(data);
	}

	function ItemViewModel(params) {
		var self = this;

		self.route = params.route;
		var thumb = "";
		self.record = ko.observable(new Record());
		self.detailsEnabled =  ko.observable(false);

		
		itemShow = function (e,showMeta) {
			data = ko.toJS(e);
			self.record(new Record(data,showMeta));
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
				toggleSearch(event,'');
			}
			self.close();
		};
	}
	
	
	return {
		viewModel: ItemViewModel,
		template: template
	};
});
