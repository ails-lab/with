define(['jquery', 'knockout', './router', 'bootstrap', 'knockout-projections', 'knockout-amd-helpers'], function($, ko, router) {

	// Knockout AMD Helpers Initialization
	ko.amdTemplateEngine.defaultPath                  = 'templates';
	ko.amdTemplateEngine.defaultSuffix                = '.tpl.html';
	ko.amdTemplateEngine.defaultRequireTextPluginName = 'text';

	// Components can be packaged as AMD modules, such as the following:
	ko.components.register('nav-bar', { require: 'components/nav-bar/nav-bar' });
	ko.components.register('side-bar', { require: 'components/side-bar/side-bar'});
	ko.components.register('home-page', { require: 'components/home-page/home' });
	ko.components.register('main-content', { require: 'components/main-content/main-content' });
	ko.components.register('search-page', { require: 'components/search-page/search' });
	ko.components.register('item-view', { require: 'components/item-view/item' });
	ko.components.register('collection', { require: 'components/collection/collection' });
	ko.components.register('login-page', { require: 'components/login-register-page/login-register' });
	ko.components.register('register-page', { require: 'components/login-register-page/login-register' });
	ko.components.register('popup-login', {
		viewModel: { require: 'components/login-register-page/login-register' },
		template: { require: 'text!components/login-register-page/popup-login.html' }
	});

	// ... or for template-only components, you can just point to a .html file directly:

	// [Scaffolded component registrations will be inserted here. To retain this feature, don't remove this comment.]

	// Start the application
	ko.applyBindings({ route: router.currentRoute });
});
