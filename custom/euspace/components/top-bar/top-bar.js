define(['knockout', 'text!./top-bar.html', 'app', 'autocomplete'], function(ko, template, app, autocomplete) {

  function TopBarViewModel(params) {
	  this.route = params.route;
	  
	}

	return { viewModel: TopBarViewModel, template: template };
});
