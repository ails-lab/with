define(['jquery', 'knockout', './router', 'bootstrap', 'knockout-projections'], function($, ko, router) {

	// Components can be packaged as AMD modules, such as the following:
	ko.components.register('nav-bar', { require: 'components/nav-bar/nav-bar' });
	ko.components.register('side-bar', { require: 'components/side-bar/side-bar'});
	ko.components.register('home-page', { require: 'components/home-page/home' });
	ko.components.register('main-content', { require: 'components/main-content/main-content' });
	ko.components.register('login-page', {
		viewModel: { require: 'components/login-register-page/login-register' },
		template: { require: 'text!components/login-register-page/login.html'}
	});
	ko.components.register('register-page', {
		viewModel: { require: 'components/login-register-page/login-register' },
		template: { require: 'text!components/login-register-page/register.html'}
	});

	// ... or for template-only components, you can just point to a .html file directly:
	ko.components.register('search-page', { template: { require: 'text!components/search-page/search.html' } });


	// [Scaffolded component registrations will be inserted here. To retain this feature, don't remove this comment.]

	// Start the application
	ko.applyBindings({ route: router.currentRoute });
});
