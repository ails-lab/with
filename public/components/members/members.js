define(['knockout', 'text!./members.html', 'app'], function(ko, template, app) {

	function MemberViewModel(params) {
		var self = this;
		
		self.myUsername   = 	ko.observable(app.currentUser.username());
		self.userId 	  = 	ko.observable(app.currentUser._id());
		self.groupId 	      = 	ko.observable();
		if(params.id !== undefined) { self.groupId(params.id); }
		self.userMembers  =     ko.mapping.fromJS([], {});
		self.groupMembers = 	ko.mapping.fromJS([], {});
		self.image		  =     "";
		// mapping to state with key is the identifier
		var usersMapping = {
				'dbId': {
					key: function(data) {
			            return ko.utils.unwrapObservable(data.username);
			        }
				}
		};
		
		
		ko.bindingHandlers.autocompleteUsername = {
			      init: function(elem, valueAccessor, allBindingsAccessor, viewModel, context) {
			    	  app.autoCompleteUserName(elem, valueAccessor, allBindingsAccessor, viewModel, context);
			      }
			 };
		
		arrayFirstIndexOf = function(array, predicate) {
		    for (var i = 0, j = array.length; i < j; i++) {
		        if (predicate.call(undefined, array[i])) {
		            return i;
		        }
		    }
		    return -1;
		}
		
		self.getMembersInfo = function(category) {
			console.log("members info");
			$.ajax({
				method      : "GET",
				contentType    : "application/json",
				url         : "/group/membersInfo/" + self.groupId(),
				data		: "category="+category,
				success		: function(result) {
					if(result.users !== undefined) {
						var users = result.users;
						ko.mapping.fromJS(users, self.usersMapping, self.userMembers);
					}
					if(result.groups !== undefined) {
		   				var userGroups = result.groups;
						ko.mapping.fromJS(userGroups, self.usersMapping, self.groupMembers);
					}
				},
				error      : function(result) {
					$.smkAlert({text: "Invalid groupId", type:'danger', time: 10});
				}
			});
		}
		// fill userMembers, groupMembers arrays on load
		self.getMembersInfo("both");
		
		self.addToUserGroup = function(clickedRights) {
			var username = $("#userName").val();
			if(username == "") { username = $("#groupName").val(); }
			$("#userName").val("");
			$("#groupName").val("");
			var userId = self.userId();
			$.ajax({
				method      : "GET",
				contentType    : "application/json",
				url         : "/user/findByUserOrGroupNameOrEmail",
				data: "userOrGroupNameOrEmail="+username,
				success		: function(result) {					
					self.excecuteAdd(result);
				},
				error      : function(result) {
					$.smkAlert({text:'There is no such username or email', type:'danger', time: 10});
				}
			});
		}
		
		self.excecuteAdd = function(userData) {
			$.ajax({
				method      : "PUT",
				contentType    : "text/plain",
				url         : "/group/addUserOrGroup/" + self.groupId()+"?id="+userData.userId,
				success		: function(result) {
					self.image = userData.image;
					if(userData.category == "user") {
						self.userMembers.push(ko.mapping.fromJS(userData));
					} else {
						self.groupMembers.push(ko.mapping.fromJS(userData));
					}
				},
				error      : function(result) {
					$.smkAlert({text: result.responseJSON.error, type:'danger', time: 10});
				}
			});
		}
		
		self.excecuteRemove = function(id, category) {
			$.ajax({
				method      : "DELETE",
				contentType    : "text/plain",
				url         : "/group/removeUserOrGroup/" + self.groupId()+"?id="+id,
				success		: function(result) {
					if(category == "user") {
						var index = arrayFirstIndexOf(self.userMembers(), function(item) {
							   return item.userId() === id;
						});
			   			if (index > -1)
			   				self.userMembers.splice(index, 1);
					} else {
						var index = arrayFirstIndexOf(self.groupMembers(), function(item) {
							   return item.userId() === id;
						});
			   			if (index > -1) {
			   				self.groupMembers.splice(index, 1);
			   			}
					}
				},
				error: function(result) {
					$.smkAlert({text:'There is no such username or email', type:'danger', time: 10});
				}
			});
		}
		
		self.showRemoveIcon = function(userData) {
			var userId = userData.userId();
			$("#removeIcon_"+userId).show();
			$("#image_"+userId).css("opacity", "0.5");
		}
		
		
		self.hideRemoveIcon = function(userId) {
			$("#removeIcon_"+userId).hide();
			$("#image_"+userId).css("opacity", "1");
		}
		
		self.closeWindow  = function() {
			app.closePopup();
		};
	}

	
	return { viewModel: MemberViewModel, template: template };
});