define(['knockout', 'text!./_image-upload.html', 'app', 'knockout-validation', 'jquery.fileupload', 'smoke'], function (ko, template, app) {

	ko.validation.init({
		errorElementClass: 'error',
		errorMessageClass: 'errormsg',
		decorateInputElement: true
	});

	function MediaUploaderModel(params) {
		var self = this;
		self.title = ko.observable('').extend({
			required: true
		});
		self.description = ko.observable();
		self.dataProvider = ko.observable().extend();
		self.collectionId = ko.observable(params.collectionId);

		self.originalUrl = ko.observable();
		self.mediumUrl = ko.observable();
		self.thumbnailUrl = ko.observable().extend({
			required: {
				message: "No image was uploaded.",
				params: true
			}
		});
		self.squareUrl = ko.observable();
		self.tinyUrl = ko.observable();
		self.validationModel = ko.validatedObservable({
			title: self.title,
			imageURL: self.imageURL
		});

		self.rights = ko.observableArray([
			'Public ("Attribution Alone")',
			'Restricted ("Restricted")',
			'Permission ("Permission")',
			'Modify ("Allow re-use and modifications")',
			'Commercial ("Allow re-use for commercial")',
			'Creative_Commercial_Modify ("use for commercial purposes modify, adapt, or build upon")',
			'Creative_Not_Commercial ("NOT Comercial")',
			'Creative_Not_Modify ("NOT Modify")',
			'Creative_Not_Commercial_Modify ("not modify, adapt, or build upon, not for commercial purposes")',
			'Creative_SA ("share alike")',
			'Creative_BY ("use by attribution")',
			'Creative ("Allow re-use")',
			'RR ("Rights Reserved")',
			'RRPA ("Rights Reserved - Paid Access")',
			'RRRA ("Rights Reserved - Restricted Access")',
			'RRFA ("Rights Reserved - Free Access")',
			'UNKNOWN ("Unknown")'
		]);
		self.withRights = ko.observable('UNKNOWN ("Unknown")');
		self.enumRights = ko.pureComputed(function () {
			return self.withRights().substring(0, self.withRights().indexOf(' '));
		});
		self.displayImage = ko.pureComputed(function () {
			if (self.mediumUrl()) {
				return self.mediumUrl();
			} else {
				return 'img/ui/upload-placeholder.png';
			}
		});

		$('#mediaupload').fileupload({
			type: "POST",
			url: '/media/create',
			success: function (data, textStatus, jqXHR) {
				self.originalUrl(data.original);
				self.mediumUrl(data.medium);
				self.thumbnailUrl(data.thumbnail);
				self.squareUrl(data.square);
				self.tinyUrl(data.tiny);
			},
			error: function (e, data) {
				console.log(e);
				console.log(data);
				$.smkAlert({ text: 'Error uploading the file', type: 'danger', time: 10});
			}
		});

		self.close = function () {
			self.title('');
			self.description('');
			self.dataProvider('');
			//self.description = ko.observable('');
			//self.dataProvider = ko.observable('').extend();

			self.originalUrl = ko.observable();
			//self.mediumUrl = ko.observable('');
			self.mediumUrl('');
			self.thumbnailUrl = ko.observable();
			self.squareUrl = ko.observable();
			self.tinyUrl = ko.observable();

			self.validationModel.errors.showAllMessages(false);
			$('.action').removeClass('active');
		};

		self.uploadImage = function () {
			if (self.validationModel.isValid()) {
				var data = {
					provenance: [{ provider: self.dataProvider() },
						{ provider: app.currentUser.username() },
						{ provider: 'UploadedByUser' }
					],
					descriptiveData: {
						label: { "default": [self.title()] },
						description: {"default": [self.description()]}
					},
					media: [{
							Original: {
								url: this.originalUrl(),
								withRights: this.enumRights(),
								originalRights: {"uri": this.enumRights()}
							},
							Medium: {
								url: this.mediumUrl(),
								withRights: this.enumRights(),
								originalRights: {"uri": this.enumRights()}
							},
							Thumbnail: {
								url: this.thumbnailUrl(),
								withRights: this.enumRights(),
								originalRights: {"uri": this.enumRights()}
							},
							Square: {
								url: this.squareUrl(),
								withRights: this.enumRights(),
								originalRights: {"uri": this.enumRights()}
							},
							Tiny: {
								url: this.tinyUrl(),
								withRights: this.enumRights(),
								originalRights: {"uri": this.enumRights()}
							}
						}]
				};
				$.ajax({
					url: '/collection/' + self.collectionId() + '/addRecord',
					method: 'POST',
					data: JSON.stringify(data),
					contentType: 'application/json',
					success: function (data) {
						self.close();
						$.smkAlert({text: 'Item added to the collection', type: 'success'});
						ko.contextFor(withcollection).$data.loadNext();
						ko.contextFor(withcollection).$data.reloadEntryCount();
					},
					error: function (result) {
						$.smkAlert({text: 'Error adding the item to the collection!', type: 'danger', time: 10});
					}
				});
			} else {
				self.validationModel.errors.showAllMessages();
			}
		};

		// self.resizePhoto = function (src, width, height) {
		// 	var img = new Image();
		// 	img.onload = function () {
		// 		var canvas = document.createElement('canvas');
		// 		var ctx = canvas.getContext("2d");
		// 		ctx.drawImage(img, 0, 0);
		//
		// 		height = typeof height !== 'undefined' ? height : -1;
		//
		// 		if (height < 0) {
		// 			if (img.width > img.height) {
		// 				height = (img.height / img.width) * width;
		// 			} else {
		// 				width = (img.width / img.height) * height;
		// 			}
		// 		}
		//
		// 		canvas.width = width;
		// 		canvas.height = height;
		// 		ctx = canvas.getContext("2d");
		// 		ctx.drawImage(img, 0, 0, width, height);
		//
		// 		var dataurl = canvas.toDataURL("image/png");
		//
		// 		console.log(dataurl);
		//
		// 		self.imageURL(dataurl);
		// 	};
		// 	img.src = src;
		// };

	}

	return {
		viewModel: MediaUploaderModel,
		template: template
	};
});
