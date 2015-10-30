define(['knockout', 'text!./top-bar.html', 'app', 'autocomplete'], function(ko, template, app, autocomplete) {

  function TopBarViewModel(params) {


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

		this.route = params.route;

		var self          = this;
		self.username     = app.currentUser.username;
		self.profileImage = ko.computed(function() { return app.currentUser.image() ? app.currentUser.image() : 'images/user.png'; });
		self.organizations = app.currentUser.organizations;
		self.projects     = app.currentUser.projects;
		self.usergroups   = app.currentUser.usergroups;

		editProfile       = function() { app.showPopup('edit-profile'); };
		newOrganization   = function() { app.showPopup('new-organization', { type: 'organization' }); };
		newProject        = function() { app.showPopup('new-organization', { type: 'project' }); };
		logout            = function() { app.logout(); };
	}

	return { viewModel: TopBarViewModel, template: template };
});
