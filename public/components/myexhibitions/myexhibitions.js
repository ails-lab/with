define(['knockout', 'text!./myexhibitions.html', 'app'], function(ko, template, app) {

	
	function MyExhibitionsModel(params) {
		
		var self = this;
		self.route = params.route;
		
	}
	
	return {viewModel: MyExhibitionsModel, template: template};
});
