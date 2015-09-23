define(['knockout', 'text!./organization-page.html', 'app', 'async!https://maps.google.com/maps/api/js?v=3&sensor=false', 'knockout-validation'], function (ko, template, app) {

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	function OrganizationViewModel(params) {
		var self = this;

		// UserGroup Fields
		self.id = ko.observable();
		self.username = ko.observable().extend({
			required: true
		});
		self.friendlyName = ko.observable().extend({
			required: true
		});
		self.thumbnail = ko.observable();
		self.about = ko.observable().extend({
			required: true
		});
		self.page = [];

		// Page Fields
		self.page.address = ko.observable();
		self.page.city = ko.observable();
		self.page.country = ko.observable();
		self.page.url = ko.observable();
		self.page.coverImage = ko.observable();
		self.page.featuredCollections = ko.observableArray();
		self.page.coordinates = [];
		self.page.coordinates.latitude = ko.observable();
		self.page.coordinates.longitude = ko.observable();

		// Display fields
		self.location = ko.computed(function () {
			return self.page.country && self.page.city ? self.page.city() + ', ' + self.page.country() : self.page.city() + self.page.country();
		});

		if (params.id !== undefined) {
			self.id(params.id);
		}

		self.getCoordinates = function (address, city, country) {
			var addr = address + ', ' + city + ', ' + country;
			var geocoder = new google.maps.Geocoder();
			geocoder.geocode({
				'address': addr
			}, function (results, status) {
				if (status == google.maps.GeocoderStatus.OK) {
					self.page.coorrdinates.latitude(results[0].geometry.location.lat());
					self.page.coorrdinates.longitude(results[0].geometry.location.lng());
				}
			});
		};

		self.load = function (id) {
			$.ajax({
				type: "GET",
<<<<<<< HEAD
				url: "/group/" + self.id(),
				processData: false,
				success: function (data, text) {
					var obj = JSON.parse(data);
					self.username(obj.username);
					self.friendlyName(obj.friendlyName);
					self.thumbnail(obj.thumbnail);
					self.about(obj.about);

					self.page.address(obj.page.address);
					self.page.city(obj.page.city);
					self.page.country(obj.page.country);
					self.page.url(obj.page.url);
					self.page.coverImage(obj.page.coverImage);
					// self.featuredCollections = ko.mapping(obj.page.featuredCollections); // TODO: Validate it is working
					self.page.coordinates.longitude(obj.page.coordinates.longitude);
					self.page.coordinates.latitude(obj.page.coordinates.latitude);
=======
				url: "/organization/" + self.id(),
				processData: false,
				success: function (data, text) {
					self.name(data.name);
					self.thumbnail(data.thumbnail);
					self.description(data.thumbnail);

					self.page.address(data.page.address);
					self.page.city(data.page.address);
					self.page.country(data.page.country);
					self.page.coverImage(data.page.coverImage);
					self.featuredCollections = ko.mapping(data.page.featuredCollections); // TODO: Validate it is working
					self.page.coordinates.longitude(data.page.coordinates.longitude);
					self.page.coordinates.latitude(data.page.coordinates.latitude);
>>>>>>> Placeholders ajax calls for organizations
				},
				error: function (request, status, error) {
					// TODO: Display error message
				}
			});
		};

		self.create = function () {
			var data = {
<<<<<<< HEAD
				username: self.username,
				friendlyName: self.friendlyName,
				thumbnail: self.thumbnail,
				about: self.about,
=======
				name: self.name,
				thumbnail: self.thumbnail,
				description: self.description,
>>>>>>> Placeholders ajax calls for organizations
				page: self.page
			};

			$.ajax({
				type: "POST",
				url: "/organization/create",
				processData: falce,
				data: ko.toJSON(data),
				success: function (data, text) {
					// TODO: Notification for success and redirect to the organization page
				},
				error: function (request, status, error) {
					// TODO: Display error message
				}
			});
		};

		self.saveChanges = function () {
			var data = {
<<<<<<< HEAD
				name: self.username,
				thumbnail: self.thumbnail,
				about: self.about,
=======
				name: self.name,
				thumbnail: self.thumbnail,
				description: self.description,
>>>>>>> Placeholders ajax calls for organizations
				page: self.page
			};

			$.ajax({
<<<<<<< HEAD
				type: "PUT",
				url: "/group/" + self.id(),
=======
				type: "POST",
				url: "/organization/" + self.id(),
>>>>>>> Placeholders ajax calls for organizations
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					// TODO: Notification that changes were saved
				},
				error: function (request, status, error) {
					// TODO: Display error message
				}
			});
		};

	}

	return {
		viewModel: OrganizationViewModel,
		template: template
	};
});