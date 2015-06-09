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

		self.close       = function() {
			app.closePopup();
		};

		self.uploadImage = function() {
			console.log("Uploading Image");
		};
	}

	return { viewModel: MediaUploaderModel, template: template };
});