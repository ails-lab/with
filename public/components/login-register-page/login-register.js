define(['knockout', 'text!./login-register.html',  'facebook', 'knockout-validation'], function(ko, template, FB) {

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
		self.templateName = ko.observable(params.title);

		// Registration Parameters
		self.acceptTerms  = ko.observable(false);
		self.genders      = ko.observableArray(['Female', 'Male', 'Unspecified']);
		self.firstName    = ko.observable('').extend({ required: true });
		self.lastName     = ko.observable('').extend({ required: true });
		self.email        = ko.observable('').extend({ required: true, email: true });
		self.username     = ko.observable('').extend({ required: true, minLength: 4, maxLength: 32 });
		self.password     = ko.observable('').extend({ required: true, minLength: 6, maxLength: 16 });
		self.gender       = ko.observable();

		self.validationModel = ko.validatedObservable({
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
		self.loginValidation = ko.validatedObservable({ emailUser: self.emailUser, emailPass: self.emailPass });

		// Functionality
		self.fbRegistration       = function() {
			FB.login(function(response){
				if (response.status === 'connected') {
					FB.api('/me', function(response) {
						self.title('You are almost ready...');
						self.description('We loaded your account with your Facebook details. Help us with just a few more questions.' +
							" You can always edit this or any other info in settings after joining.");
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
			// TODO: Perform a registration using information from Google
		}
		self.emailRegistration    = function() { self.templateName('email'); }
		self.submitRegistration   = function() {
			if (self.validationModel.isValid()) {
				// TODO: Encrypt the password
				var json = ko.toJSON(self.validationModel);
				// TODO: Submit the user information to the server
				console.log(json);
				self.templateName('postregister');
			}
			else {
				self.validationModel.errors.showAllMessages();
			}
		}

		self.emailLogin           = function() {
			if (self.loginValidation.isValid()) {
				var json = ko.toJSON(self.loginValidation);
				console.log(json);
				// TODO: Try to login. If OK, then redirect to the appropriate page (post-registration if missing, landing otherwise)
				// TODO: Before redirecting, if stayLogged is pressed, make sure user stays online (use a cookie?)
			}
			else {
				self.loginValidation.errors.showAllMessages();
			}
		}

		self.completeRegistration = function() {
			// TODO: Get values, send to server and redirect to the landing page
		}

		self.route = params.route;
	}

	return { viewModel: LoginRegisterViewModel, template: template };
});
