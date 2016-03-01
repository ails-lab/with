define(['knockout', 'text!./side-bar.html', 'app'], function (ko, template, app) {

	function SideBarViewModel(params) {
		var self = this;

		self.collectionCount = ko.pureComputed(function () {
			return app.currentUser.collectionCount;
		});
		self.exhibitionCount = ko.pureComputed(function () {
			return app.currentUser.exhibitionCount;
		});
		self.favoritesCount = ko.pureComputed(function () {
			return app.currentUser.favorites().length;
		});
	}

	return { viewModel: SideBarViewModel, template: template };
});
