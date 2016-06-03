define(['knockout', 'text!./myorganizations.html', 'app', 'moment', 'async!https://maps.google.com/maps/api/js?v=3&sensor=false', 'knockout-validation', 'smoke'], function (ko, template, app, moment) {

	ko.validation.init({
		errorElementClass: 'error',
		errorMessageClass: 'errormsg',
		decorateInputElement: true
	});

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
			innerModel.creatorName = ko.pureComputed(function () {
				return 'by ' + innerModel.firstName() + ' ' + innerModel.lastName();
			});
			innerModel.itemsCount = ko.pureComputed(function () {
				var count = innerModel.count.Collections() + innerModel.count.Exhibitions();
				return count === 1 ? count + ' Collection' : count + ' Collections';
			});
			innerModel.isCreator = ko.pureComputed(function () {
				return innerModel.creator() === app.currentUser._id();
			});
			innerModel.isAdmin = ko.pureComputed(function () {
				return innerModel.adminIds().indexOf(app.currentUser._id()) >= 0;
			});
			innerModel.date = ko.pureComputed(function () {
				return moment(innerModel.created(), 'YYYY/MM/DD').format("LL");
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
		// Check if user is logged in. If not, ask for user to login
		if (localStorage.getItem('logged_in') != "true") {
			window.location.href = "#login";
		}

		$("div[role='main']").toggleClass("homepage", false);

		var self = this;
		self.route = params.route;
		self.groups = ko.observableArray([], mapping);
		self.name = ko.observable(params.type);			// Project or Organization (display field)
		self.namePlural = ko.observable(params.title);	// Projects or Organizations (display field)
		self.entryCount = ko.observable();
		self.groupCount = ko.pureComputed(function () {
			if (self.entryCount() === 1) {
				return self.entryCount() + ' ' + self.name();
			} else {
				return self.entryCount() + ' ' + self.namePlural();
			}
		});
		self.baseURL = ko.pureComputed(function () {
			return window.location.origin + '/assets/index.html#' + self.name().toLowerCase() + '/';
		});
		

		// Project Information
		self.username = ko.observable().extend({
			required: true,
			minLength: 3,
			pattern: {
				message: 'Your username must be alphanumeric.',
				params: /^\w+$/
			}
		});
		self.friendlyName = ko.observable().extend({
			required: true,
			minLength: 3,
			pattern: {
				message: 'Your username must be alphanumeric.',
				params: /^\w+$/
			}
		});
		self.about = ko.observable();
		self.validationModel = ko.validatedObservable({
			username: self.username,
			friendlyName: self.friendlyName
		});

		// Page Fields
		self.page = {
			address: ko.observable(),
			city: ko.observable(),
			country: ko.observable(),
			url: ko.observable(),
			coordinates: {
				latitude: ko.observable(),
				longitude: ko.observable()
			}
		};

		$.ajax({
			url: '/group/list?groupType=' + params.type + '&offset=0&belongsOnly=true',
			type: 'GET',
			success: function (data) {
				ko.mapping.fromJS(data.groups, mapping, self.groups);
				self.entryCount(data.groupCount);
				WITHApp.tabAction();

				if (self.groups().length % 10 > 0) {
					$('.loadmore').text('no more results');
				}
			}
		});

		self.hideMessage = function () {
			$("section.message").toggle();
		};
		
		self.findByGroupName = function (name) {
			$.ajax({
				url: '/group/findByGroupName?name='+name,
				type: 'GET',
				success: function (data) {
					window.location = '/#organization/'+data.groupId;
				}
			});
		}

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

		self.submit = function () {
			if (self.validationModel.isValid()) {
				self.getCoordinatesAndSubmit(self.create);
			} else {
				self.validationModel.errors.showAllMessages();
			}
		};

		self.deleteGroup = function (group) {
			$.smkConfirm({
				text: group.friendlyName() + ' will be permanently deleted. Are you sure?',
				accept: 'Delete',
				cancel: 'Cancel'
			}, function (ee) {
				if (ee) {
					$.ajax({
						type: 'DELETE',
						url: '/group/' + group.dbId(),
						success: function (data, textStatus, jqXHR) {
							self.groups.remove(group);
							console.log(data);
							self.entryCount(self.entryCount() -1);
						},
						error: function (jqXHR, textStatus, errorThrown) {
							console.log(errorThrown);
						}
					});
				}
			});
		};

		self.create = function () {
			var data = {
				username: self.username,
				friendlyName: self.friendlyName,
				about: self.about,
				page: self.page
			};
			$.ajax({
				type: "POST",
				url: "/" + params.type.toLowerCase() + "/create",
				contentType: 'application/json',
				dataType: 'json',
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					self.closeSideBar();
					$.smkAlert({
						text: 'A new ' + params.type.toLowerCase() + ' was created successfully!',
						type: 'success'
					});
					app.reloadUser();
					window.location.href = "#" + params.type.toLowerCase() + '/' + data.dbId + '/edit';
				},
				error: function (request, status, error) {
					var err = JSON.parse(request.responseText);
					self.username.setError(err.error);
					self.username.isModified(true);
					self.validationModel.errors.showAllMessages();
				}
			});
		};

		self.closeSideBar = function () {
			// Reset fields
			self.friendlyName(null);
			self.username(null);
			self.about(null);
			self.page.address(null);
			self.page.city(null);
			self.page.country(null);
			self.page.url(null);

			// Close sidebar
			$('.action new').hide();

			// Hide errors
			self.validationModel.errors.showAllMessages(false);
		};

		self.loadMore = function () {
			var offset = self.groups().length;

			$.ajax({
				url: '/group/list?groupType=' + params.type + '&offset=' + offset + '&belongsOnly=true',
				type: 'GET',
				success: function (data) {
					var newItems = ko.mapping.fromJS(data, mapping);
					self.groups.push.apply(self.groups, newItems.groups());

					WITHApp.tabAction();
					if (data.groups.length === 0 || data.groups.length % 10 > 0) {
						$('.loadmore').text('no more results');
					}
				}
			});
		};

		self.editGroup = function (group) {
			window.location.href = '#' + params.type.toLowerCase() + '/' + group.dbId()  + '/edit';
		};

		/* *********************************************************************
		 *
		 * Manage Groups functions
		 *
		 * *********************************************************************/

		self.myUsername = ko.observable(app.currentUser.username());
		self.userId = ko.observable(app.currentUser._id());
		self.groupId = ko.observable();
		if (params.id !== undefined) {
			self.groupId(params.id);
		}
		self.userMembers = ko.mapping.fromJS([], {});
		self.groupMembers = ko.mapping.fromJS([], {});
		self.image = "";
		self.colors = ['blue', 'green', 'red', 'yellow'];
		// mapping to state with key is the identifier
		var usersMapping = {
			'dbId': {
				key: function (data) {
					return ko.utils.unwrapObservable(data.username);
				}
			}
		};


		ko.bindingHandlers.autocompleteUsername = {
			init: function (elem, valueAccessor, allBindingsAccessor, viewModel, context) {
				app.autoCompleteUserName(elem, valueAccessor, allBindingsAccessor, viewModel, context, function (suggestion) {
					viewModel.addToUserGroup();
				});
			}
		};
		
		ko.bindingHandlers.autocompleteGroupname = {
				init: function (elem, valueAccessor, allBindingsAccessor, viewModel, context) {
					app.autoCompleteUserName(elem, valueAccessor, allBindingsAccessor, viewModel, context, function (suggestion) {
						viewModel.findByGroupName(suggestion);
					});
				}
			};

		arrayFirstIndexOf = function (array, predicate) {
			for (var i = 0, j = array.length; i < j; i++) {
				if (predicate.call(undefined, array[i])) {
					return i;
				}
			}
			return -1;
		};

		self.getMembersInfo = function (category, group) {
			self.groupId(group.dbId());
			$.ajax({
				method : "GET",
				contentType : "application/json",
				url : "/group/membersInfo/" + self.groupId(),
				data : "category=" + category,
				success : function (result) {
					if (result.users !== undefined) {
						var users = result.users;
						ko.mapping.fromJS(users, self.usersMapping, self.userMembers);
					}
					if (result.groups !== undefined) {
						var userGroups = result.groups;
						ko.mapping.fromJS(userGroups, self.usersMapping, self.groupMembers);
					}
				},
				error : function (result) {
					$.smkAlert({text: "Invalid groupId", type: 'danger', time: 10});
				}
			});
		};

		// fill userMembers, groupMembers arrays on load
		//self.getMembersInfo("both");

		self.addToUserGroup = function () {
			var username = $("#userName").val();
			if (username == "") {
				username = $("#groupName").val();
			}
			$("#userName").val("");
			$("#groupName").val("");
			var userId = self.userId();
			$.ajax({
				method : "GET",
				contentType : "application/json",
				url : "/user/findByUserOrGroupNameOrEmail",
				data : "userOrGroupNameOrEmail=" + username,
				success : function (result) {
					self.excecuteAdd(result);
				},
				error : function (result) {
					$.smkAlert({ text: 'There is no such username or email', type: 'danger', time: 10 });
				}
			});
		};

		self.excecuteAdd = function (userData) {
			$.ajax({
				method : "PUT",
				contentType : "text/plain",
				url : "/group/addUserOrGroup/" + self.groupId() + "?id=" + userData.userId,
				success : function (result) {
					self.image = userData.image;
					if (userData.category == "user") {
						self.userMembers.push(ko.mapping.fromJS(userData));
					} else {
						self.groupMembers.push(ko.mapping.fromJS(userData));
					}
				},
				error : function (result) {
					$.smkAlert({ text: result.responseJSON.error, type: 'danger', time: 10 });
				}
			});
		};
		
		self.getAPIUrlGroup = function (belongsOnly) {
			var url = window.location.href.split("assets")[0];
			var groupCall = url + "group/list?groupType="+self.name()+"&belongsOnly="+belongsOnly;
			return groupCall;
		};


		self.makeAdmin = function (userId) {
			console.log("makeAdmin");
			$.ajax({
				method : "PUT",
				contentType : "text/plain",
				url : "/group/admin/" + self.groupId() + "?id=" + userId,
				success : function (result) {
					/*self.image = userData.image;
					self.userMembers.push(ko.mapping.fromJS(userData));*/
				},
				error : function (result) {
					$.smkAlert({ text: result.responseJSON.error, type: 'danger', time: 10 });
				}
			});
		};

		self.makeMember = function (userId) {
			console.log("makeMember");
			$.ajax({
				method : "DELETE",
				contentType : "text/plain",
				url : "/group/admin/" + self.groupId() + "?id=" + userId,
				success : function (result) {
				},
				error : function (result) {
					$.smkAlert({ text: result.responseJSON.error, type: 'danger', time: 10 });
				}
			});
		};

		self.isAdminToggle = function(admin, userId){
			if (!admin) 
				self.makeMember(userId);
			else 
				self.makeAdmin(userId);
			return true;
		}
		
		self.excecuteRemove = function (id, category) {
			$.ajax({
				method : "DELETE",
				contentType : "text/plain",
				url : "/group/removeUserOrGroup/" + self.groupId() + "?id=" + id,
				success : function (result) {
					if (category == "user") {
						var index = arrayFirstIndexOf(self.userMembers(), function (item) {
							return item.userId() === id;
						});
						if (index > -1) {
							self.userMembers.splice(index, 1);
						}
					} else {
						var index = arrayFirstIndexOf(self.groupMembers(), function (item) {
							return item.userId() === id;
						});
						if (index > -1) {
							self.groupMembers.splice(index, 1);
						}
					}
				},
				error: function (result) {
					$.smkAlert({ text: 'There is no such username or email', type: 'danger', time: 10});
				}
			});
		};

		
		/* ****************************************************************
		 *
		 * ****************************************************************/

	}

	return {
		viewModel: OrganizationsViewModel,
		template: template
	};
});
