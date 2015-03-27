define(['knockout', 'text!./login-register.html',  'facebook', 'app', 'knockout-validation', 'google'], function(ko, template, FB, app) {

	FB.init({
		appId   : '1584816805087190',
		status  : true,
		version : 'v2.2'
	});

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	function LoginRegisterViewModel(params) {
		var self = this;

		// Template variables
		self.title        = ko.observable('Join with your email address');
		self.description  = ko.observable('');
		if (typeof params.title !== 'undefined') {	// To avoid problems with template-less components
			self.templateName = ko.observable(params.title.toLowerCase());
		}

		// Registration Parameters
		self.acceptTerms  = ko.observable(false);
		self.genders      = ko.observableArray(['Female', 'Male', 'Unspecified']);
		self.id           = ko.observable('').extend({ required: false });
		self.firstName    = ko.observable('').extend({ required: true });
		self.lastName     = ko.observable('').extend({ required: true });
		self.email        = ko.observable('').extend({ required: true, email: true });
		self.username     = ko.observable('').extend({ required: true, minLength: 4, maxLength: 32 });
		self.password     = ko.observable('').extend({ required: true, minLength: 6, maxLength: 16 });
		self.gender       = ko.observable();

		self.validationModel = ko.validatedObservable({
			id        : self.id,
			firstName : self.firstName,
			lastName  : self.lastName,
			email     : self.email,
			username  : self.username,
			password  : self.password
		});

		// Email Login
		self.emailUser       = ko.observable('').extend({ required: true });
		self.emailPass       = ko.observable('').extend({ required: true });
		self.stayLogged      = ko.observable(false);
		self.loginValidation = ko.validatedObservable({ username: self.emailUser, password: self.emailPass, id: self.id });

		// Functionality
		self.fbRegistration       = function() {
			FB.login(function(response) {
				if (response.status === 'connected') {
					FB.api('/me', function(response) {
						self.title('You are almost ready...');
						self.description('We loaded your account with your Facebook details. Help us with just a few more questions.' +
							' You can always edit this or any other info in settings after joining.');
						self.id('FB' + response.id);
						self.email(response.email);
						self.firstName(response.first_name);
						self.lastName(response.last_name);
						self.username(response.first_name.toLowerCase() + '.' + response.last_name.toLowerCase());
						self.gender(response.gender === 'male' ? 'Male' : (response.gender === 'female' ? 'Female' : 'Unspecified'));
						self.templateName('email');
					});
				}
				else if (response.status === 'not_athorized') {
					// User didn't authorize the application
				}
				else {
					// User is not logged
				}
			}, {scope: 'public_profile, email'});
		}
		self.googleRegistration   = function() {
			gapi.auth.signIn({
				'clientid'     : '712515719334-u6ofvnotfug9ktv0e9kou7ms2cq9lb85.apps.googleusercontent.com',
				'cookiepolicy' : 'single_host_origin',
				'scope'        : 'profile email',
				'callback'     : function(authResult) {
					if (authResult['status']['signed_in']) {
						gapi.client.load('plus','v1', function() {
							var request = gapi.client.plus.people.get({ 'userId': 'me' });
							request.execute(function(response) {
								self.title('You are almost ready...');
								self.description('We loaded your account with your Google details. Help us with just a few more questions.' +
									' You can always edit this or any other info in settings after joining.');
								self.id('GG' + response['id']);
								self.email(response['emails'][0]['value']);
								self.firstName(response['name']['givenName']);
								self.lastName(response['name']['familyName']);
								self.username(response['name']['givenName'].toLowerCase() + '.' + response['name']['familyName'].toLowerCase());
								self.gender(response['gender'] === 'male' ? 'Male' : (response['gender'] === 'female' ? 'Female' : 'Unspecified'));
								self.templateName('email');
							});
						});
					}
				}
			});
		}
		self.emailRegistration    = function() { self.templateName('email'); }
		self.submitRegistration   = function() {
			if (self.validationModel.isValid()) {
				// TODO: Encrypt the password
				var json = ko.toJSON(self.validationModel);
				// TODO: Submit the user information to the server
				$.ajax({
					type        : "post",
					contentType : 'application/json',
					dataType    : 'json',
					processData : false,
					url         : "/user/register",
					data        : json,
					success     : function(data, text) {
						console.log("Success!");
						console.log(data);
						console.log(text);
						self.templateName('postregister');
					},
					error       : function(request, status, error) {
						console.log("Error!");
						console.log(request);
						console.log(error);
						// TODO: Load errors to the form
						self.validationModel.errors.showAllMessages();
					}
				});
			}
			else {
				self.validationModel.errors.showAllMessages();
			}
		}

		self.emailLogin           = function(popup, callback) {
			if (self.loginValidation.isValid()) {
				var json = ko.toJSON(self.loginValidation);
				console.log(json);

				$.ajax({
					type    : "get",
					url     : "/api/login", //?email=" + "finikm@gmail.com&password=123456",
					data    : { email: self.emailUser(), password: self.emailPass() },
					success : function (data, text) {
						// TODO: Redirect to the appropriate page
					},
					error   : function (request, status, error) {
						console.log(request);
						console.log(error);
						// TODO: Show error messages
					}
				});

				// $.post("/api/login", json, function(data, status) {
				// 	// TODO: Before redirecting, if stayLogged is pressed, make sure user stays online (use a cookie?)
				// 	// TODO: Add the user to the global app: app.currentUser('finik');
				// 	console.log("Success");
				// 	app.currentUser(data['username']);
				// 	// TODO: Redirect to the appropriate page
				// });

				if (typeof popup !== 'undefined') {
					self.emailUser(null);
					self.emailPass(null);
					if (popup) { self.closeLoginPopup(); }

					if (typeof callback !== 'undefined') {
						callback();
					}
				}
			}
			else {
				self.loginValidation.errors.showAllMessages();
			}
		}
		self.googleLogin          = function(popup, callback) {
			gapi.auth.signIn({
				'clientid'     : '712515719334-u6ofvnotfug9ktv0e9kou7ms2cq9lb85.apps.googleusercontent.com',
				'cookiepolicy' : 'single_host_origin',
				'scope'        : 'profile email',
				'callback'     : function(authResult) {
					if (authResult['status']['signed_in'] && authResult['status']['method'] === 'PROMPT') {
						gapi.client.load('plus','v1', function() {
							var request = gapi.client.plus.people.get({ 'userId': 'me' });
							request.execute(function(response) {
								self.emailUser(response['emails'][0]['value']);
								self.id('GG' + response['id']);

								var json = ko.toJSON(self.loginValidation);
								console.log(json);
								// TODO: Send to server to sign in
								// TODO: Add the user to the global app: app.currentUser('finik');
								app.currentUser('finik'); // TODO: REMOVE
								if (typeof popup !== 'undefined') {
									if (popup) { self.closeLoginPopup(); }

									if (typeof callback !== 'undefined') {
										callback(params.item);
									}
								}
								else {
									window.location.href = "#";
								}
							});
						});
					}
				}
			});
		}
		self.fbLogin              = function(popup, callback) {
			FB.login(function(response) {
				if (response.status === 'connected') {
					FB.api('/me', function(response) {
						self.id('FB' + response.id);
						self.emailUser(response.email);

						var json = ko.toJSON(self.loginValidation);
						console.log(json);
						// TODO: Send to server to sign in
						// TODO: Add the user to the global app: app.currentUser('finik');
						app.currentUser('finik'); // TODO: REMOVE
						if (typeof popup !== 'undefined') {
							if (popup) { self.closeLoginPopup(); }

							if (typeof callback !== 'undefined') {
								callback(params.item);
							}
						}
						else {
							window.location.href = "#";
						}
					});
				}
			}, {scope: 'email'});
		}

		showLoginPopup            = function(record) {
			$('#loginPopup').addClass('open');
		}

		self.scrollEmail          = function() {
			$('.externalLogin').slideUp();
		}

		self.closeLoginPopup      = function() {
			$('#loginPopup').removeClass('open');
			$('.externalLogin').slideDown();	// Reset dialog state
		}

		self.completeRegistration = function() {
			// TODO: Get values, send to server and redirect to the landing page
		}

		self.route = params.route;
	}

	return { viewModel: LoginRegisterViewModel, template: template };
});
