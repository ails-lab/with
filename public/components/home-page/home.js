define(["knockout", "text!./home2.html"], function(ko, homeTemplate) {

	
	
	function HomeViewModel(params) {
				

		
		this.route = params.route;
	}

	return { viewModel: HomeViewModel, template: homeTemplate };
});
