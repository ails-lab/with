define(['knockout', 'text!./item.html', 'app'], function (ko, template, app) {

	
	function Record(data,showMeta) {
		var self = this;
	    self.recordId = "";
		self.title = "";
		self.description="";
		self.thumb = "";
		self.fullres=ko.observable("");
		self.view_url="";
		self.source="";
		self.creator="";
		self.provider="";
		self.dataProvider="";
		self.dataProvider_uri="";
		self.rights="";
		self.url="";
		self.externalId = "";
		self.likes=0;
		self.collected=0;
		self.data=ko.observable('');
		self.collectedIn =  [];
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
			//self.fullres=data.fullres;
			self.description=data.description;
			self.source=data.source;
			self.creator=data.creator;
			self.provider=data.provider;
			self.dataProvider=data.dataProvider;
			self.dataProvider_uri=data.dataProvider_uri;
			self.rights=data.rights;
			self.recordId=data.recordId;
			self.externalId=data.externalId;
			self.likes=data.likes;
			self.collected=data.collected;
			self.collectedIn=data.collectedIn;
			self.data(data.data);
			var likeval=app.isLiked(self.externalId);
			
		 
		};

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
