define(['knockout', 'text!./side-bar.html', 'app'], function (ko, template, app) {

	function SideBarViewModel(params) {
		var self = this;
		self.currentRoute = ko.pureComputed(function () {
			return window.location.hash;
		});

		self.collectionCount = ko.pureComputed(function () {
			return app.currentUser.collectionCount();
		});
		self.exhibitionCount = ko.pureComputed(function () {
			return app.currentUser.exhibitionCount();
		});
		self.favoritesCount = ko.pureComputed(function () {
			return app.currentUser.favorites().length;
		});
	}

	return { viewModel: SideBarViewModel, template: template };
});
