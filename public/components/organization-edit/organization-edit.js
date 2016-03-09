define(['knockout', 'text!./organization-edit.html', 'app', 'async!https://maps.google.com/maps/api/js?v=3&sensor=false', 'knockout-validation', 'smoke'], function (ko, template, app) {




	function OrganizationEditViewModel(params) {
		// Check if user is logged in. If not, ask for user to login
		if (localStorage.getItem('logged_in') != "true") {
			window.location.href = "#login";
		}

		$("div[role='main']").toggleClass("homepage", false);

		var self = this;
		self.id = ko.observable(params.id);
		self.route = params.route;
		self.name = params.name;				// Project or Organization (display field)
		self.namePlural = params.name + 's';	// Projects or Organizations (display field)

		// Group Details
		self.username = ko.observable().extend({
			required: true,
			minLength: 3,
			pattern: {
				message: 'Your username must be alphanumeric.',
				params: /^\w+$/
			}
		});
		self.friendlyName = ko.observable().extend({
			required: true
		});
		self.about = ko.observable();
		self.validationModel = ko.validatedObservable({
			username: self.username,
			friendlyName: self.friendlyName
		});
		self.avatar = {
			Original: ko.observable(),
			Tiny: ko.observable(),
			Square: ko.observable(),
			Thumbnail: ko.observable(),
			Medium: ko.observable()
		};

		// Page Fields
		self.page = {
			address: ko.observable(),
			city: ko.observable(),
			country: ko.observable(),
			url: ko.observable(),
			coordinates: {
				latitude: ko.observable(),
				longitude: ko.observable()
			},
			cover: {
				Original: ko.observable(),
				Tiny: ko.observable(),
				Square: ko.observable(),
				Thumbnail: ko.observable(),
				Medium: ko.observable()
			}
		};

		// Computed
		self.fullAddress = ko.pureComputed(function () {
			var addr = '';

			if (self.page.address() != null) {
				addr += self.page.address();
			}
			if (self.page.city() != null) {
				if (addr.length > 0) {
					addr += ', ';
				}
				addr += self.page.city();
			}
			if (self.page.country() != null) {
				if (addr.length > 0) {
					addr += ', ';
				}
				addr += self.page.country();
			}

			return addr;
		});
		self.coords = ko.computed(function () {
			if (self.page.coordinates.latitude() && self.page.coordinates.longitude()) {
				return "https://www.google.com/maps/embed/v1/place?q=" + self.page.coordinates.latitude() + "," + self.page.coordinates.longitude() + "&key=AIzaSyAN0om9mFmy1QN6Wf54tXAowK4eT0ZUPrU";
			} else {
				return null;
			}
		});
		self.backgroundImage = ko.computed(function () {
			if (self.page.cover.Original()) {
				return 'url(' + self.page.cover.Original() + ')';
			} else {
				return null;
			}
		});
		self.isCreator = ko.observable(false);
		self.isAdmin = ko.observable(false);

		$.ajax({
			type: 'GET',
			url: '/group/' + self.id(),
			success: function (data, textStatus, jqXHR) {
				self.loadGroup(data);
			}
		});

		self.goBack = function () {
			window.location.href = '#' + params.type + 's';
		};

		self.showGroup = function () {
			window.location.href = '#' + params.type + '/' + self.id();
		};

		self.deleteGroup = function () {
			$.smkConfirm({
				text: self.friendlyName() + ' will be permanently deleted. Are you sure?',
				accept: 'Delete',
				cancel: 'Cancel'
			}, function (ee) {
				if (ee) {
					$.ajax({
						type: 'DELETE',
						url: '/group/' + self.dbId(),
						success: function (data, textStatus, jqXHR) {
							self.goBack();
						},
						error: function (jqXHR, textStatus, errorThrown) {
							console.log(errorThrown);
						}
					});
				}
			});
		};

		self.loadGroup = function (data) {
			self.username(data.username);
			self.friendlyName(data.friendlyName);
			self.about(data.about);
			if (data.avatar != null) {
				self.avatar.Original(data.avatar.Original);
				self.avatar.Square(data.avatar.Square);
				self.avatar.Thumbnail(data.avatar.Thumbnail);
				self.avatar.Medium(data.avatar.Medium);
				self.avatar.Tiny(data.avatar.Tiny);
			}

			if (data.page != null) {
				self.page.address(data.page.address);
				self.page.city(data.page.city);
				self.page.country(data.page.country);
				self.page.url(data.page.url);
				if (data.page.coordinates != null) {
					self.page.coordinates.longitude(data.page.coordinates.longitude);
					self.page.coordinates.latitude(data.page.coordinates.latitude);
				}
				if (data.page.cover != null) {
					self.page.cover.Original(data.page.cover.Original);
					self.page.cover.Square(data.page.cover.Square);
					self.page.cover.Thumbnail(data.page.cover.Thumbnail);
					self.page.cover.Medium(data.page.cover.Medium);
					self.page.cover.Tiny(data.page.cover.Tiny);
				}
			}

			self.isCreator(app.currentUser._id() === data.creator);
			self.isAdmin(data.adminIds.indexOf(app.currentUser._id()) > 0);
			WITHApp.tabAction();
		};

		self.closeSideBar = function () {
			// Reload Group to reset changes
			$.ajax({
				type: 'GET',
				url: '/group/' + self.id(),
				success: function (data, textStatus, jqXHR) {
					self.loadGroup(data);
				}
			});

			// Close sidebar
			$('.action new').hide();

			// Hide errors
			self.validationModel.errors.showAllMessages(false);
		};

		self.saveChanges = function () {
			var data = {
				username: self.username,
				friendlyName: self.friendlyName,
				avatar: self.avatar,
				about: self.about,
				page: self.page
			};
			$.ajax({
				type: 'PUT',
				url: '/group/' + self.id(),
				contentType: 'application/json',
				dataType: 'json',
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					$.smkAlert({
						text: 'Update successful!',
						type: 'success'
					});

					// Close sidebar
					$('.action new').hide();
				},
				error: function (request, status, error) {
					var err = JSON.parse(request.responseText);
					$.smkAlert({
						text: err.error,
						type: 'danger'
					});
					console.log(error);
				}
			});
		};
	}

	return {
		viewModel: OrganizationEditViewModel,
		template: template
	};
});
