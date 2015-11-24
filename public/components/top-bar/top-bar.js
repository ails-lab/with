define(['knockout', 'text!./top-bar.html', 'app', 'autocomplete', 'knockout-switch-case'], function(ko, template, app, autocomplete) {

  function TopBarViewModel(params) {
		this.route         = params.route;
		var self           = this;
		self.notifications = ko.observableArray();

		$("[data-toggle=popover]").popover({
			html: true,
			content: function() {
				var tmp = app.currentUser.notifications.userNotifications().concat(app.currentUser.notifications.groupNotifications());
				tmp.sort(function(a, b) {
					return b.openedAt - a.openedAt;
				});
				self.notifications(tmp.splice(0, 5));

				return $('#notifications-content').html();
			}
		});

		$( document ).on( 'keypress', function( event ) {

			if (event.target.nodeName != 'INPUT' && event.target.nodeName != 'TEXTAREA') {
				if (event.which === null) {
					$('[id^="modal"]').removeClass('md-show').css('display', 'none');
			    	$("#myModal").modal('hide');

					var char=String.fromCharCode(event.which);
					toggleSearch("focus",char);

				}
				else if (event.which !== 0 && event.charCode !== 0) {
					$('[id^="modal"]').removeClass('md-show').css('display', 'none');
			    	$("#myModal").modal('hide');

					var chr = String.fromCharCode(event.which);
					toggleSearch("focus", chr);
				}
				else {
					return;
				}
			}
			else {
				return;
			}
		});

		self.username      = app.currentUser.username;
		self.profileImage  = ko.computed(function() { return app.currentUser.image() ? app.currentUser.image() : 'images/user.png'; });
		self.organizations = app.currentUser.organizations;
		self.projects      = app.currentUser.projects;
		self.usergroups    = app.currentUser.usergroups;
		self.noticount     = ko.pureComputed(function() {
			return app.currentUser.notifications.unread() === 0 ? '' : app.currentUser.notifications.unread();
		});

		editProfile        = function() { app.showPopup('edit-profile'); };
		newOrganization    = function() { app.showPopup('new-organization', { type: 'organization' }); };
		newProject         = function() { app.showPopup('new-organization', { type: 'project' }); };
		logout             = function() { app.logout(); };
	}

	return { viewModel: TopBarViewModel, template: template };
});
