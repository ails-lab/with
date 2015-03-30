define(['knockout', 'text!./profile.html'  ], function(ko, template) {

	

	function ProfileViewModel(params) {
		var self = this;

	

		self.route = params.route;
	}

	return { viewModel: ProfileViewModel, template: template };
});
