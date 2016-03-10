define(['bootstrap', 'knockout', 'text!./_mycollections.html', 'knockout-else','app'], function(bootstrap, ko, template, KnockoutElse, app) {

	count = 5;
	accessLevels = {
		    READ : 0,
		    WRITE : 1,
		    OWN : 2
	}

	ko.bindingHandlers.autocompleteUsername = {
	      init: function(elem, valueAccessor, allBindingsAccessor, viewModel, context) {
	    	  app.autoCompleteUserName(elem, valueAccessor, allBindingsAccessor, viewModel, context, function(suggestion) {
					viewModel.addToSharedWithUsers("READ", suggestion);
				});
	      }
	 };

	ko.bindingHandlers.redirectToLogin = {
		init: function(elem, valueAccessor, allBindingsAccessor, viewModel, context) {
			window.location = '#login';
		}
	};
	
	/*ko.bindingHandlers.enterOnUsername = {
		 init: function(elem, valueAccessor, allBindingsAccessor, viewModel, context) {
	    	  viewModel.addToSharedWithUsers('READ');
	      }	
	}*/

	function getCollectionsSharedWithMe(isExhibition, offset, count) {
		return $.ajax({
			type        : "GET",
			contentType : "application/json",
			dataType    : "json",
			url         : "/collection/listShared",
			processData : false,
			data        : "isExhibition="+isExhibition+"&offset="+offset+"&count="+count}).done(
				function(data) {
					return data;
				}).fail(function(request, status, error) {
					console.log(JSON.parse(request.responseText));
		});
	};

	function MyCollectionsModel(params) {
		KnockoutElse.init([spec={}]);
		//$("div[role='main']").toggleClass( "homepage", false );
		var self = this;
		self.route = params.route;
		self.showsExhibitions = params.showsExhibitions;
		//self.collections = [];
		self.index = ko.observable(0);
		self.collectionSet = "none";
		var mapping = {
			create: function (options) {
		        //customize at the root level: add title and description observables, based on multiliteral
				//TODO: support multilinguality, have to be observable arrays of type [{lang: default, values: []}, ...]
		        var innerModel = ko.mapping.fromJS(options.data);
		        innerModel.title = ko.observable(self.multiLiteral(options.data.descriptiveData.label));
		        var dbDescription = options.data.descriptiveData.description;
		        if (dbDescription == undefined || dbDescription == null)
		        	innerModel.description = ko.observable("");
		        else
		        	innerModel.description = ko.observable(self.multiLiteral(dbDescription));
		        $.each(innerModel.media(), function(index, value){
		        	//withUrl = value.Thumbnail.withUrl(); still not fully working
		        	withUrl = value.Thumbnail.url();
		        	if (withUrl.indexOf("/media") == 0)
		        		innerModel.media()[index].thumbnailUrl = window.location.origin + withUrl;
		        	else
		        		innerModel.media()[index].thumbnailUrl = withUrl;
				});
		        return innerModel;
		    },
			'dbId': {
				key: function(data) {
		            return ko.utils.unwrapObservable(data.dbId);
		        }
			}
		};
		var usersMapping = {
				'dbId': {
					key: function(data) {
			            return ko.utils.unwrapObservable(data.username);
			        }
				}
		};
		self.myCollections = ko.mapping.fromJS([], mapping);
		self.titleToEdit = ko.observable("");
        self.descriptionToEdit = ko.observable("");
        self.isPublicToEdit = ko.observable(false);
        self.apiUrl = ko.observable("");
        self.usersToShare = ko.mapping.fromJS([], {});
        self.userGroupsToShare = ko.mapping.fromJS([], {});
        self.loading=ko.observable(false);
        //self.editedUsersToShare = ko.mapping.fromJS([], {});

    	self.myUsername = ko.observable(app.currentUser.username());
    	self.moreCollectionData=ko.observable(true);
    	self.moreSharedCollectionData=ko.observable(true);
    	self.sharedCollections = ko.mapping.fromJS([], mapping);

    	/*self.multiLiteral = function (multiLiteral) {
    	        return ko.computed({
    	            read: function () {
    	            	var s = app.findByLang(multiLiteral);
    	                return app.findByLang(multiLiteral);
    	            }
    	        }, this);
    	    }.bind(self.myCollections);*/

    	self.multiLiteral = function (multiLiteral) {
                return app.findByLang(multiLiteral);
        };

		self.init=function(){
        	if (self.showsExhibitions) {
				mapping.label = {
					create: function(options) {
						if (options.data.default.indexOf('Dummy') === -1) {
							return ko.observable(options.data.default);
						}
						else {
							return ko.observable('Add Title');
						}
					}
				};
				var promise = app.getUserCollections(true, 0, count);
				self.loading(true);
				var promiseShared = getCollectionsSharedWithMe(true, 0, count);
				$.when(promise,promiseShared).done(function(data,data2) {
					ko.mapping.fromJS(data[0].collectionsOrExhibitions, mapping, self.myCollections);
					ko.mapping.fromJS(data2[0].collectionsOrExhibitions, mapping, self.sharedCollections);
					self.loading(false);
		        	WITHApp.tabAction();
				});
			}
			else {
				var promise = app.getUserCollections(false, 0, count);
				self.loading(true);
				var promiseShared = getCollectionsSharedWithMe(false, 0, count);
				$.when(promise,promiseShared).done(function(data,data2) {
					ko.mapping.fromJS(data[0].collectionsOrExhibitions, mapping, self.myCollections);
					ko.mapping.fromJS(data2[0].collectionsOrExhibitions, mapping, self.sharedCollections);
					self.loading(false);
		        	WITHApp.tabAction();
				});
			}
		}
		
		self.changeList = function() {
			WITHApp.changeList();
		}
		
		self.checkLogged = function() {
			if (isLogged()==false) {
				window.location='#login';
				return;
			}	else {
				self.init();
			}
		}

		self.checkLogged();

		self.deleteMyCollection = function(collection) {
			var collectionId = collection.dbId();
			var collectionTitle = collection.title();
			self.showDelCollPopup(collectionTitle, collectionId);
		};

		self.createCollection = function() {
			alert(self.isPublicToEdit());
			var jsondata = JSON.stringify({
				administrative: { access: {
			        isPublic: self.isPublicToEdit()},
			        collectionType: "SimpleCollection"},
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
				"url": "/collection",
				"method": "post",
				"contentType": "application/json",
				"data": jsondata,
				"success": function (data) {
					self.reloadCollection(data);
					self.closeSideBar();
				},
				"error": function (result) {
					$.smkAlert({text:'An error occured', type:'danger', time: 10});
					self.closeSideBar();
				}
			});
		};

		self.nextSharedCollections = function() {
			self.moreShared(false);
		};

		self.createExhibition = function() {
			window.location = '#exhibition-edit';
		};

		self.loadCollectionOrExhibition = function(collection) {
			if (self.showsExhibitions) {
				window.location = '#exhibition-edit/'+ collection.dbId();
			}
			else {
				window.location = 'index.html#collectionview/' + collection.dbId();
			}
		};

		self.showDelCollPopup = function(collectionTitle, collectionId) {
			var myself = this;
			myself.id = collectionId;
			$.smkConfirm({
				text: "All records in " + collectionTitle + " will be deleted. Are you sure?",
				accept: 'Delete',
				cancel: 'Cancel'
			}, function (ee) {
				if (ee)
					deleteCollection(myself.id);
			});
		};

		deleteCollection = function(collectionId) {
			$.ajax({
				"url": "/collection/"+collectionId,
				"method": "DELETE",
				success: function(result){
					$.smkAlert({text:'Collection removed', type:'success'});
					self.myCollections.remove(function (item) {
                        return item.dbId() == collectionId;
                    });
					self.sharedCollections.remove(function (item) {
                        return item.dbId() == collectionId;
                    });
					var theitem= ko.utils.arrayFirst(currentUser.editables(), function(item) {
						return item.dbId===collectionId;

			        });
					currentUser.editables.remove(theitem);
					//currentUser.editables.splice(index,1);
				}
			});
		};

		/*self.moreShared = function(isExhibition){
			self.more(isExhibition, getCollectionsSharedWithMe, false);
		}*/

		self.moreCollections = function(isExhibition){
			if (self.collectionSet == "my")
				self.more(isExhibition, app.getUserCollections, true);
			else if (self.collectionSet == "shared")
				self.more(isExhibition, getCollectionsSharedWithMe, false);
		}

		self.more = function(isExhibition, funcToExecute, my) {
			if (self.loading === true) {
				setTimeout(self.moreCollections(isExhibition), 300);
			}
			if (self.loading() === false && self.moreCollectionData()===true) {
				self.loading(true);
				var offset = self.collectionSet == "my"? self.myCollections().length: self.sharedCollections().length;
				var promise = funcToExecute(isExhibition, offset, count);
				$.when(promise).done(function(data) {
					var newItems=ko.mapping.fromJS(data.collectionsOrExhibitions, mapping);
					if (my)
						self.myCollections.push.apply(self.myCollections, newItems());
					else {
						self.sharedCollections.push.apply(self.sharedCollections, newItems());
					}
					self.loading(false);
					if (data.collectionsOrExhibitions.length<count-1){
						self.moreCollectionData(false);
					} else {
					  self.moreCollectionData(true);
					}
				}).fail(function(result) {self.loading(false);});
			}
		}

		self.openShareCollection = function(collection, event) {
	        var context = ko.contextFor(event.target);
	        var collIndex = context.$index();
			self.index(collIndex);
			self.isPublicToEdit(collection.administrative.access.isPublic());
			//$(".user-selection").devbridgeAutocomplete("hide");
			$.ajax({
				method     : "GET",
				contentType    : "application/json",
				url         : "/collection/"+collection.dbId()+"/listUsers",
				success		: function(result) {
					var index = arrayFirstIndexOf(result, function(item) {
						   return item.username === self.myUsername();
					});
		   			if (index > -1)
		   				result.splice(index, 1);
		   			var users = [];
		   			var userGroups = [];
		   			$.each(result, function(i, item) {
		   				if (item.category == "user")
		   					users.push(item);
		   				else if(item.category == "group")
		   					userGroups.push(item);
					});
		   			alert(JSON.stringify(users));
					ko.mapping.fromJS(users, self.usersMapping, self.usersToShare);
					ko.mapping.fromJS(userGroups, self.usersMapping, self.userGroupsToShare);
					//app.showPopup("share-collection");
				}
			});
		}

		self.showRightsIcons = function(userData) {
			var accessRights = userData.accessRights();
			var userId = userData.userId();
			var collId = self.myCollections()[self.index()].dbId();
			$("#rightsIcons_"+userId).show();
			$("#image_"+userId).css("opacity", "0.5");
		}


		self.hideRightsIcons = function(userId) {
			$("#rightsIcons_"+userId).hide();
			$("#image_"+userId).css("opacity", "1");
		}

		self.changeRights = function(clickedRights) {
			self.shareCollection(ko.toJS(this), clickedRights);
		}

		self.addToSharedWithUsers = function(clickedRights, username) {
			var collId = self.myCollections()[self.index()].dbId();
			//var username = $("#usernameOrEmail").val();
			$.ajax({
				method      : "GET",
				contentType    : "application/json",
				url         : "/user/findByUserOrGroupNameOrEmail",
				data: "userOrGroupNameOrEmail="+username+"&collectionId="+collId,
				success		: function(result) {
					/*var index = arrayFirstIndexOf(self.usersToShare(), function(item) {
						   return item.name() === username;
					});
					if (index < 0) {*/
					self.shareCollection(result, clickedRights);
					//}
				},
				error      : function(result) {
					$.smkAlert({text:'There is no such username or email', type:'danger', time: 10});
				}
			});
		}


		self.shareCollection = function(userData, clickedRights) {
			if (userData.category == "group" && clickedRights === "OWN")
					app.showInfoPopup("Are you sure?", "Giving rights to a user group means that all members of the user" +
							" group will acquire these rights. Giving OWN rights to others means that they will have the right to delete" +
							" your collection, as well as to share it with others.", function() {
						self.callShareAPI(userData, clickedRights);
					});
			else if (userData.category == "group")
					app.showInfoPopup("Are you sure?", "Giving rights to a user group means that all members of the user" +
							" group will acquire these rights.", function() {
						self.checkIfDowngrade(userData, clickedRights);
					});
			else if (clickedRights === "OWN")
				app.showInfoPopup("Are you sure?", "Giving OWN rights to others means that they will have the right to delete your collection, " +
						"as well as to share it with others.", function() {
					self.callShareAPI(userData, clickedRights);
				});
			else
				self.checkIfDowngrade(userData, clickedRights);
		}


		//TODO: find all members of the collections that I OWN. Find all collections these records belong to.
		// Find if the user userData.username has access to these collections that are greater than clicked rights -
		//if yes, present the following message:
		//userData.username will still have currentAccess to the records [...] via collections [...] (to which userData.username has access).
		//Do you want to downgrade access rights userData.username for all collections?
		self.checkIfDowngrade = function(userData, clickedRights) {
			var currentAccessRights = userData.accessRights;
			var currentAccessOrdinal = accessLevels[currentAccessRights];
			var newAccessOrdinal = accessLevels[clickedRights];
			if (currentAccessOrdinal > newAccessOrdinal)
				//downgrade
				app.showInfoPopupTwoOptions("Downgrade records in all collections?",
						"User " + userData.username + " may still have " + currentAccessRights + " access to records that you own and are members of that collection " +
						", via other collections s/he has access to. Do you want to downgrade access rights to these records "  +
						"in all collections they belong to?", function(response) {
					self.callShareAPI(userData, clickedRights, response);
				});
			else
				self.callShareAPI(userData, clickedRights, false);
		}

		self.callback = function(callback, par1, par2) {
			callback.call(this, par1, par2);
		}

		self.callShareAPI = function(userData, clickedRights, membersDowngrade) {
			var username = userData.username;
			var collId = self.myCollections()[self.index()].dbId();
			var index = -1;
			var isGroup = false;
			if (userData.category == "user")
			  index = arrayFirstIndexOf(self.usersToShare(), function(item) {
				   return item.username() === username;
			});
			else if (userData.category == "group") {
				isGroup = true;
				index = arrayFirstIndexOf(self.userGroupsToShare(), function(item) {
					   return item.username() === username;
				});
			}
			$.ajax({
				"url": "/rights/"+collId+"/"+clickedRights+"?username="+username+"&membersDowngrade="+membersDowngrade,
				"method": "GET",
				"contentType": "application/json",
				success: function(result) {
					if (index < 0) {
						userData.accessRights = clickedRights;
						if (!isGroup)
							self.usersToShare.push(ko.mapping.fromJS(userData));
						else
							self.userGroupsToShare.push(ko.mapping.fromJS(userData));
					}
					else {
						if (clickedRights == 'NONE') {
							if (!isGroup)
								self.usersToShare.splice(index, 1);
							else
								self.userGroupsToShare.splice(index, 1);
						}
						else {
							if (!isGroup)
								self.usersToShare()[index].accessRights(clickedRights);
							else
								self.userGroupsToShare()[index].accessRights(clickedRights);
						}
					}
				}
			});
		}
		
		self.isPublicToggle = function() {
		    if (!self.isPublicToEdit()) {
		    	/*app.showInfoPopupTwoOptions("Make records private in all collections?",
						"Users may still be able to read records that you own and are members of that collection " +
						", via other collections s/he has access to. Do you want to make these records private "  +
						"in all collections they belong to?", function(response) {
		    		self.editPublicity(self.index(), false, response);
				});*/
		    	$.smkConfirm({
					text: "Users may still be able to read records that you own and are members of that collection " +
					", via other collections s/he has access to. Do you want to make these records private "  +
					"in all collections they belong to?",
					accept: 'Yes, make private in all collections.',
					cancel: 'No, only in this collection.'
				}, function (ee) {
					if (ee)
						self.editPublicity(self.index(), false, true);
					else
						self.editPublicity(self.index(), false, false);
				});
		    }
		    else
		    	self.editPublicity(self.index(), true, false);
		    return true;
		}

		self.editPublicity = function(collIndex, isPublic, membersDowngrade) {
			var collection = self.myCollections()[collIndex];
			$.ajax({
				"url": "/rights/"+collection.dbId()+"?isPublic="+isPublic+"&membersDowngrade="+membersDowngrade,
				"method": "GET",
				success: function(result) {
					if (collection.myAccess() == "OWN") {
						self.myCollections()[collIndex].administrative.access.isPublic(self.isPublicToEdit());
					}
					else {
						self.sharedCollections()[collIndex].administrative.access.isPublic(self.isPublicToEdit());
					}
				},
				error: function(error) {
					//reset
					if (self.isPublicToEdit()) {
				    	self.isPublicToEdit(false);
				    }
				    else {
				    	self.isPublicToEdit(true);
				    }
				}
			});
		}

		self.prepareForEditCollection = function(collection, event) {
	        var context = ko.contextFor(event.target);
	        var collIndex = context.$index();
			self.index(collIndex);
			if (collection.myAccess() == "OWN") {
				self.collectionSet = "my";
				self.titleToEdit(self.myCollections()[collIndex].title());
		        self.descriptionToEdit(self.myCollections()[collIndex].description());
		        //self.isPublicToEdit(self.myCollections()[collIndex].administrative.access.isPublic());
			}
			else {
				self.collectionSet = "shared";
				self.titleToEdit(self.sharedCollections()[collIndex].title());
		        self.descriptionToEdit(self.sharedCollections()[collIndex].description());
		        //self.isPublicToEdit(self.sharedCollections()[collIndex].administrative.access.isPublic());
			}
			//app.showPopup("edit-collection");
		}

		self.closePopup = function() {
			app.closePopup();
		}


		//TODO: currently changes fisrt entry of label.default and description.default
		//have to support multilinguality (both in presentation of collection, as well as in edit - drop-down list with languages)
		self.editCollection = function () {
			var collIndex = self.index();
			var collId=-1;
			if (self.collectionSet == "my")
				collId = self.myCollections()[collIndex].dbId();
			else if (self.collectionSet == "shared")
				collId = self.sharedCollections()[collIndex].dbId();
			if (collId != -1) {
				$.ajax({
					"url": "/collection/"+collId,
					"method": "PUT",
					"contentType": "application/json",
					"data": JSON.stringify(
						{descriptiveData: {
							label: {default: [self.titleToEdit()]},
							description: {default: [self.descriptionToEdit()]},
						}
					}),
					success: function(result){
						if (self.collectionSet == "my") {
							self.updateCollectionData(self.myCollections(), collIndex);
						}
						else if (self.collectionSet == "shared") {
							self.updateCollectionData(self.sharedCollections(), collIndex);
						}

						var editItem = ko.utils.arrayFirst(currentUser.editables(), function(item) {
							return item.dbId===collId;

				        });
						editItem.title=self.titleToEdit();
					},
					error: function(error) {
						var r = JSON.parse(error.responseText);
						$("#myModal").find("h4").html("An error occured");
						$("#myModal").find("div.modal-body").html(r.message);
						$("#myModal").modal('show');
					}
				});
			}
			else {
				$("#myModal").find("h4").html("An error occured");
				$("#myModal").find("div.modal-body").html("The collection cannot be edited");
				$("#myModal").modal('show');
			}
			self.closeSideBar();
			//WITHApp.tabAction();
		};
		

		self.closeSideBar = function () {
			self.isPublicToEdit();
			self.titleToEdit();
			self.descriptionToEdit();
			$('textarea').hide();
			//$('.add').show();
			$('.action').removeClass('active');
		};

		self.updateCollectionData = function(collectionSet, collIndex) {
			collectionSet[collIndex].descriptiveData.label.default = [self.titleToEdit()];
			if (collectionSet[collIndex].descriptiveData.description == undefined)
				collectionSet[collIndex].descriptiveData.description = {default: [self.descriptionToEdit()]}
			else
				collectionSet[collIndex].descriptiveData.description.default = [self.descriptionToEdit()];
			collectionSet[collIndex].title(self.titleToEdit());
			collectionSet[collIndex].description(self.descriptionToEdit());
		}

		self.reloadRecord = function(dbId, recordDataString) {
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

		self.updateCollectionFirstEntries = function(collectionSet, collIndex, recordObservable) {
			var newItemCount = collectionSet[collIndex].administrative.entryCount() + 1;
			collectionSet[collIndex].administrative.entryCount(newItemCount);
			if (newItemCount == 1) {//first entry, overwrite empty
				collectionSet[collIndex].media.splice(0, 1, recordObservable.media()[0]);
			}
			else if (newItemCount <= 5) {
				collectionSet[collIndex].media.push(recordObservable.media()[0]);
			}
		}

		self.reloadCollection = function(data) {
			var newCollection = ko.mapping.fromJS(data, mapping);
			if (data.myAccess == "OWN") {
				ko.mapping.fromJS(data, newCollection);
				self.myCollections.unshift(newCollection);
			}
			else {
				ko.mapping.fromJS(data, newCollection);
				self.sharedCollections.unshift(newCollection);
			}
			//currentUser.editables.unshift({title: newCollection.title, dbId:newCollection.dbId});

		}


		self.changeTab=function(what,data,event){
			$(event.target).parents("span.withmain").children("span.collectiontab").toggleClass('active');
			if(what=="shared"){
			   $("#mycollections").hide();
			   $("#sharedtab").show();
			   self.collectionSet = "shared";
			}
			else{
				$("#mycollections").show();
				   $("#sharedtab").hide();
				   self.collectionSet = "my";
			}

		}

		self.checkCollectionSet = function(dbId) {
			var collIndex = arrayFirstIndexOf(self.myCollections(), function(item) {
				   return item.dbId() === dbId;
			});
			var collIndexShared = arrayFirstIndexOf(self.sharedCollections(), function(item) {
				   return item.dbId() === dbId;
			});
			if (collIndex >= 0)
				return {index:collIndex, set:"my"};
			else if (collIndexShared >= 0)
				return {index:collIndexShared, set:"shared"}
			else
				return {index:-1, set:"none"};
		}

	    arrayFirstIndexOf = function(array, predicate) {
		    for (var i = 0, j = array.length; i < j; i++) {
		        if (predicate.call(undefined, array[i])) {
		            return i;
		        }
		    }
		    return -1;
		}

	    self.getAPIUrl = function() {
			var collIndex = self.index();
			var collDbId = self.myCollections()[collIndex].dbId();
			var title = self.myCollections()[collIndex].title();
			$("#myModal").addClass("modal-info");
			//$("#myModal").css("width", "600px");
	    	$("#myModal").find("h4").html('API calls for collection "' + title + '"');
			var body = $("#myModal").find("div.modal-body");
			var url   = window.location.href.split("assets")[0];
			var collectionCall = url + "collection/" + collDbId;
			var recordsCall = collectionCall + "/list/start=0&offset="+count+"&format=all";
			var rightsCall = url + "rights/" + collDbId + "/WRITE?username=withuser";
			body.html('<h5>Get collection data:<\h5> <font size="2"><pre>' + collectionCall + '</pre>' +
					'<br> <h5>Get collection records:<\h5> <font size="2"><pre>' + recordsCall +'</pre></font>' +
					'<br> <h5>Give rights to other user:<\h5> <font size="2"><pre>' + rightsCall +'</pre></font>' +
					'<br> Results depend on logged in user\'s rights.');
			$("#myModal").modal('show');
			$('#myModal').on('hidden.bs.modal', function () {
				$("#myModal").removeClass("modal-info");
			})
	    }
	    $('#bottomBar').fadeIn(500);

	    self.playExhibition = function(dbId) {
	    	window.location.hash = '#exhibitionview/' + dbId;
	    };

	}

	return {viewModel: MyCollectionsModel, template: template};
});