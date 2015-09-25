define(['bootstrap', 'knockout', 'text!./mycollections.html', 'knockout-else','app'], function(bootstrap, ko, template, KnockoutElse, app) {

	function Entry(entryData) {
		var entry = ko.mapping.fromJS(entryData);
		ko.mapping.fromJS(entryData, entry);
		return entry;
	}

	ko.bindingHandlers.autocompleteUsername = {
	      init: function(elem, valueAccessor, allBindingsAccessor, viewModel, context) {
	    	  $(elem).devbridgeAutocomplete({
			   		 minChars: 3,
			   		 lookupLimit: 10,
			   		 serviceUrl: "/user/listNames",
			   		 paramName: "prefix",
			   		 ajaxSettings: {
			   			 dataType: "json"
			   		 },
			   		 transformResult: function(response) {
			   			var result = [];
			   			var myUsername = ko.utils.unwrapObservable(valueAccessor());
			   			//TODO: filter out usersToShare from the dropDown 
			   			var index = arrayFirstIndexOf(response, function(item) {
							   return item.value === myUsername;
						});
			   			if (index > -1)
			   				response.splice(index, 1);
			   			var usersAndParents = [];
			   			$.each(response, function(i, obj) {
			   				if (obj.data.isParent == null == undefined || obj.data.isParent == null || obj.data.isParent === true)
			   					usersAndParents.push(obj);
					    });
				   		return {"suggestions": usersAndParents};
				   	},
				   	orientation: "auto",    
				    onSearchComplete: function(query, suggestions) {
				    	 $(".autocomplete-suggestion").addClass("autocomplete-suggestion-extra");
				    },
					formatResult: function(suggestion, currentValue) {
						var s = '<strong>' + currentValue + '</strong>';
						s    += suggestion.value.substring(currentValue.length);
						s    += ' <span class="label pull-right">' + suggestion.data.category + '</span>';
						return s;
					}
			 });
	      }
	 };

	function getCollectionsSharedWithMe() {
		return $.ajax({
			type        : "GET",
			contentType : "application/json",
			dataType    : "json",
			url         : "/collection/listShared",
			processData : false,
			data        : "access=read&offset=0&count=20"}).done(
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
        //self.editedUsersToShare = ko.mapping.fromJS([], {});
        self.myUsername = ko.observable(app.currentUser.username());
        if (self.showsExhibitions) {
			mapping.title = {
				create: function(options) {
					if (options.data.indexOf('Dummy') === -1) {

						return ko.observable(options.data);
					}
					return ko.observable('Add Title');
				}
			};
			var promise = app.getUserExhibitions();
			$.when(promise).done(function(data) {
				console.log(data);
				ko.mapping.fromJS(data, mapping, self.myCollections);
			});
			self.sharedCollections = ko.mapping.fromJS([], mapping);
		}
		else {
			var promise = app.getUserCollections();
			$.when(promise).done(function(data) {
				//convert rights map to array
				var newData = convertToRightsMap(data);
				ko.mapping.fromJS(newData, mapping, self.myCollections);
			});
			//TODO: Load more sharedCollections with scrolling
			self.sharedCollections = ko.mapping.fromJS([], mapping);
			var promiseShared = getCollectionsSharedWithMe();
			$.when(promiseShared).done(function(data) {
				ko.mapping.fromJS(convertToRightsMap(data), mapping, self.sharedCollections);
			});
		}
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
			var collections = sessionStorage.getItem('EditableCollections');
			if (collections == undefined) {
				collections = localStorage.getItem('EditableCollections');
			}
			if (collections != undefined)
				return JSON.parse(collections);
			else
				return [];
		}

	    //Storage needs to be updated, because collection.js gets user collections from there
		self.saveCollectionsToStorage = function(collections) {
			if (sessionStorage.getItem('EditableCollections') != null)
				sessionStorage.setItem('EditableCollections', JSON.stringify(collections));
			if (localStorage.getItem('EditableCollections') != null)
				localStorage.setItem('EditableCollections', JSON.stringify(collections));
		};


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
					var collections = self.getEditableFromStorage();
					var index = arrayFirstIndexOf(collections, function(item) {
						   return item.dbId === collectionId;
					});
					collections.splice(index, 1);
					self.saveCollectionsToStorage(collections);
				}
			});
		};

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
					//self.usersToShare(result);
					ko.mapping.fromJS(result, self.usersMapping, self.usersToShare);
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
					//TODO: if clicked rights=OWN approval popup
					if (clickedRights === "OWN") {
						$("#myModal").find("h4").html("Are you sure?");
						var body = $("#myModal").find("div.modal-body");
						body.html("Sharing with others users means that they will have the right to delete your collection, " +
								"as well as share it with others.");

						var footer = $("#myModal").find("div.modal-footer");
						if (footer.is(':empty')) {
					        var cancelBtn = $('<button type="button" class="btn btn-default">Cancel</button>').appendTo(footer);
					        cancelBtn.click(function() {
					        	$("#myModal").modal('hide');
					        });
					        var confirmBtn = $('<button type="button" class="btn btn-primary">Confirm</button>').appendTo(footer);
					        confirmBtn.click(function() {
					        	$("#myModal").modal('hide');
								self.shareCollection(result, clickedRights);
					        });
					    }
						$("#myModal").modal('show');
						$('#myModal').on('hidden.bs.modal', function () {
							$("#myModal").find("div.modal-footer").empty();
						});
						$('#myModal').addClass("topOfModal");
					}
					self.shareCollection(result, clickedRights);
					//}
				},
				error      : function(result) {
					$.smkAlert({text:'There is no such username or email', type:'danger', time: 10});
				}
			});
		}

		self.shareCollection = function(userData, clickedRights) {
			var collId = self.myCollections()[self.index()].dbId();
			var username = userData.username;
			var index = arrayFirstIndexOf(self.usersToShare(), function(item) {
				   return item.username() === username;
			});
			$.ajax({
				"url": "/rights/"+collId+"/"+clickedRights+"?username="+username,
				"method": "GET",
				"contentType": "application/json",
				success: function(result) {
					if (index < 0) {
						alert("1");
						userData.accessRights = clickedRights;
						self.usersToShare.push(ko.mapping.fromJS(userData));
					}
					else {
						if (clickedRights == 'NONE')
							self.usersToShare.splice(index, 1);
						else
							self.usersToShare()[index].accessRights(clickedRights);
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
						var editables = self.getEditableFromStorage();
						var editIndex = arrayFirstIndexOf(editables, function(item) {
							   return item.dbId === collId;
						});
						editables[editIndex].title = self.titleToEdit();
						self.saveCollectionsToStorage(editables);
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
			var editables = self.getEditableFromStorage();
			editables.unshift({title: newCollection.title, dbId:newCollection.dbId});
			self.saveCollectionsToStorage(editables);
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
