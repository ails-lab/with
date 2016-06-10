define(['bootstrap', 'knockout', 'text!./myannotations.html', 'knockout-else','app', 'moment', 'knockout-validation', 'easypiechart'], function (bootstrap, ko, template, KnockoutElse, app, moment) {
	
	function MyAnnotationsModel(params) {
		KnockoutElse.init([spec = {}]);
		var self = this;
		self.route = params.route;
	}
	
	return {viewModel: MyAnnotationsModel, template: template};
});