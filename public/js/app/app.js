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
		var userData;
		if (sessionStorage.getItem('User') !== null) {
			userData = JSON.parse(sessionStorage.getItem("User"));
		}
		else if (localStorage.getItem('User') !== null) {
			userData = JSON.parse(localStorage.getItem('User'));
		}
		getUserCollections(userData);
	};

	getUserCollections = function(userData) {
		var username = userData.username;
		var email = userData.email;
		var ownerId =  userData._id.$oid;
		//ownerId, email, 	access: String ?="owned", offset: Integer ?=0, count: Integer ?=10
	};

	logout           = function() {
		sessionStorage.removeItem('User');
		localStorage.removeItem('User');
		isLogged(false);
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