define(['knockout', 'text!./login-register.html',  'facebook'], function(ko, template) {

	FB.init({
		appId   : '1584816805087190',
		status  : true,
		version : 'v2.2'
	});

	var template;

	function LoginRegisterViewModel(params) {

		this.route = params.route;
	}

	function FBLogin() {
		FB.getLoginStatus(function(response) {
			console.log(response);
		});
	}

	return { viewModel: LoginRegisterViewModel, template: template };
});
