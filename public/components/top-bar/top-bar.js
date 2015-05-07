define(['knockout', 'text!./top-bar.html', 'app', 'autocomplete'], function(ko, template, app, autocomplete) {

  function TopBarViewModel(params) {


		$( document ).on( 'keypress', function( event ) {

			if (event.target.nodeName != 'INPUT') {
				if (event.which === null) {
					var char=String.fromCharCode(event.which);
					toggleSearch("focus",char);

				}
				else if (event.which !== 0 && event.charCode !== 0) {
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

		editProfile       = function() { app.showPopup('edit-profile'); };
		logout            = function() { app.logout(); };
	}

	return { viewModel: TopBarViewModel, template: template };
});
