define(['knockout', 'text!./reset-password.html', 'app', 'knockout-validation',], function(ko, template, app) {

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	function ResetPasswordViewModel(params) {
		var self           = this;
		self.useremail     = ko.observable().extend({ required: true });
		self.password      = ko.observable().extend({ required: true, minLength: 6, maxLength: 16 });
		self.password2     = ko.observable().extend({ equal: self.password });
		self.templateName  = ko.observable('new-password');

		self.token         = ko.observable(params.token);

		self.resetPassword = function() {
			$.ajax({
				type    : "get",
				url     : "/user/resetPassword/" + self.useremail(),
				success : function(data) {
					self.useremail('');
					app.closePopup();
				},
				error   : function(request, status, error) {
					var err = JSON.parse(request.responseText);
					self.useremail.setError(err.error.email);
					self.useremail.isModified(true);
					// console.log(request);
				}
			});
		};

		self.newPassword   = function() {
			$.ajax({
				type    : "post",
				url     : "/user/changePassword",
				data    : "",
				success : function(data) {
					// TODO: Show message that password has changed successfuly
				},
				error   : function(request, status, error) {
					// TODO: Display appropriate error for not being able to change password
				}
			});
		};

		self.testToken     = function() {
			$.ajax({
				type    : "get",
				url     : "",
				success : function(data) {
					// TODO: Load the form
				},
				error   : function(request, status, error) {
					// TODO: Show appopriate message
				}
			});
		};
	}

	return { viewModel: ResetPasswordViewModel, template: template };
});