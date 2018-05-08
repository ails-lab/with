define(['knockout', 'text!./side-bar.html', 'app'], function (ko, template, app) {

	function SideBarViewModel(params) {
		var self = this;
		self.currentRoute = window.location.hash;

		self.checkRoute = function (path) {
			return !self.currentRoute.lastIndexOf(path, 0);
		};

		self.collectionCount = ko.pureComputed(function () {
			return app.currentUser.collectionCount();
		});
		self.exhibitionCount = ko.pureComputed(function () {
			return app.currentUser.exhibitionCount();
		});
		self.favoritesCount = ko.pureComputed(function () {
			return app.currentUser.favorites().length;
		});
		
		self.annotationCount = ko.pureComputed(function () {
			return app.currentUser.annotationCount();
		});
	}

	return { viewModel: SideBarViewModel, template: template };
});
