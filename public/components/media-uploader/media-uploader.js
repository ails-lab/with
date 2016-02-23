define(['knockout', 'text!./image-upload.html', 'app', 'knockout-validation', 'jquery.fileupload', 'smoke'], function (ko, template, app) {

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	function MediaUploaderModel(params) {
		var self = this;
		self.title = ko.observable().extend({
			required: true
		});
		self.description = ko.observable().extend();
		self.imageURL = ko.observable();
		self.collectionId = ko.observable(params.collectionId);

		self.originalUrl = ko.observable();
		self.mediumUrl = ko.observable();
		self.thumbnailUrl = ko.observable();
		self.squareUrl = ko.observable();
		self.tinyUrl = ko.observable();

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
		self.enumRights = ko.pureComputed(function() {
			return self.withRights().substring(0, self.withRights().indexOf(' '));
		});

		$('#imageupload').fileupload({
			type: "POST",
			url: '/media/create',
			progressall: function (e, data) {
				var progress = parseInt(data.loaded / data.total * 100, 10);
				$('#progress .progress-bar').css('width', progress + '%');
			},
			done: function (e, data) {
				if (data.files && data.files[0]) {
					var reader = new FileReader();
					reader.onload = function (e) {
						self.resizePhoto(e.target.result, 200);
						$('#progress .progress-bar').css('display', 'none');
					};
					reader.readAsDataURL(data.files[0]);
				}
				self.originalUrl(data.result.original);
				self.mediumUrl(data.result.medium);
				self.thumbnailUrl(data.result.thumbnail);
				self.squareUrl(data.result.square);
				self.tinyUrl(data.result.tiny);
			},
			error: function (e, data) {
				console.log(e);
				console.log(data);
				$.smkAlert({text:'Error uploading the file', type:'danger', time: 10});
			}
		});

		self.close = function () {
			app.closePopup();
		};

		self.uploadImage = function () {
			var data = {
				provenance: [{
					provider: 'UploadedByUser'
				}],
				descriptiveData: {
					label: self.title(),
					description: self.description()
				},
				media: [{
					Original: {
						url: this.originalUrl(),
						withRights: this.enumRights()
					},
					Medium: {
						url: this.mediumUrl(),
						withRights: this.enumRights()
					},
					Thumbnail: {
						url: this.thumbnailUrl(),
						withRights: this.enumRights()
					},
					Square: {
						url: this.squareUrl(),
						withRights: this.enumRights()
					},
					Tiny: {
						url: this.tinyUrl(),
						withRights: this.enumRights()
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
					$.smkAlert({text: 'Error adding the item to the collection!', type:'danger', time: 10});
				}
			});
		};

		self.resizePhoto = function (src, width, height) {
			var img = new Image();
			img.onload = function () {
				var canvas = document.createElement('canvas');
				var ctx = canvas.getContext("2d");
				ctx.drawImage(img, 0, 0);

				height = typeof height !== 'undefined' ? height : -1;

				if (height < 0) {
					if (img.width > img.height) {
						height = (img.height / img.width) * width;
					} else {
						width = (img.width / img.height) * height;
					}
				}

				canvas.width = width;
				canvas.height = height;
				ctx = canvas.getContext("2d");
				ctx.drawImage(img, 0, 0, width, height);

				var dataurl = canvas.toDataURL("image/png");

				self.imageURL(dataurl);
			};
			img.src = src;
		};

	}

	return {
		viewModel: MediaUploaderModel,
		template: template
	};
});
