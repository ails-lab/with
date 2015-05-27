define("app", ['knockout', 'facebook'], function(ko, FB) {

	var self         = this;
	self.currentUser = {
		"_id"              : ko.observable(),
		"email"            : ko.observable(),
		"username"         : ko.observable(),
		"firstName"        : ko.observable(),
		"lastName"         : ko.observable(),
		"gender"           : ko.observable(),
		"facebookId"       : ko.observable(),
		"googleId"         : ko.observable(),
		"image"            : ko.observable(),
		"recordLimit"      : ko.observable(),
		"collectedRecords" : ko.observable(),
		"storageLimit"     : ko.observable(),
	};
	isLogged         = ko.observable(false);

	loadUser         = function(data, remember, loadCollections) {
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

		// Save to session
		if (typeof(Storage) !== 'undefined') {
			if (remember) {
				localStorage.setItem("User", JSON.stringify(data));
			}
			else {
				sessionStorage.setItem("User", JSON.stringify(data));
			}
		}

		isLogged(true);

		if (typeof(loadCollections) === 'undefined' || loadCollections === true) {
			return [self.getEditableCollections()];//[self.getEditableCollections(), self.getUserCollections()];
		}
	};

	getPublicCollections = function() {
		return $.ajax({
			type        : "GET",
			contentType : "application/json",
			dataType    : "json",
			url         : "/collection/list",
			processData : false,
			data        : "access=read&offset=0&count=20"}).done(
					//"filterByUser=" +  self.currentUser.username() + "&filterByUserId=" + self.currentUser._id() +
				//"&filterByEmail=" + self.currentUser.email() + "&access=read&offset=0&count=20"}).done(

					//"username=" + self.currentUser.username()+"&ownerId=" + self.currentUser._id() + "&email=" + self.currentUser.email() + "&offset=0" + "&count=20"}).done(

			function(data) {
				// console.log("User collections " + JSON.stringify(data));
				//sessionStorage.setItem('PublicCollections', JSON.stringify(data));
			}).fail(function(request, status, error) {

				//var err = JSON.parse(request.responseText);
			}
		);
	}
	
	self.getEditableCollections = function() {
		  return $.ajax({
				type        : "GET",
				contentType : "application/json",
				dataType    : "json",
				url         : "/collection/list",
				processData : false,
				data        : "access=write&offset=0&count=20"}).done(
						//"filterByUser=" +  self.currentUser.username() + "&filterByUserId=" + self.currentUser._id() +
					//"&filterByEmail=" + self.currentUser.email() + "&access=read&offset=0&count=20"}).done(

						//"username=" + self.currentUser.username()+"&ownerId=" + self.currentUser._id() + "&email=" + self.currentUser.email() + "&offset=0" + "&count=20"}).done(

				function(data) {
					var array = JSON.parse(JSON.stringify(data));
					var editables = [];
					array.forEach(function(item){
						editables.push({title: item.title, dbId: item.dbId});
					});
					if (sessionStorage.getItem('User') !== null) 
						  sessionStorage.setItem("EditableCollections", JSON.stringify(editables));
					  else if (localStorage.getItem('User') !== null) 
						  localStorage.setItem("EditableCollections", JSON.stringify(editables));
				}).fail(function(request, status, error) {
					colsole.log(JSON.parse(request.responseText));
				}
			);
	};
	
	self.getUserCollections = function() {
		return $.ajax({
			type        : "GET",
			contentType : "application/json",
			dataType    : "json",
			url         : "/collection/list",
			processData : false,
			data        : "access=owned&offset=0&count=20"}).done(
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
	

	logout           = function() {
		$.ajax({
			type        : "GET",
			url         : "/user/logout",
			success     : function() {
				sessionStorage.removeItem('User');
				localStorage.removeItem('User');
				sessionStorage.removeItem('EditableCollections');
				localStorage.removeItem('EditableCollections');
				sessionStorage.removeItem('UserCollections');
				localStorage.removeItem('UserCollections');
				isLogged(false);
				window.location.href="/assets/index.html";
			}
		});
	};

	showPopup        = function(name) {
		popupName(name);
		$('#popup').modal('show');
	};

	// Closing modal dialog and setting back to empty to dispose the component
	closePopup       = function() {
		$('#popup').modal('hide');
		popupName("empty");
	};

	// Check if user information already exist in session
	if (sessionStorage.getItem('User') !== null) {
		var sessionData = JSON.parse(sessionStorage.getItem('User'));
		loadUser(sessionData, false);
	}
	else if (localStorage.getItem('User') !== null) {
		var storageData = JSON.parse(localStorage.getItem('User'));
		loadUser(storageData, true);
	}

	return { currentUser: currentUser, loadUser: loadUser, showPopup: showPopup, closePopup: closePopup, logout: logout,
		getUserCollections: getUserCollections};
});
