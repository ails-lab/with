define(['bootstrap', 'knockout', 'text!./mycollections.html', 'knockout-else','app'], function(bootstrap, ko, template, KnockoutElse, app) {

	function Entry(entryData) {
		var entry = ko.mapping.fromJS(entryData);
		ko.mapping.fromJS(entryData, entry);
		return entry;
	}

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
		    }
		};
		self.myCollections = ko.mapping.fromJS([], mapping);
		//self.myCollections = ko.observableArray([]);
		self.titleToEdit = ko.observable("");
        self.descriptionToEdit = ko.observable("");
        self.isPublicToEdit = ko.observable(true);
        self.apiUrl = ko.observable("");
        var promise = app.getUserCollections();
		$.when(promise).done(function(data) {
			ko.mapping.fromJS(data, mapping, self.myCollections);
		});
		/*if (sessionStorage.getItem('UserCollections') !== null)
			  self.collections = JSON.parse(sessionStorage.getItem("UserCollections"));
		if (localStorage.getItem('UserCollections') !== null)
		    self.collections = JSON.parse(localStorage.getItem("UserCollections"));*/
		self.sharedCollections = ko.mapping.fromJS([], mapping);
		var promiseShared = getCollectionsSharedWithMe();
		$.when(promiseShared).done(function(data) {
			ko.mapping.fromJS(data, mapping, self.sharedCollections);
		});
		
		self.deleteMyCollection = function(collection) {
			var collectionId = collection.dbId();
			var collectionTitle = collection.title();
			showDelCollPopup(collectionTitle, collectionId);
		};

		self.createCollection = function() {
			createNewCollection();
		};
		
	    
		
		showDelCollPopup = function(collectionTitle, collectionId) {
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
			/*var collectionsUnwrapped = ko.mapping.toJS(self.myCollections);
			if (sessionStorage.getItem('UserCollections') !== null) {
				sessionStorage.setItem('UserCollections', JSON.stringify(collectionsUnwrapped));
			}
			else if (localStorage.getItem('UserCollections') !== null) {
				localStorage.setItem('UserCollections', JSON.stringify(collectionsUnwrapped));

			}*/
		};
		
		self.reoladRecordDeletion = function(collId, itemId) {
			
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

		self.closeEditPopup = function() {
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
							/*self.myCollections()[collIndex].title(self.titleToEdit());
							self.myCollections()[collIndex].description(self.descriptionToEdit());
							self.myCollections()[collIndex].isPublic(self.isPublicToEdit());*/
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
			self.closeEditPopup();
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
			/*$.ajax({
				"url": "/collection/"+dbId,
				"method": "GET",
				success: function(data){
					//TODO: Confirm that 1) myCollections array is updated (recursively) 2) as well as firstEntries
					//self.myCollections()[index](self.load());
					//alert(JSON.stringify(self.editableCollection()));
					ko.mapping.fromJS(data, self.myCollections);
					var collIndex = arrayFirstIndexOf(viewModel.items(), function(item) {
						   return item.dbId === dbId;
					}));
					self.myCollections()[collIndex].remove((data);
				}
			});*/
			var recordData = JSON.parse(recordDataString);
			var recordObservable = ko.mapping.fromJS(recordData);
			ko.mapping.fromJS(recordData, recordObservable);
			var collData = self.checkCollectionSet(dbId);
			var collIndex = collData.index;
			if (collData.set == "my") {
				/*var newItemCount = self.myCollections()[collIndex].itemCount() + 1;
				self.myCollections()[collIndex].itemCount(newItemCount);
				self.myCollections()[collIndex].firstEntries.push(recordObservable);*/
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

	    arrayFirstIndexOf=function(array, predicate, predicateOwner) {
		    for (var i = 0, j = array.length; i < j; i++) {
		        if (predicate.call(predicateOwner, array[i])) {
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
			body.html('<h5>Get collection data:<\h5> <font size="2"><pre>' + collectionCall + '</pre>' +
					'<br> <h5>Get collection records:<\h5> <font size="2"><pre>' + recordsCall +'</pre></font>');
			$("#myModal").modal('show');
			$('#myModal').on('hidden.bs.modal', function () {
				$("#myModal").removeClass("modal-info");
			})
	    }

	    self.removeClass = function() {
	    	$("#myModal").removeClass("modal-info");
	    }
	    

	}

	return {viewModel: MyCollectionsModel, template: template};
});
