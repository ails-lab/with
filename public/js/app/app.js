define("app", ['knockout', 'facebook', 'jquery.sticky','imagesloaded','smoke'], function (ko, FB,sticky,imagesLoaded) {

	var self = this;
	
	self.WITHApp="";
	
	
	self.settings = $.extend({

		// page
		page  	  : 'default',

		// masonry
		mSelector : '.grid',
		mItem	  : '.item',
		mSizer	  : '.sizer',

		// mobile menu
		mobileSelector : '.mobilemenu',
		mobileMenu 	   : '.main .menu'
	});
	self.transDuration = '0.4s';
	var isFirefox = typeof InstallTrigger !== 'undefined'; // Firefox 1.0+
	if (isFirefox) {
		self.transDuration = 0;
	}
	
	require(["./js/app/plugin"], function(WITHApp) {
		self.WITHApp=new WITHApp.WITHApp.ui({
					// page name
					page  	  : $( 'body' ).attr( 'data-page' ),

					// masonry
					mSelector : '.grid',
					mItem	  : '.item',
					mSizer	  : '.sizer',

					// mobile menu
					mobileSelector : '.mobilemenu',
					mobileMenu 	   : '.main .menu'
		 	})

		 return {
				WITHApp: self.WITHApp
			};
		 
			});
	
		 /* for all isotopes binding */
		 function initOrUpdate(method) {
				return function (element, valueAccessor, allBindings, viewModel, bindingContext) {
					function isotopeAppend(ele) {
						if (ele.nodeType === 1 && ele.className.indexOf ("item")>-1) { // Element type
							$(ele).css("display","none");
								
							$(element).imagesLoaded(function () {
								if(ko.contextFor(ele).$parent.loading!=undefined){
								  ko.contextFor(ele).$parent.loading(false);
								  ko.contextFor(ele).$data.isLoaded(true); 
								}
								$(element).isotope('appended', ele).isotope('layout');
								$(ele).css("display","");
								
								
							});
							
						}
						
					}

					
					
					function attachCallback(valueAccessor) {
						return function() {
							return {
								data: valueAccessor(),
								afterAdd: isotopeAppend
							};
						};
					}

					var data = ko.utils.unwrapObservable(valueAccessor());
					//extend foreach binding
					ko.bindingHandlers.foreach[method](element,
						 attachCallback(valueAccessor), // attach 'afterAdd' callback
						 allBindings, viewModel, bindingContext);

					if (method === 'init') {
						/* this is very important, when hiting back button this makes it scroll to correct position*/
						var height = $(element).height();

						if( height > 0 ) { // or some other number
						    $(element).height( height );
						}
						
						/* finished back button fix*/
						 $(element).imagesLoaded(function () {
							$(element).isotope({
								itemSelector: '.item',
								transitionDuration: transDuration,
								masonry: {
									columnWidth		: '.sizer',
									percentPosition	: true
								}
							});
							
						});

						ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
							$(element).isotope("destroy");
						});
						
					} 
					
				};
			}
		 
		 /* scroll binding for infinite load*/
		 ko.bindingHandlers.scroll = {
					updating: true,

					init: function (element, valueAccessor, allBindingsAccessor) {
						var self = this;
						self.updating = true;
						ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
							$(window).off("scroll.ko.scrollHandler");
							self.updating = false;
						});
					},

					update: function (element, valueAccessor, allBindingsAccessor) {
						var props = allBindingsAccessor().scrollOptions;
						var offset = props.offset ? props.offset : "0";
						var loadFunc = props.loadFunc;
						var load = ko.utils.unwrapObservable(valueAccessor());
						var self = this;

						if (load) {
							$(window).on("scroll.ko.scrollHandler", function () {
								if ($(window).scrollTop() >= $(document).height() - $(window).height() - 300) {
									if (self.updating) {
										loadFunc();
										self.updating = false;
									}
								} else {
									self.updating = true;
								}
								
								 if ($(window).scrollTop() > 100) {
										$('.scroll-top-wrapper').addClass('show');
									} else {
										$('.scroll-top-wrapper').removeClass('show');
									}
							});
						} else {
							element.style.display = "none";
							$(window).off("scroll.ko.scrollHandler");
							self.updating = false;
						}
					}
				};

	
	
	self.currentUser = {
		"_id": ko.observable(),
		"email": ko.observable(),
		"username": ko.observable(),
		"firstName": ko.observable(),
		"lastName": ko.observable(),
		"gender": ko.observable(),
		"facebookId": ko.observable(),
		"googleId": ko.observable(),
		"image": ko.observable(),
		"recordLimit": ko.observable(),
		"collectedRecords": ko.observable(),
		"storageLimit": ko.observable(),
		"favorites": ko.observableArray(),
		"favoritesId": ko.observable(),
		"usergroups": ko.observableArray(),
		"organizations": ko.observableArray(),
		"projects": ko.observableArray()
	};
	isLogged = ko.observable(false);

	loadUser = function (data, remember, loadCollections) {
		self.currentUser._id(data.dbId);
		self.currentUser.email(data.email);
		self.currentUser.username(data.username);
		self.currentUser.firstName(data.firstName);
		self.currentUser.lastName(data.lastName);
		self.currentUser.gender(data.gender);
		self.currentUser.facebookId(data.facebookId);
		self.currentUser.googleId(data.googleId);
		self.currentUser.recordLimit(data.recordLimit);
		self.currentUser.collectedRecords(data.collectedRecords);
		self.currentUser.storageLimit(data.storageLimit);
		self.currentUser.image(data.image);
		self.currentUser.favoritesId(data.favoritesId);
		self.currentUser.usergroups(data.usergroups);
		self.currentUser.organizations(data.organizations);
		self.currentUser.projects(data.projects);

		self.loadFavorites();

		// Save to session
		if (typeof (Storage) !== 'undefined') {
			if (remember) {
				localStorage.setItem("User", JSON.stringify(data));
			} else {
				sessionStorage.setItem("User", JSON.stringify(data));
			}
		}

		isLogged(true);

		if (typeof (loadCollections) === 'undefined' || loadCollections === true) {
			return [self.getEditableCollections()]; //[self.getEditableCollections(), self.getUserCollections()];
		}
	};

	self.reloadUser = function() {
		if (self.currentUser._id === undefined) { return; }

		$.ajax({
			url: '/user/' + self.currentUser._id(),
			type: 'GET',
			success: function(data, text) {
				loadUser(data, false, false);
			}
		});
	};

	self.loadFavorites = function () {
		$.ajax({
				url: "/collection/favorites",
				type: "GET",
			})
			.done(function (data, textStatus, jqXHR) {
				self.currentUser.favorites(data);
				for(var i in data) {
					if($("#" + data[i])){
						$("#" + data[i]).addClass('active');
					}
				}
			})
			.fail(function (jqXHR, textStatus, errorThrown) {
				$.smkAlert({text:'Error loading Favorites', type:'danger', time: 10});
				console.log("Error loading favorites!");
		});
	};

	likeItem = function (record, update) {
		var id, data;
		if (ko.isObservable(record.externalId)) {
			id = record.externalId();
			data = {
				source: record.source(),
				sourceId: record.recordId(),
				title: record.title(),
				provider: record.provider(),
				creator: record.creator(),
				description: record.description(),
				rights: record.rights(),
				type: '',
				thumbnailUrl: record.thumb(),
				sourceUrl: record.view_url(),
				collectionId: self.currentUser.favoritesId(),
				externalId: record.externalId()
			};
		}
		else {
			id = record.externalId;
			data = {
				source: record.source,
				sourceId: record.recordId,
				title: record.title,
				provider: record.provider,
				creator: record.creator,
				description: record.description,
				rights: record.rights,
				type: '',
				thumbnailUrl: record.thumb,
				sourceUrl: record.view_url,
				collectionId: self.currentUser.favoritesId,
				externalId: record.externalId
			};
		}
		if (!self.isLiked(id)) {	// Like
			$.ajax({
				type: "POST",
				url: "/collection/liked",
				data: JSON.stringify(data), //ko.toJSON(record),
				contentType: "application/json",
				success: function (data, textStatus, jqXHR) {
					self.currentUser.favorites.push(id);
					update(true);
				},
				error: function (jqXHR, textStatus, errorThrown) {
					$.smkAlert({text:'An error has occured. Please try again.', type:'danger', time: 10});
					console.log(errorThrown);
				}
			});
		} else {	// Unlike
			$.ajax({
				type: "DELETE",
				url: "/collection/unliked/" + id,
				success: function (data, textStatus, jqXHR) {
					self.currentUser.favorites.remove(id);
					update(false);
				},
				error: function (jqXHR, textStatus, errorThrown) {
					$.smkAlert({text:'An error has occured. Please try again.', type:'danger', time: 10});
					console.log(errorThrown);
				}
			});
		}
	};

	self.getPublicCollections = function () {
		return $.ajax({
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			url: "/collection/list",
			processData: false,
			data: "isPublic=true&offset=0&count=20"//&isExhibition=false"
		}).done(
			//"filterByUser=" +  self.currentUser.username() + "&filterByUserId=" + self.currentUser._id() +
			//"&filterByEmail=" + self.currentUser.email() + "&access=read&offset=0&count=20"}).done(

			//"username=" + self.currentUser.username()+"&ownerId=" + self.currentUser._id() + "&email=" + self.currentUser.email() + "&offset=0" + "&count=20"}).done(

			function (data) {
				// console.log("User collections " + JSON.stringify(data));
				sessionStorage.setItem('PublicCollections', JSON.stringify(data.collectionsOrExhibitions));
			}).fail(function (request, status, error) {

			//var err = JSON.parse(request.responseText);
		});
	};

	self.getEditableCollections = function () {
		return $.ajax({
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			url: "/collection/list",
			processData: false,
			data: "offset=0&count=500&isExhibition=false&directlyAccessedByUserName="+JSON.stringify([{user:self.currentUser.username(),rights:"WRITE"}]),
		}).done(
			//"filterByUser=" +  self.currentUser.username() + "&filterByUserId=" + self.currentUser._id() +
			//"&filterByEmail=" + self.currentUser.email() + "&access=read&offset=0&count=20"}).done(

			//"username=" + self.currentUser.username()+"&ownerId=" + self.currentUser._id() + "&email=" + self.currentUser.email() + "&offset=0" + "&count=20"}).done(

			function (data) {
				var array = JSON.parse(JSON.stringify(data.collectionsOrExhibitions));
				var editables = [];
				array.forEach(function (item) {
					editables.push({
						title: item.title,
						dbId: item.dbId
					});
				});
				if (sessionStorage.getItem('User') !== null)
					sessionStorage.setItem("EditableCollections", JSON.stringify(editables));
				else if (localStorage.getItem('User') !== null)
					localStorage.setItem("EditableCollections", JSON.stringify(editables));
			}).fail(function (request, status, error) {
		});
	};

	self.getUserCollections = function (isExhibition) {
		//filter = [{username:'maria.ralli',access:'OWN'}];
		return $.ajax({
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			url: "/collection/list",
			processData: false,
			data: "creator="+self.currentUser.username()+"&offset=0&count=20&isExhibition="+isExhibition+"&totalHits=true"
		}).done(
			function (data) {
				// console.log("User collections " + JSON.stringify(data));
				/*if (sessionStorage.getItem('User') !== null)
					  sessionStorage.setItem("UserCollections", JSON.stringify(data));
				  else if (localStorage.getItem('User') !== null)
					  localStorage.setItem("UserCollections", JSON.stringify(data));*/
				return data;
			}).fail(function (request, status, error) {
			//var err = JSON.parse(request.responseText);
		});
	};

	self.getAllUserCollections = function () {
		//filter = [{username:'maria.ralli',access:'OWN'}];
		return $.ajax({
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			url: "/collection/list",
			processData: false,
			data: "creator="+self.currentUser.username()+"&offset=0&count=1000&isExhibition=false&totalHits=true"
		}).done(
			function (data) {
				// console.log("User collections " + JSON.stringify(data));
				/*if (sessionStorage.getItem('User') !== null)
					  sessionStorage.setItem("UserCollections", JSON.stringify(data));
				  else if (localStorage.getItem('User') !== null)
					  localStorage.setItem("UserCollections", JSON.stringify(data));*/
				return data;
			}).fail(function (request, status, error) {
			//var err = JSON.parse(request.responseText);
		});
	};

	self.isLiked = function (id) {
		return self.currentUser.favorites.indexOf(id) < 0 ? false : true;
	};

	logout = function () {
		$.ajax({
			type: "GET",
			url: "/user/logout",
			success: function () {
				self.clearSession();
				window.location.href = "/assets/index.html";
				//update custom spaces
				window.opener.location.reload();
			}
		});
	};

	self.clearSession = function() {
		sessionStorage.removeItem('User');
		localStorage.removeItem('User');
		sessionStorage.removeItem('EditableCollections');
		localStorage.removeItem('EditableCollections');
		sessionStorage.removeItem('PublicCollections');
		//sessionStorage.removeItem('UserCollections');
		//localStorage.removeItem('UserCollections');
		isLogged(false);
	};

	$('#myModal').on('hidden.bs.modal', function () {
		$("#myModal").find("div.modal-body").html('');
		$("#myModal").find("h4").html("");
		$("#myModal").find("div.modal-footer").html('');

	});

	showPopup = function (name, params) {
		popupName(name);
		if (params !== undefined) {
			popupParams(params);
		}
		$('#popup').modal('show');
	};

	// Closing modal dialog and setting back to empty to dispose the component
	closePopup = function () {
		$('#popup').modal('hide');
		popupName("empty");
		popupParams("{}");
	};

	// Check if user information already exist in session
	if (sessionStorage.getItem('User') !== null) {
		var sessionData = JSON.parse(sessionStorage.getItem('User'));
		loadUser(sessionData, false);
	} else if (localStorage.getItem('User') !== null) {
		var storageData = JSON.parse(localStorage.getItem('User'));
		loadUser(storageData, true);
	}

	return {
		currentUser: currentUser,
		loadUser: loadUser,
		reloadUser: reloadUser,
		showPopup: showPopup,
		closePopup: closePopup,
		logout: logout,
		getUserCollections: getUserCollections,
		getAllUserCollections: getAllUserCollections,
		getPublicCollections: getPublicCollections,
		getEditableCollections: getEditableCollections,
		isLiked: isLiked,
		loadFavorites: loadFavorites,
		likeItem: likeItem,
		WITHApp: WITHApp,
		initOrUpdate: initOrUpdate,
		scroll: scroll
	};
});
