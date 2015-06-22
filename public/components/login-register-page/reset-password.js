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
		self.messageTitle  = ko.observable();
		self.messageBody   = ko.observable();

		self.token         = ko.observable(params.token);

		// Validate token
		$.ajax({
			type        : "post",
			url         : "/user/changePassword",
			contentType : "application/json",
			data        : JSON.stringify({ "token": params.token }),
			success     : function(data) {
				self.templateName('new-password');
			},
			error       : function(request, status, error) {
				self.messageTitle("Error reseting the password");
				self.messageBody("Invalid or expired token. Please try again.");
				self.templateName("error-message");
			}
		});

		self.resetPassword = function() {
			$.ajax({
				type    : "get",
				url     : "/user/resetPassword/" + self.useremail(),
				success : function(data) {
					self.useremail('');
					app.closePopup();
					// Show message that an email was sent
					$("#myModal").find("h4").html("Password reset");
					$("#myModal").find("div.modal-body").html("<p>An email was successfuly sent. Follow the instructions to create a new password</p>");
					$("#myModal").modal('show');
				},
				error   : function(request, status, error) {
					var err = JSON.parse(request.responseText);
					self.useremail.setError(err.error.email);
					self.useremail.isModified(true);
				}
			});
		};

		self.newPassword   = function() {
			$.ajax({
				type        : "post",
				url         : "/user/changePassword",
				contentType : "application/json",
				data        : JSON.stringify({ "token": params.token, "password": self.password() }),
				success     : function(data) {
					self.messageTitle("Password Reset");
					self.messageBody("Password changed successfuly. You can now login to WITH");
					self.templateName("error-message");
				},
				error       : function(request, status, error) {
					var err = JSON.parse(request.responseText);
					$("#myModal").find("h4").html("Error");
					$("#myModal").find("div.modal-body").html("<p>" + err.error + "</p>");
					$("#myModal").modal('show');
				}
			});
		};

		self.gotoLogin     = function() {
			window.location.href="/assets/index.html#login";
		};
	}

	return { viewModel: ResetPasswordViewModel, template: template };
});