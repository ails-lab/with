define(['knockout', 'text!./mycollections.html', 'knockout-else', 'app'], function(ko, template, KnockoutElse, app) {

	function Entry(entryData) {
		this.entryThumbnailUrl = ko.observable(entryData.thumbnailUrl);
		//this.entryTitle = entryData.title;
		//this.entrySourceId = entryData.sourceId;
	}

	function MyCollection(collectionData) {
		var self=this;
		self.title = ko.observable("");
		self.dbId = ko.observable(-1);
		self.description = ko.observable("");
		self.thumbnail = ko.observable("");
		self.itemCount =ko.observable(0);
		self.isPublic = ko.observable(false);
		self.created ="";
		self.lastModified = ko.observable("");
		self.category = ko.observable("");
		self.firstEntries = ko.observableArray([]);
		self.url="";
		
	   self.load=function(collectionData){
		if (collectionData.title != null)
			self.title (collectionData.title);
		if (collectionData.dbId != null)
			self.dbId (collectionData.dbId);
		if (collectionData.description != null)
			self.description(collectionData.description);
		if (collectionData.thumbnail != null)
			self.thumbnail(collectionData.thumbnail);
		self.itemCount(collectionData.itemCount);
		self.isPublic(collectionData.isPublic);
		self.created = collectionData.created;
		self.lastModified(collectionData.lastModified);
		if (collectionData.category != null)
			self.category (collectionData.category);
		
		self.firstEntries(ko.utils.arrayMap(collectionData.firstEntries, function(entryData) {
		    return new Entry(entryData);
		}));
		self.url = ko.computed(function () {
		       return "index.html#collectionview/" +self.dbId();
		   }, this);
		
	   }
		
		self.reload = function() {
			$.ajax({
				"url": "/collection/"+self.dbId(),
				"method": "GET",
				success: function(data){
					self.title(data.title);
					self.description(data.description);
					self.itemCount(data.itemCount);
					
					self.firstEntries(ko.utils.arrayMap(data.firstEntries, function(entryData) {
					    return new Entry(entryData);
					}));
					
			      }
			});}
		
		if(collectionData != undefined) self.load(collectionData);
	}
	
	function MyCollectionsModel(params) {
		KnockoutElse.init([spec={}]);
		var self = this;
		self.route = params.route;
		var collections = [];
		self.collectionToEdit = ko.observable(new MyCollection({}));
		self.titleToEdit = ko.observable();
		self.catrgoryToEdit = ko.observable();
		self.myCollections = ko.observableArray([]);
		var promise = app.getUserCollections();
		$.when(promise).done(function() {
			if (sessionStorage.getItem('UserCollections') !== null) 
			  collections = JSON.parse(sessionStorage.getItem("UserCollections"));
			if (localStorage.getItem('UserCollections') !== null) 
			  collections = JSON.parse(localStorage.getItem("UserCollections"));
			self.myCollections(ko.utils.arrayMap(collections, function(collectionData) {
			    return new MyCollection(collectionData);
			}));
		});
		
		//$("edit-collection").modal("open");
		self.deleteMyCollection = function(collection) {
			collectionId = collection.dbId();
			collectionTitle = collection.title();
			showDelCollPopup(collectionTitle, collectionId);
		};
		
		self.createCollection = function() {
			createNewCollection();
		}
		
		showDelCollPopup = function(collectionTitle, collectionId) {
			$("#myModal").find("h4").html("Do you want to delete this collection?");
			var body = $("#myModal").find("div.modal-body");
			body.html("All records in that collection will be deleted.");
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
		
	    //Storage needs to be updated, because collection.js gets user collections from there
		saveCollectionsToStorage = function(collections) {
			if (sessionStorage.getItem('User') !== null) {
				sessionStorage.setItem('UserCollections', JSON.stringify(collections));
			}
			else if (localStorage.getItem('User') !== null) {
				localStorage.setItem('UserCollections', JSON.stringify(collections));
			}
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
					saveCollectionsToStorage(self.myCollections());
					//self.myCollections()[0].title("New title!");
				}
			});
		};
		
		
		self.addNew=function(cdata){
			var myc=new MyCollection(cdata);
			self.myCollections.push(myc);
			
		}
		
		self.openEditCollectionPopup = function(collection, event) {
	        var context = ko.contextFor(event.target);
			var index = context.$index();
	        var myCollectionToEdit = self.myCollections()[index];
	        self.collectionToEdit(myCollectionToEdit);
	        alert(JSON.stringify(self.collectionToEdit().title()));
			app.showPopup("edit-collection");
		}
		
		self.closeEditCollectionPopup = function() {
			app.closePopup();
		}
		
		editCollection = function(collection) {//(title, description, category, isPublic, thumbnail) {
			alert("1");
			alert(collection.title);
		};
		
		closeEditPopup = function() {
			closePopup();
		}
	
	}
	
	return {viewModel: MyCollectionsModel, template: template};
});
