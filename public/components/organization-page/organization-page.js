define(['knockout', 'text!./organization-page.html', 'app', 'async!https://maps.google.com/maps/api/js?v=3&sensor=false', 'knockout-validation', 'jquery.fileupload'], function (ko, template, app) {

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	/* Custom bindingHandler for error message */
	ko.bindingHandlers.validationCore = {
		init: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
			var span = document.createElement('SPAN');
			span.className = 'help-block';
			var parent = $(element).parent().closest
			(".input-group");
			if (parent.length > 0) {
				$(parent).after(span);
			} else {
				$(element).after(span);
			}
			ko.applyBindingsToNode(span, { validationMessage: valueAccessor() });
		}
	};

	function OrganizationViewModel(params) {
		var self = this;

		// Generic Parameters
		self.baseURL = ko.observable('http://localhost:9000/assets/index.html#organization/');

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

		self.validationModel = ko.validatedObservable({
			username: self.username,
			friendlyName: self.friendlyName,
			about: self.about
		});

		// Page Fields
		self.page = {
			address: ko.observable(),
			city: ko.observable(),
			country: ko.observable(),
			url: ko.observable(),
			coverImage: ko.observable(),
			coverThumbnail: ko.observable(),
			// featuredCollections: ko.observableArray(),
			coordinates: {
				latitude: ko.observable(),
				longitude: ko.observable()
			}
		};

		// Display fields
		self.location = ko.computed(function () {
			return self.page.country && self.page.city ? self.page.city() + ', ' + self.page.country() : self.page.city() + self.page.country();
		});
		self.coverThumbnail = ko.computed(function () {
			return self.page.coverThumbnail ? '/media/' + self.page.coverThumbnail() : null;
		});
		self.logo = ko.computed(function() {
			return self.thumbnail ? '/media/' + self.thumbnail() : null;
		});

		if (params.id !== undefined) {
			self.id(params.id);
		}

		$('#imageupload').fileupload({
			type: "POST",
			url: '/media/create',
			acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
			maxFileSize: 50000,
    		done: function (e, data) {
				var urlID = data.result.results[0].thumbnailUrl.substring('/media/'.length);
				self.thumbnail(urlID);
			},
			error: function (e, data) {
				$.smkAlert({
					text: 'Error uploading the file',
					type: 'danger',
					time: 10
				});
			}
		});

		$('#coverupload').fileupload({
			type: "POST",
			url: '/media/create',
			acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
			maxFileSize: 500000,
			done: function (e, data) {
				self.page.coverImage(data.result.results[0].externalId);
				var urlID = data.result.results[0].thumbnailUrl.substring('/media/'.length);
				self.page.coverThumbnail(urlID);
			},
			error: function (e, data) {
				$.smkAlert({
					text: 'Error uploading the file',
					type: 'danger',
					time: 10
				});
			}
		});

		// Getting the coordinates from Google Maps is done asynchronously, so we have to pass the create/update functions
		// as parameters to be used as callbacks
		self.getCoordinatesAndSubmit = function (submitFunc) {
			if (self.page.address && self.page.city && self.page.country) {
				var address = self.page.address() + ', ' + self.page.city() + ', ' + self.page.country();
				var geocoder = new google.maps.Geocoder();
				geocoder.geocode({
					'address': address
				}, function (results, status) {
					if (status == google.maps.GeocoderStatus.OK) {
						self.page.coordinates.latitude(results[0].geometry.location.lat());
						self.page.coordinates.longitude(results[0].geometry.location.lng());
					}

					submitFunc();
				});
			} else {
				submitFunc();
			}
		};

		self.load = function (id) {
			$.ajax({
				type: "GET",
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
					self.page.coverThumbnail(obj.page.coverThumbnail);
					// self.featuredCollections = ko.mapping(obj.page.featuredCollections); // TODO: Validate it is working
					self.page.coordinates.longitude(obj.page.coordinates.longitude);
					self.page.coordinates.latitude(obj.page.coordinates.latitude);
				},
				error: function (request, status, error) {
					// TODO: Display error message
					console.log(error);
				}
			});
		};

		self.submit = function (type) {
			if (self.validationModel.isValid()) {
				if (type === 'new') {
					self.getCoordinatesAndSubmit(self.create);
				} else if (type === 'update') {
					self.getCoordinatesAndSubmit(self.update);
				} else {
					console.log('Unknown type: ' + type);
				}
			}
			else {
				self.validationModel.errors.showAllMessages();
			}
		};

		self.create = function () {
			var data = {
				username: self.username,
				friendlyName: self.friendlyName,
				thumbnail: self.thumbnail,
				about: self.about,
				page: self.page
			};

			$.ajax({
				type: "POST",
				url: "/organization/create",
				contentType: 'application/json',
				dataType: 'json',
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					$.smkAlert({
						text: 'Organization created successfully!',
						type: 'success'
					});
					self.closeWindow();
				},
				error: function (request, status, error) {
					// TODO: Display error message
					console.log(error);
				}
			});
		};

		self.update = function () {
			var data = {
				name: self.username,
				thumbnail: self.thumbnail,
				about: self.about,
				page: self.page
			};

			$.ajax({
				type: "PUT",
				url: "/group/" + self.id(),
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					$.smkAlert({
						text: 'Organization updated successfully!',
						type: 'success'
					});
					self.closeWindow();
				},
				error: function (request, status, error) {
					// TODO: Display error message
				}
			});
		};

		self.closeWindow = function () {
			app.closePopup();
		};
	}

	return {
		viewModel: OrganizationViewModel,
		template: template
	};
});
