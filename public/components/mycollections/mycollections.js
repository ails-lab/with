define(['knockout', 'text!./mycollections.html', 'knockout-else', 'app'], function(ko, template, KnockoutElse, app) {
	
	function Entry(entryData) {
		//this.entryThumbnailUrl = ko.observable(entryData.thumbnailUrl);
		//this.entryTitle = entryData.title;
		//this.entrySourceId = entryData.sourceId;
		var entry = ko.mapping.fromJS(entryData);
		ko.mapping.fromJS(entryData, entry);
		return entry;
	}

	self.reload = function(dbId) {
		$.ajax({
			"url": "/collection/"+dbId,
			"method": "GET",
			success: function(data){
				//TODO: Confirm that 1) myCollections array is updated (recursively) 2) as well as firstEntries
				//self.myCollections()[index](self.load());
				//alert(JSON.stringify(self.editableCollection()));
				ko.mapping.fromJS(data, self.myCollections);
			}
		});				
	};
	
	function MyCollectionsModel(params) {
		KnockoutElse.init([spec={}]);
		var self = this;
		self.route = params.route;
		var collections = [];
		self.index = ko.observable(0);
		//self.collectionToEdit = ko.observable(new MyCollection({}));
		self.myCollections = ko.mapping.fromJS([]);
		var promise = app.getUserCollections();
		self.titleToEdit = ko.observable("defaultTitle");
        self.descriptionToEdit = ko.observable("");
        self.isPublicToEdit = ko.observable(true);
		$.when(promise).done(function() {
			if (sessionStorage.getItem('UserCollections') !== null) 
			  collections = JSON.parse(sessionStorage.getItem("UserCollections"));
			if (localStorage.getItem('UserCollections') !== null) 
			  collections = JSON.parse(localStorage.getItem("UserCollections"));
			/*self.myCollections(ko.utils.arrayMap(collections, function(collectionData) {
			    return new MyCollection(collectionData);
			}));*/
			ko.mapping.fromJS(collections, self.myCollections);
		});
		
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
                        return item.dbId() == collectionId;
                    });
					saveCollectionsToStorage(self.myCollections);
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
	        self.index(index);
	        self.titleToEdit(self.myCollections()[index].title());
	        self.descriptionToEdit(self.myCollections()[index].description());
	        self.isPublicToEdit(self.myCollections()[index].isPublic());
			app.showPopup("edit-collection");
		}
		
		self.closeEditCollectionPopup = function() {
			app.closePopup();
		}
		
		editCollection = function (title, description, isPublic) {
			$.ajax({
				"url": "/collection/"+collectionId,
				"method": "POST",
				"data": {"title": title,
						"description": description,
						"isPublic": isPublic
					},
				success: function(result){
					self.myCollections.remove(function (item) {
                        return item.dbId() == collectionId;
                    });
					saveCollectionsToStorage(self.myCollections);
				}
			});
		};
		
		closeEditPopup = function() {
			closePopup();
		}
		
		self.privateToggle=function(e,arg){
			$(arg.currentTarget).parent().find('.btn').toggleClass('active');
		    if ($(arg.currentTarget).parent().find('.btn-primary').size()>0) {
		    	$(arg.currentTarget).parent().find('.btn').toggleClass('btn-primary');
		    }
		    $(arg.currentTarget).parent().find('.btn').toggleClass('btn-default');
			 self.isPublic = $("#publiccoll .active").data("value");

		}
	}
	
	return {viewModel: MyCollectionsModel, template: template};
});
