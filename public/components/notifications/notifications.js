define(['knockout', 'text!./notifications-page.html', 'app', 'knockout-else'], function (ko, template, app, KnockoutElse) {

	function NotificationsViewModel(params) {
		var self = this;
		self.route = params.route;

		self.userNotifications = app.currentUser.notifications.userNotifications;
		self.groupNotifications = app.currentUser.notifications.groupNotifications;

		self.accept = function (notification) {
			$.ajax({
				type: 'PUT',
				url: '/notifications/accept/' + notification.dbId,
			}).done(function (data, textStatus, jqXHR) {
				// TODO: Update the notification list
			}).fail(function (jqXHR, textStatus, errorThrown) {
				// TODO: Display error message
			});
		};

		self.reject = function (notification) {
			$.ajax({
				type: 'PUT',
				url: '/notifications/reject/' + notification.dbId,
			}).done(function (data, textStatus, jqXHR) {
				// TODO: Update the notification list
			}).fail(function (jqXHR, textStatus, errorThrown) {
				// TODO: Display error message
			});
		};

		self.markRead = function (notifications) {
			$.ajax({
				type: 'PUT',
				url: '/user/readNotifications',
				data: ids,
				contentType: 'application/json',
				dataType: 'json'
			}).done(function (data, textStatus, jqXHR) {
				// TODO: Update list
			}).fail(function (jqXHR, textStatus, error) {
				// TODO: Show error message
			});
		};

		if (!isLogged()) {
			window.location = '#login'; // Redirect to login
		}
	}

	return {
		viewModel: NotificationsViewModel,
		template: template
	};
});
