define(['knockout', 'text!./notifications-page.html', 'app'], function (ko, template, app) {

	function NotificationsViewModel(params) {
		var self = this;
		this.route = params.route;

	}

	return {
		viewModel: NotificationsViewModel,
		template: template
	};
});
