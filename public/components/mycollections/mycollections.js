define(['bootstrap', 'knockout', 'text!./mycollections.html', 'knockout-else','app'], function(bootstrap, ko, template, KnockoutElse, app) {

	function Entry(entryData) {
		var entry = ko.mapping.fromJS(entryData);
		ko.mapping.fromJS(entryData, entry);
		return entry;
	}

	
	ko.bindingHandlers.autocompleteUsername = {
	      init: function(elem, valueAccessor, allBindingsAccessor, viewModel, context) {
	    	  app.autoCompleteUserName(elem, valueAccessor, allBindingsAccessor, viewModel, context);
	      }
	 };
	
	ko.bindingHandlers.redirectToLogin = {
		init: function(elem, valueAccessor, allBindingsAccessor, viewModel, context) {
			window.location = '#login';		
		}
	};
	
	

	function getCollectionsSharedWithMe(isExhibition) {
		return $.ajax({
			type        : "GET",
			contentType : "application/json",
			dataType    : "json",
			url         : "/collection/listShared",
			processData : false,
			data        : "isExhibition="+isExhibition+"&offset=0&count=20"}).done(
				function(data) {
					return data;
				}).fail(function(request, status, error) {
					console.log(JSON.parse(request.responseText));
		});
	};

	function MyCollectionsModel(params) {
		KnockoutElse.init([spec={}]);
		var self = this;
		//self.route = params.route;
		self.showsExhibitions = params.showsExhibitions;
		self.collections = [];
		self.index = ko.observable(0);
		self.collectionSet = "none";
		var mapping = {
			'dbId': {
				key: function(data) {
		            return ko.utils.unwrapObservable(data.dbId);
		        }
			},
		    'firstEntries': {
		        key: function(data) {
		            return ko.utils.unwrapObservable(data.dbId);
		        }
		    },
		    'rights': {
		    	key: function(data) {
		            return ko.utils.unwrapObservable(data.userId);
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
		//TODO: Load more myCollections with scrolling
		self.myCollections = ko.mapping.fromJS([], mapping);
		self.titleToEdit = ko.observable("");
        self.descriptionToEdit = ko.observable("");
        self.isPublicToEdit = ko.observableArray([]);
        self.apiUrl = ko.observable("");
        self.usersToShare = ko.mapping.fromJS([], {});
        self.userGroupsToShare = ko.mapping.fromJS([], {});
        self.loading=ko.observable(false);
        //self.editedUsersToShare = ko.mapping.fromJS([], {});

    	self.myUsername = ko.observable(app.currentUser.username());
    	self.moreCollectionData=ko.observable(true);
    	self.moreSharedCollectionData=ko.observable(true);
    	self.sharedCollections = ko.mapping.fromJS([], mapping);
    	
		
		
		self.init=function(){
			
        	if (self.showsExhibitions) {
				mapping.title = {
					create: function(options) {
						console.log(options);
						if (options.data.indexOf('Dummy') === -1) {
							return ko.observable(options.data);
						}
						return ko.observable('Add Title');
					}
				};
				var promise = app.getUserCollections(true);
				var promiseShared = getCollectionsSharedWithMe(true);
				$.when(promise,promiseShared).done(function(data,data2) {
					//convert rights map to array
					ko.mapping.fromJS(convertToRightsMap(data[0].collectionsOrExhibitions), mapping, self.myCollections);
					ko.mapping.fromJS(convertToRightsMap(data2[0].collectionsOrExhibitions), mapping, self.sharedCollections);
					self.loading(false);
				});
			}
			else {
				var promise = app.getUserCollections(false);
				self.loading(true);
				var promiseShared = getCollectionsSharedWithMe(false);
				$.when(promise,promiseShared).done(function(data,data2) {
					//convert rights map to array
					ko.mapping.fromJS(convertToRightsMap(data[0].collectionsOrExhibitions), mapping, self.myCollections);
					ko.mapping.fromJS(convertToRightsMap(data2[0].collectionsOrExhibitions), mapping, self.sharedCollections);
					self.loading(false);
				});
				
			}
        
		}
		
		self.checkLogged=function(){
			if(isLogged()==false){
		
			window.location='#login';
			return;
		  }else{self.init();}
		}
		
		self.checkLogged();
		
		convertToRightsMap = function(data) {
			$.each(data, function(j, c) {
				var rightsArray = [];
				$.each(c.rights, function(i, obj) {
					rightsArray.push({"userId": i, "right": obj});
			    });
				data[j].rights = rightsArray;
			});
			return data;
		}

		self.deleteMyCollection = function(collection) {
			var collectionId = collection.dbId();
			var collectionTitle = collection.title();
			self.showDelCollPopup(collectionTitle, collectionId);
		};

		self.createCollection = function() {
			createNewCollection();
		};
		
		self.nextSharedCollections = function() {
			self.moreShared(false);
		};
		
		self.nextCollections = function() {
			self.moreCollections(false);
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
			$("#myModal").find("h4").html("Do you want to delete collection "+collectionTitle+"?");
			var body = $("#myModal").find("div.modal-body");
			body.html("All records in that collection will be deleted.");

			var footer = $("#myModal").find("div.modal-footer");
			if (footer.is(':empty')) {
		        var cancelBtn = $('<button type="button" class="btn btn-default">Cancel</button>').appendTo(footer);
		        cancelBtn.click(function() {
		        	$("#myModal").modal('hide');
		        });
		        var confirmBtn = $('<button type="button" class="btn btn-danger">Delete</button>').appendTo(footer);
		        confirmBtn.click(function() {
		        	deleteCollection(myself.id);
		        	$("#myModal").modal('hide');
		        });
		    }
			$("#myModal").modal('show');
			$('#myModal').on('hidden.bs.modal', function () {
				$("#myModal").find("div.modal-footer").empty();
			})
		};

		self.getEditableFromStorage = function () {
			
			var collections = [];
			if (currentUser!== null){
				collections = currentUser.editables();
			    return collections;}
			else {
				$.smkAlert({text:'An error has occured. You are no longer logged in', type:'danger', permanent: true});
				return [];
			}
			
			
		}

	    

		deleteCollection = function(collectionId) {
			$.ajax({
				"url": "/collection/"+collectionId,
				"method": "DELETE",
				success: function(result){
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
		
		
		
		

		self.moreShared=function(isExhibition){
			
			if (self.loading === true) {
				setTimeout(self.moreShared(isExhibition), 300);
			}
			if (self.loading() === false && self.moreSharedCollectionData()===true) {
				/*if(self.sharedCollections().length<19){
					self.moreSharedCollectionData(false);
				}else{*/
					self.loading(true);
					var offset = self.sharedCollections().length;
					$.ajax({
						"url": "/collection/listShared?offset="+offset+"&count=20&isExhibition="+isExhibition,
						"method": "get",
						"contentType": "application/json",
						"success": function (data) {
							var newData = convertToRightsMap(data.collectionsOrExhibitions);
							var newItems=ko.mapping.fromJS(newData, mapping);
							self.sharedCollections.push.apply(self.sharedCollections, newItems());
							self.loading(false);
							if(data.collectionsOrExhibitions.length<19){
								self.moreSharedCollectionData(false);
							}else{
							  self.moreSharedCollectionData(true);}
							
						},
						"error": function (result) {
							self.loading(false);
						}
					});
				/*}*/
			}
			
			
		}
		

		self.moreCollections=function(isExhibition){
			
			if (self.loading === true) {
				setTimeout(self.moreCollections(isExhibition), 300);
			}
			if (self.loading() === false && self.moreCollectionData()===true) {
				if(self.myCollections().length<19){
					self.moreCollectionData(false);
				}else{
					self.loading(true);
					var offset = self.myCollections().length;
					$.ajax({
						"url": "/collection/list?creator="+app.currentUser.username()+"&offset="+offset+"&count=20&isExhibition="+isExhibition+"&totalHits=false",
						"method": "get",
						"contentType": "application/json",
						"success": function (data) {
							var newData = convertToRightsMap(data.collectionsOrExhibitions);
							var newItems=ko.mapping.fromJS(newData, mapping);
							self.myCollections.push.apply(self.myCollections, newItems());
							self.loading(false);
							if(data.collectionsOrExhibitions.length<19){
								self.moreCollectionData(false);
							}
						},
						"error": function (result) {
							self.loading(false);
						}
					});
				}
			}
			
			
		}
		
		self.openShareCollection = function(collection, event) {
	        var context = ko.contextFor(event.target);
	        var collIndex = context.$index();
			self.index(collIndex);
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
					ko.mapping.fromJS(users, self.usersMapping, self.usersToShare);
					ko.mapping.fromJS(userGroups, self.usersMapping, self.userGroupsToShare);
					app.showPopup("share-collection");
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
		
		self.showInfoPopup = function(title, bodyText, callback) {
			$("#myModal").find("h4").html("Are you sure?");
			var body = $("#myModal").find("div.modal-body");
			body.html(bodyText);

			var footer = $("#myModal").find("div.modal-footer");
			if (footer.is(':empty')) {
		        var cancelBtn = $('<button type="button" class="btn btn-default">Cancel</button>').appendTo(footer);
		        cancelBtn.click(function() {
		        	$("#myModal").modal('hide');
		        });
		        var confirmBtn = $('<button type="button" class="btn btn-primary">Confirm</button>').appendTo(footer);
		        confirmBtn.click(function() {
		        	$("#myModal").modal('hide');
		        	callback();
		        });
		    }
			$("#myModal").modal('show');
			$('#myModal').on('hidden.bs.modal', function () {
				$("#myModal").find("div.modal-footer").empty();
			});
			$('#myModal').addClass("topOfModal");
		}
		
		self.addToSharedWithUsers = function(clickedRights) {
			var collId = self.myCollections()[self.index()].dbId();
			var username = $("#usernameOrEmail").val();
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
					self.showInfoPopup("Are you sure?", "Giving rights to a user group means that all members of the user" +
							" group will acquire these rights. Sharing with others means that they will have the right to delete" +
							" your collection, as well as share it with others.", function() {
						self.callShareAPI(userData, clickedRights);
					});
			else if (userData.category == "group")
					self.showInfoPopup("Are you sure?", "Giving rights to a user group means that all members of the user" +
							" group will acquire these rights.", function() {
						self.callShareAPI(userData, clickedRights);
					});
			else if (clickedRights === "OWN") 
				self.showInfoPopup("Are you sure?", "Sharing with others means that they will have the right to delete your collection, " +
						"as well as share it with others.", function() {
					self.callShareAPI(userData, clickedRights);
				});
			else 
				self.callShareAPI(userData, clickedRights);
		}
		
		self.callShareAPI = function(userData, clickedRights) {
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
				"url": "/rights/"+collId+"/"+clickedRights+"?username="+username,
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
		
		self.openEditCollectionPopup = function(collection, event) {
	        var context = ko.contextFor(event.target);
	        var collIndex = context.$index();
			self.index(collIndex);
			if (collection.access() == "OWN") {
				self.collectionSet = "my";
				self.titleToEdit(self.myCollections()[collIndex].title());
		        self.descriptionToEdit(self.myCollections()[collIndex].description());
		        self.isPublicToEdit(self.myCollections()[collIndex].isPublic());
			}
			else {
				self.collectionSet = "shared";
				self.titleToEdit(self.sharedCollections()[collIndex].title());
		        self.descriptionToEdit(self.sharedCollections()[collIndex].description());
		        self.isPublicToEdit(self.sharedCollections()[collIndex].isPublic());
			}
			app.showPopup("edit-collection");
		}

		self.closePopup = function() {
			app.closePopup();
		}



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
					"method": "POST",
					"contentType": "application/json",
					"data": JSON.stringify({title: self.titleToEdit(),
							description: self.descriptionToEdit(),
							isPublic: self.isPublicToEdit()
						}),
					success: function(result){
						if (self.collectionSet == "my") {
							self.updateCollectionData(self.myCollections(), collIndex);
						}
						else if (self.collectionSet == "shared") {
							self.updateCollectionData(self.sharedCollections(), collIndex);
						}
						
						var editItem= ko.utils.arrayFirst(currentUser.editables(), function(item) {
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
			self.closePopup();
		};

		self.updateCollectionData = function(collectionSet, collIndex) {
			collectionSet[collIndex].title(self.titleToEdit());
			collectionSet[collIndex].description(self.descriptionToEdit());
			collectionSet[collIndex].isPublic(self.isPublicToEdit());
		}

		self.privateToggle=function(e,arg){
		    if (self.isPublicToEdit())
		    	self.isPublicToEdit(false);
		    else
		    	self.isPublicToEdit(true);
		}


		self.reloadRecord = function(dbId, recordDataString) {
			var recordData = JSON.parse(recordDataString);
			var recordObservable = ko.mapping.fromJS(recordData);
			ko.mapping.fromJS(recordData, recordObservable);
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
			var newItemCount = collectionSet[collIndex].itemCount() + 1;
			collectionSet[collIndex].itemCount(newItemCount);
			collectionSet[collIndex].firstEntries.push(recordObservable);
		}

		self.reloadCollection = function(data) {
			var newCollection = ko.mapping.fromJS(data);
			if (data.access == "OWN") {
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
			}
			else{
				$("#mycollections").show();
				   $("#sharedtab").hide();
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
			var recordsCall = collectionCall + "/list/start=0&offset=20&format=all";
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

	}

	return {viewModel: MyCollectionsModel, template: template};
});