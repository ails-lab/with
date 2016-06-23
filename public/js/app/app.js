define("app", ['knockout', 'facebook', 'imagesloaded', 'moment', './js/app/plugin','./js/app/params','smoke'], function (ko, FB, imagesLoaded, moment,plugin,params) {

	var self = this;

	//hold the selected lang value from ui
	self.lang=ko.observable("");

	self.WITHApp = "";

	self.settings = $.extend({
		// page
		page: 'default',

		// masonry
		mSelector: '.grid',
		mItem: '.item',
		mSizer: '.sizer',

		// mobile menu
		mobileSelector: '.mobilemenu',
		mobileMenu: '.main .menu'
	});
	self.custom = false;
	self.transDuration = 0;
	var isFirefox = typeof InstallTrigger !== 'undefined'; // Firefox 1.0+
	if (isFirefox) {
		self.transDuration = 0;
	}

	self.loadDependancies = function () {
		/* we are in WITH*/
		if (plugin.WITHApp) {
			self.WITHApp = new plugin.WITHApp.ui({
				// page name
				page: $('body').attr('data-page'),

				// masonry
				mSelector: '.grid',
				mItem: '.item',
				mSizer: '.sizer',

				// mobile menu
				mobileSelector: '.mobilemenu',
				mobileMenu: '.main .menu'
			});
			return {
				WITHApp: self.WITHApp
			};
		} else {
			self.custom = true;
			self.WITHApp = new plugin.EUSpaceApp.ui({

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

		 self.WITHApp.projectName = params._args.projectName;
		 self.WITHApp.projectId = params._args.projectId;
		 self.WITHApp.featuredExhibition=params._args.featuredExhibition;
		 setTimeout(function(){ WITHApp.init(); }, 1000);

		 return {
				WITHApp: self.WITHApp
			};
		}
	}

	self.loadDependancies();
	/* for all isotopes binding */
	function initOrUpdate(method) {
		return function (element, valueAccessor, allBindings, viewModel, bindingContext) {
			function isotopeAppend(ele) {
				if (ele.nodeType === 1 && ele.className.indexOf("item") > -1) { // Element type
					$(ele).css("display", "none");

					$(element).imagesLoaded(function () {
						if (ko.contextFor(ele) && ko.contextFor(ele).$parent.loading) {
							ko.contextFor(ele).$parent.loading(false);
							ko.contextFor(ele).$data.isLoaded(true);
						}
						$(element).isotope('appended', ele).isotope('layout');
						$(ele).css("display", "");
					});
				}
			}

			function attachCallback(valueAccessor) {
				return function () {
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

				if (height > 0) { // or some other number
					$(element).height(height);
				}

				/* finished back button fix*/
				$(element).imagesLoaded(function () {
					$(element).isotope({
						itemSelector: '.item',
						transitionDuration: transDuration,
						masonry: {
							columnWidth: '.sizer',
							percentPosition: true
						}
					});
				});

				ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
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
			var functPar1 = props.functPar1;
			var load = ko.utils.unwrapObservable(valueAccessor());
			var self = this;

			if (load) {
				$(window).on("scroll.ko.scrollHandler", function () {
					if ($(window).scrollTop() >= $(document).height() - $(window).height() - 300) {
						if (self.updating) {
							if (functPar1 !== undefined && functPar1 !== null)

								loadFunc(functPar1);
							else
								loadFunc();
							//self.updating = false;
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

	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
	self.notificationSocket = new WS("ws://" + window.location.host + "/notifications/socket");

	self.receiveEvent = function (event) {
		var notification = JSON.parse(event.data);

		self.addNotification(notification);

		switch (notification.activity) {
		case "GROUP_INVITE":
			$.smkAlert({
				text: '<strong>' + notification.senderName + '</strong> invites you to join <strong>' + notification.groupName + '</strong>',
				type: 'info',
				time: 5
			});
			break;
		case "GROUP_INVITE_ACCEPT":
			$.smkAlert({
				text: '<strong>' + notification.senderName + '</strong> joined <strong>' + notification.groupName + '</strong>',
				type: 'info',
				time: 5
			});
			break;
		case "GROUP_INVITE_DECLINED":
			$.smkAlert({
				text: '<strong>' + notification.senderName + '</strong> declined your invitation to join <strong>' + notification.groupName + '</strong>',
				type: 'info',
				time: 5
			});
			break;
		case "GROUP_REQUEST":
			$.smkAlert({
				text: '<strong>' + notification.senderName + '</strong> wants to join <strong>' + notification.groupName + '</strong>',
				type: 'info',
				time: 5
			});
			break;
		case "GROUP_REQUEST_ACCEPT":
			$.smkAlert({
				text: 'You joined <strong>' + notification.groupName + '</strong>',
				type: 'info',
				time: 5
			});
			break;
		case "GROUP_REQUEST_DENIED":
			$.smkAlert({
				text: 'Your request to join <strong>' + notification.groupName + '</strong> was declined',
				type: 'info',
				time: 5
			});
			break;
		case "COLLECTION_SHARE":
			if (notification.shareInfo.sharedWithGroup) {
				$.smkAlert({
					text: '<strong>' + notification.senderName + '</strong> wants to share collection <strong>' + notification.shareInfo.resourceName + '</strong> with <strong>' + notification.groupName + '</strong>',
					type: 'info',
					time: 5
				});
			} else {
				$.smkAlert({
					text: '<strong>' + notification.senderName + '</strong> wants to share collection <strong>' + notification.shareInfo.resourceName + '</strong> with you',
					type: 'info',
					time: 5
				});
			}
			break;
		case "COLLECTION_SHARED":
			$.smkAlert({
				text: '<strong>' + notification.shareInfo.resourceName + '</strong> is now shared with <strong>' + notification.shareInfo.userOrGroupName + '</strong>',
				type: 'info',
				time: 5
			});
			break;
		case "COLLECTION_UNSHARED":
			senderName = notification.shareInfo.sharedWithGroup ? notification.shareInfo.userOrGroupName : 'you';
			$.smkAlert({
				text: '<strong>' + notification.shareInfo.resourceName + '</strong> is no longer shared with <strong>' + senderName + '</strong>',
				type: 'info',
				time: 5
			});
			break;
		case "COLLECTION_REJECTED":
			$.smkAlert({
				text: '<strong>' + notification.shareInfo.userOrGroupName + '</strong> is not interested in collection <strong>' + notification.shareInfo.resourceName + '</strong>',
				type: 'info',
				time: 5
			});
			break;
		default:
			$.smkAlert({
				text: "Unknown Notification Type: <strong>" + notification.activity + "</strong>",
				type: 'warning',
				time: 5
			});
		}
	};
	self.notificationSocket.onmessage = self.receiveEvent;
	self.notificationSocket.onclose = function (evt) {
		console.log("disconected");
	};

	function waitForConnection(callback, interval) {
		if (self.notificationSocket.readyState === 1) {
			callback();
		} else {
			// optional: implement backoff for interval here
			setTimeout(function () {
				waitForConnection(callback, interval);
			}, interval);
		}
	}

	self.currentUser = {
		"_id": ko.observable(),
		"email": ko.observable(),
		"username": ko.observable(),
		"firstName": ko.observable(''),
		"lastName": ko.observable(''),
		"gender": ko.observable(),
		"facebookId": ko.observable(),
		"googleId": ko.observable(),
		"image": ko.observable(),
		"avatar": {
			"Original": ko.observable(),
			"Medium": ko.observable(),
			"Square": ko.observable(),
			"Thumbnail": ko.observable(),
			"Tiny": ko.observable()
		},
		"recordLimit": ko.observable(),
		"collectedRecords": ko.observable(),
		"storageLimit": ko.observable(),
		"favorites": ko.observableArray(),
		"favoritesId": ko.observable(),
		"usergroups": ko.observableArray(),
		"organizations": ko.observableArray(),
		"projects": ko.observableArray(),
		"editables":ko.observableArray([]),
		"notifications": {
			'unread': ko.observable(0),
			'userNotifications': ko.observableArray(),
			'groupNotifications': ko.observableArray()
		},
		"collectionCount": ko.observable(0),
		"exhibitionCount": ko.observable(0),
		"sharedCollectionCount": ko.observable(0),
		"sharedExhibitionCount": ko.observable(0),
		"annotationCount": ko.observable(0)
	};
	isLogged = ko.observable(false);

	loadUser = function (data, remember, loadCollections) {
		self.currentUser._id(data.dbId);
		self.currentUser.favoritesId(data.favorites);
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
		if (typeof data.avatar != 'undefined') {	// New users don't have avatars
			self.currentUser.avatar.Original(data.avatar.Original);
			self.currentUser.avatar.Tiny(data.avatar.Tiny);
			self.currentUser.avatar.Square(data.avatar.Square);
			self.currentUser.avatar.Thumbnail(data.avatar.Thumbnail);
			self.currentUser.avatar.Medium(data.avatar.Medium);
		}
		self.currentUser.usergroups(data.usergroups);
		self.currentUser.organizations(data.organizations);
		self.currentUser.projects(data.projects);
		self.currentUser.annotationCount(data.annotationCount);
		self.loadNotifications(data.notifications);

		$(".star").each(function () {
			$(this).css("display", "");
		});
		$(".collect").each(function () {
			$(this).css("display", "");
		});
		self.loadFavorites();

		isLogged(true);

		localStorage.setItem('logged_in', true);
		waitForConnection(function () {
			self.notificationSocket.send('{"action":"login","id":"' + data.dbId + '"}');
		}, 1000);

		if (typeof (loadCollections) === 'undefined' || loadCollections === true) {
			return [self.getEditableCollections()]; //[self.getEditableCollections(), self.getUserCollections()];
		}
	};

	self.reloadUser = function () {
		if (self.currentUser._id() === undefined) {
			return;
		}

		$.ajax({
			url: '/user/' + self.currentUser._id(),
			type: 'GET',
			success: function (data, text) {
				loadUser(data, false, true);
			},
			//async: false
		});

		self.loadCounters();
	};

	self.loadCounters = function () {
		$.ajax({
			url: '/collection/countMyAndShared',
			type: 'GET',
			success: function (data, text) {
				self.currentUser.exhibitionCount(data.my.Exhibition);
				self.currentUser.collectionCount(data.my.SimpleCollection);
				self.currentUser.sharedExhibitionCount(data.sharedWithMe.Exhibition);
				self.currentUser.sharedCollectionCount(data.sharedWithMe.SimpleCollection);
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

				// for (var i in data) {
				// 	if ($("#" + data[i])) {
				// 		$("#" + data[i]).addClass('active');
				// 	}
				// }
			})
			.fail(function (jqXHR, textStatus, errorThrown) {
				$.smkAlert({
					text: 'Error loading Favorites',
					type: 'danger',
					time: 10
				});
				console.log("Error loading favorites!");
			});
	};

	self.loadNotifications = function (data) {
		// Reset previous notification instances
		self.currentUser.notifications.userNotifications.removeAll();
		self.currentUser.notifications.groupNotifications.removeAll();
		self.currentUser.notifications.unread(0);

		if (data) {
			// Sort notification array
			data.sort(function (a, b) {
				return a.openedAt - b.openedAt;
			});

			// Load notifications
			for (var i = 0; i < data.length; i++) {
				if (data[i].resourceName === 'DELETED') {
					return;
				}

				if (data[i].activity != null) {
					self.addNotification(data[i]);
				}
			}
		}
	};

	self.addNotification = function (data) {
		data.date = ko.pureComputed(function () {
			return moment(data.openedAt).fromNow();
		});
		data.image = ko.computed(function() {
			if (null == data.senderLogoUrl) {
				return 'images/user.png';
			} else {
				return data.senderLogoUrl;
			}
		});

		data.pending = ko.observable(data.pendingResponse);
		data.unread = ko.observable(data.readAt === null || data.readAt == undefined);

		// Only pending notifications are displayed in the counter
		if (data.pending()) {
			self.currentUser.notifications.unread(self.currentUser.notifications.unread() + 1);
		}

		switch (data.activity) {
		case "GROUP_INVITE":
			data.message = '<strong>' + data.senderName + '</strong> invites you to join <a href="#organization/' + data.group + '">' + data.groupName + '</a>';
			self.currentUser.notifications.userNotifications.unshift(data);
			break;
		case "GROUP_INVITE_ACCEPT":
			data.message = '<strong>' + data.senderName + '</strong> joined <a href="#organization/' + data.group + '">' + data.groupName + '</a>';
			self.currentUser.notifications.groupNotifications.unshift(data);
			break;
		case "GROUP_INVITE_DECLINED":
			data.message = '<strong>' + data.senderName + '</strong> declined your invitation to join <a href="#organization/' + data.group + '">' + data.groupName + '</a>';
			self.currentUser.notifications.groupNotifications.unshift(data);
			break;
		case "GROUP_REQUEST":
			data.message = '<strong>' + data.senderName + '</strong> wants to join <a href="#organization/' + data.group + '">' + data.groupName + '</a>';
			self.currentUser.notifications.groupNotifications.unshift(data);
			break;
		case "GROUP_REQUEST_ACCEPT":
			data.message = 'You joined <a href="#organization/' + data.group + '">' + data.groupName + '</a>';
			self.currentUser.notifications.userNotifications.unshift(data);
			break;
		case "GROUP_REQUEST_DENIED":
			data.message = 'Your request to join <a href="#organization/' + data.group + '">' + data.groupName + '</a> was declined';
			self.currentUser.notifications.userNotifications.unshift(data);
			break;
		case "COLLECTION_SHARE":
			if (data.shareInfo.sharedWithGroup) {
				data.message = '<strong>' + data.senderName + '</strong> wants to share collection <a href="#collectionview/' + data.resoure + '">' + data.resourceName + '</a> with <strong>' + data.shareInfo.userOrGroupName + '</strong>';
				self.currentUser.notifications.groupNotifications.unshift(data);
			} else {
				data.message = '<strong>' + data.senderName + '</strong> wants to share collection <a href="#collectionview/' + data.resource + '">' + data.resourceName + '</a> with you';
				self.currentUser.notifications.userNotifications.unshift(data);
			}
			break;
		case "COLLECTION_SHARED":
			if (data.shareInfo.sharedWithGroup) {
				data.message = '<a href="#collectionview/' + data.resource + '">' + data.resourceName + '</a> is now shared with <strong>' + data.shareInfo.userOrGroupName + '</strong>';
			} else {
				data.message = '<a href="#collectionview/' + data.resource + '">' + data.resourceName + '</a> is now shared with <strong>' + data.senderName + '</strong>';
			}
			self.currentUser.notifications.userNotifications.unshift(data);
			break;
		case "COLLECTION_UNSHARED":
			if (data.shareInfo.sharedWithGroup) {
				data.message = '<strong>' + data.resourceName + '</strong> is no longer shared with <strong>' + data.shareInfo.userOrGroupName + '</strong>';
				self.currentUser.notifications.groupNotifications.unshift(data);
			} else {
				data.message = '<strong>' + data.resourceName + '</strong> is no longer shared with you';
				self.currentUser.notifications.userNotifications.unshift(data);
			}
			break;
		case "COLLECTION_REJECTED":
			var senderName = data.shareInfo.userOrGroupName;
			data.message = '<strong>' + senderName + '</strong> is not interested in collection <strong>' + data.resourceName + '</strong>';
			self.currentUser.notifications.userNotifications.unshift(data);
			break;
		default:
			data.message = "<strong>Unknown Notification Type:</strong> " + data.activity;
			self.currentUser.notifications.userNotifications.unshift(data);
		}

		return data;
	};

	likeItem = function (record, update) {
		var id, data;
		data = JSON.stringify(record.data());
		id = record.externalId;
		if (!self.isLiked(id)) { // Like
			$.ajax({
				type: "POST",
				url: "/collection/liked",
				data: data,
				contentType: "application/json",
				processData: false,
				success: function (data, textStatus, jqXHR) {
					self.currentUser.favorites.push(id);
					update(true);
				},
				error: function (jqXHR, textStatus, errorThrown) {
					$.smkAlert({
						text: 'An error has occured. Please try again.',
						type: 'danger',
						time: 10
					});
					console.log(errorThrown);
				}
			});
		} else { // Unlike
			console.log(encodeURIComponent(id));
			$.ajax({
				type: "POST",
				url: "/collection/unliked",
				contentType: "application/json",
				data: JSON.stringify({externalId: id}),
				success: function (data, textStatus, jqXHR) {
					self.currentUser.favorites.remove(id);
					update(false);
				},
				error: function (jqXHR, textStatus, errorThrown) {
					$.smkAlert({
						text: 'An error has occured. Please try again.',
						type: 'danger',
						time: 10
					});
					console.log(errorThrown);
				}
			});
		}
	};

	self.getPublicCollections = function () {
		console.log("public");
		return $.ajax({
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			url: "/collection/listPublic",
			processData: false,
			data: "offset=0&count=20" //&isExhibition=false"
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

	//TODO: check if it returns exhibitions. then can add unsaved records directly to exhibitions
	self.getEditableCollections = function () {
		return $.ajax({
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			url: "/collection/list",
			processData: false,
			data: "offset=0&count=50&directlyAccessedByUserOrGroup=" + JSON.stringify([{
				user: self.currentUser.username(),
				rights: "WRITE"
			}]),
		}).done(
			function (data) {
				var array = JSON.parse(JSON.stringify(data.collectionsOrExhibitions));
				self.currentUser.editables.removeAll();
				var temparray=[];
				array.forEach(function (item) {
					temparray.push({
						title: self.findByLang(item.descriptiveData.label),
						dbId: item.dbId
					});
				});
				self.currentUser.editables.push.apply(self.currentUser.editables,temparray);
				return (self.currentUser.editables());
			}).fail(function (request, status, error) {});
	};

	self.getUserCollections = function (isExhibition, offset, count) {
		//filter = [{username:'maria.ralli',access:'OWN'}];
		return $.ajax({
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			url: "/collection/list",
			processData: false,
			data: "creator=" + self.currentUser.username() + "&offset="+offset+"&count="+count+"&isExhibition=" + isExhibition + "&collectionHits=true"
		}).done(
			function (data) {
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
			data: "creator=" + self.currentUser.username() + "&offset=0&count=1000&isExhibition=false&collectionHits=true"
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
		if(id)
		return self.currentUser.favorites.indexOf(id) < 0 ? false : true;
		else return false;
	};

	logout = function () {
		$.ajax({
			type: "GET",
			url: "/user/logout",
			success: function () {
				waitForConnection(function () {
					self.notificationSocket.send('{"action":"logout","id":"' + self.currentUser._id() + '"}');
				}, 1000);
				self.clearSession();
				localStorage.setItem('logged_in', false);
				window.location.href = "/assets/index.html";
				//update custom spaces
				//window.opener.location.reload();
			}
		});
	};

	self.clearSession = function () {
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

	self.showInfoPopup = function(title, bodyText, callback) {
		$("#myModal").find("h4").html(title);
		var body = $("#myModal").find("div.modal-body");
		body.html(bodyText);

		var footer = $("#myModal").find("div.modal-footer");
		if (footer.is(':empty')) {
	        var cancelBtn = $('<button type="button" class="btn btn-default">Cancel</button>').appendTo(footer);
	        cancelBtn.click(function() {
	        	$("#myModal").modal('hide');
	        });
	        var confirmBtn = $('<button type="button" class="btn btn-primary">Confirm</button>').appendTo(footer);
	        confirmBtn.click(function() {
	        	$("#myModal").modal('hide');
	        	callback();
	        });
	    }
		$("#myModal").modal('show');
		$('#myModal').on('hidden.bs.modal', function () {
			$("#myModal").find("div.modal-footer").empty();
		});
		$('#myModal').addClass("topOfModal");
	}

	self.showInfoPopupTwoOptions = function(title, bodyText, callback) {
		$("#myModal").find("h4").html(title);
		var body = $("#myModal").find("div.modal-body");
		body.html(bodyText);

		var footer = $("#myModal").find("div.modal-footer");
		if (footer.is(':empty')) {
	        var cancelBtn = $('<button type="button" class="btn btn-default">No</button>').appendTo(footer);
	        cancelBtn.click(function() {
	        	$("#myModal").modal('hide');
	        	callback.call(this, false);
	        });
	        var confirmBtn = $('<button type="button" class="btn btn-primary">Yes</button>').appendTo(footer);
	        confirmBtn.click(function() {
	        	$("#myModal").modal('hide');
	        	callback.call(this, true);
	        });
	    }
		$("#myModal").modal('show');
		$('#myModal').on('hidden.bs.modal', function () {
			$("#myModal").find("div.modal-footer").empty();
		});
		$('#myModal').addClass("topOfModal");
	}


	// Closing modal dialog and setting back to empty to dispose the component
	closePopup = function () {
		$('#popup').modal('hide');
		popupName("empty");
		popupParams("{}");
	};

	autoCompleteUserName = function (elem, valueAccessor, allBindingsAccessor, viewModel, context, callback) {
		var onlyParents = allBindingsAccessor.get('onlyParents') || false;
		var forUsers = allBindingsAccessor.get('forUsers') || false;
		var paramsJSON = { "onlyParents": onlyParents, "forUsers" : forUsers};
		if (allBindingsAccessor.has('forGroupType')) {
			paramsJSON.forGroupType = allBindingsAccessor.get('forGroupType');
		}
		$(elem).devbridgeAutocomplete({
			minChars: 3,
			lookupLimit: 10,
			serviceUrl: "/user/listNames",
			paramName: "prefix",
			params: paramsJSON,
			ajaxSettings: {
				dataType: "json"
			},
			transformResult: function (response) {
				var myUsername = ko.utils.unwrapObservable(valueAccessor());
				var index = arrayFirstIndexOf(response, function (item) {
					return item.value === myUsername;
				});
				if (index > -1)
					response.splice(index, 1);
				/*var usersAndParents = [];
	   			$.each(response, function(i, obj) {
	   				if (obj.data.isParent == undefined || obj.data.isParent == null || obj.data.isParent === true)
	   					usersAndParents.push(obj);
			    });*/
				return {
					"suggestions": response
				};
			},
			orientation: "auto",
			onSearchComplete: function (query, suggestions) {
				$(".autocomplete-suggestion").addClass("autocomplete-suggestion-extra");
			},
			formatResult: function (suggestion, currentValue) {
				var prefix = suggestion.value.substring(0, currentValue.length);
				var s =
					/*'<img class="img-responsive img-circle" src="';
				s	 += (currentUser.image() ? currentUser.image() : 'images/user.png') + '" />'
				s	 +=*/
					'<strong>' + prefix + '</strong>';
				s += suggestion.value.substring(currentValue.length);
				s += ' <span class="label pull-right">' + suggestion.data.category + '</span>';
				return s;
			},
			triggerSelectOnValidInput: false,
			onSelect: function (suggestion) {
				if (callback !== undefined && callback != null)
					callback.call(this, suggestion.value);
			}
		});
	};

	/* Check if user information already exist in session
	if (sessionStorage.getItem('User') !== null) {
		var sessionData = JSON.parse(sessionStorage.getItem('User'));
		loadUser(sessionData, false);
	} else if (localStorage.getItem('User') !== null) {
		var storageData = JSON.parse(localStorage.getItem('User'));
		loadUser(storageData, true);
	}*/


	function readCookie(name) {
	    var nameEQ = encodeURIComponent(name) + "=";
	    var ca = document.cookie.split(';');
	    for (var i = 0; i < ca.length; i++) {
	        var c = ca[i];
	        while (c.charAt(0) === ' ') c = c.substring(1, c.length);
	        if (c.indexOf(nameEQ) === 0) return decodeURIComponent(c.substring(nameEQ.length, c.length));
	    }
	    return null;
	}

	function ExtractQueryString(cookie) {
	    var oResult = {};
	    var aQueryString = cookie.replace(/\"/g, "").split("&");
	    for (var i = 0; i < aQueryString.length; i++) {
	        var aTemp = aQueryString[i].split("=");
	        if (aTemp[1].length > 0) {
	            oResult[aTemp[0]] = unescape(aTemp[1]);
	        }
	    }
	    return oResult;
	}

	self.checkLogged=function(){
		var user=null;
		var usercookie=readCookie("PLAY_SESSION");
		if(usercookie)
		usercookie.replace(/\"/g, "");
		if(usercookie!=null){
		   var keys=ExtractQueryString(usercookie);
		   if(self.currentUser._id()==undefined || self.currentUser._id().length==0){
		   	if(keys["username"]){self.currentUser.username(keys["username"]);}
		   	if(keys["user"]){self.currentUser._id(keys["user"]);self.reloadUser();}}
		    return (keys["user"]==undefined ? false : true);
		} else{return false;}

	};

	self.checkLogged();

	self.findByLang=function(val, language) {
		if (language == undefined || language == null)
			language = "default";
		if (val !== undefined && val !== null)
			if (val[language]) {
				var label = val[language];
				if(val[language][0]){
					label = val[language][0];
				}
				if (label)
					return label;
			}
		return "";
     }
	
	arrayFirstIndexOf = function (array, predicate) {
		for (var i = 0, j = array.length; i < j; i++) {
			if (predicate.call(undefined, array[i])) {
				return i;
			}
		}
		return -1;
	};

	 self.findProvenanceValues=function(array, selection) {
			selvalue="";
			if(selection=="dataProvider"){
			  if(array.length>1 && array[0].provider)
				  selvalue=array[0].provider;


			 }
			else if(selection=="dataProvider_uri"){
				  if(array.length>1){
					  selvalue=array[0].uri;

					  if(array[0].uri && array[0].uri.length>0){
						  selvalue=array[0].uri;
			        	}

				 }}
			else if (selection=="provider"){
				  if(array.length==3){

					  if(array[1].uri && array[1].uri.length>0){
			        		if(array[1].provider && array[1].provider.length>0){
			        			selvalue="<a href='"+array[1].uri+"' target='blank'>"+array[1].provider+"</a>";
			        		}

			        	}else if(array[1].provider){
			              selvalue+=array[1].provider;}

				     }
			}
			else if (selection=="provider_uri"){
				  if(array.length==3)
					  if(array[1].uri && array[1].uri.length>0){

			        			selvalue=array[1].uri;
			        		}


			}
			else if (selection=="source"){
				var size=array.length-1;
				if(array[size].provider){
	              selvalue+=array[size].provider;}

		     }
			else if (selection=="source_uri"){
				var size=array.length-1;
				if(array[size].uri && array[size].uri.length>0){
	        			selvalue=array[size].uri;
	        	} else if (size>0 && array[size-1].uri && array[size-1].uri.length>0) {
        			selvalue=array[size-1].uri;
	        	}

		     }
			else if (selection=="id"){
				var size=array.length-1;
				if(array[size].resourceId && array[size].resourceId.length>0){

	        			selvalue+=array[size].resourceId;
	        		}


		     }
			return selvalue;

		}


	 self.findResOrLit=function(data) {

			selvalue="";
			var uilang="";
		    if(self.lang().length==0){
						uilang="default";
					}

			if(data){
			if(data[uilang]){

			                	selvalue=data[uilang];
			   }
			else if(data.uri){
				selvalue=data.uri;
			}
			else if(data["en"]){

            	selvalue=data["en"];
				}
			}
			return selvalue;

		}
//self.reloadUser(); // Reloads the user on refresh

	/* function to alert all tabs on log in changes*/
	function storageChange(event) {
		if(event.key == 'logged_in' ) {
			//console.log("logged in:"+event.newValue);

				window.location.reload();
				if(event.newValue=="true"){
					console.log("just logare");

				}

	    }
	}
	window.addEventListener('storage', storageChange, false);

	return {
		currentUser: currentUser,
		loadUser: loadUser,
		reloadUser: reloadUser,
		showPopup: showPopup,
		closePopup: closePopup,
		findByLang: findByLang,
		findProvenanceValues: findProvenanceValues,
		findResOrLit: findResOrLit,
		autoCompleteUserName: autoCompleteUserName,
		logout: logout,
		getUserCollections: getUserCollections,
		getAllUserCollections: getAllUserCollections,
		getPublicCollections: getPublicCollections,
		getEditableCollections: getEditableCollections,
		showInfoPopup: showInfoPopup,
		showInfoPopupTwoOptions: showInfoPopupTwoOptions,
		isLiked: isLiked,
		isLogged:isLogged,
		loadFavorites: loadFavorites,
		loadCounters: loadCounters,
		likeItem: likeItem,
		initOrUpdate: initOrUpdate,
		scroll: scroll,
		arrayFirstIndexOf: arrayFirstIndexOf
	};
});
