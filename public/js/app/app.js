define("app", ['knockout', 'facebook', 'smoke'], function (ko, FB) {

	var self = this;
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
		"favoritesId": ko.observable()
	};
	isLogged = ko.observable(false);

	loadUser = function (data, remember, loadCollections) {
		self.currentUser._id(data._id.$oid);
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

	self.loadFavorites = function () {
		$.ajax({
				url: "/collection/favorites",
				type: "GET",
			})
			.done(function (data, textStatus, jqXHR) {
				self.currentUser.favorites(data);
				for(i in data){
					if($("#"+data[i])){
						$("#"+data[i]).addClass('active');
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
		showPopup: showPopup,
		closePopup: closePopup,
		logout: logout,
		getUserCollections: getUserCollections,
		getAllUserCollections: getAllUserCollections,
		getPublicCollections: getPublicCollections,
		getEditableCollections: getEditableCollections,
		isLiked: isLiked,
		loadFavorites: loadFavorites,
		likeItem: likeItem
	};
});