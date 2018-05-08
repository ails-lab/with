define(['jquery','knockout', './router', './app','knockout-mapping', 'bootstrap', 'knockout-projections', 'knockout-amd-helpers', 'header'], function ($, ko, router,app, kom) {

	// Knockout AMD Helpers Initialization
	ko.amdTemplateEngine.defaultPath                  = 'templates';
	ko.amdTemplateEngine.defaultSuffix                = '.tpl.html';
	ko.amdTemplateEngine.defaultRequireTextPluginName = 'text';

	ko.mapping = kom;

	// Components can be packaged as AMD modules, such as the following:
	ko.components.register('nav-bar', { require: 'components/nav-bar/nav-bar' });
	ko.components.register('top-bar', { require: 'components/top-bar/top-bar'});
	ko.components.register('side-bar', { require: 'components/side-bar/side-bar'});
	ko.components.register('home-page', { require: 'components/home-page/home' });
	ko.components.register('dashboard', { require: 'components/dashboard/dashboard' });
	ko.components.register('main-content', { require: 'components/main-content/main-content' });
	ko.components.register('search-page', { require: 'components/search-page/search' });
	ko.components.register('item-view', { require: 'components/item-view/item' });
	ko.components.register('collection', { require: 'components/collection/collection' });
	ko.components.register('login-page', { require: 'components/login-register-page/login-register' });
	ko.components.register('register-page', { require: 'components/login-register-page/login-register' });
	ko.components.register('myexhibitions', { require: 'components/myexhibitions/myexhibitions' });
	ko.components.register('mycollections', { require: 'components/mycollections/mycollections' });
	ko.components.register('myannotations', { require: 'components/myannotations/myannotations' });
	ko.components.register('organizations', { require: 'components/myorganizations/myorganizations' });
	ko.components.register('organization-edit', {
		viewModel: { require: 'components/organization-edit/organization-edit'},
		template: { require: 'text!components/organization-edit/organization-edit.html' }
	});
	ko.components.register('collection-view', { require: 'components/collection-view/collection-view' });
	ko.components.register('exhibition-view', { require: 'components/exhibition-view/exhibition-view' });
	ko.components.register('3DRoom', { require: 'components/3DRoom/room' });
	ko.components.register('facets', { require: 'components/facets/facets' });
	ko.components.register('exhibition-edit', { require: 'components/exhibition-edit/exhibition-edit' });
	ko.components.register('popup-exhibition-edit', { require: 'components/exhibition-edit/popup-exhibition-edit' });
	ko.components.register('api-docu', { require: 'components/api-documentation/api-documentation' });
	ko.components.register('notifications-page', {
		viewModel: { require: 'components/notifications/notifications' },
		template: { require: 'text!components/notifications/notifications-page.html' }
	});

	ko.components.register('testsearch', { require: 'components/testsearch/testsearch' });
	ko.components.register('import-collections', { require: 'components/import-collections/import-collections' });

	ko.components.register('myfavorites', {
		viewModel: { require: 'components/collection-view/collection-view' },
		template: { require: 'text!components/collection-view/_myfavorites.html' }
	});
	ko.components.register('itemview', {
		viewModel: { require: 'components/item-view/item' },
		template: { require: 'text!components/item-view/item.html' }
	});

	ko.components.register('popup-login', {
		viewModel: { require: 'components/login-register-page/login-register' },
		template: { require: 'text!components/login-register-page/popup-login.html' }
	});
	ko.components.register('profile', { require: 'components/profile-page/profile' });
	ko.components.register('edit-collection', {
		viewModel: { instance: 'components/mycollections/mycollections' },
		template: { require: 'text!components/mycollections/edit-collection.html' }
	});
	ko.components.register('share-collection', {
		viewModel: { instance: 'components/mycollections/mycollections' },
		template: { require: 'text!components/mycollections/share-collection.html' }
	});
	ko.components.register('reset-password', { require: 'components/login-register-page/reset-password' });
	ko.components.register('new-password', {
		viewModel: { require: 'components/login-register-page/reset-password' },
		template: { require: 'text!components/login-register-page/login-register.html' }
	});
	ko.components.register('image-upload', {
		viewModel: { require: 'components/media-uploader/media-uploader' },
		template: { require: 'text!components/media-uploader/_image-upload.html' }
	});
	ko.components.register('organization-page', {
		viewModel: { require: 'components/organization-page/organization-page' },
		template: { require: 'text!components/organization-page/organization-page.html' }
	});
	ko.components.register('group-page', {
		viewModel: { require: 'components/organization-page/provider' },
		template: { require: 'text!components/organization-page/provider.html' }
	});

	ko.components.register('new-organization', {
		viewModel: { require: 'components/organization-page/organization-page' },
		template: { require: 'text!components/organization-page/new-organization.html' }
	});
	ko.components.register('members-popup', {
		viewModel: { require: 'components/members/members' },
		template: { require: 'text!components/members/members-popup.html' }
	});

	ko.components.register('about', { require: 'components/statichtml/statichtml' });
	ko.components.register('terms', { require: 'components/statichtml/statichtml' });
	ko.components.register('faq', { require: 'components/statichtml/statichtml' });
	ko.components.register('privacy', { require: 'components/statichtml/statichtml' });
	ko.components.register('feedback', { require: 'components/statichtml/statichtml' });

	// ... or for template-only components, you can just point to a .html file directly:
	ko.components.register('empty', { template: '&nbsp;' });

	// [Scaffolded component registrations will be inserted here. To retain this feature, don't remove this comment.]
	popupName = ko.observable('empty');
	popupParams = ko.observable({});

	// Start the application
	ko.applyBindings({ route: router.currentRoute, popupName: popupName, popupParams: popupParams });
});
