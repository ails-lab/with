define(["knockout", "text!./home.html"], function(ko, homeTemplate) {

	
	
	function HomeViewModel(params) {
		 this.route = params.route;
		 document.body.setAttribute("data-page","home");
	}

	return { viewModel: HomeViewModel, template: homeTemplate };
});
