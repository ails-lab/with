define("app", ['knockout', 'facebook', 'moment', 'smoke'], function (ko, FB, moment) {

	var self = this;
	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
	self.notificationSocket = new WS("ws://localhost:9000/notifications/socket");

	self.receiveEvent = function(event) {
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
				if (notification.groupname) {
					$.smkAlert({
						text: '<strong>' + notification.senderName + '</strong> wants to share collection: <strong>' + notification.collectionName + '</strong> with <strong>' + notification.groupName + '</strong>',
						type: 'info',
						time: 5
					});
				} else {
					$.smkAlert({
						text: '<strong>' + notification.senderName + '</strong> wants to share collection: <strong>' + notification.collectionName + '</strong> with you',
						type: 'info',
						time: 5
					});
				}
				break;
			case "COLLECTION_SHARED":
				var senderName = notification.groupName ? notification.groupName : notification.senderName;
				$.smkAlert({
					text: '<strong>' + notification.collectionName + '</strong> is now shared with <strong>' + senderName + '</strong>',
					type: 'info',
					time: 5
				});
				break;
			case "COLLECTION_UNSHARED":
				senderName = notification.groupName ? notification.groupName : notification.senderName;
				$.smkAlert({
					text: '<strong>' + notification.collectionName + '</strong> is no longer shared with <strong>' + senderName + '</strong>',
					type: 'info',
					time: 5
				});
				break;
			case "COLLECTION_REJECTED":
				senderName = notification.groupName ? notification.groupName : notification.senderName;
				$.smkAlert({
					text: '<strong>' + notification.senderName + '</strong> is not interested in collection: <strong>' + collectionName + '</strong>',
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
	self.notificationSocket.onclose = function(evt) { console.log("disconected"); };

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
		"projects": ko.observableArray(),
		"notifications": {
			'unread': ko.observable(0),
			'userNotifications': ko.observableArray(),
			'groupNotifications': ko.observableArray()
		}
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

		self.loadNotifications(data.notifications);
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
		waitForConnection(function () {
	        self.notificationSocket.send('{"action":"login","id":"'+data.dbId+'"}');
	    }, 1000);

		if (typeof (loadCollections) === 'undefined' || loadCollections === true) {
			return [self.getEditableCollections()]; //[self.getEditableCollections(), self.getUserCollections()];
		}
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

	self.loadNotifications = function(data) {
		for (var i = 0; i < data.length; i++) {
			self.addNotification(data[i]);
		}
	};

	self.addNotification = function(data) {
		data.date = ko.pureComputed(function() {
			return moment(data.openedAt).fromNow();
		});

		data.pending = ko.observable(data.pendingResponse);

		if (!data.readAt) {
			self.currentUser.notifications.unread(self.currentUser.notifications.unread() + 1);
		}

		switch (data.activity) {
			case "GROUP_INVITE":
				data.message = '<strong>' + data.senderName + '</strong> invites you to join <strong><a href="#organization/' + data.group + '">' + data.groupName + '</a></strong>';
				self.currentUser.notifications.userNotifications.unshift(data);
				break;
			case "GROUP_INVITE_ACCEPT":
				data.message = '<strong>' + data.senderName + '</strong> joined <strong><a href="#organization/' + data.group + '">' + data.groupName + '</a></strong>';
				self.currentUser.notifications.groupNotifications.unshift(data);
				break;
			case "GROUP_INVITE_DECLINED":
				data.message = '<strong>' + data.senderName + '</strong> declined your invitation to join <strong><a href="#organization/' + data.group + '">' + data.groupName + '</a></strong>';
				self.currentUser.notifications.groupNotifications.unshift(data);
				break;
			case "GROUP_REQUEST":
				data.message = '<strong>' + data.senderName + '</strong> wants to join <strong><a href="#organization/' + data.group + '">' + data.groupName + '</a></strong>';
				self.currentUser.notifications.groupNotifications.unshift(data);
				break;
			case "GROUP_REQUEST_ACCEPT":
				data.message = 'You joined <strong><a href="#organization/' + data.group + '">' + data.groupName + '</a></strong>';
				self.currentUser.notifications.userNotifications.unshift(data);
				break;
			case "GROUP_REQUEST_DENIED":
				data.message = 'Your request to join <strong><a href="#organization/' + data.group + '">' + data.groupName + '</a></strong> was declined';
				self.currentUser.notifications.userNotifications.unshift(data);
				break;
			case "COLLECTION_SHARE":
				if (data.groupName) {
					data.message = '<strong>' + data.senderName + '</strong> wants to share collection: <strong><a href="#collectionview/' + data.collection + '">' + data.collectionName + '</a></strong> with <strong>' + data.groupName + '</strong>';
					self.currentUser.notifications.groupNotifications.unshift(data);
				}
				else {
					data.message = '<strong>' + data.senderName + '</strong> wants to share collection: <strong><a href="#collectionview/' + data.collection + '">' + data.collectionName + '</a></strong> with you';
					self.currentUser.notifications.userNotifications.unshift(data);
			}
				break;
			case "COLLECTION_SHARED":
				if (data.groupName) {
					data.message = '<strong><a href="#collectionview/' + data.collection + '">' + data.collectionName + '</a></strong> is now shared with <strong>' + data.groupName + '</strong>';
				} else {
					data.message = '<strong><a href="#collectionview/' + data.collection + '">' + data.collectionName + '</a></strong> is now shared with <strong>' + data.senderName + '</strong>';
				}
				self.currentUser.notifications.userNotifications.unshift(data);
				break;
			case "COLLECTION_UNSHARED":
				if (data.groupName) {
					data.message = '<strong>' + data.collectionName + '</strong> is no longer shared with <strong>' + data.groupName + '</strong>';
					self.currentUser.notifications.groupNotifications.unshift(data);
				} else {
					data.message = '<strong>' + data.collectionName + '</strong> is no longer shared with you';
					self.currentUser.notifications.userNotifications.unshift(data);
				}
				break;
			case "COLLECTION_REJECTED":
				var senderName = data.groupName ? data.groupName : data.senderName;
				data.message ='<strong>' + senderName + '</strong> is not interested in collection: <strong>' + collectionName + '</strong>';
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

	self.getUserCollections = function () {
		//filter = [{username:'maria.ralli',access:'OWN'}];
		return $.ajax({
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			url: "/collection/list",
			processData: false,
			data: "creator="+self.currentUser.username()+"&offset=0&count=20&isExhibition=false&totalHits=true"
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

	self.getUserExhibitions = function() {
		return $.ajax({
			type        : "GET",
			contentType : "application/json",
			dataType    : "json",
			url         : "/collection/list",
			processData : false,
			data        : "creator="+self.currentUser.username()+"&offset=0&count=20&isExhibition=true"}).done(
			function(data) {
				// console.log("User collections " + JSON.stringify(data));
				/*if (sessionStorage.getItem('User') !== null)
					  sessionStorage.setItem("UserCollections", JSON.stringify(data));
				  else if (localStorage.getItem('User') !== null)
					  localStorage.setItem("UserCollections", JSON.stringify(data));*/
				return data;
			}).fail(function(request, status, error) {
				//var err = JSON.parse(request.responseText);
			}
		);
	};

	self.isLiked = function (id) {
		return self.currentUser.favorites.indexOf(id) < 0 ? false : true;
	};

	logout = function () {
		$.ajax({
			type: "GET",
			url: "/user/logout",
			success: function () {
				waitForConnection(function () {
			        self.notificationSocket.send('{"action":"logout","id":"'+self.currentUser._id()+'"}');
			    }, 1000);
				self.clearSession();
				window.location.href = "/assets/index.html";
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
		showPopup: showPopup,
		closePopup: closePopup,
		logout: logout,
		getUserCollections: getUserCollections,
		getPublicCollections: getPublicCollections,
		getUserExhibitions: getUserExhibitions,
		getEditableCollections: getEditableCollections,
		isLiked: isLiked,
		loadFavorites: loadFavorites,
		likeItem: likeItem
	};
});
