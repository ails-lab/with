define(['knockout', 'text!./dashboard.html', 'app'], function (ko, template, app) {

	function DashboardViewModel(params) {
		// If User is not logged in, then redirect him to the login page
		if (localStorage.getItem('logged_in') != "true") {
			window.location.href = "#login";
		}

		var self = this;
		self.route = params.route;
		self.userNotifications = app.currentUser.notifications.userNotifications;
		self.groupNotifications = app.currentUser.notifications.groupNotifications;
		self.notifications = ko.pureComputed(function () {
			return self.userNotifications().concat(self.groupNotifications()).sort(function (a, b) {
				return b.openedAt - a.openedAt;
			});
		});

		WITHApp.initTooltip();
		self.hideMessage = function () {
			$("section.message").toggle();
		};

		self.accept = function (notification) {
			$.ajax({
				type: 'PUT',
				url: '/notifications/accept/' + notification.dbId
			}).done(function (data, textStatus, jqXHR) {
				notification.pending(false);
				notification.unread(false);
				app.currentUser.notifications.unread(app.currentUser.notifications.unread() - 1);
			}).fail(function (jqXHR, textStatus, errorThrown) {
				// TODO: Display error message
			});
		};

		self.reject = function (notification) {
			$.ajax({
				type: 'PUT',
				url: '/notifications/reject/' + notification.dbId
			}).done(function (data, textStatus, jqXHR) {
				notification.pending(false);
				notification.unread(false);
				app.currentUser.notifications.unread(app.currentUser.notifications.unread() - 1);
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
				for (var i = 0; i < notifications().length; i++) {
					if (!notifications()[i].pendingResponse && notifications()[i].unread()) {
						notifications()[i].unread(false);
						app.currentUser.notifications.unread(app.currentUser.notifications.unread() - 1);
					}
				}
			}).fail(function (jqXHR, textStatus, error) {
				console.log(error);
				// TODO: Show error message
			});
		};

		self.markUserNotificationsRead = function () {
			self.markRead(app.currentUser.notifications.userNotifications);
		};

		self.markGroupNotificationsRead = function () {
			self.markRead(app.currentUser.notifications.groupNotifications);
		};
	}

	return {
		viewModel: DashboardViewModel,
		template: template
	};
});
