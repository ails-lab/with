define(['bootstrap', 'knockout', 'text!./mycollections.html', 'knockout-else','app'], function(bootstrap, ko, template, KnockoutElse, app) {

	function Entry(entryData) {
		var entry = ko.mapping.fromJS(entryData);
		ko.mapping.fromJS(entryData, entry);
		return entry;
	}

	function MyCollectionsModel(params) {
		KnockoutElse.init([spec={}]);
		var self = this;
		//self.route = params.route;
		self.collections = [];
		self.index = ko.observable(0);
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
		//self.myCollections = ko.mapping.fromJS([], mapping);
		self.myCollections = ko.observableArray([]);
		var promise = app.getUserCollections();
		self.titleToEdit = ko.observable("");
        self.descriptionToEdit = ko.observable("");
        self.isPublicToEdit = ko.observable(true);
        self.index = ko.observable(0);
        self.apiUrl = ko.observable("");
		$.when(promise).done(function() {
			if (sessionStorage.getItem('UserCollections') !== null)
			  self.collections = JSON.parse(sessionStorage.getItem("UserCollections"));
			if (localStorage.getItem('UserCollections') !== null)
			  self.collections = JSON.parse(localStorage.getItem("UserCollections"));
			/*self.myCollections(ko.utils.arrayMap(collections, function(collectionData) {
			    return new MyCollection(collectionData);
			}));*/
			ko.mapping.fromJS(self.collections, mapping, self.myCollections);
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
				$("#myModal").find("div.modal-footer").html();
			})
		};

	    //Storage needs to be updated, because collection.js gets user collections from there
		saveCollectionsToStorage = function(collections) {
			var collectionsUnwrapped = ko.mapping.toJS(collections);
			if (sessionStorage.getItem('UserCollections') !== null) {
				sessionStorage.setItem('UserCollections', JSON.stringify(collectionsUnwrapped));
			}
			else if (localStorage.getItem('UserCollections') !== null) {
				localStorage.setItem('UserCollections', JSON.stringify(collectionsUnwrapped));

			}
		};

		deleteCollection = function(collectionId) {
			$.ajax({
				"url": "/collection/"+collectionId,
				"method": "DELETE",
				success: function(result){
					self.myCollections.remove(function (item) {
                        return item.dbId() == collectionId;
                    });
					saveCollectionsToStorage(self.myCollections);
				}
			});
		};


		self.openEditCollectionPopup = function(collection, event) {
	        var context = ko.contextFor(event.target);
	        var collIndex = context.$index();
			self.index(collIndex);
	        self.titleToEdit(self.myCollections()[collIndex].title());
	        self.descriptionToEdit(self.myCollections()[collIndex].description());
	        self.isPublicToEdit(self.myCollections()[collIndex].isPublic());
			app.showPopup("edit-collection");
		}

		self.closeEditPopup = function() {
			app.closePopup();
		}

		self.editCollection = function () {
			var collIndex = self.index();
			$.ajax({
				"url": "/collection/"+self.myCollections()[collIndex].dbId(),
				"method": "POST",
				"contentType": "application/json",
				"data": JSON.stringify({title: self.titleToEdit(),
						description: self.descriptionToEdit(),
						isPublic: self.isPublicToEdit()
					}),
				success: function(result){
					self.myCollections()[collIndex].title(self.titleToEdit());
					self.myCollections()[collIndex].description(self.descriptionToEdit());
					self.myCollections()[collIndex].isPublic(self.isPublicToEdit());
					saveCollectionsToStorage(self.myCollections);
				},
				error: function(error) {
					var r = JSON.parse(error.responseText);
					$("#myModal").find("h4").html("An error occured");
					$("#myModal").find("div.modal-body").html(r.message);
					$("#myModal").modal('show');
				}
			});
			self.closeEditPopup();
		};

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
			var collIndex = arrayFirstIndexOf(self.myCollections(), function(item) {
				   return item.dbId() === dbId;
			});
			var recordData = JSON.parse(recordDataString);
			var recordObservable = ko.mapping.fromJS(recordData);
			ko.mapping.fromJS(recordData, recordObservable);
			var newItemCount = self.myCollections()[collIndex].itemCount() + 1;
			self.myCollections()[collIndex].itemCount(newItemCount);
			self.myCollections()[collIndex].firstEntries.push(recordObservable);
			saveCollectionsToStorage(self.myCollections);
		};

		self.reloadCollection = function(data) {
			var newCollection = ko.mapping.fromJS(data);
			ko.mapping.fromJS(data, newCollection);
			self.myCollections.unshift(newCollection);
			saveCollectionsToStorage(self.myCollections);
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
			var recordsCall = collectionCall + "\list\start=0&offset=20&format=all";
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
