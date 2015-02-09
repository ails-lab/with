define(['knockout', 'text!./login-register.html',  'facebook'], function(ko, template, FB) {

	FB.init({
		appId   : '1584816805087190',
		status  : true,
		version : 'v2.2'
	});

	function LoginRegisterViewModel(params) {
		$(document).off("keypress");
		var self = this;
	// Check if user is logged in in facebook
		self.fblogin = function() { FBLogin(); }

		self.route = params.route;
	}

	function FBLogin() {
		FB.login(function(response) {
			statusChangeCallback(response);
		}, {scope: 'public_profile, email'});
	}

	function statusChangeCallback(response) {
		console.log(response);
		if (response.status === 'connected') {
			console.log(response.authResponse.accessToken);

			FB.api('/me', function(response) {
				console.log(JSON.stringify(response));
			});
		}
		else if (response.status === 'not_authorized') {
			// User didn't authorize the application
		}
		else {
			// User is not logged
		}
	}

	/*
	 * not needed
	 * LoginRegisterViewModel.prototype.dispose = function() {
		$(document).on("keypress", keypressHandler);
	}*/

	return { viewModel: LoginRegisterViewModel, template: template };
});
