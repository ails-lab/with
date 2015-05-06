define(['knockout', 'text!./mycollections.html', 'knockout-else', 'app'], function(ko, template, KnockoutElse, app) {

	
	function MyCollection(collectionData) {
		this.title = ko.observable(collectionData.title);
		this.dbId = collectionData.dbId;
		this.description = ko.observable(collectionData.description);
		if (collectionData.thumbnail != null)
			this.thumbnail = ko.observable(collectionData.thumbnail);
		this.itemCount = collectionData.itemCount;
		this.isPublic = collectionData.isPublic;
		this.created = collectionData.created;
		this.lastModified = ko.observable(collectionData.lastModified);
		if (collectionData.category != null)
			this.category = ko.observable(collectionData.category);
		this.firstEntries = collectionData.firstEntries
	}
	
	function MyCollectionsModel(params) {
		KnockoutElse.init([spec={}]);
		var self = this;
		self.route = params.route;
		var collections = [];
		var promise = app.getUserCollections();
		self.myCollections = ko.observableArray([]);
		$.when(promise).done(function() {
			if (sessionStorage.getItem('UserCollections') !== null) 
			  collections = JSON.parse(sessionStorage.getItem("UserCollections"));
			if (localStorage.getItem('UserCollections') !== null) 
			  collections = JSON.parse(localStorage.getItem("UserCollections"));
			self.myCollections(ko.utils.arrayMap(collections, function(collectionData) {
			    return new MyCollection(collectionData);
			}));
		});
		
		self.deleteMyCollection = function(collection) {
			collectionId = collection.dbId;
			collectionTitle = collection.title;
			showDelCollPopup(collectionTitle, collectionId);
		};
		
		showDelCollPopup = function(collectionTitle, collectionId) {
			$("#myModal").find("h4").html("Do you want to delete this collection?");
			var body = $("#myModal").find("div.modal-body");
			body.empty();
	        var confirmBtn = $('<button> Confirm </button>').appendTo(body);
	        confirmBtn.click(function() {
	        	deleteCollection(collectionId);
	        	$("#myModal").modal('hide');
	        });
	        var cancelBtn = $('<button> Cancel </button>').appendTo(body);
	        cancelBtn.click(function() {
	        	$("#myModal").modal('hide');
	        });
			$("#myModal").modal('show');
		};
		
		self.closeDelCollPopup = function() {
			
		};
		
		deleteCollection = function(collectionId) {
			$.ajax({
				"url": "/collection/"+collectionId,
				"method": "DELETE",
				//"contentType": "application/json",
				//"data": {id: collectionId}),
				success: function(result){
					self.myCollections.remove(function (item) {
                        return item.dbId == collectionId;
                    });
					//self.myCollections()[0].title("New title!");
				}
			});
		};
	
	}
	
	return {viewModel: MyCollectionsModel, template: template};
});
