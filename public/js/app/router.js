define(["knockout", "crossroads", "hasher"], function (ko, crossroads, hasher) {

	// This module configures crossroads.js, a routing library. If you prefer, you
	// can use any other routing library (or none at all) as Knockout is designed to
	// compose cleanly with external libraries.
	//
	// You *don't* have to follow the pattern established here (each route entry
	// specifies a 'page', which is a Knockout component) - there's nothing built into
	// Knockout that requires or even knows about this technique. It's just one of
	// many possible ways of setting up client-side routes.

	return new Router({
		routes: [
			{ url: '',          params: { page: 'home-page',     title: 'Home' } },
			{ url: 'home',          params: { page: 'home-page',     title: 'Home' } },
			{ url: 'dashboard', params: { page: 'dashboard', title: 'Dashboard' } },
			{ url: 'search',    params: { page: 'search-page',   title: 'Search' } },
			{ url: 'login',     params: { page: 'login-page',    title: 'Login' } },
			{ url: 'profile',     params: { page: 'profile',    title: 'Profile' } },
			{ url: 'mycollections',     params: { page: 'mycollections',    title: 'My Collections',    showsExhibitions: false } },
			{ url: 'myexhibitions',     params: { page: 'mycollections',    title: 'My Exhibitions',   showsExhibitions: true } },
			{ url: 'register',  params: { page: 'register-page', title: 'Register'} },
			{ url: 'email',     params: { page: 'email-page',    title: 'Register' } },
			{ url: 'item/{id}',     params: { page: 'itemview',    title: 'Media Item' } },
			{ url: 'collect/{id}',     params: { page: 'item-view',    title: 'Collect' } },
			{ url: 'collectionview/{id}',     params: { page: 'collection-view',    title: 'Collection View', type: 'collection' } },
			{ url: 'collectionview/{id}/count/{count}',     params: { page: 'collection-view',    title: 'Collection View', type: 'collection' } },
			{ url: 'exhibitionview/{id}',     params: { page: 'exhibition-view',    title: 'Exhibition View' } },
			{ url: 'reset/{token}', params: { page: 'new-password', title: 'Reset Password' } },
			{ url: 'exhibition-edit',     params: { page: 'exhibition-edit',    title: 'Exhibition Edit' } },
			{ url: 'exhibition-edit/{id}',     params: { page: 'exhibition-edit',    title: 'Exhibition Edit' } },
			{ url: 'apidoc', params: { page: 'api-docu', title: 'API Documentation' } },
			{ url: 'testsearch', params: { page: 'testsearch', title: 'testsearch' } },
			{ url: 'import-collections', params: { page: 'import-collections', title: 'import-collections' } },
			{ url: 'myfavorites', params: { page: 'collection-view', title: 'My Favorites', type: 'favorites' } },
			{ url: 'gallery/{id}/{skin}',     params: { page: '3DRoom',    title: 'Gallery View' } },
			{ url: 'organization/{id}', params: { page: 'group-page', title: 'Organization', 'type': 'organization' } },
			{ url: 'organization/{id}/edit', params: { page: 'organization-edit', title: 'Edit Organization', type: 'organization', name: 'Organization' }},
			{ url: 'project/{id}/edit', params: { page: 'organization-edit', title: 'Edit Project', type: 'project', name: 'Project' }},
			{ url: 'organization/{id}/count/{count}',     params: { page: 'group-page', title: 'Organization', 'type': 'organization' } },
			{ url: 'organizations', params: { page: 'organizations', title: 'Organizations', 'type': 'Organization' } },
			{ url: 'projects', params: { page: 'organizations', title: 'Projects', 'type': 'Project' } },
			{ url: 'project/{id}', params: { page: 'group-page', title: 'Project', 'type': 'project' } },
			{ url: 'notifications', params: { page: 'notifications-page', title: 'Notifications' } }
		]
	});

	function Router(config) {
		var currentRoute = this.currentRoute = ko.observable({});

		ko.utils.arrayForEach(config.routes, function (route) {
			crossroads.addRoute(route.url, function (requestParams) {
				currentRoute(ko.utils.extend(requestParams, route.params));
			});
		});

		activateCrossroads();
	}

	function activateCrossroads() {
		//temp fix: scrollbar moves to top when route changes
		function resetScroll() {
			document.body.scrollTop = document.documentElement.scrollTop = 0;
		}
		function parseHash(newHash, oldHash) {
			if (oldHash) {
				if (oldHash.indexOf("provider/") === 0) {
					var scrollPosition = $(window).scrollTop();
					oldHash = oldHash.substring(9);
					oldHash = oldHash.substring(0, oldHash.indexOf('/'));
					sessionStorage.setItem("provider" + oldHash, scrollPosition);
				} else if (oldHash.indexOf("collectionview/") === 0) {
					var scrollPosition = $(window).scrollTop();
					oldHash = oldHash.substring(15);
					if (oldHash.indexOf('/') != -1) {
						oldHash = oldHash.substring(0, oldHash.indexOf('/'));
					}

					sessionStorage.setItem("collection-viewscroll" + oldHash, scrollPosition);
				}
			}
			crossroads.parse(newHash);

		}

		crossroads.ignoreState = true;
		crossroads.normalizeFn = crossroads.NORM_AS_OBJECT;
		hasher.initialized.add(parseHash);
		hasher.changed.add(parseHash);
		hasher.changed.add(resetScroll);

		hasher.init();
	}
});
