define("app", ['knockout'], function(ko) {

	var self        = this;
	var currentUser = ko.observable(false);
	isLogged        = ko.observable(false);
	// var data = {
	// 	"_id"              : "551fcf2777c887fab78fd064",
	// 	"email"            : "finikm@gmail.com",
	// 	"username"         : "finik",
	// 	"firstName"        : "Marios",
	// 	"lastName"         : "Phinikettos",
	// 	"gender"           : "MALE",
	// 	"facebookId"       : "",
	// 	"googleId"         : "",
	// 	"recordLimit"      : 0,
	// 	"collectedRecords" : 0,
	// 	"storageLimit"     : 0.0000000000000000
	// };
	// currentUser = ko.observable(ko.mapping.fromJS(data));
	// isLogged = ko.observable(true);

	loadUser        = function(data) {
		this.currentUser = ko.observable(ko.mapping.fromJS(data));
		isLogged(true);
	}

	return { currentUser: currentUser, loadUser: loadUser };
});