define(['bridget','knockout', 'text!./organization-edit.html', 'isotope','imagesloaded', 'app', 'async!https://maps.google.com/maps/api/js?v=3&sensor=false', 'knockout-validation', 'smoke', 'jquery.fileupload'], function (bridget,ko, template,Isotope,imagesLoaded, app) {

	$.bridget('isotope', Isotope);

	ko.validation.init({
		errorElementClass: 'error',
		errorMessageClass: 'errormsg',
		decorateInputElement: true
	});

	ko.bindingHandlers.profileisotope = {
				init: app.initOrUpdate('init'),
				update: app.initOrUpdate('update')
			};


	var mapping = {
		create: function (options) {
			var self = this;
			// use extend instead of map to avoid observables

			self = $.extend(self, options.data);
			self.title = findByLang(self.descriptiveData.label);
			self.thumbnail = ko.computed(function () {
				if (self.media && self.media[0] && self.media[0].Thumbnail) {
					var data = self.media[0].Thumbnail.url;
					if (data) {
						return data;
					} else {
						return "img/ui/ic-noimage.png";
					}
				}
				return "img/ui/ic-noimage.png";
			});
			self.type = ko.computed(function () {
				if (self.resourceType) {
					if (self.resourceType.indexOf("SimpleCollection") != -1) {
						return "COLLECTION";
					} else if (self.resourceType.indexOf("Exhibition") != -1) {
						return "EXHIBITION";
					}
				} else {
					return "";
				}
			});
			self.css = ko.computed(function () {
				if (self.resourceType) {
					if (self.resourceType.indexOf("SimpleCollection") != -1) {
						return "item collection";
					} else if (self.resourceType.indexOf("Exhibition") != -1) {
						return "item exhibition";
					}
				} else {
					return "item space";
				}
			});
			self.url = ko.computed(function () {
				if (self.resourceType) {
					if (self.resourceType.indexOf("SimpleCollection") != -1) {
						return 'index.html#collectionview/' + self.dbId;
					} else if (self.resourceType.indexOf("Exhibition") != -1) {
						return 'index.html#exhibitionview/' + self.dbId;
					}
				} else {
					return "index.html#collectionview/"+ self.dbId;
				}
			});
			self.owner = ko.computed(function () {
				if (self.withCreatorInfo) {
					return self.withCreatorInfo.username;
				}
			});
			self.count = ko.computed(function () {
				if (self.administrative.entryCount === 1) {
					return self.administrative.entryCount + ' item';
				} else {
					return self.administrative.entryCount + ' items';
				}
			});

			return self;
		}
	};

	function Collection(data) {
		var self=this;

		var mapping = {
				create: function(options) {
			    	var self=this;
			        // use extend instead of map to avoid observables

			    	self=$.extend(self, options.data);

			    	self.title=findByLang(self.descriptiveData.label);
			    	self.entryCount=self.administrative.entryCount;
			    	self.thumbnail = ko.computed(function() {
			          if(self.media && self.media[0] && self.media[0].Thumbnail){
			        	var data=self.media[0].Thumbnail.url;
			        	 if(data){
			 				return data;}
			 			  else{
			 				   return "img/ui/ic-noimage.png";
			 			   }
			        	}
			        	return "img/ui/ic-noimage.png";
			        });

			        self.type=ko.computed(function() {
			        	if(self.resourceType){
			        		if (self.resourceType.indexOf("SimpleCollection")!=-1)
			        		  return "COLLECTION";
			        		else if (self.resourceType.indexOf("Exhibition")!=-1)
			        			return "EXHIBITION";
			        	}else return "";
			        });

			        self.css=ko.computed(function() {
			        	if(self.resourceType){
			        		if (self.resourceType.indexOf("SimpleCollection")!=-1)
			        		  return "item collection";
			        		else if (self.resourceType.indexOf("Exhibition")!=-1)
			        			return "item exhibition";

			        	}else return "item exhibition";
			        });

			        self.url=ko.computed(function() {
			        	if(self.resourceType){
			        		if (self.resourceType.indexOf("SimpleCollection")!=-1)
				    		  return 'index.html#collectionview/'+ self.dbId;
			        		else if (self.resourceType.indexOf("Space")!=-1){
			        			return self.administrative.isShownAt;
			        		}
			        		else if (self.resourceType.indexOf("Exhibition")!=-1){
				    			return 'index.html#exhibitionview/'+ self.dbId;
				    		}
			        	}else return "index.html#collectionview/"+ self.dbId;
			        });
			        self.owner=ko.computed(function(){
			        	if(self.withCreatorInfo){
			        		return self.withCreatorInfo.username;
			        	}
			        });

			        return self;
			     }

		};


		var recmapping={
				'dbId': {
					key: function(data) {
			            return ko.utils.unwrapObservable(data.dbId);
			        }
				 }};
		self.isLoaded = ko.observable(false);
		self.records=ko.mapping.fromJS([], recmapping);


		self.data = ko.mapping.fromJS({"dbID":"","administrative":"","descriptiveData":""}, mapping);

		self.load = function(data) {
			self.data=ko.mapping.fromJS(data, mapping);

		};


		if(data != undefined){
			self.load(data);

		}
	}


	function OrganizationEditViewModel(params) {
		// Check if user is logged in. If not, ask for user to login
		if (localStorage.getItem('logged_in') != "true") {
			window.location.href = "#login";
		}

		$("div[role='main']").toggleClass("homepage", false);

		var self = this;
		self.id = ko.observable(params.id);
		self.route = params.route;
		self.collections=ko.observableArray();

		self.name = params.name;				// Project or Organization (display field)
		self.namePlural = params.name + 's';	// Projects or Organizations (display field)
		var $container = $("#orggrid").isotope({
			itemSelector: '.item',
			transitionDuration: transDuration,
			masonry: {
				columnWidth		: '.sizer',
				percentPosition	: true

			}
		});

		self.revealItems = function (data) {
			if(data.length==0){ //self.loading(false);
				return([]);
			}
			var items=[];
			for (var i in data) {
				var result = new Collection(data[i]);
				items.push(result);

			}


			/*if new items are found then empty collection and realod*/
			if(items.length>0){
				self.collections.removeAll();
				//self.collections.removeAll();
				self.collections.push.apply(self.collections, items);
				return items;}
			else {return [];}
		};


		/* *********************************************************************
		 *
		 * Manage Groups functions
		 *
		 * *********************************************************************/

		self.myUsername = ko.observable(app.currentUser.username());
		self.userId = ko.observable(app.currentUser._id());
		self.userMembers = ko.mapping.fromJS([], {});
		self.groupMembers = ko.mapping.fromJS([], {});
		self.image = "";
		self.colors = ['blue', 'green', 'red', 'yellow'];
		// mapping to state with key is the identifier
		var usersMapping = {
			'dbId': {
				key: function (data) {
					return ko.utils.unwrapObservable(data.username);
				}
			}
		};


		ko.bindingHandlers.autocompleteUsername = {
			init: function (elem, valueAccessor, allBindingsAccessor, viewModel, context) {
				app.autoCompleteUserName(elem, valueAccessor, allBindingsAccessor, viewModel, context, function (suggestion) {
					viewModel.addToUserGroup();
				});
			}
		};

		arrayFirstIndexOf = function (array, predicate) {
			for (var i = 0, j = array.length; i < j; i++) {
				if (predicate.call(undefined, array[i])) {
					return i;
				}
			}
			return -1;
		};

		self.getMembersInfo = function (category) {
			$.ajax({
				method : "GET",
				contentType : "application/json",
				url : "/group/membersInfo/" + self.id(),
				data : "category=" + category,
				success : function (result) {
					if (result.users !== undefined) {
						var users = result.users;
						ko.mapping.fromJS(users, self.usersMapping, self.userMembers);
					}
					if (result.groups !== undefined) {
						var userGroups = result.groups;
						ko.mapping.fromJS(userGroups, self.usersMapping, self.groupMembers);
					}
				},
				error : function (result) {
					$.smkAlert({text: "Invalid groupId", type: 'danger', time: 10});
				}
			});
		};

		// fill userMembers, groupMembers arrays on load
		//self.getMembersInfo("both");

		self.addToUserGroup = function () {
			var username = $("#userName").val();
			if (username == "") {
				username = $("#groupName").val();
			}
			$("#userName").val("");
			$("#groupName").val("");
			var userId = self.userId();
			$.ajax({
				method : "GET",
				contentType : "application/json",
				url : "/user/findByUserOrGroupNameOrEmail",
				data : "userOrGroupNameOrEmail=" + username,
				success : function (result) {
					self.excecuteAdd(result);
				},
				error : function (result) {
					$.smkAlert({ text: 'There is no such username or email', type: 'danger', time: 10 });
				}
			});
		};

		self.excecuteAdd = function (userData) {
			$.ajax({
				method : "PUT",
				contentType : "text/plain",
				url : "/group/addUserOrGroup/" + self.id() + "?id=" + userData.userId,
				success : function (result) {
					self.image = userData.image;
					if (userData.category == "user") {
						self.userMembers.push(ko.mapping.fromJS(userData));
					} else {
						self.groupMembers.push(ko.mapping.fromJS(userData));
					}
				},
				error : function (result) {
					$.smkAlert({ text: result.responseJSON.error, type: 'danger', time: 10 });
				}
			});
		};

		self.makeAdmin = function (userId) {
			$.ajax({
				method : "PUT",
				contentType : "text/plain",
				url : "/group/admin/" + self.id() + "?id=" + userId,
				success : function (result) {
					
				},
				error : function (result) {
					$.smkAlert({ text: result.responseJSON.error, type: 'danger', time: 10 });
				}
			});
		};

		self.makeMember = function (userId) {
			$.ajax({
				method : "DELETE",
				contentType : "text/plain",
				url : "/group/admin/" + self.id() + "?id=" + userId,
				success : function (result) {
				},
				error : function (result) {
					$.smkAlert({ text: result.responseJSON.error, type: 'danger', time: 10 });
				}
			});
		};

		self.isAdminToggle = function(admin
			, userId){
			if (!admin) 
				self.makeMember(userId);
			else 
				self.makeAdmin(userId);
			return true;
		}

		self.excecuteRemove = function (id, category) {
			$.ajax({
				method : "DELETE",
				contentType : "text/plain",
				url : "/group/removeUserOrGroup/" + self.id() + "?id=" + id,
				success : function (result) {
					if (category == "user") {
						var index = arrayFirstIndexOf(self.userMembers(), function (item) {
							return item.userId() === id;
						});
						if (index > -1) {
							self.userMembers.splice(index, 1);
						}
					} else {
						var index = arrayFirstIndexOf(self.groupMembers(), function (item) {
							return item.userId() === id;
						});
						if (index > -1) {
							self.groupMembers.splice(index, 1);
						}
					}
				},
				error: function (result) {
					$.smkAlert({ text: 'There is no such username or email', type: 'danger', time: 10});
				}
			});
		};

		/* ****************************************************************
		 *
		 * ****************************************************************/
		
		

		// Group Details
		self.username = ko.observable().extend({
			required: true,
			minLength: 3,
			pattern: {
				message: 'Your username must be alphanumeric.',
				params: /^\w+$/
			}
		});
		self.friendlyName = ko.observable().extend({
			required: true
		});
		self.about = ko.observable();
		self.validationModel = ko.validatedObservable({
			username: self.username,
			friendlyName: self.friendlyName
		});
		self.avatar = {
			Original: ko.observable(),
			Tiny: ko.observable(),
			Square: ko.observable(),
			Thumbnail: ko.observable(),
			Medium: ko.observable()
		};
		//self.collections = ko.mapping.fromJS([], mapping);

		// Page Fields
		self.page = {
			address: ko.observable(),
			city: ko.observable(),
			country: ko.observable(),
			url: ko.observable(),
			cover: {
				Original: ko.observable(),
				Tiny: ko.observable(),
				Square: ko.observable(),
				Thumbnail: ko.observable(),
				Medium: ko.observable()
			},			
			featuredCollections: ko.observableArray(),
			featuredExhibitions: ko.observableArray()
		};



		// Computed & Utility Observables
		self.count = ko.observable(0);
		self.coordinates = {
			latitude: ko.observable(),
			longitude: ko.observable()
		};
		self.fullAddress = ko.pureComputed(function () {
			var addr = '';

			if (self.page.address() != null) {
				addr += self.page.address();
			}
			if (self.page.city() != null) {
				if (addr.length > 0) {
					addr += ', ';
				}
				addr += self.page.city();
			}
			if (self.page.country() != null) {
				if (addr.length > 0) {
					addr += ', ';
				}
				addr += self.page.country();
			}

			return addr;
		});
		self.coords = ko.computed(function () {
			if (self.coordinates.latitude() && self.coordinates.longitude()) {
				return "https://www.google.com/maps/embed/v1/place?q=" + self.coordinates.latitude() + "," + self.coordinates.longitude() + "&key=AIzaSyAN0om9mFmy1QN6Wf54tXAowK4eT0ZUPrU";
			} else {
				return null;
			}
		});
		self.backgroundImage = ko.computed(function () {
			if (self.page.cover.Original()) {
				return 'url(' + self.page.cover.Original() + ')';
			} else {
				return null;
			}
		});
		self.isCreator = ko.observable(false);
		self.isAdmin = ko.observable(false);
		self.logo = ko.pureComputed(function () {
			if (self.avatar.Square()) {
				return self.avatar.Square();
			} else {
				return 'img/content/profile-placeholder.png';
			}
		});
		self.coverThumbnail = ko.pureComputed(function () {
			if (self.page.cover.Original()) {
				return self.page.cover.Original();
			} else {
				return 'img/ui/upload-placeholder.png';
			}
		});

		$.ajax({
			type: 'GET',
			url: '/group/' + self.id(),
			success: function (data, textStatus, jqXHR) {
				self.loadGroup(data);
			},
			error: function () {
				self.goBack();
			}
		});

		// Logo Upload function
		$('#logoupload').fileupload({
			type: "POST",
			url: '/media/create',
			acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
			maxFileSize: 50000,
			done: function (e, data) {
				self.avatar.Original(data.result.original);
				self.avatar.Tiny(data.result.tiny);
				self.avatar.Square(data.result.square);
				self.avatar.Thumbnail(data.result.thumbnail);
				self.avatar.Medium(data.result.medium);

				self.saveChanges();
			},
			error: function (e, data) {
				$.smkAlert({
					text: 'Error uploading the file',
					type: 'danger',
					time: 10
				});
			}
		});

		// Cover Upload function
		$('#coverupload').fileupload({
			type: "POST",
			url: '/media/create',
			acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
			maxFileSize: 500000,
			done: function (e, data) {
				self.page.cover.Original(data.result.original);
				self.page.cover.Tiny(data.result.tiny);
				self.page.cover.Square(data.result.square);
				self.page.cover.Thumbnail(data.result.thumbnail);
				self.page.cover.Medium(data.result.medium);
			},
			error: function (e, data) {
				$.smkAlert({
					text: 'Error uploading the file',
					type: 'danger',
					time: 10
				});
			}
		});

		self.goBack = function () {
			window.location.href = '#' + params.type + 's';
		};

		self.showGroup = function () {
			window.location.href = '#' + params.type + '/' + self.id();
		};

		self.deleteGroup = function () {
			$.smkConfirm({
				text: self.friendlyName() + ' will be permanently deleted. Are you sure?',
				accept: 'Delete',
				cancel: 'Cancel'
			}, function (ee) {
				if (ee) {
					$.ajax({
						type: 'DELETE',
						url: '/group/' + self.dbId(),
						success: function (data, textStatus, jqXHR) {
							self.goBack();
						},
						error: function (jqXHR, textStatus, errorThrown) {
							console.log(errorThrown);
						}
					});
				}
			});
		};

		self.loadGroup = function (data) {
			self.username(data.username);
			self.friendlyName(data.friendlyName);
			self.about(data.about);
			if (data.avatar != null) {
				self.avatar.Original(data.avatar.Original);
				self.avatar.Square(data.avatar.Square);
				self.avatar.Thumbnail(data.avatar.Thumbnail);
				self.avatar.Medium(data.avatar.Medium);
				self.avatar.Tiny(data.avatar.Tiny);
			} else {
				self.avatar.Original(null);
				self.avatar.Square(null);
				self.avatar.Thumbnail(null);
				self.avatar.Medium(null);
				self.avatar.Tiny(null);
			}

			if (data.page != null) {
				self.page.address(data.page.address);
				self.page.city(data.page.city);
				self.page.country(data.page.country);
				self.page.url(data.page.url);
				if (data.page.coordinates != null) {
					self.coordinates.longitude(data.page.coordinates.longitude);
					self.coordinates.latitude(data.page.coordinates.latitude);
				}
				if (data.page.cover != null) {
					self.page.cover.Original(data.page.cover.Original);
					self.page.cover.Square(data.page.cover.Square);
					self.page.cover.Thumbnail(data.page.cover.Thumbnail);
					self.page.cover.Medium(data.page.cover.Medium);
					self.page.cover.Tiny(data.page.cover.Tiny);
				} else {
					self.page.cover.Original(null);
					self.page.cover.Square(null);
					self.page.cover.Thumbnail(null);
					self.page.cover.Medium(null);
					self.page.cover.Tiny(null);
				}

				//self.page.featuredCollections =	ko.mapping.fromJS(data.page.featuredCollections);
				//self.page.featuredExhibitions =	ko.mapping.fromJS(data.page.featuredExhibitions);
				if (data.page.featuredCollections != null){
					self.page.featuredCollections(data.page.featuredCollections);	
				}
				else{
				self.page.featuredCollections([]);	
				}


				if (data.page.featuredExhibitions != null){
					self.page.featuredExhibitions(data.page.featuredExhibitions);	
				}
				else{
				self.page.featuredExhibitions([]);	
				}
				
				
			}

			// Load Collections
			self.getProfileCollections();

			self.isCreator(app.currentUser._id() === data.creator);
			self.isAdmin(data.adminIds.indexOf(app.currentUser._id()) > 0);
			WITHApp.tabAction();
		};

		self.closeSideBar = function () {
			// Reload Group to reset changes
			console.log("close Sidebar");
			$.ajax({
				type: 'GET',
				url: '/group/' + self.id(),
				success: function (data, textStatus, jqXHR) {
					self.loadGroup(data);
				}
			});

			// Close the side-bar
			$('.action').removeClass('active');

			// Hide errors
			self.validationModel.errors.showAllMessages(false);
		};

		self.saveChanges = function () {
			var data = {
				username: self.username,
				friendlyName: self.friendlyName,
				avatar: self.avatar,
				about: self.about,
				page: self.page
			};
			$.ajax({
				type: 'PUT',
				url: '/group/' + self.id(),
				contentType: 'application/json',
				dataType: 'json',
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					$.smkAlert({
						text: 'Update successful!',
						type: 'success'
					});

					// Read the updated coordinates
					if (data.page.coordinates != null) {
						self.coordinates.latitude(data.page.coordinates.latitude);
						self.coordinates.longitude(data.page.coordinates.longitude);
					} else {
						self.coordinates.latitude(null);
						self.coordinates.longitude(null);
					}

					// Close the side-bar
					$('.action').removeClass('active');
				},
				error: function (request, status, error) {
					var err = JSON.parse(request.responseText);
					$.smkAlert({
						text: err.error,
						type: 'danger'
					});
				}
			});
		};



		self.getProfileCollections = function () {
			$.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				data: "offset=0&count=50&directlyAccessedByUserOrGroup=" + JSON.stringify([{group: self.username(), rights: "WRITE"}])
			}).success(function (data, textStatus, jqXHR) {
				var items=self.revealItems(data['collectionsOrExhibitions']);

				if(items.length>0){
					 var $newitems=getItems(items);

					 providerIsotopeImagesReveal( $container,$newitems );

					}

			});
		};


		loadUrl = function (data,event) {
			event.preventDefault();
			window.location.href = data;

			return false;
		};
		
		function getItem(collection) {
			  //var tile= '<div class="'+collection.data.css()+'"> <div class="wrap">';
			  var tile= '<div class="item ' + collection.data.dbId + '"> <div class="wrap">';
			   tile+='<a href="'+collection.data.url()+'">'
                +'<div class="thumb"><img src="'+collection.data.thumbnail()+'"><div class="counter">'+collection.data.entryCount+' ITEMS</div></div>'
                +' <div class="info"><div class="type">'+collection.data.type()+'</div><h2 class="title">'+collection.data.title+'</h2></div>'
                +'</a>'
                +'<div class="action-group"><div class="wrap"><ul><li><a href="#" data-toggle="tooltip" data-placement="top" title="Unshare media" class="fa fa-ban" onclick="unShareCollection(\'' + collection.data.dbId + '\',event);"></a></li>';
                if (isFeatured(collection.data.dbId,collection.data.type()))
                	tile +='<li><a href="#" data-toggle="tooltip" data-placement="top" title="Set as Featured" id='+collection.data.dbId+' class="fa fa-star  featuredbutton" style="color:#ff9806" onclick="toggleFeaturedCollection(\'' + collection.data.dbId + '\',\'' + collection.data.type() + '\',event);"></a></li>';
                
            	else
            		tile +='<li><a href="#" data-toggle="tooltip" data-placement="top" title="Set as Featured" id='+collection.data.dbId+' class="fa fa-star  featuredbutton"  onclick="toggleFeaturedCollection(\'' + collection.data.dbId + '\',\'' + collection.data.type() + '\',event);"></a></li>';
            		
                tile+='</ul></div></div>'
                +'</div></div>';
			return tile;

		}

		function isFeatured(id,type){
			var result;
			if (type=="COLLECTION" && self.page.featuredCollections.indexOf(id) > -1) {			
				result = true;
			}
			else if (type=="EXHIBITION" && self.page.featuredExhibitions.indexOf(id) > -1) {
				result =  true;
			}
			else {
					result =  false;
			}
			return result;
		}

		toggleFeaturedCollection = function (id,type,event) {
        	event.preventDefault();

			if (isFeatured(id,type)) self.removeFeatured(id,type);

			else self.addFeatured(id,type);
			

		};

		unShareCollection = function (id, event) {
        	event.preventDefault();

		//	var $elem = $(event.target).parents(".item");
					$.ajax({
						url:'/rights/' + id +'/NONE'+'?username='+ self.username()+'&membersDowngrade=false',
						type: 'GET',
						contentType: "application/json",
						dataType: 'json',
						success: function (data, textStatus, xhr) {
							$.ajax({
								type: "GET",
								contentType: "application/json",
								dataType: "json",
								url: "/collection/list",
								processData: false,
								data: "offset=0&count=50&directlyAccessedByUserOrGroup=" + JSON.stringify([{group: self.username(), rights: "WRITE"}])
							}).success(function (data, textStatus, jqXHR) {
								var items=self.revealItems(data['collectionsOrExhibitions']);

								
									 var $newitems=getItems(items);

									 providerIsotopeImagesReveal( $container,$newitems );

										

								});
						},
						error: function (xhr, textStatus, errorThrown) {
							$.smkAlert({
								text: 'An error has occured',
								type: 'danger',
								time: 10
							});
						}
					});
			};


		

		self.addFeatured = function (id,type) {
			if (type=="COLLECTION"){
				var data = {
				"fCollections":[
								id
							],
				"fExhibitions":[
				]
				};
			}
			else if (type=="EXHIBITION"){
				var data = {
				"fCollections":[
							],
				"fExhibitions":[
							id
					]
				};
			}
			$.ajax({
				type: 'POST',
				url: '/group/' + self.id() +'/addFeatured',
				contentType: 'application/json',
				dataType: 'json',
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					/*$.smkAlert({
						text: 'Update successful!',
						type: 'success'
					});*/

					if (type=="COLLECTION"){
					self.page.featuredCollections.push(id);
					}
					else if(type=="EXHIBITION"){
					self.page.featuredExhibitions.push(id);
					}
					$(document.getElementById(id)).css( "color", "#ff9806" );

				},
				error: function (request, status, error) {
					var err = JSON.parse(request.responseText);
					$.smkAlert({
						text: err.error,
						type: 'danger'
					});
				}
			});
		};

		self.removeFeatured = function (id,type) {
			if (type=="COLLECTION"){
				var data = {
				"fCollections":[
								id
							],
				"fExhibitions":[
				]
				};
			}
			else if (type=="EXHIBITION"){
				var data = {
				"fCollections":[
							],
				"fExhibitions":[
							id
					]
				};
			}
			$.ajax({
				type: 'POST',
				url: '/group/' + self.id() +'/removeFeatured',
				contentType: 'application/json',
				dataType: 'json',
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					/*$.smkAlert({
						text: 'Update successful!',
						type: 'success'
					});*/
					if (type=="COLLECTION"){
						var index = self.page.featuredCollections().indexOf(id);
						if (index > -1) {
							self.page.featuredCollections().splice(index, 1);
						}
					}
					else if(type=="EXHIBITION"){
						var index = self.page.featuredExhibitions().indexOf(id);
						if (index > -1) {
							self.page.featuredExhibitions().splice(index, 1);
						}
					}

					$(document.getElementById(id)).css( "color", "" );
				},
				error: function (request, status, error) {
					var err = JSON.parse(request.responseText);
					$.smkAlert({
						text: err.error,
						type: 'danger'
					});
				}
			});
		};


  function getItems(data) {
	  var items = '';
	  for ( i in data) {
	    items += getItem(data[i]);
	  }
	  return $( items );
	}





    providerIsotopeImagesReveal = function( $container,$items ) {
		  var iso = $container.data('isotope');
		  var itemSelector = iso.options.itemSelector;
		  var $allitems=$(".item");
		  $container.isotope( 'remove', $allitems );
		  $container.isotope('layout');
		  // append to container
		  $container.append( $items );
		// hide by default
		  $items.hide();
		  $items.imagesLoaded().progress( function( imgLoad, image ) {
		    // get item
		    var $item = $( image.img ).parents( itemSelector );
		    // un-hide item
		    
		    $item.show();
		    if(iso)
				  iso.appended($item);
				else{
					$.error("iso gone");
				}
		    

		  });

		  return this;
		};




	  self.filter=function(data, event) {
		  			  var selector = event.currentTarget.attributes.getNamedItem("data-filter").value;
					  $(event.currentTarget).siblings().removeClass("active");
					  $(event.currentTarget).addClass("active");
					  $( settings.mSelector ).isotope({ filter: selector });
					  return false;
				}



	}

	return {
		viewModel: OrganizationEditViewModel,
		template: template
	};
});
