define(['knockout', 'text!./myorganizations.html', 'app'], function (ko, template, app) {


	var mapping = {
		create: function (options) {
			var innerModel = ko.mapping.fromJS(options.data);
			innerModel.logo = ko.pureComputed(function () {
				return innerModel.avatar() != null ? innerModel.avatar.Square : 'img/content/profile-placeholder.png';
			});
			innerModel.cover = ko.pureComputed(function () {
				return innerModel.page.cover() != null ? innerModel.page.cover.Medium : 'img/content/background-space.png';
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

		// Check if user is logged in. If not, ask for user to login
		if (localStorage.getItem('logged_in') != "true") {
			window.location.href = "#login";
		}

		$.ajax({
			url: '/group/list?groupType=' + params.type + '&offset=0&start=0&belongsOnly=true',
			type: 'GET',
			success: function (data) {
				console.log(data);
				ko.mapping.fromJS(data, mapping, self.groups);
				console.log(self.groups());
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
