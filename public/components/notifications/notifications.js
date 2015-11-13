define(['knockout', 'text!./notifications-page.html', 'app', 'knockout-else'], function (ko, template, app, KnockoutElse) {

	function NotificationsViewModel(params) {
		var self = this;
		self.route = params.route;
		self.notifications = ko.observableArray();
		self.groupNotifications = ko.observableArray();

		self.formatDate = function (timestamp) {
			var d = new Date(timestamp);
			return d.toTimeString(); //getDate() + '/' + (d.getMonth()+1) + '/' + d.getFullYear();
		};

		self.processNotifications = function (data) {
			for (var i = 0; i<data.length; i++) {
				data[i].date = self.formatDate(data[i].openedAt);
				switch (data[i].activity) {
					case "GROUP_INVITE":
						data[i].message = '<strong>' + data[i].senderName + '</strong> invites you to join <strong><a href="#organization/' + data[i].group + '">' + data[i].groupName + '</a></strong>';
						self.notifications.push(data[i]);
						break;
					case "GROUP_INVITE_ACCEPT":
						data[i].message = '<strong>' + data[i].senderName + '</strong> joined <strong><a href="#organization/' + data[i].group + '">' + data[i].groupName + '</a></strong>';
						self.groupNotifications.push(data[i]);
						break;
					case "GROUP_INVITE_DECLINED":
						data[i].message = '<strong>' + data[i].senderName + '</strong> declined your invitation to join <strong><a href="#organization/' + data[i].group + '">' + data[i].groupName + '</a></strong>';
						self.groupNotifications.push(data[i]);
						break;
					case "GROUP_REQUEST":
						data[i].message = '<strong>' + data[i].senderName + '</strong> wants to join <strong><a href="#organization/' + data[i].group + '">' + data[i].groupName + '</a></strong>';
						self.groupNotifications.push(data[i]);
						break;
					case "GROUP_REQUEST_ACCEPT":
						data[i].message = 'You joined <strong><a href="#organization/' + data[i].group + '">' + data[i].groupName + '</a></strong>';
						self.notifications.push(data[i]);
						break;
					case "GROUP_REQUEST_DENIED":
						data[i].message = 'Your request to join <strong><a href="#organization/' + data[i].group + '">' + data[i].groupName + '</a></strong> was declined';
						self.notifications.push(data[i]);
						break;
					default:
						data[i].message = "<strong>Unknown Notification Type:</strong> " + data[i].activity;
						self.notifications.push(data[i]);
				}
			}
		};

		self.accept = function (dbId) {
			$.ajax({
				type: 'PUT',
				url: '/user/notifications' + dbId + '/accept',
				contentType: 'application/json',
				dataType: 'json'
			}).done(function (data, textStatus, jqXHR) {
				// TODO: Update the notification list
			}).fail(function (jqXHR, textStatus, errorThrown) {
				// TODO: Display error message
			});
		};

		self.decline = function (dbId) {
			$.ajax({
				type: 'PUT',
				url: '/user/notifications' + dbId + '/decline',
				contentType: 'application/json',
				dataType: 'json'
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

		self.loadNotifications = function () {
			$.ajax({
				type: 'GET',
				url: '/user/notifications',
				contentType: 'application/json',
				dataType: 'json'
			}).done(function (data, textStatus, jqXHR) {
				console.log(data);
				self.processNotifications(data);
			}).fail(function (jqXHR, textStatus, errorThrown) {
				// TODO: Display error message
			});
		};

		if (isLogged()) {
			self.loadNotifications();
		} else {
			window.location = '#login'; // Redirect to login
		}
	}

	return {
		viewModel: NotificationsViewModel,
		template: template
	};
});
