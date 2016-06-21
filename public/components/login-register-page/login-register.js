define(['knockout', 'text!./login-register.html',  'facebook', 'app', 'knockout-validation', 'google'], function (ko, template, FB, app) {

	if (window.location.origin === 'http://localhost:9000') {
		FB.init({
			appId   : '1692894374279432',
			status  : true,
			version : 'v2.2'
		});
	} else {
		FB.init({
			appId   : '1584816805087190',
			status  : true,
			version : 'v2.2'
		});
	}

	ko.validation.init({
		errorElementClass: 'error',
		errorMessageClass: 'errormsg',
		decorateInputElement: true
	});

	function LoginRegisterViewModel(params) {
		var self = this;
		self.callback = null;
		if (params.callback) {
			self.callback = params.callback;
		}

		// If User is already logged in, then redirect him to dashboard
		if (isLogged()) {
			window.location.href = "#dashboard";
		}

		function tokenRequest(event) {
			var origin = event.origin;
			var source = event.source;

			if (event.data == "requestToken") {
				if (isLogged()) {
					// we trust localhost and ntua,
					// TODO: make a requester for allow access to origin
					if (new RegExp("^https?://localhost:?/?").test(origin) ||
						new RegExp("^https?://[^/:]*.image.ntua.gr:?/?").test(origin)	||
								false /* or new RegExp in here */) {
						$.ajax({
							url: "/user/token",
							type: "GET"
						}).done(function (data,text) {
							source.postMessage("Token: " + data, "*");
							window.removeEventListener("message", tokenRequest);

						});
					} else {
						console.log("rejected from " + origin);
					}
				}
			}
		}
		window.addEventListener("message", tokenRequest, false);

		// Template variables
		self.title        = ko.observable('Register with your email address');
		self.description  = ko.observable('');
		self.hasAccount   = ko.observable(false);
		if (typeof params.title !== 'undefined') {	// To avoid problems with template-less components
			self.templateName = ko.observable(params.title.toLowerCase());
		}

		// Registration Parameters
		self.acceptTerms  = ko.observable(false);
		self.genders      = ko.observableArray(['Female', 'Male', 'Unspecified']);
		self.facebookid   = ko.observable('');
		self.googleid     = ko.observable('');
		self.firstName    = ko.observable('').extend({ required: true });
		self.lastName     = ko.observable('').extend({ required: true });
		self.email        = ko.observable('').extend({ required: true, email: true });
		self.username     = ko.observable('').extend({ required: true, minLength: 4, maxLength: 32 });
		self.password     = ko.observable('').extend({
			required  : {
				onlyIf : function () {
					return (self.facebookid() === '' && self.googleid() === '');
				}
			}
		});
		self.password2    = ko.observable('').extend({ equal: self.password });
		self.gender       = ko.observable();
		self.record       = ko.observable(params.item);

		self.validationModel = ko.validatedObservable({
			firstName : self.firstName,
			lastName  : self.lastName,
			email     : self.email,
			username  : self.username,
			password  : self.password,
			password2 : self.password2
		});

		// Email Login
		self.emailUser       = ko.observable('').extend({ required: true });
		self.emailPass       = ko.observable('').extend({ required: true });
		self.stayLogged      = ko.observable(false);
		self.loginValidation = ko.validatedObservable({ email: self.emailUser, password: self.emailPass });

		// Control variables
		self.usingEmail  = ko.observable(true);

		// Functionality
		self.fbRegistration       = function () {
			FB.login(function (response) {
				if (response.status === 'connected') {
					FB.api('/me', function (response) {
						self.title('You are almost ready...');
						self.description('We loaded your account with your Facebook details. Help us with just a few more questions.' +
							' You can always edit this or any other info in settings after joining.');
						self.facebookid(response.id);
						self.email(response.email);
						self.firstName(response.first_name);
						self.lastName(response.last_name);
						self.username(response.first_name.toLowerCase() + '.' + response.last_name.toLowerCase());
						self.gender(response.gender === 'male' ? 'Male' : (response.gender === 'female' ? 'Female' : 'Unspecified'));
						self.usingEmail(false);

						$.ajax({
							type    : "get",
							url     : "/user/emailAvailable?email=" + response.email,
							success : function () {
								self.templateName('email');
							},
							error   : function () {
								self.hasAccount(true);
								self.templateName('login');
								window.history.pushState(null, "Login", "#login");
							}
						});
					});
				} else if (response.status === 'not_athorized') {
					// User didn't authorize the application
				} else {
					// User is not logged
				}
			}, {scope: 'public_profile, email'});
		};

		self.googleRegistration   = function () {
			gapi.auth.signIn({
				'clientid'     : '712515719334-u6ofvnotfug9ktv0e9kou7ms2cq9lb85.apps.googleusercontent.com',
				'cookiepolicy' : 'single_host_origin',
				'scope'        : 'profile email',
				'callback'     : function (authResult) {
					if (authResult.status.signed_in && authResult.status.method === 'PROMPT') {
						gapi.client.load('plus', 'v1', function () {
							var request = gapi.client.plus.people.get({ 'userId': 'me' });
							request.execute(function (response) {
								self.title('You are almost ready...');
								self.description('We have loaded your profile with your Google details. Help us with just a few more questions.' +
									' You can always edit this or any other info in settings after joining.');
								self.googleid(response.id);
								self.email(response.emails[0].value);
								self.firstName(response.name.givenName);
								self.lastName(response.name.familyName);
								self.username(response.name.givenName.toLowerCase() + '.' + response.name.familyName.toLowerCase());
								self.gender(response.gender === 'male' ? 'Male' : (response.gender === 'female' ? 'Female' : 'Unspecified'));
								self.usingEmail(false);

								$.ajax({
									type    : "get",
									url     : "/user/emailAvailable?email=" + response.emails[0].value,
									success : function () {
										self.templateName('email');
									},
									error   : function (request, status, error) {
										self.hasAccount(true);
										self.templateName('login');
										window.history.pushState(null, "Login", "#login");
									}
								});
							});
						});
					}
				}
			});
		};

		self.emailRegistration    = function () {
			self.usingEmail(true);
			self.templateName('email');
		};

		self.submitRegistration   = function () {
			if (self.validationModel.isValid()) {
				var data = {
					firstName  : self.firstName,
					lastName   : self.lastName,
					username   : self.username,
					email      : self.email,
					password   : self.password,
					gender     : self.gender,
					googleId   : self.googleid,
					facebookId : self.facebookid
				};

				var json = ko.toJSON(data);
				$.ajax({
					type        : "post",
					contentType : 'application/json',
					dataType    : 'json',
					processData : false,
					url         : "/user/register",
					data        : json,
					success     : function (data, text) {
						var promises = app.loadUser(data);
						/*make sure call is finished before proceeding*/
						$.when.apply($, promises).done(function () {
							self.completeRegistration();
						});
					},
					error       : function (request, status, error) {
						var err = JSON.parse(request.responseText);
						if (err.error.email !== undefined) {
							self.email.setError(err.error.email);
							self.email.isModified(true);
						}
						if (err.error.username !== undefined) {
							self.username.setError(err.error.username + " (Suggestions: " + err.proposal[0] + ", " + err.proposal[1] + ")");
							self.username.isModified(true);
						}
						if (err.error.password !== undefined) {
							self.password.setError(err.error.password);
							self.password.isModified(true);
						}
						self.validationModel.errors.showAllMessages();
					}
				});
			} else {
				self.validationModel.errors.showAllMessages();
			}
		};

		self.emailLogin           = function (popup) {
			if (self.loginValidation.isValid()) {
				var json = ko.toJSON(self.loginValidation);

				$.ajax({
					type        : "post",
					contentType : 'application/json',
					dataType    : 'json',
					processData : false,
					url         : "/user/login",
					data        : json,
					success     : function (data, text) {
						var promises = app.loadUser(data, self.stayLogged());
						$.when.apply($, promises).done(function () {
							if (typeof popup !== 'undefined') {
								self.emailUser(null);
								self.emailPass(null);
								if (popup) {
									self.closeLoginPopup();
								}

								if (self.callback != null) {
									self.callback(self.record());
								}
							} else {
								window.location.href = "#dashboard";
								window.location.reload(true);
							}
						});
						$("#loginPopup").trigger("loginEvent", []);
					},
					error   : function (request, status, error) {
						var err = JSON.parse(request.responseText);
						if (err.error.email !== undefined) {
							self.emailUser.setError(err.error.email);
							self.emailUser.isModified(true);
						}
						if (err.error.password !== undefined) {
							self.emailPass.setError(err.error.password);
							self.emailPass.isModified(true);
						}
						self.loginValidation.errors.showAllMessages();
					}
				});
			} else {
				self.loginValidation.errors.showAllMessages();
			}
		};

		self.googleLogin          = function (popup) {
			gapi.auth.signIn({
				'clientid'     : '712515719334-u6ofvnotfug9ktv0e9kou7ms2cq9lb85.apps.googleusercontent.com',
				'cookiepolicy' : 'single_host_origin',
				'scope'        : 'profile email',
				'callback'     : function (authResult) {
					if (authResult.status.signed_in && authResult.status.method === 'PROMPT') {
						gapi.client.load('plus', 'v1', function () {
							var request = gapi.client.plus.people.get({ 'userId': 'me' });
							request.execute(function (response) {
								var data = {
									accessToken : authResult.access_token,
									googleId    : response.id
								};
								var json = ko.toJSON(data);

								$.ajax({
									type        : "post",
									contentType : "application/json",
									dataType    : "json",
									processData : "false",
									url         : "/user/login",
									data        : json,
									success     : function (data, text) {
										var promises = app.loadUser(data, true);

										/*make sure call is finished before proceeding*/
										$.when.apply($, promises).done(function () {

											if (typeof popup !== 'undefined') {
												if (popup) {
													self.closeLoginPopup();
												}

												if (self.callback != null) {
													self.callback(self.record());
												}
											} else {
												window.location.href = "#dashboard";
												window.location.reload(true);
											}
										});
										$("#loginPopup").trigger("loginEvent", []);
									},
									error       : function (request, status, error) {
										var err = JSON.parse(request.responseText);
										if (err.error === "User not registered") {
											self.title('Before you use WITH for the first time, we have to create your profile...');
											self.description('We have loaded your profile with your Google details. Help us with just a few more questions.' +
												' You can always edit this or any other info in settings after joining.');
											self.googleid(response.id);
											self.email(response.emails[0].value);
											self.firstName(response.name.givenName);
											self.lastName(response.name.familyName);
											self.username(response.name.givenName.toLowerCase() + '.' + response.name.familyName.toLowerCase());
											self.gender(response.gender === 'male' ? 'Male' : (response.gender === 'female' ? 'Female' : 'Unspecified'));
											self.usingEmail(false);
											self.templateName('email');
										}
									}
								});
							});
						});
					}
				}
			});
		};

		self.fbLogin              = function (popup) {
			FB.login(function (response) {
				if (response.status === 'connected') {
					FB.api('/me', function (response) {
						var data = {
							accessToken : FB.getAuthResponse().accessToken,
							facebookId  : response.id
						};

						var json = ko.toJSON(data);

						$.ajax({
							type        : "post",
							contentType : "application/json",
							dataType    : "json",
							processData : "false",
							url         : "/user/login",
							data        : json,
							success     : function (data, text) {
								var promises = app.loadUser(data, true);
								$.when.apply($, promises).done(function () {
									if (typeof popup !== 'undefined') {
										if (popup) {
											self.closeLoginPopup();
										}

										if (self.callback != null) {
											self.callback(self.record());
										}
									} else {
										window.location.href = "#dashboard";
										window.location.reload(true);
									}
								});
								$("#loginPopup").trigger("loginEvent", []);
							},
							error       : function (request, status, error) {
								var err = JSON.parse(request.responseText);
								if (err.error === "User not registered" || err.error === "Couldn't validate user") {
									self.title('Before you use WITH for the first time, we have to create your profile...');
									self.description('We have loaded your profile with your Facebook details. Help us with just a few more questions.' +
										' You can always edit this or any other info in settings after joining.');
									self.facebookid(response.id);
									self.email(response.email);
									self.firstName(response.first_name);
									self.lastName(response.last_name);
									self.username(response.first_name.toLowerCase() + '.' + response.last_name.toLowerCase());
									self.gender(response.gender === 'male' ? 'Male' : (response.gender === 'female' ? 'Female' : 'Unspecified'));
									self.usingEmail(false);
									self.templateName('email');
								}
							}
						});
					});
				}
			}, {scope: 'email'});
		};

		self.forgotPassword       = function () {
			app.showPopup('reset-password');
		};

		showLoginPopup            = function (record) {
			self.record(record);
			$('#loginPopup').addClass('open');
		};

		self.scrollEmail          = function () {
			$('.externalLogin').slideUp();
		};

		self.scrollDownEmail      = function () {
			$('.externalLogin').slideDown();
		};

		self.closeLoginPopup      = function () {
			$('#loginPopup').removeClass('open');
			$('.externalLogin').slideDown();	// Reset dialog state
		};

		self.completeRegistration = function () {
			// TODO: Get values, send to server
			var withLoginPopup = localStorage.getItem('withLoginPopup');
			//alert(withLoginPopup);
			if (withLoginPopup) {
				window.location.href = withLoginPopup;
				localStorage.removeItem(withLoginPopup);
			} else {
				window.location.href = "#dashboard";
			}
		};

		self.route = params.route;
	}

	return { viewModel: LoginRegisterViewModel, template: template };
});
