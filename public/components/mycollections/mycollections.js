define(['knockout', 'text!./mycollections.html', 'knockout-else'], function(ko, template, KnockoutElse) {

	

	function MyCollectionsModel(params) {
		KnockoutElse.init([spec={}]);
		var self = this;
		self.route = params.route;
		var collections = [];
		if (sessionStorage.getItem('UserCollections') !== null) 
		  collections = JSON.parse(sessionStorage.getItem("UserCollections"));
		if (localStorage.getItem('UserCollections') !== null) 
		  collections = JSON.parse(localStorage.getItem("UserCollections"));
		self.myCollections = ko.observableArray(collections);
		
		self.deleteMyCollection = function(collection) {
			collectionId = collection.dbId;
			collectionTitle = collection.title;
			showDelCollPopup(collectionTitle);
		};
		
		showDelCollPopup = function(collectionTitle) {
			 
		};
		
		self.closeDelCollPopup = function() {
			
		};
		
		self.deleteCollection = function() {
			alert(collectionId);
		};
	
	}
	
	return {viewModel: MyCollectionsModel, template: template};
});
