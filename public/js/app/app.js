define("app", ['knockout'], function(ko) {

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
		"recordLimit"      : ko.observable(),
		"collectedRecords" : ko.observable(),
		"storageLimit"     : ko.observable()
	};
	isLogged         = ko.observable(false);

	loadUser         = function(data, remember) {
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
		storeUserCollections();
	};

	storeUserCollections = function() {
		getUserCollections();
	};

	getUserCollections = function() {
		var username = self.currentUser.username();
		var email = self.currentUser.email();
		var userId =  self.currentUser._id();
		$.ajax({
			type        : "GET",
			contentType : "application/json",
			dataType    : "json",
			url         : "/collection/list",
			processData : false,
			data        : "displayName=" + username+"&ownerId=" + userId + "&email=" + email + "&offset=0" + "&count=20",
				/*displayName: username,
				ownerId: userId,
				email: email,
				offset: 0,
				count: 20
			}),*/
			success     : function(data, text) {
				console.log("User collections " + JSON.stringify(data));
				if (sessionStorage.getItem('User') !== null) {
					sessionStorage.setItem('UserCollections', JSON.stringify(data));
				}
				else if (localStorage.getItem('User') !== null) {
					localStorage.setItem('UserCollections', JSON.stringify(data));
				}
			},
			error 		: function(request, status, error) {
				//var err = JSON.parse(request.responseText);
			}
		});
	};

	logout           = function() {
		$.ajax({
			type        : "GET",
			url         : "/user/logout",
			success     : function() {
				sessionStorage.removeItem('User');
				localStorage.removeItem('User');
				isLogged(false);
			}
		});
	}

	// Check if user information already exist in session
	if (sessionStorage.getItem('User') !== null) {
		var data = JSON.parse(sessionStorage.getItem('User'));
		loadUser(data, false);
	}
	else if (localStorage.getItem('User') !== null) {
		var data = JSON.parse(localStorage.getItem('User'));
		loadUser(data, true);
	}

	return { currentUser: currentUser, loadUser: loadUser, logout: logout };
});