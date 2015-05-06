define(['knockout', 'text!./mycollections.html', 'knockout-else', 'app'], function(ko, template, KnockoutElse, app) {

	

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
			self.myCollections(collections);
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
				}
			});
			
			//TODO: remove deleted collection from UserCollections in sessionStorage and reload mycollections
		};
	
	}
	
	return {viewModel: MyCollectionsModel, template: template};
});
