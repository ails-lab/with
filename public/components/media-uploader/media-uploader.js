define(['knockout', 'text!./image-upload.html', 'app', 'knockout-validation', 'jquery.fileupload'], function(ko, template, app) {

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	function MediaUploaderModel(params) {
		var self         = this;
		self.title       = ko.observable().extend({ required: true });
		self.description = ko.observable().extend();
		self.imageURL    = ko.observable();

		$('#imageupload').fileupload({
			add : function(e, data) {
				if (data.files && data.files[0]) {
					var reader    = new FileReader();
					reader.onload = function(e) {
						self.resizePhoto(e.target.result, 200);
					};
					reader.readAsDataURL(data.files[0]);
				}
			}
		});

		self.close       = function() {
			app.closePopup();
		};

		self.uploadImage = function() {
			console.log("Uploading Image");
		};

		self.resizePhoto      = function(src, width, height) {
			var img    = new Image();
			img.onload = function() {
				var canvas     = document.createElement('canvas');
				var ctx        = canvas.getContext("2d");
				ctx.drawImage(img, 0, 0);

				height = typeof height !== 'undefined' ? height : -1;

				if (height < 0) {
					if (img.width > img.height) {
						height = (img.height / img.width) * width;
					}
					else {
						width = (img.width / img.height) * height;
					}
				}

				canvas.width  = width;
				canvas.height = height;
				ctx           = canvas.getContext("2d");
				ctx.drawImage(img, 0, 0, width, height);

				var dataurl   = canvas.toDataURL("image/png");

				self.imageURL(dataurl);
			};
			img.src = src;
		};

	}

	return { viewModel: MediaUploaderModel, template: template };
});