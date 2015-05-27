define(['knockout', 'text!./reset-password.html', 'app', 'knockout-validation',], function(ko, template, app) {

	function ResetPasswordViewModel(params) {
		var self       = this;
		self.useremail = ko.observable().extend({ required: true });

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
	}

	return { viewModel: ResetPasswordViewModel, template: template };
});