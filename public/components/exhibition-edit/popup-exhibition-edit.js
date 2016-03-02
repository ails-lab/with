define(['knockout', 'text!./popup-exhibition-edit.html', 'app'], function (ko, template, app) {

	function updateRecord(dbId, text, videoUrl, colId, position) {

		var jsonData = {};
		jsonData = {
			"contextDataType": "ExhibitionData",
			"target": {
				"collectionId": colId,
				"position": position
			},
			"body" : {
				"text": {"default": text},
				"videoUrl": videoUrl
			}
		};
		jsonData = JSON.stringify(jsonData);
		return $.ajax({
			"url": "/record/contextData	",
			"method": "put",
			"contentType": "application/json",
			"data": jsonData,
			"success": function (data) {

			},
			"error": function (result) {

			}
		});
	}

	function ItemEditViewModel(params) {
		var self = this;
		self.route = params.route;
		self.title = ko.observable('');
		self.placeholder = ko.observable('');
		self.popUpMode = '';
		self.modeIsVideo = ko.observable(false);
		self.videoUrl = ko.observable('');
		self.videoIsSet = ko.observable(false);
		self.textInput = ko.observable('');
		self.primaryButtonTitle = ko.observable('check');
		self.cancelButtonTitle = ko.observable('cancel');
		self.exhibitionItem = {};
		self.exhibitionItemIsSet = ko.observable(false);
		self.isUpdating = ko.observable(false);
		self.colId = '';
		self.position = -1;

		editItem = function (exhibitionItem, colId, position, editMode) {
			//alert(JSON.stringify(exhibitionItem));
			self.colId = colId;
			self.position = position;
			self.exhibitionItem = exhibitionItem;
			self.exhibitionItemIsSet(true);
			self.videoIsSet(false);
			self.setUpPopUp(exhibitionItem, colId, position, editMode);
			$('#myModal').modal('show');
		};

		self.setUpPopUp = function (exhibitionItem, colId, position, popUpMode) {
			self.popUpMode = popUpMode;
			if (self.popUpMode === 'PopUpVideoMode') {
				self.modeIsVideo(true);
				self.videoUrl(exhibitionItem.contextData()[0].body.videoUrl());
				self.title('Add a youtube video');
				self.placeholder('Enter youtube video url');
				if (self.exhibitionItem.containsVideo()) {
					self.videoIsSet(true);
					self.cancelButtonTitle('delete');
					self.primaryButtonTitle('save');
				} else {
					self.cancelButtonTitle('cancel');
					self.primaryButtonTitle('embed');
				}
			} else {
				self.modeIsVideo(false);
				self.title('Add text');
				self.placeholder('');
				self.textInput(exhibitionItem.contextData()[0].body.text.default());
				self.primaryButtonTitle('save');
				if (self.exhibitionItem.containsText()) {
					self.cancelButtonTitle('delete');
				} else {
					self.cancelButtonTitle('cancel');
				}
			}
		};

		//setup button actions
		self.rightButtonTapped = function () {
			var methodName = self.primaryButtonTitle();
			methodName = methodName + self.popUpMode;
			console.log(methodName);
			self[methodName]();
		};

		self.leftButtonTapped = function () {
			var methodName = self.cancelButtonTitle();
			methodName = methodName + self.popUpMode;
			self[methodName]();
		};

		//right button actions Video
		self.embedPopUpVideoMode = function () {
			if (self.videoUrl().match(/youtube\.com.*(\?v=|\/embed\/)(.{11})/) === null) {
				return;
			}
			self.videoIsSet(true);
			var youtube_video_id = self.videoUrl().match(/youtube\.com.*(\?v=|\/embed\/)(.{11})/).pop();
			var thumbnailPath = '//img.youtube.com/vi/' + youtube_video_id + '/0.jpg';
			var embeddedVideoPath = 'https://www.youtube.com/embed/' + youtube_video_id;

			self.videoUrl(embeddedVideoPath);
			self.primaryButtonTitle('save');
		};

		self.savePopUpVideoMode = function () {
			self.isUpdating(true);
			var promise = updateRecord(self.exhibitionItem.dbId, self.textInput(), self.videoUrl(), self.colId, self.position);
			$.when(promise).done(function (data) {
				self.exhibitionItem.contextData()[0].body.videoUrl(self.videoUrl());
				self.textInput('');
				self.videoUrl('');
			}).fail(function (data) {
				alert('video insertion failed');
			}).always(function (data) {
				self.isUpdating(false);
				$('#myModal').modal('hide');
			});
		};

		//left button actions
		self.cancelPopUpVideoMode = function () {
			self.videoUrl('');
			$('#myModal').modal('hide');
		};

		self.deletePopUpVideoMode = function () {
			self.isUpdating(true);
			var promise = updateRecord(self.exhibitionItem.dbId, self.textInput(), '', self.colId, self.position);
			$.when(promise).done(function (data) {
				self.exhibitionItem.contextData()[0].body.videoUrl('');
				self.videoUrl('');
			}).fail(function (data) {
				alert('deletion failed');
			}).always(function (data) {
				self.isUpdating(false);
				$('#myModal').modal('hide');
			});
		};

		//right button actions Text
		self.savePopUpTextMode = function () {
			var promise = updateRecord(self.exhibitionItem.dbId, self.textInput(), self.videoUrl(), self.colId, self.position);
			self.isUpdating(true);
			$.when(promise).done(function (data) {
				self.exhibitionItem.contextData()[0].body.text.default(self.textInput());
				self.textInput('');
				self.videoUrl('');
			}).fail(function (data) {
				alert('text insertion failed');
			}).always(function (data) {
				self.isUpdating(false);
				$('#myModal').modal('hide');
			});
		};

		//left button actions
		self.cancelPopUpTextMode = function () {
			self.textInput('');
			$('#myModal').modal('hide');
		};

		self.deletePopUpTextMode = function () {
			self.isUpdating(true);
			var promise = updateRecord(self.exhibitionItem.dbId, '', self.videoUrl(), self.colId, self.position);
			$.when(promise).done(function (data) {
				self.exhibitionItem.contextData()[0].body.text.default('');
				self.textInput('');
			}).fail(function (data) {
				alert('deletion failed');
			}).always(function (data) {
				self.isUpdating(false);
				$('#myModal').modal('hide');
			});
		};
	}

	return {
		viewModel: ItemEditViewModel,
		template: template
	};
});