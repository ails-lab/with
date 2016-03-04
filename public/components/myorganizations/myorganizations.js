define(['knockout', 'text!./myorganizations.html', 'app'], function (ko, template, app) {


	var mapping = {
		create: function (options) {
			var innerModel = ko.mapping.fromJS(options.data);
			innerModel.logo = ko.pureComputed(function () {
				if (typeof innerModel.avatar.Square === 'undefined') {
					return 'img/content/profile-placeholder.png';
				} else {
					return innerModel.avatar.Square();
				}
			});
			innerModel.cover = ko.pureComputed(function () {
				if (typeof innerModel.page.cover.Medium === 'undefined') {
					return 'img/content/background-space.png';
				} else {
					return innerModel.page.cover.Medium();
				}
			});

			return innerModel;
		},
		'dbId': {
			key: function (data) {
				return ko.utils.unwrapObservable(data.dbId);
			}
		}
	};

	function OrganizationsViewModel(params) {
		var self = this;
		self.route = params.route;
		self.groups = ko.observableArray([], mapping);
		self.name = ko.observable(params.type);
		self.namePlural = ko.observable(params.title);

		// Check if user is logged in. If not, ask for user to login
		if (localStorage.getItem('logged_in') != "true") {
			window.location.href = "#login";
		}

		$.ajax({
			url: '/group/list?groupType=' + params.type + '&offset=0&start=0&belongsOnly=true',
			type: 'GET',
			success: function (data) {
				ko.mapping.fromJS(data, mapping, self.groups);
				WITHApp.tabAction();
			}
		});

		self.hideMessage = function () {
			$("section.message").toggle();
		};

	}

	return {
		viewModel: OrganizationsViewModel,
		template: template
	};
});
