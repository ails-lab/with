define(['knockout', 'text!./mycollections.html'  ], function(ko, template) {

	

	function ProfileViewModel(params) {
		var self = this;
		self.route = params.route;
		var collections = [];
		if (sessionStorage.getItem('UserCollections') !== null) 
		  collections = JSON.parse(sessionStorage.getItem("UserCollections"));
		if (localStorage.getItem('UserCollections') !== null) 
		  collections = JSON.parse(localStorage.getItem("UserCollections"));
		self.myCollections = ko.observableArray(collections);
	}

	return {viewModel: ProfileViewModel, template: template};
});
