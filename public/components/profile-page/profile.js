define(['knockout', 'text!./profile.html', 'app', 'knockout-validation', 'jquery.fileupload'], function(ko, template, app) {

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	function ProfileViewModel(params) {
		var self         = this;
		self.firstName   = ko.observable();
		self.lastName    = ko.observable();
		self.aboutMe     = ko.observable();
		self.imageURL    = ko.observable();
		self.location    = ko.observable();
		self.facebookId  = ko.observable();
		self.googleId    = ko.observable();
		self.hasGoogle   = ko.computed(function() { return self.googleId() ? true : false; });
		self.hasFacebook = ko.computed(function() { return self.facebookId() ? true : false; });

		// Load the User information from the database and initialize the template
		$.ajax({
			type    : "get",
			url     : "/user/" + app.currentUser._id(),
			success : function(data) {
				// console.log(data);
				var obj = $.parseJSON(data);
				self.firstName(obj.firstName);
				self.lastName(obj.lastName);
				self.aboutMe(obj.aboutMe);
				self.imageURL(obj.image);
				self.facebookId(obj.facebookId);
				self.googleId(obj.googleId);
				self.location(obj.location);
			},
			error   : function(request, status, error) {
				console.log(request);
			}
		});

		$('#imageupload').fileupload({
			add : function(e, data) {
				if (data.files && data.files[0]) {

					var img       = document.createElement("img");
					var reader    = new FileReader();
					reader.onload = function(e) {
						img.src        = e.target.result;

						// Resize Image
						var canvas     = document.createElement('canvas');
						var ctx        = canvas.getContext("2d");
						ctx.drawImage(img, 0, 0);
						var MAX_WIDTH  = 100;
						var MAX_HEIGHT = 100;
						var width      = img.width;
						var height     = img.height;

						if (width < height) {
							if (width > MAX_WIDTH) {
								height *= MAX_WIDTH / width;
								width = MAX_WIDTH;
							}
						} else {
							if (height > MAX_HEIGHT) {
								width *= MAX_HEIGHT / height;
								height = MAX_HEIGHT;
							}
						}

						canvas.width  = MAX_WIDTH;
						canvas.height = MAX_HEIGHT;
						ctx           = canvas.getContext("2d");
						ctx.drawImage(img, 0, 0, width, height);

						var dataurl   = canvas.toDataURL("image/png");

						self.imageURL(dataurl);
					};
					reader.readAsDataURL(data.files[0]);

					// var reader    = new FileReader();
					// reader.onload = function(e) {
					// 	self.imageURL(e.target.result);
					// };
					// reader.readAsDataURL(data.files[0]);
				}
			}
		});

		// Call the global closePopup function to dispose the component without saving the changes
		self.closeWindow   = function() {
			app.closePopup();
		};

		// Save the changes and dispose the component
		self.updateProfile = function() {
			// console.log(self.imageURL());
			var data = {
				firstName : self.firstName,
				lastName  : self.lastName,
				location  : self.location,
				about     : self.aboutMe,
				image     : self.imageURL
			};
			var json = ko.toJSON(data);
			$.ajax({
				type        : "put",
				contentType : 'application/json',
				dataType    : 'json',
				processData : false,
				url         : "/user/" + app.currentUser._id(),
				data        : json,
				success     : function(data) {
					console.log(data);
					app.currentUser.image(self.imageURL());
					// TODO: Reload User Profile
				},
				error       : function(request, status, error) {
					console.log(error);
				}
			});
			app.closePopup();
		};

		self.loadFromFacebook = function() {
			FB.api(
				"/" + self.facebookId() + "/picture?type=normal",
				function(response) {
					if (response && !response.error) {
						self.imageURL(response.data.url);
					}
				}
			);
		};

		self.loadFromGoogle   = function() {
			$.ajax({
				type    : "get",
				url     : "https://www.googleapis.com/plus/v1/people/" + self.googleId() + "?key=AIzaSyA2X9H_PW8pfGnaHBc6VhQMjDPp7Yxaj5k",
				success : function(data) {
					var url = data.image.url;
					url     = url.replace('sz=50', 'sz=100');	// Resize image
					self.imageURL(url);
				}
			});
		};
	}

	return { viewModel: ProfileViewModel, template: template };
});