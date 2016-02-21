define(['knockout', 'text!./exhibition-edit.html', 'jquery.ui', 'autoscroll', 'app', 'bootstrap-select', 'bootstrap-switch', 'jquery.lazyload'], function (ko, template, jqueryUI, autoscroll, app, bootstrapSelect, bootstrapSwitch, jqueryLazyLoad) {

	function setUpSwitch(exhibition) {
		$("input.switch").bootstrapSwitch({
			onText: 'Public',
			offText: 'Restricted',
			size: 'small',
			onColor: 'success',
			offColor: 'danger',
			state: exhibition.administrative.access.isPublic()
		});
		$("input.switch").on('switchChange.bootstrapSwitch', function (event, state) {
			var promise = updateExhibitionProperty(exhibition, 'isPublic', state);;
			$.when(promise).done(function (data) {

			});
		});
	}

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

	function createExhibition() {
		return $.ajax({
			type: "POST",
			url: "/collection?collectionType=Exhibition",
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
					default: [ exhibition.title() ]
				}
			};
		} else {
			jsonObject.descriptiveData = {
				description: {
					default: [ exhibition.description() ]
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
		return $.ajax({
			"url": "/collection/" + exhibitionId + "/addRecord?position=" + position,
			"method": "post",
			"contentType": "application/json",
			"data": ko.toJSON(record),
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
			success: function(data) {
				// Empty
			},
			error: function(result) {
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
		self.checkLogged = function () {

			if (isLogged() == false) {
				window.location = '#login';
				return;
			}
		};

		self.checkLogged();
		self.loading = ko.observable(false);
		self.title = ko.observable('');
		self.description = ko.observable('');
		self.dbId = ko.observable();
		self.creationMode = !params.hasOwnProperty('id');
		self.loadingExhibitionItems = false;
		self.loadingInitialItemsCount = 0;
		var _removeItem = false;
		var _startIndex = -1;
		if (!self.creationMode) {
			self.dbId(params.id);
		}
		self.userSavedItemsArray = ko.observableArray([]); //holds the selected collections items
		self.collectionItemsArray = ko.observableArray([]); //holds the exhibitions items

		var collections = [];
		var promise = app.getAllUserCollections();
		self.myCollections = ko.mapping.fromJS([]); //ko.observableArray([]); //holds all the collections
		$.when(promise).done(function (data) {
			var collections = [];
			if (data.hasOwnProperty('collectionsOrExhibitions')) {
				collections = data['collectionsOrExhibitions'];
			}
			self.myCollections(ko.utils.arrayMap(collections, function (collectionData) {
				return new MyCollection(collectionData);
			}));

			//then initialise select
			$('.selectpicker').selectpicker();
		});

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

		if (self.creationMode) {
			$('.outer').css("visibility", "visible");
			var promiseCreateExhibtion = createExhibition();
			$.when(promiseCreateExhibtion).done(function (data) {
				ko.mapping.fromJS(data, mappingExhibition, self);
				setUpSwitch(self);
				self.title('');
				self.description('');
			});
		} else {
			self.loadingExhibitionItems = true;
			var promise = getExhibition(self.dbId());
			$.when(promise).done(function (data) {
				console.log(data);
				$('.outer').css("visibility", "visible");
				ko.mapping.fromJS(data, mappingExhibition, self);
				self.title(app.findByLang(data.descriptiveData.label));
				self.description(app.findByLang(data.descriptiveData.description));
				setUpSwitch(self);

				$.ajax({
					url: "/collection/" + self.dbId() + "/list?count=5&start=0",
					method: "get",
					success: function (data) {
						self.firstEntries = data.records;
						self.loadingInitialItemsCount = data.entryCount;
						self.firstEntries.map(function (record) {
							record.containsAudio = ko.pureComputed(function() {
								return record.contextData[0].body.audioUrl() !== "";
							});
							record.containsVideo = ko.pureComputed(function() {
								return record.contextData[0].body.videoUrl() !== "";
							});
							record.containsText = ko.pureComputed(function() {
								return record.contextData[0].body.text.default() !== "";
							});
						});

						self.collectionItemsArray(self.firstEntries);
						self.loadingExhibitionItems = true;
					},
					error: function (result) {
						// Empty
					}
				});


// TODO: Update Code
				// self.loadingInitialItemsCount = self.firstEntries.length;
				// self.firstEntries.map(function (record) { //fix for now till service gets implemented
				// 	record.additionalText = ko.observable('');
				// 	record.videoUrl = ko.observable('');
				// 	record.containsVideo = ko.observable(false);
				// 	record.containsText = ko.observable(false);
				// 	var exhibitionItemInfo = record.exhibitionRecord;
				// 	if (exhibitionItemInfo !== undefined) {
				// 		record.additionalText(exhibitionItemInfo.annotation);
				// 		record.videoUrl(exhibitionItemInfo.videoUrl);
				// 		record.containsVideo(!isEmpty(record.videoUrl()));
				// 		record.containsText(!isEmpty(record.additionalText()));
				// 	}
				// });
				// self.collectionItemsArray(self.firstEntries);
				// self.loadingExhibitionItems = true;
			});
		}

		self.selectedCollection = ko.observable();
		self.currentItemSet = ko.observable(false);
		self.currentItem = ko.observable();
		self.selectedCollection.subscribe(function (newCollection) {
			// Load the first 20 records when loading a different collection
			$.ajax({
				url: "/collection/" + newCollection.dbId + "/list?count=20&start=0",
				method: "get",
				success: function (data) {
					self.userSavedItemsArray(data.records);
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

		showViewModel = function () {
			console.log(ko.toJSON(self));
			console.log(self.myCollections());
		};

		self.textfieldsLostFocus = function (isTitleUpdate) {
			updateExhibition(self, isTitleUpdate);
		};

		self.xButtonClicked = function (item) {
			var index = self.collectionItemsArray.indexOf(item);
			if (index > -1) {
				self.collectionItemsArray.remove(item);
				deleteItemFromExhibition(self.dbId(), item.dbId, index);
			} else {
				self.userSavedItemsArray.remove(item);
			}
		};

		self.detailsEnabled = ko.observable(false);

		self.enableDetails = function (data, event) {
			self.currentItem(data);
			self.detailsEnabled(true);
			//fix hover
			$(event.target).parent().addClass('box-Hover');
		};

		self.disableDetails = function (data, event) {
			self.detailsEnabled(false);
			$(event.target).parent().removeClass('box-Hover');
		};

		self.itemsLoaded = 0;
		self.showNewItem = function (elem, index, record) {
			$(elem).hide().fadeIn(500);
			if ($(elem).hasClass('boxBasic boxItem')) { //it gets called for all elements rendered with the foreach
				if (!self.creationMode && self.itemsLoaded < self.loadingInitialItemsCount) {
					self.itemsLoaded++;
					$(elem).find('#loadingIcon').fadeOut();
					return;
				}

				if (_bIsMoveOperation) {
					if (_startIndex > 0) {
						moveItemInExhibition(self.dbId(), record.dbId, _startIndex, index);
						_startIndex = -1;		// Move was successful, reset the startIndex
					} else {
						saveItemToExhibition(record, index, self.dbId());
						_removeItem = true;		// Item was added but not removed, so mark it for removal
					}
					$(elem).find('#loadingIcon').fadeOut();
				} else {
					saveItemToExhibition(record, index, self.dbId());
					$(elem).find('#loadingIcon').fadeOut();
				}
			}
		};

		self.removeItem = function (elem, index, record) {
			if (_bIsMoveOperation) {
				_startIndex = index;	// If removeItem is executed before the showNewItem, save the startIndex for moving the record
				if (_removeItem) { 	// If removeItem is executed after the showNewItem, remove the item
					deleteItemFromExhibition(self.dbId(), record.dbId, index);
					_removeItem = false;
				}
			}
			$(elem).fadeOut(500);
		};

		self.showPopUpVideo = function (data) {
			editItem(data, 'PopUpVideoMode');
		};

		self.showPopUpText = function (data) {
			editItem(data, 'PopUpTextMode');
		};

		//custom binding
		var _draggedItem;
		var _bIsMoveOperation = false;
		ko.bindingHandlers.drag = {
			init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
				var dragElement = $(element);
				var dragOptions = {
					start: function (event, ui) {
						$('#outer-bottom').css({
							"overflow": 'visible'
						});
						$('.bottom-box').removeClass("box-Hover");
						_draggedItem = ko.utils.unwrapObservable(valueAccessor().item);
						_bIsMoveOperation = ko.utils.unwrapObservable(valueAccessor().move);
						ui.helper.css({
							"z-index": 500
						});
						if (_bIsMoveOperation) {
							if (ui.helper.width() > 150) {
								var newAspectHeight = 150 / ui.helper.width() * ui.helper.height();
								ui.helper.css({
									"width": 150
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
						$("#outer-bottom").css({
							overflow: "scroll"
						});
						$("#outer-bottom").css({
							"overflow-y": "hidden"
						});
						//un comment below statement
						//$('.outer').autoscroll('destroy');
						_draggedItem = undefined;
					}
				};
				if (dragElement.hasClass('boxBasic boxItem')) { //if it is move operation
					dragOptions.appendTo = $('.left');
					dragOptions.helper = function () {

						var $imageElementHelper = $(this).find('.itemImage').clone();
						$imageElementHelper.css('margin', 0).css({
							'padding-top': 0
						});

						//to fix the scroll issues remove it from div
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
							$(this).find('#droppable-Children').css({
								display: "block"
							});
							dropElement.animate({
								width: "150px"
							}, 200);
						}
					},
					out: function (event, ui) {
						$(this).find('#droppable-Children').css({
							display: "none"
						});
						dropElement.animate({
							width: "60px"
						}, 200);
					},
					drop: function (event, ui) {
						var indexNewItem = ko.utils.unwrapObservable(valueAccessor().index);
						//clone it
						var newItem = JSON.parse(JSON.stringify(_draggedItem));
						newItem.contextData = [{
							contextDataType: "ExhibitionData",
							body: {
								text: {
									default: ko.observable('')
								},
								videoUrl: ko.observable(''),
								audioUrl: ko.observable('')
							}
						}];
						newItem.containsAudio = ko.pureComputed(function() {
							return newItem.contextData[0].body.audioUrl() !== "";
						});
						newItem.containsVideo = ko.pureComputed(function() {
							return newItem.contextData[0].body.videoUrl() !== "";
						});
						newItem.containsText = ko.pureComputed(function() {
							return newItem.contextData[0].body.text.default() !== "";
						});
						// newItem.additionalText = ko.observable('');
						// newItem.containsText = ko.observable(false); //add the observables here
						// newItem.containsVideo = ko.observable(false);
						// newItem.videoUrl = ko.observable('');
						var arrayCollection = self.collectionItemsArray;
						//dont do anything if it is moved to its direct left or right dashed box
						if (_bIsMoveOperation) {
							var indexDraggedItem = arrayCollection.indexOf(_draggedItem);
							if (indexDraggedItem == indexNewItem || (indexDraggedItem + 1) == indexNewItem) {
								_draggedItem = undefined;
								return;
							}
						}
						dropElement.animate({
							width: "60px"
						}, 200);
						dropElement.find('#droppable-Children').css({
							display: "none"
						});
						arrayCollection.splice(indexNewItem, 0, newItem);
						if (_bIsMoveOperation) {
							arrayCollection.remove(_draggedItem);
						}
						_draggedItem = undefined;
					}
				};
				dropElement.droppable(dropOptions);
			}
		};

		ko.bindingHandlers.scroll = {
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
				var parentContainer = element.parentElement;
				if (load) {
					$(parentContainer).on("scroll.ko.scrollHandler", function () {
						var parentContainer = element.parentElement;
						//console.log('Is scrolling');
						//console.log('left: ' + parentContainer.scrollLeft + ' offset: ' + parentContainer.offsetWidth  + 'scrollview width' + parentContainer.scrollWidth);
						if (parentContainer.scrollWidth - (parentContainer.scrollLeft + parentContainer.offsetWidth) < 150) {
							if (self.updating) {
								loadFunc();
								self.updating = false;
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
					"url": "/collection/" + collectionID + "/list?count=20&start=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						console.log(data.itemCount);
						if (isForExhibition) {
							data.records.map(function (record) { //fix for now till service gets implemented
								record.additionalText = ko.observable('');
								record.videoUrl = ko.observable('');
								record.containsVideo = ko.observable(false);
								record.containsText = ko.observable(false);
								var exhibitionItemInfo = record.exhibitionRecord;
								if (exhibitionItemInfo !== undefined) {
									record.additionalText(exhibitionItemInfo.annotation);
									record.videoUrl(exhibitionItemInfo.videoUrl);
									record.containsVideo(!isEmpty(record.videoUrl()));
									record.containsText(!isEmpty(record.additionalText()));
								}
							});
							self.loadingInitialItemsCount = self.loadingInitialItemsCount + data.records.length;
							self.collectionItemsArray.push.apply(self.collectionItemsArray, data.records);
						} else {
							self.userSavedItemsArray.push.apply(self.userSavedItemsArray, data.records);
						}
						self.loading(false);
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
		};
		//----knockout solution----//

		//for the side scrolling
		$('.left').droppable({
			tolerance: "touch",
			over: function (event, ui) {
				$('.outer').autoscroll({
					direction: 'left',
					step: 1000,
					scroll: true
				});
			},
			out: function (event, ui) {
				$('.outer').autoscroll('destroy');
			}
		});

		$('.right').droppable({
			tolerance: "touch",
			over: function (event, ui) {
				$('.outer').autoscroll({
					direction: 'right',
					step: 1000,
					scroll: true
				});
			},
			out: function (event, ui) {
				$('.outer').autoscroll('destroy');
			}
		});

		//fix for hover issue
		self.showXbutton = function (data, event) {
			$('.bottom-box').removeClass('box-Hover');
			$(event.target).addClass('box-Hover');
		};

		//hide the nav bar
		$('#bottomBar').fadeOut(500);
	};

	return {
		viewModel: ExhibitionEditModel,
		template: template
	};
});