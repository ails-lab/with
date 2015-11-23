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
				notification.pending(false);
				notification.unread(false);
				app.currentUser.notificatios.unread(app.currentUser.notification.unread() - 1);
			}).fail(function (jqXHR, textStatus, errorThrown) {
				// TODO: Display error message
			});
		};

		self.reject = function (notification) {
			$.ajax({
				type: 'PUT',
				url: '/notifications/reject/' + notification.dbId,
			}).done(function (data, textStatus, jqXHR) {
				notification.pending(false);
				notification.unread(false);
				app.currentUser.notificatios.unread(app.currentUser.notification.unread() - 1);
			}).fail(function (jqXHR, textStatus, errorThrown) {
				// TODO: Display error message
			});
		};

		self.markRead = function (notifications) {
			var ids = [];
			for (var i = 0; i < notifications().length; i++) {
				if (!notifications()[i].pendingResponse && notifications()[i].unread()) {
					ids.push(notifications()[i].dbId);
				}
			}

			if (ids.length === 0) {
				return;
			}

			$.ajax({
				type: 'PUT',
				url: '/notifications/read',
				data: JSON.stringify(ids),
				contentType: 'application/json',
				dataType: 'json'
			}).done(function (data, textStatus, jqXHR) {
				for (var i = 0; i < notifications.length; i++) {
					if (!notifications[i].pendingResponse) {
						notifications[i].unread(false);
					}
				}
			}).fail(function (jqXHR, textStatus, error) {
				// TODO: Show error message
			});
		};

		if (!isLogged()) {
			window.location = '#login'; // Redirect to login
		}

		self.markRead(app.currentUser.notifications.userNotifications);
		self.markRead(app.currentUser.notifications.groupNotifications);
	}

	return {
		viewModel: NotificationsViewModel,
		template: template
	};
});
