define(['knockout', 'text!./members.html', 'app'], function(ko, template, app) {

	function MemberViewModel(params) {
		var self = this;

		self.closeWindow   = function() {
			app.closePopup();
		};
	}

	return { viewModel: MemberViewModel, template: template };
});