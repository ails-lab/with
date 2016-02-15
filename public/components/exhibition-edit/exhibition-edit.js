define(['knockout', 'text!./exhibition-edit.html', 'jquery.ui', 'autoscroll', 'app', 'bootstrap-select', 'bootstrap-switch', 'jquery.lazyload'], function (ko, template, jqueryUI, autoscroll, app, bootstrapSelect, bootstrapSwitch, jqueryLazyLoad) {

	function setUpSwitch(exhibition) {
		console.log(exhibition);
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
		this.firstEntries = collectionData.firstEntries;
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
		/*console.log('save item to index :' + position + 'record : '+ record);*/
		var jsonData = JSON.stringify({
			source: record.source,
			sourceId: record.sourceId,
			title: record.title,
			description: record.description,
			thumbnailUrl: record.thumbnailUrl,
			sourceUrl: record.sourceUrl,
			position: position
		});
		return $.ajax({
			"url": "/collection/" + exhibitionId + "/addRecord",
			"method": "post",
			"contentType": "application/json",
			"data": jsonData,
			"success": function (data) {
                // Empty
			},
			"error": function (result) {
                // Empty
			}
		});
	}

	function deleteItemFromExhibition(exhibitionId, recordId) {
		$.ajax({

			"url": "/collection/" + exhibitionId + "/removeRecord" + "?recId=" + recordId,
			"method": "delete",
			"success": function (data) {
                // Empty
			},
			"error": function (result) {
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
		if (!self.creationMode) {
			self.dbId(params.id);
		}
		self.userSavedItemsArray = ko.observableArray([]); //holds the selected collections items
		self.collectionItemsArray = ko.observableArray([]); //holds the exhibitions items

		var collections = [];
		var promise = app.getAllUserCollections();
		self.myCollections = ko.observableArray([]); //holds all the collections
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
			create: function(options) {
				var vm = ko.mapping.fromJS(options.data);
				return vm;
			},
			'dbId': {
				key: function (data) {
					return ko.utils.unwrapObservable(data.dbId);
				}
			},
			'firstEntries': {
				key: function (options) {
					return ko.utils.unwrapObservable(data.dbId);
				}
			},
			'copy': ["firstEntries"]
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
				if (self.title().indexOf('Dummy') !== -1) {
					self.title('');
				}
				if (self.description().indexOf('Description') !== -1) {
					self.description('');
				}
				setUpSwitch(self);

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
				self.loadingExhibitionItems = true;
			});
		}

		self.selectedCollection = ko.observable();
		self.currentItemSet = ko.observable(false);
		self.currentItem = ko.observable();
		self.selectedCollection.subscribe(function (newCollection) {
			self.userSavedItemsArray(newCollection.firstEntries);
// TODO: Update Code
			// self.currentItem(self.userSavedItemsArray()[0]);
			// if (self.userSavedItemsArray().length > 0) {
			// 	self.currentItemSet(true);
			// }
		});

		self.selectedCollection.subscribe(function (newCollection) {
			self.userSavedItemsArray(newCollection.firstEntries);
// TODO: Update Code
			// self.currentItem(self.userSavedItemsArray()[0]);
			// if (self.userSavedItemsArray().length > 0) {
			// 	self.currentItemSet(true);
			// }
		});

		showViewModel = function () {
			console.log(ko.toJSON(self));
			console.log(self.myCollections());
		};

		self.textfieldsLostFocus = function (isTitleUpdate) {
			updateExhibition(self, isTitleUpdate);
		};

		self.xButtonClicked = function (item) {
			if (self.collectionItemsArray.indexOf(item) > -1) {
				self.collectionItemsArray.remove(item);
				deleteItemFromExhibition(self.dbId(), item.dbId);
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

				var promiseSaveItem = saveItemToExhibition(record, index, self.dbId());
				$.when(promiseSaveItem).done(function (data) {
					//now delete not to affect server order
					if (_bIsMoveOperation) {
						deleteItemFromExhibition(self.dbId(), record.dbId);
					}
					//update the dbid
					record.dbId = data.dbId;
					$(elem).find('#loadingIcon').fadeOut();
				}).fail(function (data) {
					$(elem).find('#loadingIcon').fadeOut();
				});
			}
		};

		self.removeItem = function (elem) {
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
						newItem.additionalText = ko.observable('');
						newItem.containsText = ko.observable(false); //add the observables here
						newItem.containsVideo = ko.observable(false);
						newItem.videoUrl = ko.observable('');
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