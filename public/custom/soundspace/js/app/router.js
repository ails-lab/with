define(["knockout", "crossroads", "hasher"], function(ko, crossroads, hasher) {

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
			{ url: 'search',    params: { page: 'search-page',   title: 'Search' } },
			{ url: 'login',     params: { page: 'login-page',    title: 'Login' } },
			{ url: 'profile',     params: { page: 'profile',    title: 'Profile' } },
			{ url: 'mycollections',     params: { page: 'mycollections',    title: 'My Collections',    showsExhibitions: false } },
			{ url: 'myexhibitions',     params: { page: 'mycollections',    title: 'My Exhibitions',   showsExhibitions: true } },
			{ url: 'annotations-end', params: { page: 'annotations-end', title: 'Annotations End' } },
			{ url: 'register',  params: { page: 'register-page', title: 'Register'} },
			{ url: 'email',     params: { page: 'email-page',    title: 'Register' } },
			{ url: 'collect/{id}',     params: { page: 'item-view',    title: 'Collect' } },
			{ url: 'item/{id}',     params: { page: 'item-view',    title: 'Media Item' } },
			{ url: '!item/{id}',     params: { page: 'item-view',    title: 'Media Item' } },
			{ url: 'provider/{id}',     params: { page: 'provider',    title: 'Content Provider' } },
			{ url: 'provider/{id}/count/{count}',     params: { page: 'provider',    title: 'Content Provider' } },
			{ url: 'providers',     params: { page: 'providers',    title: 'Content Providers' } },
			{ url: 'collectionview/{id}',     params: { page: 'collection-view',    title: 'Collection View' } },
			{ url: 'collectionview/{id}/count/{count}',     params: { page: 'collection-view',    title: 'Collection View' } },
			{ url: 'exhibitionview/{id}',     params: { page: 'exhibition-view',    title: 'Exhibition View' } },
			{ url: 'reset/{token}', params: { page: 'new-password', title: 'Reset Password' } },
			{ url: 'exhibition-edit',     params: { page: 'exhibition-edit',    title: 'Exhibition Edit' } },
			{ url: 'exhibition-edit/{id}',     params: { page: 'exhibition-edit',    title: 'Exhibition Edit' } },
			{ url: 'apidoc', params: { page: 'api-docu', title: 'API Documentation' } },
			{ url: 'testsearch', params: { page: 'testsearch', title: 'testsearch' } },
			{ url: 'myfavorites', params: { page: 'myfavorites', title: 'My Favorites' } },
			{ url: 'about',     params: { page: 'about',  title: 'About' } },
			{ url: 'contact',     params: { page: 'contact',  title: 'Contact' } },
			{ url: 'terms',     params: { page: 'terms',  title: 'Terms' } },
			{ url: 'privacy',     params: { page: 'privacy',  title: 'Privacy' } },
			{ url: 'gallery/{id}/{skin}',     params: { page: '3DRoom',    title: 'Gallery View' } }
		]
	});

	function Router(config) {
		var currentRoute = this.currentRoute = ko.observable({});

		ko.utils.arrayForEach(config.routes, function(route) {
			crossroads.addRoute(route.url, function(requestParams) {
				currentRoute(ko.utils.extend(requestParams, route.params));
			});
		});

		activateCrossroads();
	}
	
	
	

	function activateCrossroads() {
		function resetMenu(){
			/*in case we changed from item view without closing */
			$("div[role='main']").removeClass("itemopen");
			$('body').css('overflow','visible');
			//now reset mobile menu
			var $menu = $( '.main .menu');
			
	          if($menu.hasClass("visible")){
	        	  $menu.toggleClass( 'visible' );

					// toggle button
					$( '.mobilemenu' ).toggleClass( 'active' );
	          }
	         
	     	 
		}
		function parseHash(newHash, oldHash) {
			if(oldHash)
				if(oldHash=="home"){
					 var scrollPosition = $(window).scrollTop();
				     sessionStorage.setItem("homemasonryscroll", scrollPosition);
				     
				}
				else if(oldHash.indexOf("provider/")==0){
					 var scrollPosition = $(window).scrollTop();
					 oldHash=oldHash.substring(9);
					 oldHash=oldHash.substring(0,oldHash.indexOf('/'));
					 sessionStorage.setItem("provider"+oldHash, scrollPosition);
				}
				else if (oldHash.indexOf("collectionview/")==0){
					var scrollPosition = $(window).scrollTop();
					dispatchDocumentEvent('Pundit.hide');
					
					 oldHash=oldHash.substring(15);
					 oldHash=oldHash.substring(0,oldHash.indexOf('/'));
					 
					 console.log("collection-viewscroll"+oldHash);
					 sessionStorage.setItem("collection-viewscroll"+oldHash, scrollPosition);
				     
				}
			crossroads.parse(newHash);
		
		
		}
		crossroads.ignoreState= false; 
		crossroads.normalizeFn = crossroads.NORM_AS_OBJECT;
		hasher.initialized.add(parseHash);
		hasher.changed.add(parseHash);
		hasher.changed.add(resetMenu);
		
		hasher.init();
	}
	
	
});
