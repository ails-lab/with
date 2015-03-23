define(['knockout', 'text!./top-bar.html', 'app'], function(ko, template, app) {

  function TopBarViewModel(params) {

		// This viewmodel doesn't do anything except pass through the 'route' parameter to the view.
		// You could remove this viewmodel entirely, and define 'side-bar' as a template-only component.
		// But in most apps, you'll want some viewmodel logic to determine what navigation options appear.

		this.route = params.route;

	}

	return { viewModel: TopBarViewModel, template: template };
});
