define("app", ['knockout'], function(ko) {

	var currentUser = ko.observable();

	return { currentUser: currentUser };
});