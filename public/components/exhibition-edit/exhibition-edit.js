define(['knockout', 'text!./_exhibition-edit.html', 'jquery.ui', 'autoscroll', 'app', 'jquery.lazyload', 'knockout-else', 'knockout-validation','smoke', 'jquery.fileupload'], 
		function (ko, template, jqueryUI, autoscroll, app, jqueryLazyLoad, KnockoutElse) {
	
	collectionItemCount = 14;
	exhibitionItemCount = 4;
	
	function setUpSwitch(exhibition) {
		// Set the initial value
		$('#toggle2').prop('checked', exhibition.administrative.access.isPublic());

		// Add event handler for change event
		$("#toggle2").change(function () {
			state = this.checked;
			$.ajax({
				url: '/rights/' + exhibition.dbId() + '?isPublic=' + state,
				method: 'GET',
				contentType: "application/json",
				success: function () {
					// Empty
				},
				error: function () {
					// Empty
				}
			});
		});
	}
	
	ko.validation.init({
		errorElementClass: 'error',
		errorMessageClass: 'errormsg',
		decorateInputElement: true
	});

	function MyCollection(collectionData) {
		this.dbId = collectionData.dbId;
		this.title = ko.observable(app.findByLang(collectionData.descriptiveData.label));
		this.description = ko.observable(app.findByLang(collectionData.descriptiveData.description));
		this.itemCount = collectionData.administrative.entryCount;
		this.isPublic = collectionData.administrative.access.isPublic;
		this.created = collectionData.administrative.created;
		this.lastModified = collectionData.administrative.lastModified;
	}

	function updateExhibitionProperty(exhibition, propertyName, newValue) {
		var jsonObject = {};
		jsonObject[propertyName] = newValue;
		jsonObject["resourceType"] = "Exhibition";
		var jsonData = JSON.stringify(jsonObject);
		return $.ajax({
			type: "PUT",
			url: "/collection/" + exhibition.dbId(),
			data: jsonData,
			contentType: "application/json",
			success: function () {
				// Empty
			}
		});
	}

	function getExhibition(id) {
		return $.ajax({
			type: "GET",
			url: "/collection/" + id,
			success: function () {
				// Empty
			}
		});
	}

	function updateExhibition(exhibition, isTitleUpdate) {
		var jsonObject = {};
		if (isTitleUpdate) {
			jsonObject.descriptiveData = {
				label: {
					default: [exhibition.title()]
				}
			};
		} else {
			jsonObject.descriptiveData = {
				description: {
					default: [exhibition.description()]
				}
			};
		}
		var jsonData = JSON.stringify(jsonObject);
		return $.ajax({
			type: "PUT",
			url: "/collection/" + exhibition.dbId(),
			data: jsonData,
			contentType: "application/json",
			success: function () {
				// Empty
			}
		});
	}

	function saveItemToExhibition(record, position, exhibitionId) {
		var recordJS = ko.toJS(record);
		delete recordJS.__ko_mapping__;
		return $.ajax({
			"url": "/collection/" + exhibitionId + "/addRecord?position=" + position,
			"method": "post",
			"contentType": "application/json",
			"data": JSON.stringify(recordJS),
			"success": function (data) {
				// Empty
			},
			"error": function (result) {
				// Empty
			}
		});
	}

	function deleteItemFromExhibition(exhibitionId, recordId, position) {
		$.ajax({
			"url": "/collection/" + exhibitionId + "/removeRecord" + "?recId=" + recordId + '&position=' + position,
			"method": "delete",
			"success": function (data) {
				// Empty
			},
			"error": function (result) {
				// Empty
			}
		});
	}

	function moveItemInExhibition(exhibitionId, recordId, oldPosition, newPosition) {
		$.ajax({
			url: "/collection/" + exhibitionId + "/moveRecord" + "?recordId=" + recordId + '&oldPosition=' + oldPosition + '&newPosition=' + newPosition,
			method: 'put',
			success: function (data) {
				// Empty
			},
			error: function (result) {
				// Empty
			}
		});
	}

	function isEmpty(str) {
		return (!str || 0 === str.length);
	}
	
	

	var ExhibitionEditModel = function (params) {
		var self = this;
		self.route = params.route;
		KnockoutElse.init([spec = {}]);
		WITHApp.tabAction();
		WITHApp.initTooltip();
		self.checkLogged = function () {
			if (localStorage.getItem('logged_in') != "true") {
				window.location.href = "#login";
			}
		};
		self.mapping = {
			create: function (options) {
				// customize at the root level: add title and description
				// observables, based on multiliteral
				// TODO: support multilinguality, have to be observable arrays
				// of type [{lang: default, values: []}, ...]
		        var record = options.data;
		        var newRecord =  ko.mapping.fromJS(record, {});
				if ($.isEmptyObject(newRecord.contextData.body)) {
					body = ko.mapping.fromJS({
							"text": {"default": ""},
							"mediaType": null,
							"mediaUrl": "",
							"mediaDescription": "",
							"textPosition": "RIGHT"
						}, {});
					newRecord.contextData.body =body;
				}
				newRecord.containsContextField = function(field) {
					return ko.pureComputed(function () {
						if (newRecord.contextData.body == undefined || newRecord.contextData.body[field] == undefined) {
							return false;
						} else {
							return (newRecord.contextData.body[field]() !== null && newRecord.contextData.body[field]() !== '');
						}
					});
				};
				
				newRecord.containsText = ko.pureComputed(function () {
					if (newRecord.contextData.body == undefined 
						|| newRecord.contextData.body.text == undefined 
						|| newRecord.contextData.body.text.default == undefined) {
						return false;
					} else {
						return newRecord.contextData.body.text.default() !== '';
					}
				 });
				 newRecord.embeddedVideoUrl = ko.pureComputed(function () {
					if (newRecord.containsContextField("mediaUrl")() && newRecord.containsContextField("mediaType")()
						&& newRecord.contextData.body.mediaType() == "VIDEO") {
						var urlMatch = newRecord.contextData.body.mediaUrl().match(/youtube\.com.*(\?v=|\/embed\/)(.{11})/);
						if (urlMatch !== null) {
							var youtube_video_id = urlMatch.pop();
							var embeddedVideoPath = 'https://www.youtube.com/embed/' + youtube_video_id;
							return embeddedVideoPath;
						}
						else
							return null;
					}
					else
						return null;
				});
				newRecord.description = ko.observable("");
				newRecord.title = ko.mapping.fromJS(app.findByLang(record.descriptiveData.label), {});
				if (record.descriptiveData.description  === undefined || typeof record.descriptiveData.description.default  === 'undefined') {
					newRecord.description = ko.observable("");
				} else {
					newRecord.description = ko.observable(app.findByLang(record.descriptiveData.description.default[0]));
				}	
		        var fullres="";
			    
		        if(newRecord.media()!=null &&  newRecord.media()[0] !=null && newRecord.media()[0].Original!=null  && newRecord.media()[0].Original.url != undefined) {
		        	 fullres=newRecord.media()[0].Original.url();
		        }
		        if (fullres.indexOf("empty")!=-1) {fullres="";}
		        newRecord.fullres=ko.observable(fullres);
		        
		        var withUrl = "";
		        if (newRecord.media()!=null &&  newRecord.media()[0] !=null && newRecord.media()[0].Thumbnail!=null  && newRecord.media()[0].Thumbnail.url()!="null") {
		        	withUrl=newRecord.media()[0].Thumbnail.url();
		        }       
			    if (withUrl == "") {
						newRecord.thumbnailUrl = ko.observable("img/ui/ic-noimage.png");
			    }
			    else {
				   if (withUrl.indexOf("/media") == 0) {
						newRecord.thumbnailUrl = ko.observable(window.location.origin + withUrl);
				   } else {
						newRecord.thumbnailUrl = ko.observable(withUrl);
				   }
			    }
			    return newRecord;
		    }
		}		
		self.searchPage = 0;
		self.checkLogged();
		self.loading = ko.observable(false);
		self.title = ko.observable('');
		self.description = ko.observable('');
		self.credits = ko.observable('');
		self.dbId = ko.observable();
		self.loadingExhibitionItems = false;
		self.loadingInitialItemsCount = 0;
		var _removeItem = false;
		var _startIndex = -1;
		self.dbId(params.id);
		self.userSavedItemsArray = ko.mapping.fromJS([], {});  // holds the
																// selected
																// collections
																// items
		self.collectionItemsArray = ko.mapping.fromJS([], {}); // holds the
																// exhibitions
																// items
		self.itemText = ko.observable("");
		self.itemTextPosition = ko.observable(true);
		self.itemVideoUrl = ko.observable("").extend({
			required: true,
			pattern: {
				message: 'Not valid youtube video url.',
				params: /youtube\.com.*(\?v=|\/embed\/)(.{11})/
			}
		});
		self.itemAudioUrl = ko.observable("").extend({
			required: true,
			pattern: {
				message: 'Not valid url.',
				params: (/^(?:(?:https?|ftp):\/\/)(?:\S+(?::\S*)?@)?(?:(?!10(?:\.\d{1,3}){3})(?!127(?:\.‌​\d{1,3}){3})(?!169\.254(?:\.\d{1,3}){2})(?!192\.168(?:\.\d{1,3}){2})(?!172\.(?:1[‌​6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1‌​,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00‌​a1-\uffff0-9]+-?)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]+-?)*[a-z\u‌​00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:\/[^\s]*)?$/i)
			}
		});
		self.itemPosition = ko.observable(0);
		self.itemId = ko.observable("");
		self.itemMediaDescription = ko.observable("").extend({maxLength: 50});
		self.validationModelVideo = ko.validatedObservable({
			itemVideoUrl: self.itemVideoUrl
		});
		self.validationModelAudio = ko.validatedObservable({
			itemAudioUrl: self.itemAudioUrl
		});
		self.validationModelMediaDescription = ko.validatedObservable({
			itemMediaDescription: self.itemMediaDescription
		});
		self.backgroundImg = ko.observable("");
		self.displayCoverImage = ko.pureComputed(function () {
			if (self.backgroundImg && self.backgroundImg() != "") {
				return self.backgroundImg();
			} else {
				return 'img/ui/upload-placeholder.png';
			}
		});
		self.newBackgroundImg = ko.observable(false);
		var collections = [];
		var promise = app.getAllUserCollections();
		self.myCollections = ko.mapping.fromJS([]); // holds all the collections
		$.when(promise).done(function (data) {
			var collections = [];
			if (data.hasOwnProperty('collectionsOrExhibitions')) {
				collections = data['collectionsOrExhibitions'];
			}
			self.myCollections(ko.utils.arrayMap(collections, function (collectionData) {
				return new MyCollection(collectionData);
			}));

			// then initialise select
			// $('.selectpicker').selectpicker();
		});
		self.itemMediaType = ko.observable("");
		
		var mappingExhibition = {
			'dbId': {
				key: function (data) {
					return ko.utils.unwrapObservable(data.dbId);
				}
			},
			'media': {
				key: function (options) {
					return ko.utils.unwrapObservable(data.dbId);
				}
			},
			'copy': ["media"]
		};

        self.creationMode=true;
		self.loadingExhibitionItems = true;
		var promise = getExhibition(self.dbId());
		$.when(promise).done(function (data) {
			$('.outer').css("visibility", "visible");
			ko.mapping.fromJS(data, mappingExhibition, self);
			self.title(app.findByLang(data.descriptiveData.label));
			self.description(app.findByLang(data.descriptiveData.description));
			self.credits(data.descriptiveData.credits);
			setUpSwitch(self);
			if (self.descriptiveData.backgroundImg == null || self.descriptiveData.backgroundImg.Original == null || 
					self.descriptiveData.backgroundImg.Original.withUrl == null || 
					self.descriptiveData.backgroundImg.Original.withUrl == "") {
				self.backgroundImg("");
			} 						
			else {
				if (self.descriptiveData.backgroundImg.Original.withUrl().indexOf("/media") == 0) {
					self.backgroundImg(window.location.origin + self.descriptiveData.backgroundImg.Original.withUrl());
				} else {
					self.backgroundImg(self.descriptiveData.backgroundImg.Original.withUrl());
				}
			}
			$.ajax({
				url: "/collection/" + self.dbId() + "/list?count="+exhibitionItemCount+"&start=0",
				method: "get",
				success: function (data) {
					self.firstEntries = data.records;
					self.loadingInitialItemsCount = data.entryCount;
					ko.mapping.fromJS(self.firstEntries, self.mapping, self.collectionItemsArray);
					self.loadingExhibitionItems = true;
					WITHApp.initTooltip();
					WITHApp.tabAction();
				},
				error: function (result) {
					// Empty
				}
			});
		});

		self.selectedCollection = ko.observable();
		self.currentItemSet = ko.observable(false);
		self.currentItem = ko.observable();
		self.selectedCollection.subscribe(function (newCollection) {
			// Load the first 20 records when loading a different collection
			$.ajax({
				url: "/collection/" + newCollection.dbId + "/list?count="+collectionItemCount+"&start=0",
				method: "get",
				success: function (data) {
					self.firstEntries = data.records;
					ko.mapping.fromJS(self.firstEntries, self.mapping, self.userSavedItemsArray);
					self.searchPage = 0;
					WITHApp.initTooltip();
					// self.userSavedItemsArray(data.records);
				},
				error: function (result) {
					// Empty
				}
			});

			self.currentItem(self.userSavedItemsArray()[0]);
			if (self.userSavedItemsArray().length > 0) {
				self.currentItemSet(true);
			}
		});

		self.textfieldsLostFocus = function (isTitleUpdate) {
			updateExhibition(self, isTitleUpdate);
		};

		self.xButtonClicked = function (item) {
			var index = self.collectionItemsArray.indexOf(item);
			if (index > -1) {
				self.collectionItemsArray.remove(item);
				deleteItemFromExhibition(self.dbId(), item.dbId(), index);
			} else {
				self.userSavedItemsArray.splice(index, 1);
			}
		};

		self.itemsLoaded = 0;
        
		self.textPositionToBoolean = function (textPosition) {
			if (textPosition == "RIGHT")
				return true;
			else if (textPosition == "LEFT")
				return false;
			else 
				return undefined;
		};
		
		self.textPositionToString = function (textPosition) {
			if (textPosition)
				return "RIGHT";
			else 
				return "LEFT";
		};
		
		self.showNewItem = function (elem, index, record) {
			$(elem).hide().fadeIn(500);
			if ($(elem).hasClass('box fill')) { // it gets called for all
												// elements rendered with the
												// foreach
				if (self.creationMode && self.itemsLoaded < self.loadingInitialItemsCount) {
					self.itemsLoaded++;
					if(self.itemsLoaded==self.loadingInitialItemsCount){self.creationMode=false;}
					$(elem).find('#loadingIcon').fadeOut();
					return;
				}
				if (_bIsMoveOperation) {
					if (_startIndex >= 0) {
						moveItemInExhibition(self.dbId(), record.dbId(), _startIndex, index);
						_startIndex = -1;		// Move was successful, reset
												// the startIndex
					} else {
						if(self.itemsLoaded<self.loadingInitialItemsCount){self.creationMode=true;}
						saveItemToExhibition(record, index, self.dbId());
						_removeItem = true;		// Item was added but not
												// removed, so mark it for
												// removal
					}
					$(elem).find('#loadingIcon').fadeOut();
				} else {
					saveItemToExhibition(record, index, self.dbId());
					if(self.itemsLoaded<self.loadingInitialItemsCount){self.creationMode=true;}
					
					$(elem).find('#loadingIcon').fadeOut();
				}
			}
		};

		self.removeItem = function (elem, index, record) {
			if (_bIsMoveOperation) {
				_startIndex = index;	// If removeItem is executed before the
										// showNewItem, save the startIndex for
										// moving the record
				if (_removeItem) {		// If removeItem is executed after the
										// showNewItem, remove the item
					deleteItemFromExhibition(self.dbId(), record.dbId(), index);
					_removeItem = false;
				}
			}
			$(elem).fadeOut(500);
		};

		self.prepareForItemEdit = function (exhibitionItem, event) {
			var context = ko.contextFor(event.target);
			var index = context.$index();
			self.itemPosition(index);
			if (typeof exhibitionItem.contextData.body.mediaType !== 'undefined')
				self.itemMediaType(exhibitionItem.contextData.body.mediaType());
			if (self.itemMediaType() == "VIDEO") {
				if (typeof exhibitionItem.contextData.body.mediaUrl !== 'undefined') {
					self.itemVideoUrl(exhibitionItem.contextData.body.mediaUrl());
				}
			}
			else if (self.itemMediaType() == "AUDIO") {
				if (typeof exhibitionItem.contextData.body.mediaUrl !== 'undefined') {
					self.itemAudioUrl(exhibitionItem.contextData.body.mediaUrl());
				}
			}
			if (typeof exhibitionItem.contextData.body.text !== 'undefined' &&  typeof exhibitionItem.contextData.body.text.default !== 'undefined') {
				self.itemText(exhibitionItem.contextData.body.text.default());
				self.itemTextPosition(self.textPositionToBoolean(exhibitionItem.contextData.body.textPosition()));
			}
			if (typeof exhibitionItem.contextData.body.mediaDescription !== 'undefined') {
				self.itemMediaDescription(exhibitionItem.contextData.body.mediaDescription());
			}
			self.itemId(exhibitionItem.dbId());
			$('.action').removeClass('active');
			$('.action.editvideo').addClass('active');
		};
		
		self.editItem = function (editMode) {
			console.log(self.validationModelMediaDescription.isValid() + " " + self.itemMediaDescription());
			if (self.itemMediaType() == "" || (self.itemMediaType() == "VIDEO" && ((self.itemVideoUrl() == "" || self.validationModelVideo.isValid()))) ||
					(self.itemMediaType() == "AUDIO" && ((self.itemAudioUrl() == "" || self.validationModelAudio.isValid()))) 
					&& self.validationModelMediaDescription.isValid()) {
				var mediaUrl;
				if (self.itemMediaType() == "VIDEO") {
					var itemEmbeddedVideoUrl = "";
					var urlMatch = self.itemVideoUrl().match(/youtube\.com.*(\?v=|\/embed\/)(.{11})/);
					if (urlMatch !== null) {
						var youtube_video_id = urlMatch.pop();
						itemEmbeddedVideoUrl = 'https://www.youtube.com/embed/' + youtube_video_id;	
					}
					mediaUrl = itemEmbeddedVideoUrl;
				}
				else if (self.itemMediaType() == "AUDIO")
					mediaUrl = self.itemAudioUrl();
				var promise = self.updateRecord(self.itemId(), self.itemText(), self.textPositionToString(self.itemTextPosition()), self.itemMediaType(), mediaUrl, self.itemMediaDescription(), self.dbId(), self.itemPosition());
				$.when(promise).done(function (data) {
					if (typeof self.collectionItemsArray()[self.itemPosition()].contextData.body.text === 'undefined'
						&& typeof self.collectionItemsArray()[self.itemPosition()].contextData.body.mediaUrl === 'undefined') {
						self.collectionItemsArray()[self.itemPosition()].contextData.body = {
							text: {
								default: ko.observable("")
							},
							mediaUrl : ko.observable(""),
							mediaDescription : ko.observable(""),
							meduaType: ko.observable(null)
						};
					}
					if (editMode == "editText") {
						self.collectionItemsArray()[self.itemPosition()].contextData.body.text.default(self.itemText());
						self.collectionItemsArray()[self.itemPosition()].contextData.body.textPosition(self.textPositionToString(self.itemTextPosition()));
					} else if (editMode == "editMedia") {
						self.collectionItemsArray()[self.itemPosition()].contextData.body.mediaType(self.itemMediaType());
						self.collectionItemsArray()[self.itemPosition()].contextData.body.mediaDescription(self.itemMediaDescription());
						if (self.itemMediaType() == "VIDEO")
							self.collectionItemsArray()[self.itemPosition()].contextData.body.mediaUrl(self.itemVideoUrl());
						else if (self.itemMediaType() == "AUDIO")
							self.collectionItemsArray()[self.itemPosition()].contextData.body.mediaUrl(self.itemAudioUrl());
					}
					self.closeSideBar();
				}).fail(function (data) {
					// alert('text edit failed');
					self.closeSideBar();
				});
			} else {
				self.validationModelMediaDescription.errors.showAllMessages();
				self.validationModelVideo.errors.showAllMessages();
				self.validationModelAudio.errors.showAllMessages();
			}
		};
		
		self.deleteMedia = function(exhibitionItem, event) {
			var context = ko.contextFor(event.target);
			var index = context.$index();
			var promise = self.updateRecord(exhibitionItem.dbId(), self.itemText(), self.textPositionToString(self.itemTextPosition()), null, "", "", self.dbId(), index);
			$.when(promise).done(function (data) {
				self.collectionItemsArray()[index].contextData.body.mediaUrl("");
				self.collectionItemsArray()[index].contextData.body.mediaDescription("");
				self.collectionItemsArray()[index].contextData.body.mediaType(null);
			});
		}
		
		self.closeSideBar = function () {
			self.itemPosition(-1);
			self.itemText("");
			self.itemTextPosition(true);
			self.itemVideoUrl("");
			self.itemAudioUrl("");
			self.itemMediaDescription("");
			self.itemId("");
			self.itemMediaType("");
			self.newBackgroundImg(false);
			// $('textarea').hide();
			// $('.add').show();
			$('.action').removeClass('active');
		};
		
		self.updateRecord = function(dbId, text, textPosition, mediaType, mediaUrl, mediaDescription, colId, position) {
			var jsonData = {};
			jsonData = {
				"contextDataType": "ExhibitionData",
				"target": {
					"recordId": dbId,
					"position": position
				},
				"body" : {
					"text": {"default": text},
					"textPosition": textPosition,
					"mediaUrl": mediaUrl,
					"mediaDescription": mediaDescription,
					"mediaType": mediaType
				}
			};
			jsonData = JSON.stringify(jsonData);
			return $.ajax({
				"url": "/record/contextData?collectionId="+colId,
				"method": "put",
				"contentType": "application/json",
				"data": jsonData,
				"success": function (data) {
				},
				"error": function (result) {
				}
			});
		}

		self.playExhibition = function () {
			window.location.hash = '#exhibitionview/' + self.dbId();
		};
		
		$("#searchBox").keyup(function(event){
	        if (event.keyCode == 13) {
	        	self.searchPage=1;
	        	self.search();
	        }
	    });
		
		self.search = function() {
			$.ajax({
				"url": "/api/advancedsearch",
				"method": "post",
				"contentType": "application/json",
				"data": JSON.stringify({
					searchTerm: $("#searchBox").val(),
					page: self.searchPage,
					pageSize:20,
				    source:["WITHin"],
				    filters:[]
				}),
				"success": function(reply) {
					if (reply.responses && reply.responses[0] && reply.responses[0].items) {
						var records = reply.responses[0].items.culturalCHO;
						if (self.searchPage > 1) {
							$.each(records, function( index, value ) {
								var exhibitionRecord = ko.mapping.fromJS(value, self.mapping);
								self.userSavedItemsArray.push(exhibitionRecord);
							});
						}
						else if (self.searchPage == 1) {
							ko.mapping.fromJS(records, self.mapping, self.userSavedItemsArray);
	
						}
					}
				}
			});
		}
		
		self.saveExhBackrImg = function() {
			if (self.newBackgroundImg()) {
				var jsonData = {
					descriptiveData: {
						backgroundImg: {
								Original: {
									url: self.backgroundImg()
								},
								Thumbnail: {
									url: self.thumbnailBIUrl
								}
						}
					},
					resourceType: "Exhibition"
				};
				return $.ajax({
					type: "PUT",
					url: "/collection/" + self.dbId(),
					data: JSON.stringify(jsonData),
					contentType: "application/json",
					success: function () {
						self.closeSideBar();
					}
				});
			}
			// updateExhibitionProperty(self, "descriptiveData.backgroundImg",
			// self.backgroundImg());
		}
		self.editCredits = function() {
			$('.action').removeClass('active');
			$('.action.creditsection').addClass('active');
			var jsonData = {
				descriptiveData: {
					credits: self.credits()
				},
				resourceType: "Exhibition"
			};
			$.ajax({
				type: "PUT",
				url: "/collection/" + self.dbId(),
				data: JSON.stringify(jsonData),
				contentType: "application/json",
				success: function () {
					self.closeSideBar();
				}
			});
		}
		
		self.deleteExhibition = function() {
			// ko.contextFor(mycollections).$data.deleteMyCollection(exhibition);
			// copy-pasted here from mycollections!!!
			self.showDelCollPopup(self.title(), self.dbId());
			self.closeSideBar();
		}
		
		// copy-pasted from mycollections!!!
		self.showDelCollPopup = function(collectionTitle, collectionId) {
			var myself = this;
			myself.id = collectionId;
			$.smkConfirm({
				text: "All records in " + collectionTitle + " will be deleted. Are you sure?",
				accept: 'Delete',
				cancel: 'Cancel'
			}, function (ee) {
				if (ee) {
					deleteCollection(myself.id);
				}
			});
		};
		
		deleteCollection = function(collectionId) {
			$.ajax({
				"url": "/collection/" + collectionId,
				"method": "DELETE",
				success: function (result) {
					$.smkAlert({text: 'Exhibition removed', type: 'success'});
					// redirect to myCollections
					window.location.href = "#mycollections";
				}
			});
		};
		
		self.selectMediaType = function(selectedMediaType) {
			// self.selectedMediaType(selectedMediaType);
			if (selectedMediaType == "VIDEO") {
				
			}
			else if (selectedMediaType == "AUDIO") {
				
			}
		}
		
		self.bindFileUpload = function() {
			$('.action').removeClass('active');
			$('.action.upload').addClass('active');				
			$('#mediaupload').fileupload({
				type: "POST",
				url: '/media/create',
				success: function (data, textStatus, jqXHR) {
					self.backgroundImg(data.original);
					self.newBackgroundImg(true);
					// self.mediumBIUrl(data.medium);
					self.thumbnailBIUrl = data.thumbnail;
					// self.squareBIUrl(data.square);
					// self.tinyBIUrl(data.tiny);
				},
				error: function (e, data) {
					console.log(data);
					$.smkAlert({ text: 'Error uploading the file', type: 'danger', time: 10});
				}
			});
		}
		
		var _draggedItem;
		var _bIsMoveOperation = false;
		
		ko.bindingHandlers.drag = {
			init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
				var dragElement = $(element);
				var dragOptions = {
					start: function (event, ui) {
						$('#collscroll').css({
							"overflow": 'visible'
						});
						$('.bottom-box').removeClass("box-Hover");
						
						_draggedItem = ko.utils.unwrapObservable(valueAccessor().item);
						_bIsMoveOperation = ko.utils.unwrapObservable(valueAccessor().move);
						ui.helper.css({
							"z-index": 500
						});
						if (_bIsMoveOperation) {
							if (ui.helper.width() > 100) {
								var newAspectHeight = 100 / ui.helper.width() * ui.helper.height();
								ui.helper.css({
									"width": 100
								});
								ui.helper.css({
									"height": newAspectHeight
								});
								ui.helper.css({
									opacity: 0.8
								});
							}
						}
					},
					stop: function (event, ui) {
						$("#collscroll").css({
							overflow: "scroll"
						});
						$("#collscroll").css({
							"overflow-y": "hidden"
						});
						// un comment below statement
						// $('.outer').autoscroll('destroy');
						_draggedItem = undefined;
					}
				};
				if (dragElement.hasClass('box fill')) { // if item selected from
														// exhibitions
					// dragOptions.appendTo = $('.left');
					dragOptions.helper = function () {
						var $imageElementHelper = $(this).find('#itemImage').clone();
						$imageElementHelper.css('margin', 0).css({
							'padding-top': 0
						});
						// to fix the scroll issues remove it from div
						return $imageElementHelper;
					};
					dragOptions.cursorAt = {
						top: 0,
						left: 75
					};
					dragOptions.cursor = "move";
					dragOptions.revert = false;
				} else {
					dragOptions.helper = 'clone';
					dragOptions.cursor = 'pointer';
					dragOptions.revert = 'invalid';
				}
				dragElement.draggable(dragOptions).disableSelection();
			}
		};

		ko.bindingHandlers.drop = {
			init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
				var dropElement = $(element);
				var dropOptions = {
					hoverClass: "drop-hover",
					tolerance: "intersect",
					over: function (event, ui) {
						if (!_bIsMoveOperation) {
							dropElement.animate({
								width: "150px"
							}, 200);
						}
					},
					out: function (event, ui) {
						dropElement.animate({
							width: "60px"
						}, 200);
					},
					drop: function (event, ui) {
						var indexNewItem = ko.utils.unwrapObservable(valueAccessor().index);
						var newItem = ko.mapping.fromJS(_draggedItem, self.mapping);
						if (newItem.fullres().length==0) {
							newItem.fullres(newItem.thumbnailUrl());
							$.smkAlert({
								text: 'Full resolution image not found! Thumbnail image will be used which might be unsuitable for exhibition.',
								type: 'danger',
								permanent: true
							});
						}
						// newItem.contextData()[0].target.position(indexNewItem);
						var indexDraggedItem = self.collectionItemsArray.indexOf(_draggedItem);
						_startIndex = indexDraggedItem;
						// don't do anything if it is moved to its direct left
						// or right dashed box
						if (_bIsMoveOperation) {// moved from exhibition itself
							if (indexDraggedItem == indexNewItem || (indexDraggedItem + 1) == indexNewItem) {
								_draggedItem = undefined;
								return;
							}
						}
						dropElement.animate({
							width: "60px"
						}, 200);
						/*
						 * dropElement.find('#droppable-Children').css({
						 * display: "none" });
						 */
						self.creationMode=false;
						if (_bIsMoveOperation) {
							_removeItem = false;
							self.collectionItemsArray.splice(indexDraggedItem, 1);
						}
						self.collectionItemsArray.splice(indexNewItem, 0, newItem);
						_draggedItem = undefined;
						WITHApp.initTooltip();
						WITHApp.tabAction();
					}
				};
				dropElement.droppable(dropOptions);
			}
		};
		
		ko.bindingHandlers.dropBackgroundImg = {
				init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
					var dropElement = $(element);
					var dropOptions = {
						drop: function (event, ui) {
							console.log(_draggedItem);
							var newItem = ko.mapping.fromJS(_draggedItem, self.mapping);
							if (newItem.fullres().length==0) {
								$.smkAlert({
									text: 'Full resolution image not found! Thumbnail image will be used which might be unsuitable for exhibition.',
									type: 'danger',
									permanent: true
								});
								self.backgroundImg(newItem.thumbnailUrl());
							}
							else {
								self.backgroundImg(newItem.fullres());								
							}
							if (newItem.thumbnail)
								self.thumbnailBIUrl = newItem.thumbnail();
							else {
								self.thumbnailBIUrl = newItem.fullres();
							}
							self.newBackgroundImg(true);
						}
					};
					dropElement.droppable(dropOptions);
				}	
		};

		ko.bindingHandlers.hscroll = {
			updating: true,
			init: function (element, valueAccessor, allBindingsAccessor) {
				var self = this;
				self.updating = true;
				var parentContainer = element.parentElement;
				ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
					$(parentContainer).off("scroll.ko.scrollHandler");
					self.updating = false;
				});
			},
			update: function (element, valueAccessor, allBindingsAccessor) {
				var props = allBindingsAccessor().scrollOptions;
				var offset = props.offset ? props.offset : "0";
				var loadFunc = props.loadFunc;
				var load = ko.utils.unwrapObservable(valueAccessor());
				var self = this;
				if (load) {
					$(element).on("scroll.ko.scrollHandler", function () {
						var parentContainer = element.parentElement;
						if (parentContainer.scrollWidth - (parentContainer.scrollLeft + parentContainer.offsetWidth) < 150) {
							if (self.updating) {
								loadFunc();
								// self.updating = false;
							}
						} else {
							self.updating = true;
						}
					});
				} else {
					element.style.display = "none";
					$(parentContainer).off("scroll.ko.scrollHandler");
					self.updating = false;
				}
			}
		};
		
		ko.bindingHandlers.hscroll2 = {
				updating: true,
				init: function (element, valueAccessor, allBindingsAccessor) {
					var self = this;
					self.updating = true;
					var parentContainer = element.parentElement;
					ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
						$(parentContainer).off("scroll.ko.scrollHandler");
						self.updating = false;
					});
				},
				update: function (element, valueAccessor, allBindingsAccessor) {
					var props = allBindingsAccessor().scrollOptions;
					var offset = props.offset ? props.offset : "0";
					var loadFunc = props.loadFunc;
					var load = ko.utils.unwrapObservable(valueAccessor());
					var self = this;
					if (load) {
						$(element).on("scroll.ko.scrollHandler", function () {
							var parentContainer = element;
							if (parentContainer.scrollWidth - (parentContainer.scrollLeft + parentContainer.offsetWidth) < 150) {
								if (self.updating) {
									loadFunc();
								}
							} else {
								self.updating = true;
							}
						});
					} else {
						element.style.display = "none";
						$(parentContainer).off("scroll.ko.scrollHandler");
						self.updating = false;
					}
				}
			};

		self.loadNextCollection = function () {
			self.moreItems(false);
		};

		self.loadNextExhibition = function () {
			self.moreItems(true);
		};

		self.moreItems = function (isForExhibition) {
			if (self.searchPage > 0) {// items in collectionItemsArray are
										// search results
				if (!isForExhibition) {
					self.searchPage++;
					self.search();
				}
			}
			else {
				if (isForExhibition)
					count = exhibitionItemCount;
				else
					count = collectionItemCount;
				if (self.loading === true) {
					setTimeout(self.moreItems(), 300);
				}
				if (self.loading() === false) {
					self.loading(true);
					var offset;
					var collectionID;
					if (isForExhibition) {
						offset = self.collectionItemsArray().length;
						collectionID = self.dbId();
					} else {
						offset = self.userSavedItemsArray().length;
						collectionID = self.selectedCollection().dbId;
					}
					$.ajax({
						"url": "/collection/" + collectionID + "/list?count="+count+"&start=" + offset,
						"method": "get",
						"contentType": "application/json",
						"success": function (data) {
							if (isForExhibition) {
								self.loadingInitialItemsCount = self.loadingInitialItemsCount + data.records.length;
								$.each(data.records, function( index, value ) {
									var exhibitionRecord = ko.mapping.fromJS(value, self.mapping);
									self.collectionItemsArray.push(exhibitionRecord);
								});
							} else {
								$.each(data.records, function( index, value ) {
									var exhibitionRecord = ko.mapping.fromJS(value, self.mapping);
									self.userSavedItemsArray.push(exhibitionRecord);
								});
							}
							self.loading(false);
							WITHApp.initTooltip();
							WITHApp.tabAction();
						},
						"error": function (result) {
							self.loading(false);
						}
					});
				}
			}
		};
		
		
		$('.leftscr').droppable({
			tolerance: "touch",
			over: function (event, ui) {
				$('.scroll').autoscroll({
					direction: 'left',
					step: 1000,
					scroll: true
				});
			},
			out: function (event, ui) {
				$('.scroll').autoscroll('destroy');
			}
		});

		$('.rightscr').droppable({
			tolerance: "touch",
			over: function (event, ui) {
				$('.scroll').autoscroll({
					direction: 'right',
					step: 1000,
					scroll: true
				});
			},
			out: function (event, ui) {
				$('.scroll').autoscroll('destroy');
			}
		});

		// fix for hover issue
		self.showXbutton = function (data, event) {
			$('#bottom-box').removeClass('box-Hover');
			$(event.target).addClass('box-Hover');
		};

		// hide the nav bar
		$('#bottomBar').fadeOut(500);
	};
	
	return {
		viewModel: ExhibitionEditModel,
		template: template
	};
});
