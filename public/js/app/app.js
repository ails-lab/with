define("app", ['knockout'], function(ko) {

	var currentUser = ko.observable();
	isLogged        = ko.computed(function() { return currentUser() !== undefined });

	return { currentUser: currentUser };
});