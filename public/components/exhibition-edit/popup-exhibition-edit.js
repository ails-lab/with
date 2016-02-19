define(['knockout', 'text!./popup-exhibition-edit.html', 'app'], function (ko, template, app) {

	function updateRecord(dbId, text, videoUrl) {

		var jsonData = {};
		jsonData.exhibitionRecord = {
			"annotation": text,
			"videoUrl": videoUrl
		};
		jsonData = JSON.stringify(jsonData);
		//alert(jsonData);
		return $.ajax({
			"url": "/record/" + dbId,
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

		editItem = function (exhibitionItem, editMode) {
			//alert(JSON.stringify(exhibitionItem));
			self.exhibitionItem = exhibitionItem;
			self.exhibitionItemIsSet(true);
			self.videoIsSet(false);
			self.setUpPopUp(exhibitionItem, editMode);
			$('#myModal').modal('show');
		};

		self.setUpPopUp = function (exhibitionItem, popUpMode) {
			console.log(popUpMode);
			self.popUpMode = popUpMode;
			console.log('mode is video : ' + self.popUpMode === 'PopUpVideoMode');
			if (self.popUpMode === 'PopUpVideoMode') {
				self.modeIsVideo(true);
				self.videoUrl(exhibitionItem.videoUrl());
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
				self.textInput(exhibitionItem.additionalText());
				self.primaryButtonTitle('save');
				if (self.exhibitionItem.containsText()) {
					self.cancelButtonTitle('delete');
				} else {
					self.cancelButtonTitle('cancel');
				}
			}
		}

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
		}

		self.savePopUpVideoMode = function () {
			self.isUpdating(true);
			var promise = updateRecord(self.exhibitionItem.dbId, self.textInput(), self.videoUrl());
			$.when(promise).done(function (data) {
				self.exhibitionItem.containsVideo(true);
				self.exhibitionItem.videoUrl(self.videoUrl());
			}).fail(function (data) {

				alert('video insertion failed');
			}).always(function (data) {
				self.isUpdating(false);
				$('#myModal').modal('hide');
			});
		}

		//left button actions
		self.cancelPopUpVideoMode = function () {
			$('#myModal').modal('hide');
		}

		self.deletePopUpVideoMode = function () {
			self.isUpdating(true);
			var promise = updateRecord(self.exhibitionItem.dbId, self.textInput(), '');
			$.when(promise).done(function (data) {
				self.exhibitionItem.containsVideo(false);
				self.exhibitionItem.videoUrl('');
			}).fail(function (data) {
				alert('deletion failed');
			}).always(function (data) {
				self.isUpdating(false);
				$('#myModal').modal('hide');
			});
		}

		//right button actions Text
		self.savePopUpTextMode = function () {
			var promise = updateRecord(self.exhibitionItem.dbId, self.textInput(), self.videoUrl());
			self.isUpdating(true);
			$.when(promise).done(function (data) {
				self.exhibitionItem.additionalText(self.textInput());
				self.exhibitionItem.containsText(true);
			}).fail(function (data) {
				alert('text insertion failed');
			}).always(function (data) {
				self.isUpdating(false);
				$('#myModal').modal('hide');
			});
		}

		//left button actions
		self.cancelPopUpTextMode = function () {
			$('#myModal').modal('hide');
		}

		self.deletePopUpTextMode = function () {
			self.isUpdating(true);
			var promise = updateRecord(self.exhibitionItem.dbId, '', self.videoUrl());
			$.when(promise).done(function (data) {
				self.exhibitionItem.additionalText('');
				self.exhibitionItem.containsText(false);
			}).fail(function (data) {
				alert('deletion failed');
			}).always(function (data) {
				self.isUpdating(false);
				$('#myModal').modal('hide');
			});
		}
	}

	return {
		viewModel: ItemEditViewModel,
		template: template
	};
});