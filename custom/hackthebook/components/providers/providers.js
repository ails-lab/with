define(['bridget', 'knockout', 'text!./providers.html','isotope','imagesloaded','app','smoke'], function(bridget,ko, template, Isotope,imagesLoaded,app) {

  
  
  
  $.bridget('isotope', Isotope);
	
  
  
  function randomInt(min,max)
  {
      return Math.floor(Math.random()*(max-min+1)+min);
  }

	
	ko.bindingHandlers.providerIsotope = {
				init: app.initOrUpdate('init'),
				update: app.initOrUpdate('update')
			};

	

	function Provider(data) {
		var self = this;
	    self.id = "";
		self.name = "";
		self.logo="";
		self.country = "";
		self.background="";
		self.totalCollections=0;
		self.totalExhibitions=0;
		
		self.isLoaded = ko.observable(false);
		 
		self.load = function(data) {
			if(data.title==undefined){
				self.title="No title";
			}else{self.title=data.title;}
			//get 20 collections and exhibitions
			self.url="#provider/"+data.dbId+"/count/20";
			if(data.avatar && data.avatar.Square)
			 self.logo=data.avatar.Square;
			self.country=data.page.country;
			self.totalCollections=data.totalCollections;
			self.totalExhibitions=data.totalExhibitions;
			self.name=data.friendlyName !=null? data.friendlyName : data.username;
			if(data.page.cover && data.page.cover.Thumbnail)
			 self.background= data.page.cover.Thumbnail;
			
		};

		
		if(data != undefined) self.load(data);
		
	}

	function ProvidersViewModel(params) {
		this.route = params.route;
	    document.body.setAttribute("data-page","contentproviders");
	    setTimeout(function(){ WITHApp.init(); }, 300);
		var self = this;

		var $container = $(".grid");
		self.route = params.route;
		var counter = 1;
		self.collname = ko.observable('');
		self.access = ko.observable("READ");
		self.id = ko.observable(params.id);
		self.owner = ko.observable('');
		self.ownerId = ko.observable(-1);
		self.itemCount = ko.observable(0);
		self.providers = ko.observableArray();

		
	   
		self.description = ko.observable('');
		self.selectedRecord = ko.observable(false);

		self.loading = ko.observable(false);

		
		self.revealItems = function (data) {
			if(data.length==0){ self.loading(false);}
			
			for (var i in data) {
				var result = data[i];
				var record = new Provider(data[i]);
				
				self.providers().push(record);
			}
			self.providers.valueHasMutated();
		};
		
	

		self.loadProviders = function () {
			
			// replace with group/descendantOrganizations/:projectId
			self.loading(true);
			$.ajax({
				"url": "/group/descendantOrganizations/" + WITHApp.projectId+"?collectionHits=true",//TO BE ADDED WHEN fixed+"?collectionHits=true",
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					
					self.revealItems(data);
					
					
				},
				error: function (xhr, textStatus, errorThrown) {
					self.loading(false);
					$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
				}
			});
		};

		self.loadProviders();
		

		
	}

	return { viewModel: ProvidersViewModel, template: template };
});
