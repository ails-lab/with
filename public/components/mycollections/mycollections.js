define(['bootstrap', 'knockout', 'text!./mycollections.html', 'knockout-else','app', 'moment', 'knockout-validation'], function (bootstrap, ko, template, KnockoutElse, app, moment) {

	count = 12;
	accessLevels = {
		READ : 0,
		WRITE : 1,
		OWN : 2
	};

	ko.bindingHandlers.autocompleteUsername1 = {
		init: function (elem, valueAccessor, allBindingsAccessor, viewModel, context) {
			app.autoCompleteUserName(elem, valueAccessor, allBindingsAccessor, viewModel, context, function (suggestion) {
				viewModel.addToSharedWithUsers("READ", suggestion);
			});
		}
	}

	ko.bindingHandlers.redirectToLogin = {
		init: function (elem, valueAccessor, allBindingsAccessor, viewModel, context) {
			window.location = '#login';
		}
	};

	ko.validation.init({
		errorElementClass: 'error',
		errorMessageClass: 'errormsg',
		decorateInputElement: true
	});

	function getCollectionsSharedWithMe(isExhibition, offset, count) {
		return $.ajax({
			type        : "GET",
			contentType : "application/json",
			dataType    : "json",
			url         : "/collection/listShared",
			processData : false,
			data        : "isExhibition=" + isExhibition + "&offset=" + offset + "&count=" + count + "&collectionHits=true"
		}).done(function (data) {
			return data;
		}).fail(function (request, status, error) {
			console.log(JSON.parse(request.responseText));
		});
	}
	
	

	function MyCollectionsModel(params) {
		KnockoutElse.init([spec = {}]);
		var self = this;
		//WITHApp.tabAction();
		//WITHApp.initTooltip();
		self.route = params.route;
		self.showsExhibitions = params.showsExhibitions;
		self.showImportFromEuropeana = true;//(app.currentUser.username()=="foodanddrink");
		
		self.index = ko.observable(0);

		self.collectionSet = ko.observable("my");
		
		self.openAction = function (myclass){
			$( '.action' ).removeClass( 'active' );
			$( '.action.'+myclass ).addClass( 'active' );				
		};
		
		self.europeanaID = ko.observable("");
		self.importEuropeanaCollection = function () {
		    $.ajax({
		    	"url": "/collection/importEuropeanaCollection?id="+self.europeanaID(),
		    	"method": "GET",
		    	"success": function( data, textStatus, jQxhr ){
		    	    $.smkAlert({text: 'Collection Imported', type: 'success'});
		    		self.reloadCollection(data);
		    		app.currentUser.collectionCount(app.currentUser.collectionCount() + 1);
		    		self.collectionCount(self.collectionCount() + 1);
		    		self.closeSideBar();
				},
				"error": function (result) {
					$.smkAlert({ text: 'An error occured', type: 'danger', time: 10 });
					self.closeSideBar();
				}         
		    });		
		};
		
		self.importCollectionName = ko.observable("");
		self.europeanaLimit = ko.observable(-1);
		self.europeanaSearch = ko.observable("");
		self.europeanaSearchTail = ko.observable("");
		self.importEuropeanaSearch = function () {
		    var jsondata = JSON.stringify({
		    	collectionName: self.importCollectionName(),
				limit : self.europeanaLimit(),
				query : {
					searchTerm : self.europeanaSearch(),
					tail : self.europeanaSearchTail(),
					page : 1,
					pageSize : 20
				}
			});

		    $.ajax({
					"url": "/collection/importSearch",
					"method": "POST",
					"contentType": "application/json",
					"data":  jsondata,
					"success": function( data, textStatus, jQxhr ){
			    	    $.smkAlert({text: 'Collection Imported', type: 'success'});
			    		self.reloadCollection(data);
			    		app.currentUser.collectionCount(app.currentUser.collectionCount() + 1);
			    		self.collectionCount(self.collectionCount() + 1);
			    		self.closeSideBar();
					},
					"error": function (result) {
						$.smkAlert({ text: 'An error occured', type: 'danger', time: 10 });
						self.closeSideBar();
					}
        
				});

		};
		
		

		var mapping = {
			create: function (options) {
				//customize at the root level: add title and description observables, based on multiliteral
				//TODO: support multilinguality, have to be observable arrays of type [{lang: default, values: []}, ...]
				var innerModel = ko.mapping.fromJS(options.data);
				innerModel.title = ko.observable(self.multiLiteral(options.data.descriptiveData.label));
				var dbDescription = options.data.descriptiveData.description;
				if (dbDescription == undefined || dbDescription == null) {
					innerModel.description = ko.observable("");
				} else {
					innerModel.description = ko.observable(self.multiLiteral(dbDescription));
				}
				$.each(innerModel.media(), function (index, value) {
					//withUrl = value.Thumbnail.withUrl(); still not fully working
					var withUrl = value.Thumbnail.withUrl();
				    var url     = value.Thumbnail.url();
					if (withUrl == "" || withUrl == null) {
						if(innerModel.administrative.entryCount() > 0)
							innerModel.media()[index].thumbnailUrl = ko.observable("img/ui/ic-noimage.png");
					} 						
					else {
						if (withUrl.indexOf("/media") == 0) {
							innerModel.media()[index].thumbnailUrl = window.location.origin + withUrl;
						} else {
							innerModel.media()[index].thumbnailUrl = withUrl;
						}
					}
				});
				innerModel.itemCount = ko.pureComputed(function () {
					var count = innerModel.administrative.entryCount();
					return count === 1 ? count + ' Item' : count + ' Items';
				});
				innerModel.creatorName = ko.pureComputed(function () {
					var firstName = innerModel.withCreatorInfo.firstName();
					var lastName = innerModel.withCreatorInfo.lastName();
					if (firstName !== undefined && firstName !== null
							&& lastName !== undefined && lastName !== null)
						return 'by ' + firstName + ' ' + lastName;
					else
						return 'by ' + innerModel.withCreatorInfo.username();
				});
				innerModel.date = ko.pureComputed(function () {
					return moment(innerModel.administrative.created()).format("MMMM Do YYYY");
				});
				innerModel.backgroundImg = ko.pureComputed(function () {
					if (!(innerModel.descriptiveData.backgroundImg == null || innerModel.descriptiveData.backgroundImg.Thumbnail == null || 
							innerModel.descriptiveData.backgroundImg.Thumbnail.withUrl == null || 
							innerModel.descriptiveData.backgroundImg.Thumbnail.withUrl() == "")) {
						if (innerModel.descriptiveData.backgroundImg.Thumbnail.withUrl().indexOf("/media") == 0) {
							return window.location.origin + innerModel.descriptiveData.backgroundImg.Thumbnail.withUrl();
						} else {
							return innerModel.descriptiveData.backgroundImg.Thumbnail.withUrl();
						}
					} 						
					else {
						return "";
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

		var usersMapping = {
			'dbId': {
				key: function (data) {
					return ko.utils.unwrapObservable(data.username);
				}
			}
		};

		self.myCollections = ko.mapping.fromJS([], mapping);
		self.query = ko.observable("");
		self.searchMyCollections = ko.computed(function() {
		    if (self.query() === "") {
		    	return self.myCollections;
		    } else {
		    	return self.myCollections.filter(function(i) {
		    	   return (i.title()).toLowerCase().indexOf(self.query().toLowerCase()) == 0;
		    	 });
		    }
		});
		self.sharedCollections = ko.mapping.fromJS([], mapping);
		self.searchSharedCollections = ko.computed(function() {
		    if (self.query() === "") {
		    	return self.sharedCollections;
		    } else {
		    	return self.sharedCollections.filter(function(i) {
		    	   return (i.title()).toLowerCase().indexOf(self.query().toLowerCase()) == 0;
		    	 });
		    }
		});
		self.titleToEdit = ko.observable("").extend({ required: true, minLength: 2});
		self.descriptionToEdit = ko.observable("");
		self.isPublicToEdit = ko.observable(false);
		self.apiUrl = ko.observable("");
		self.usersToShare = ko.mapping.fromJS([], {});
		self.userGroupsToShare = ko.mapping.fromJS([], {});
		self.loading = ko.observable(false);
		//self.editedUsersToShare = ko.mapping.fromJS([], {});

		self.myUsername = ko.observable(app.currentUser.username());
		self.collectionCount = ko.observable();
		self.exhibitionCount = ko.observable();
		self.collectionSharedCount = ko.observable();
		self.exhibitionSharedCount = ko.observable();
		self.collectionOrExhibitionCount =  ko.pureComputed(function () {
			var count, type;
			if (self.showsExhibitions) {
				if (self.collectionSet()==='my')
					count = self.exhibitionCount();
				else
					count = self.exhibitionSharedCount();
				type =  ' exhibition'; 
			} else {
				if (self.collectionSet()==='my')
					count = self.collectionCount();
				else
					count = self.collectionSharedCount();
				type = ' collection';
			}
			if (count!== 1) {
				return count + type +'s';
			} else {
				return count + type;
			}
		});
		
		self.moreCollectionData = ko.observable(true);
		self.moreSharedCollectionData = ko.observable(true);
		self.validationModel = ko.validatedObservable({
			title: self.titleToEdit
		});

		self.multiLiteral = function (multiLiteral) {
			return app.findByLang(multiLiteral);
		};

		self.init = function () {
			WITHApp.tabAction();
			
			if (self.showsExhibitions) {
				mapping.label = {
					create: function (options) {
						if (options.data.default.indexOf('Dummy') === -1) {
							return ko.observable(options.data.default);
						} else {
							return ko.observable('Add Title');
						}
					}
				};
				var promise = app.getUserCollections(true, 0, count);
				self.loading(true);
				var promiseShared = getCollectionsSharedWithMe(true, 0, count);
				$.when(promise, promiseShared).done(function (data,data2) {
					ko.mapping.fromJS(data[0].collectionsOrExhibitions, mapping, self.myCollections);
					ko.mapping.fromJS(data2[0].collectionsOrExhibitions, mapping, self.sharedCollections);
					self.exhibitionCount(data[0].totalExhibitions);
					self.exhibitionSharedCount(data2[0].totalExhibitions);
					self.loading(false);
					WITHApp.tabAction();
					WITHApp.initTooltip();
				});
			} else {
				var promise = app.getUserCollections(false, 0, count);
				self.loading(true);
				var promiseShared = getCollectionsSharedWithMe(false, 0, count);
				$.when(promise, promiseShared).done(function (data,data2) {
					ko.mapping.fromJS(data[0].collectionsOrExhibitions, mapping, self.myCollections);
					ko.mapping.fromJS(data2[0].collectionsOrExhibitions, mapping, self.sharedCollections);
					self.collectionCount(data[0].totalCollections);
					self.collectionSharedCount(data2[0].totalCollections);
					self.loading(false);
					WITHApp.tabAction();
					WITHApp.initTooltip();
				});
			}
		};

		WITHApp.changeList();
		WITHApp.initTooltip();

		self.checkLogged = function () {
			// Check if user is logged in. If not, ask for user to login
			if (localStorage.getItem('logged_in') != "true") {
				window.location.href = "#login";
			} else {
				if(currentUser.username())
				self.init();
				else{
					setTimeout(function(){ self.init(); }, 1500);
					
				}
			}
		};

		self.checkLogged();

		self.hideMessage = function () {
			$("section.message").toggle();
		};

		self.deleteMyCollection = function (collection) {
			var collectionId = collection.dbId();
			var collectionTitle = collection.title();
			self.showDelCollPopup(collectionTitle, collectionId);
			self.closeSideBar();
		};



		self.createCollection = function (collectionType) {
			if (self.validationModel.isValid()) {
				var jsondata = JSON.stringify({
					administrative: {
						access: {
							isPublic: self.isPublicToEdit()
						},
					},
					descriptiveData : {
						label : {
							default : [self.titleToEdit()]
						},
						description : {
							default : [self.descriptionToEdit()]
						}
					},
					resourceType: collectionType
				});
				$.ajax({
					"url": "/collection",
					"method": "post",
					"contentType": "application/json",
					"data": jsondata,
					"success": function (data) {
						self.reloadCollection(data);
						if (collectionType == 'SimpleCollection') {
							app.currentUser.collectionCount(app.currentUser.collectionCount() + 1);
							self.collectionCount(self.collectionCount() + 1);
				    	}
				        if (collectionType == 'Exhibition') {
							app.currentUser.exhibitionCount(app.currentUser.exhibitionCount() + 1);
							administrative.created	= self.exhibitionCount(self.exhibitionCount() + 1);
							window.location = '#exhibition-edit/'+data.dbId;
				        }
						self.closeSideBar();
					},
					"error": function (result) {
						$.smkAlert({ text: 'An error occured', type: 'danger', time: 10 });
						self.closeSideBar();
					}
				});
			} else {
				self.validationModel.errors.showAllMessages();
			}
		};


		self.showDelCollPopup = function (collectionTitle, collectionId) {
			var myself = this;
			myself.id = collectionId;
			$.smkConfirm({
				text: "All records in " + collectionTitle + " will be deleted. Are you sure?",
				accept: 'Delete',
				cancel: 'Cancel'
			}, function (ee) {
				if (ee) {
					deleteCollection(myself.id);
				}
			});
		};

		deleteCollection = function (collectionId) {
			$.ajax({
				"url": "/collection/" + collectionId,
				"method": "DELETE",
				success: function (result) {
					$.smkAlert({text: 'Collection removed', type: 'success'});
					self.myCollections.remove(function (item) {
						return item.dbId() == collectionId;
					});
					self.sharedCollections.remove(function (item) {
						return item.dbId() == collectionId;
					});
					/*var theitem = ko.utils.arrayFirst(currentUser.editables(), function (item) {
						return item.dbId === collectionId;
					});
					currentUser.editables.remove(theitem);*/
					if (self.showsExhibitions) {
						app.currentUser.exhibitionCount(app.currentUser.exhibitionCount() - 1);
						self.exhibitionCount(self.exhibitionCount() - 1);
					}
					else {
						app.currentUser.collectionCount(app.currentUser.collectionCount() - 1);
						self.collectionCount(self.collectionCount() - 1);
					}
				}
			});
		};

		self.moreCollections = function (isExhibition) {
			if (self.collectionSet() == "my") {
				self.more(isExhibition, app.getUserCollections, true);
			} else if (self.collectionSet() == "shared") {
				self.more(isExhibition, getCollectionsSharedWithMe, false);
			}
		};

		self.more = function (isExhibition, funcToExecute, my) {
			if (self.loading() === true) {
				setTimeout(self.moreCollections(isExhibition), 300);
			}
			if (self.loading() === false && self.moreCollectionData() === true) {
				self.loading(true);
				var offset = self.collectionSet() == "my" ? self.myCollections().length : self.sharedCollections().length;
				var promise = funcToExecute(isExhibition, offset, count);
				$.when(promise).done(function (data) {
					var newItems = ko.mapping.fromJS(data.collectionsOrExhibitions, mapping);
					if (my) {
						self.myCollections.push.apply(self.myCollections, newItems());
					} else {
						self.sharedCollections.push.apply(self.sharedCollections, newItems());
					}
					self.loading(false);
					if (data.collectionsOrExhibitions.length < count - 1) {
						self.moreCollectionData(false);
					} else {
						self.moreCollectionData(true);
					}
				}).fail(function (result) {
					self.loading(false);
				});
			}
		};

		self.openShareCollection = function (collection, event) {
			var context = ko.contextFor(event.target);
			var collIndex = context.$index();
			self.index(collIndex);
			self.isPublicToEdit(collection.administrative.access.isPublic());
			//$(".user-selection").devbridgeAutocomplete("hide");
			$.ajax({
				method     : "GET",
				contentType    : "application/json",
				url         : "/collection/" + collection.dbId() + "/listUsers",
				success		: function (result) {
					var index = arrayFirstIndexOf(result, function (item) {
						return item.username === self.myUsername();
					});
					if (index > -1) {
						result.splice(index, 1);
					}

					var users = [];
					var userGroups = [];
					$.each(result, function (i, item) {
						if (item.accessRights == "READ") {
							item.accessChecked = ko.observable(false);
						} else {
							item.accessChecked = ko.observable(true);
						}
						if (item.category == "user") {
							users.push(item);
						} else if (item.category == "group") {
							userGroups.push(item);
						}
					});
					ko.mapping.fromJS(users, self.usersMapping, self.usersToShare);
					ko.mapping.fromJS(userGroups, self.usersMapping, self.userGroupsToShare);
					$( '.action' ).removeClass( 'active' );
					$( '.action.access' ).addClass( 'active' );
				}
			});
		};

		self.searchCollectionName = function (elem, valueAccessor, allBindingsAccessor, viewModel, context, callback) {
			
//			self.myCollections.filter(function() {
//		        return this == 'two';
//		    }).css('color','red')		
		};
				
		self.changeRights = function (userData) {
			if (userData.accessChecked()) {
				self.shareCollection(userData, "WRITE");
			} else {
				self.shareCollection(userData, "READ");
			}
			return true;
		};

		self.removeRights = function (userData) {
			self.shareCollection(userData, "NONE");
			return true;
		};

		self.addToSharedWithUsers = function (clickedRights, username) {
			var indexUsers = arrayFirstIndexOf(self.usersToShare(), function (item) {
				return item.username() === username;
			});
			if (indexUsers < 0) {
				indexUsers = arrayFirstIndexOf(self.userGroupsToShare(), function (item) {
					return item.username() === username;
				});
			}
			if (indexUsers < 0) {
				var collId = self.myCollections()[self.index()].dbId();
				$.ajax({
					method      : "GET",
					contentType    : "application/json",
					url         : "/user/findByUserOrGroupNameOrEmail",
					data: "userOrGroupNameOrEmail=" + username + "&collectionId=" + collId,
					success		: function (result) {
						result.accessChecked = ko.observable(false);
						var koResult = ko.mapping.fromJS(result);
						self.shareCollection(koResult, clickedRights);
					},
					error      : function (result) {
						$.smkAlert({ text: 'There is no such username or email', type: 'danger', time: 10 });
					}
				});
			}
		};


		self.shareCollection = function (userData, clickedRights) {
			if (userData.category == "group" && clickedRights === "OWN") {
				$.smkConfirm({
					text: "Giving rights to a user group means that all members of the user" +
					" group will acquire these rights. Giving OWN rights to others means that they will have the right to delete" +
					" your collection, as well as to share it with others. Are you sure?",
					accept: 'Confirm',
					cancel: 'Cancel'
				}, function (ee) {
					if (ee) {
						self.callShareAPI(userData, clickedRights);
					}
				});
			} else if (userData.category == "group") {
				$.smkConfirm({
					text: "Giving rights to a user group means that all members of the user" +
							" group will acquire these rights. Are you sure?",
					accept: 'Confirm',
					cancel: 'Cancel'
				}, function (ee) {
					if (ee) {
						self.checkIfDowngrade(userData, clickedRights);
					}
				});
			} else if (clickedRights === "OWN") {
				$.smkConfirm({
					text: "Giving OWN rights to others means that they will have the right to delete your collection, " +
					"as well as to share it with others. Are you sure?",
					accept: 'Confirm',
					cancel: 'Cancel'
				}, function (ee) {
					if (ee) {
						self.callShareAPI(userData, clickedRights);
					}
				});
			} else {
				self.checkIfDowngrade(userData, clickedRights);
			}
		};


		//TODO: find all members of the collections that I OWN. Find all collections these records belong to.
		// Find if the user userData.username has access to these collections that are greater than clicked rights -
		//if yes, present the following message:
		//userData.username will still have currentAccess to the records [...] via collections [...] (to which userData.username has access).
		//Do you want to downgrade access rights userData.username for all collections?
		self.checkIfDowngrade = function (userData, clickedRights) {
			var currentAccessRights = userData.accessRights();
			var currentAccessOrdinal = accessLevels[currentAccessRights];
			var newAccessOrdinal = accessLevels[clickedRights];
			if (currentAccessOrdinal > newAccessOrdinal) {
				//downgrade
				$.smkConfirm({
					text: "User " + userData.username() + " may still have " + currentAccessRights +
					" access to records that you own and are members of that collection " +
					", via other collections s/he has access to. Do you want to downgrade access rights to these records "  +
					"in all collections they belong to?",
					accept: 'Yes, downgrade all collections.',
					cancel: 'No, only in this collection.'
				}, function (ee) {
					if (ee) {
						self.callShareAPI(userData, clickedRights, true);
					} else {
						self.callShareAPI(userData, clickedRights, false);
					}
				});
			} else {
				self.callShareAPI(userData, clickedRights, false);
			}
		};

		self.callback = function (callback, par1, par2) {
			callback.call(this, par1, par2);
		};

		self.callShareAPI = function (userData, clickedRights, membersDowngrade) {
			var username = userData.username();
			var collId = self.myCollections()[self.index()].dbId();
			var index = -1;
			var isGroup = false;
			if (userData.category() == "user") {
				index = arrayFirstIndexOf(self.usersToShare(), function (item) {
					return item.username() === username;
				});
			} else if (userData.category() == "group") {
				isGroup = true;
				index = arrayFirstIndexOf(self.userGroupsToShare(), function (item) {
					return item.username() === username;
				});
			}
			$.ajax({
				"url": "/rights/" + collId + "/" + clickedRights + "?username=" + username + "&membersDowngrade=" + membersDowngrade,
				"method": "GET",
				"contentType": "application/json",
				success: function (result) {
					if (index < 0) {
						userData.accessRights(clickedRights);
		   				if (userData.accessRights() == "READ")
		   					userData.accessChecked(false);
		   				else
		   					userData.accessChecked(true);
						if (!isGroup)
							self.usersToShare.push(userData);
						else
							self.userGroupsToShare.push(userData);
					} else {
						if (clickedRights == 'NONE') {
							if (!isGroup) {
								self.usersToShare.splice(index, 1);
							} else {
								self.userGroupsToShare.splice(index, 1);
							}
						} else {
							if (!isGroup) {
								self.usersToShare()[index].accessRights(clickedRights);
							} else {
								self.userGroupsToShare()[index].accessRights(clickedRights);
							}
						}
					}
				}
			});
		};

		self.isPublicToggle = function () {
			if (!self.isPublicToEdit()) {
				$.smkConfirm({
					text: "Users may still be able to read records that you own and are members of that collection " +
					", via other collections s/he has access to. Do you want to make these records private "  +
					"in all collections they belong to?",
					accept: 'Yes, make private in all collections.',
					cancel: 'No, only in this collection.'
				}, function (ee) {
					if (ee) {
						self.editPublicity(self.index(), false, true);
					} else {
						self.editPublicity(self.index(), false, false);
					}
				});
			} else {
				self.editPublicity(self.index(), true, false);
			}

			return true;
		};

		self.editPublicity = function (collIndex, isPublic, membersDowngrade) {
			var collection = self.myCollections()[collIndex];
			$.ajax({
				"url": "/rights/" + collection.dbId() + "?isPublic=" + isPublic + "&membersDowngrade=" + membersDowngrade,
				"method": "GET",
				success: function (result) {
					if (collection.myAccess() == "OWN") {
						self.myCollections()[collIndex].administrative.access.isPublic(self.isPublicToEdit());
					} else {
						self.sharedCollections()[collIndex].administrative.access.isPublic(self.isPublicToEdit());
					}
				},
				error: function (error) {
					//reset
					if (self.isPublicToEdit()) {
						self.isPublicToEdit(false);
					} else {
						self.isPublicToEdit(true);
					}
				}
			});
		};
		
		self.prepareForEditCollection = function (collection, event) {
			var context = ko.contextFor(event.target);
			var collIndex = context.$index();
			self.index(collIndex);
			if (self.showsExhibitions)
				window.location = '#exhibition-edit/' + collection.dbId();
			else {
				if (self.collectionSet() == "my") {
					//self.collectionSet("my");
					self.titleToEdit(self.myCollections()[collIndex].title());
					self.descriptionToEdit(self.myCollections()[collIndex].description());
					self.isPublicToEdit(self.myCollections()[collIndex].administrative.access.isPublic());
				} else {
					//self.collectionSet("shared");
					self.titleToEdit(self.sharedCollections()[collIndex].title());
					self.descriptionToEdit(self.sharedCollections()[collIndex].description());
				}
				$( '.action' ).removeClass( 'active' );
				$( '.action.edit' ).addClass( 'active' );
			}
		};

		//TODO: currently changes fisrt entry of label.default and description.default
		//have to support multilinguality (both in presentation of collection, as well as in edit - drop-down list with languages)
		self.editCollection = function () {
			if(self.validationModel.isValid()) {
				var collIndex = self.index();
				var collId = -1;
				if (self.collectionSet() == "my") {
					collId = self.myCollections()[collIndex].dbId();
				} else if (self.collectionSet() == "shared") {
					collId = self.sharedCollections()[collIndex].dbId();
				}
				if (collId != -1) {
					var jsondata = JSON.stringify({
						descriptiveData : {
							label : {
								default : [self.titleToEdit()]
							},
							description : {
								default : [self.descriptionToEdit()]
							}
						}
					});
					$.ajax({
						"url": "/collection/" + collId,
						"method": "PUT",
						"contentType": "application/json",
						"data": jsondata,
						success: function (result) {
							if (self.collectionSet() == "my") {
								self.updateCollectionData(self.myCollections(), collIndex);
							} else if (self.collectionSet() == "shared") {
								self.updateCollectionData(self.sharedCollections(), collIndex);
							}
							/*var editItem = ko.utils.arrayFirst(currentUser.editables(), function (item) {
								return item.dbId === collId;
							});
							editItem.title = self.titleToEdit();*/
							self.closeSideBar();
						},
						error: function (error) {
							var r = JSON.parse(error.responseText);
							$.smkAlert({ text: r.message, type: 'danger', time: 10 });
						}
					});
				} else {
					$.smkAlert({ text: 'An error occured', type: 'danger', time: 10 });
					self.closeSideBar();
				}
			} else {
				self.validationModel.errors.showAllMessages();
			}
		};

		
		self.closeSideBar = function () {
			self.isPublicToEdit(false); //default value for isPublic
			self.titleToEdit();
			self.descriptionToEdit();
			$('#usernameOrEmail').val("");
			//$('textarea').hide();
			//$('.add').show();
			$('.action').removeClass('active');
		};
		
		self.flushEditableFields = function () {
			self.isPublicToEdit();
			self.titleToEdit();
			self.descriptionToEdit();
		};

		self.updateCollectionData = function (collectionSet, collIndex) {
			collectionSet[collIndex].descriptiveData.label.default = [self.titleToEdit()];
			if (collectionSet[collIndex].descriptiveData.description == undefined) {
				collectionSet[collIndex].descriptiveData.description = { default: [self.descriptionToEdit()] };
			} else {
				collectionSet[collIndex].descriptiveData.description.default = [self.descriptionToEdit()];
			}
			collectionSet[collIndex].title(self.titleToEdit());
			collectionSet[collIndex].description(self.descriptionToEdit());
		};

		self.reloadRecord = function (dbId, recordDataString) {
			var recordData = JSON.parse(recordDataString);
			var recordObservable = ko.mapping.fromJS(recordData, mapping);
			var collData = self.checkCollectionSet(dbId);
			var collIndex = collData.index;
			if (collData.set == "my") {
				self.updateCollectionFirstEntries(self.myCollections(), collIndex, recordObservable);
			}
			if (collData.set == "shared") {
				self.updateCollectionFirstEntries(self.sharedCollections(), collIndex, recordObservable);
			}
		};

		self.updateCollectionFirstEntries = function (collectionSet, collIndex, recordObservable) {
			var newItemCount = collectionSet[collIndex].administrative.entryCount() + 1;
			collectionSet[collIndex].administrative.entryCount(newItemCount);
			if (newItemCount == 1) {//first entry, overwrite empty
				collectionSet[collIndex].media.splice(0, 1, recordObservable.media()[0]);
			} else if (newItemCount <= 5) {
				collectionSet[collIndex].media.push(recordObservable.media()[0]);
			}
		};

		self.reloadCollection = function (data) {
			var newCollection = ko.mapping.fromJS(data, mapping);
			if (data.myAccess == "OWN") {
				ko.mapping.fromJS(data, newCollection);
				self.myCollections.unshift(newCollection);
			} else {
				ko.mapping.fromJS(data, newCollection);
				self.sharedCollections.unshift(newCollection);
			}
		};

		self.changeTab = function (data, event) {
			//var context = ko.contextFor(event.target);
			var clickedElement = (event.currentTarget) ? event.currentTarget : event.srcElement;
			if ($(clickedElement).text() == "My Collections") {
				self.collectionSet("my");
				$("#pickerTitle").text("My Collections");
			} else if ($(clickedElement).text() == "Collections shared with me") {
				self.collectionSet("shared");
				$("#pickerTitle").text("Collections shared with me");
			} else if ($(clickedElement).text() == "My Exhibitions") {
				self.collectionSet("my");
				$("#pickerTitle").text("My Exhibitions");
			} else if ($(clickedElement).text() == "Exhibitions shared with me") {
				self.collectionSet("shared");
				$("#pickerTitle").text("Exhibitions shared with me");
			} else {
				$.smkAlert({ text: 'Not a valid operation!', type: 'danger', time: 10 });
			}
			$('.tab').removeClass('active');
			$(clickedElement).addClass('active');
			self.moreCollectionData(true);
		};

		self.checkCollectionSet = function (dbId) {
			var collIndex = arrayFirstIndexOf(self.myCollections(), function (item) {
				return item.dbId() === dbId;
			});
			var collIndexShared = arrayFirstIndexOf(self.sharedCollections(), function (item) {
				return item.dbId() === dbId;
			});
			if (collIndex >= 0) {
				return {index: collIndex, set: "my"};
			} else if (collIndexShared >= 0) {
				return {index: collIndexShared, set: "shared"};
			} else {
				return {index: -1, set: "none"};
			}
		};

		self.sortByTitle = function () {
			$("#sorting .text").text("Title");
			if (self.collectionSet() == "my")
				self.myCollections.sort(function (left, right) {
					return left.title() < right.title() ? -1 : 1;
				});
			if (self.collectionSet() == "shared")
				self.sharedCollections.sort(function (left, right) {
					return left.title() < right.title() ? -1 : 1;
				});
		}

		self.sortByDate = function () {
			$("#sorting .text").text("Date");
			if (self.collectionSet() == "my")
				self.myCollections.sort(function (left, right) {
					return right.administrative.created() < left.administrative.lastModified() ? -1: 1;
				});
			if (self.collectionSet() == "shared")
				self.sharedCollections.sort(function (left, right) {
					return right.administrative.created() < left.administrative.lastModified() ? -1: 1;
				});
		}

		arrayFirstIndexOf = function (array, predicate) {
			for (var i = 0, j = array.length; i < j; i++) {
				if (predicate.call(undefined, array[i])) {
					return i;
				}
			}
			return -1;
		};

		/*self.getAPIUrl = function() {
			var collIndex = self.index();
			var collDbId = self.myCollections()[collIndex].dbId();
			var title = self.myCollections()[collIndex].title();
			$("#myModal").addClass("modal-info");
			//$("#myModal").css("width", "600px");
			$("#myModal").find("h4").html('API calls for collection "' + title + '"');
			var body = $("#myModal").find("div.modal-body");
			var url   = window.location.href.split("assets")[0];
			var collectionCall = url + "collection/" + collDbId;
			var recordsCall = collectionCall + "/list/start=0&offset=" + count + "&format=all";
			var rightsCall = url + "rights/" + collDbId + "/WRITE?username=withuser";
			body.html('<h5>Get collection data:<\h5> <font size="2"><pre>' + collectionCall + '</pre>' +
					'<br> <h5>Get collection records:<\h5> <font size="2"><pre>' + recordsCall + '</pre></font>' +
					'<br> <h5>Give rights to other user:<\h5> <font size="2"><pre>' + rightsCall + '</pre></font>' +
					'<br> Results depend on logged in user\'s rights.');
			$("#myModal").modal('show');
			$('#myModal').on('hidden.bs.modal', function () {
				$("#myModal").removeClass("modal-info");
			});
		};

	    self.playExhibition = function(dbId) {
	    	window.location.hash = '#exhibitionview/' + dbId;
	    };*/
	}

	return {viewModel: MyCollectionsModel, template: template};
});
