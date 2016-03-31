define(['jquery','knockout', './router', 'knockout-mapping', 'bootstrap', 'knockout-projections', 'knockout-amd-helpers', 'header','./params','./app'], function ($, ko, router, kom,params,app) {

	// Knockout AMD Helpers Initialization
	ko.amdTemplateEngine.defaultPath                  = 'templates';
	ko.amdTemplateEngine.defaultSuffix                = '.tpl.html';
	ko.amdTemplateEngine.defaultRequireTextPluginName = 'text';

	ko.mapping = kom;

	// Components can be packaged as AMD modules, such as the following:
	ko.components.register('top-bar', { require: 'components/top-bar/top-bar'});
	ko.components.register('home-page', { require: 'components/home-page/home' });
	ko.components.register('main-content', { require: 'components/main-content/main-content' });
	ko.components.register('search-page', { require: 'components/search-page/search' });
	ko.components.register('item-view', { require: 'components/item-view/item' });
	ko.components.register('collection-popup', { require: 'components/collection-popup/collection-popup' });
	ko.components.register('collection-view', { require: 'components/collection-view/collection-view' });
	ko.components.register('exhibition-view', { require: 'components/exhibition-view/exhibition-view' });
	ko.components.register('facets', { require: 'components/facets/facets' });
	ko.components.register('providers', { require: 'components/providers/providers' });
	ko.components.register('provider', { require: 'components/provider/provider' });
	ko.components.register('about', { require: 'components/statichtml/statichtml' });
	ko.components.register('contact', { require: 'components/statichtml/statichtml' });
	ko.components.register('privacy', { require: 'components/statichtml/statichtml' });
	ko.components.register('terms', { require: 'components/statichtml/statichtml' });
	ko.components.register('login-page', { require: 'components/login-register-page/login-register' });
	
	ko.components.register('thesaurus', { require: 'components/thesaurus/thesaurus' });
	
	ko.components.register('popup-login', {
		viewModel: { require: 'components/login-register-page/login-register' },
		template: { require: 'text!components/login-register-page/popup-login.html' }
	});
	// ... or for template-only components, you can just point to a .html file directly:
	
	
	ko.components.register('empty', { template: '&nbsp;' });

	// [Scaffolded component registrations will be inserted here. To retain this feature, don't remove this comment.]
	popupName = ko.observable('empty');
	popupParams = ko.observable({});


	// Start the application
	ko.applyBindings({ route: router.currentRoute, popupName: popupName, popupParams: popupParams });
});
