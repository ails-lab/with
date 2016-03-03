define(['knockout', 'text!./organization-page.html', 'app', 'bridget', 'isotope', 'imagesloaded', 'async!https://maps.google.com/maps/api/js?v=3&sensor=false', 'knockout-validation', 'jquery.fileupload', 'knockout.x-editable', 'smoke'], function (ko, template, app, bridget, Isotope, imagesLoaded) {

	$.bridget('isotope', Isotope);

	self.loading = ko.observable(false);

	self.transDuration = '0.4s';
	var isFirefox = typeof InstallTrigger !== 'undefined'; // Firefox 1.0+
	if (isFirefox) {
		self.transDuration = 0;
	}

	// Settings for X-Editable
	$.fn.editable.defaults.url = function (params) {
		var d = new $.Deferred();
		console.log(params);
		var data = {};
		if (params.name === 'address') {
			data = {
				page: {
					address: params.value.address,
					city: params.value.city,
					country: params.value.country
				}
			};
		} else if (params.name === 'url') {
			data = {
				page: {
					url: params.value
				}
			};
		} else {
			data[params.name] = params.value;
		}

		$.ajax({
			type: 'PUT',
			url: '/group/' + params.pk,
			contentType: 'application/json',
			dataType: 'json',
			processData: false,
			data: JSON.stringify(data),
			success: function (data, text) {
				d.resolve(data, text);
			},
			error: function (request, status, error) {
				d.reject(request, status, error);
			}
		});
	};

	// Custom field for editing the address
	var Address = function (options) {
		this.init('address', options, Address.defaults);
	};
	$.fn.editableutils.inherit(Address, $.fn.editabletypes.abstractinput);
	$.extend(Address.prototype, {
		render: function () { // Renders input from tpl
			this.$input = this.$tpl.find('input');
		},
		value2html: function (value, element) { // Default method to show value in element
			if (!value) {
				$(element).empty();
				return;
			}

			var html = '';
			if (value.city && value.country) {
				html = $('<div>').text(value.city).html() + ', ' + $('<div>').text(value.country).html();
				// html = '';
			} else if (value.city || value.county) {
				html = $('<div>').text(value.city).html() + $('<div>').text(value.country).html();
			}

			$(element).html(html);
		},
		html2value: function (html) {
			return null;
		},
		value2str: function (value) {
			var str = '';
			if (value) {
				for (var k in value) {
					str = str + k + ':' + value[k] + ';';
				}
			}
			return str;
		},
		str2value: function (str) {
			return str;
		},
		value2input: function (value) {
			if (!value) {
				return;
			}
			this.$input.filter('[name="address"]').val(value.address);
			this.$input.filter('[name="city"]').val(value.city);
			this.$input.filter('[name="country"]').val(value.country);
		},
		input2value: function () {
			return {
				address: this.$input.filter('[name="address"]').val(),
				city: this.$input.filter('[name="city"]').val(),
				country: this.$input.filter('[name="country"]').val()
			};
		},
		activate: function () {
			this.$input.filter('[name="address"]').focus();
		},
		autosubmit: function () {
			this.$input.keydown(function (e) {
				if (e.which === 13) {
					$(this).closest('form').submit();
				}
			});
		}
	});
	Address.defaults = $.extend({}, $.fn.editabletypes.abstractinput.defaults, {
		tpl: '<div class="editable-address"><label class="address"><span>Address: </span><input type="text" name="address" class="form-control" data-bind="value: page.address"></label></div>' +
			'<div class="editable-address"><label class="address"><span>City: </span><input type="text" name="city" class="form-control" data-bind="value: page.city"></label></div>' +
			'<div class="editable-address"><label class="address"><span>Country: </span><input type="text" name="country" class="form-control" data-bind="value: page.country"></label></div>',

		inputclass: ''
	});

	$.fn.editabletypes.address = Address;

	var settings = $.extend({
		// page
		page: 'default',

		// masonry
		mSelector: '.grid',
		mItem: '.item',
		mSizer: '.sizer',

		// mobile menu
		mobileSelector: '.mobilemenu',
		mobileMenu: '.main .menu'
	}, {});

	var initProfileScroll = function () {

		// windows scroll event
		$(window).on('scroll touchmove', function () {

			// set class
			toggleProfileClasses();
		});

		// function init
		function toggleProfileClasses() {

			// check window height
			if ($(window).height() > 600 && $(window).width() > 767) {

				// stick part of the banner on top
				if ($(document).scrollTop() >= 169) {
					$('.profilebar').addClass('fixed');
				} else {
					if ($('.profilebar').hasClass('fixed')) {
						$('.profilebar').removeClass('fixed');
					}
				}

				// check
				if ($('.filter').length > 0) {

					// vars
					var offset = $('.filter').offset(),
						topPos = parseInt(offset.top) - 226;

					// stick part of the banner on top
					if ($(document).scrollTop() >= topPos) {
						$('.filter').addClass('fixed');
					} else {
						if ($('.filter').hasClass('fixed')) {
							$('.filter').removeClass('fixed');
						}
					}
				}
			}
		}

		// set on init
		toggleProfileClasses();
	};

	function initOrUpdate(method) {
		return function (element, valueAccessor, allBindings, viewModel, bindingContext) {
			function isotopeAppend(ele) {
				if (ele.nodeType === 1) { // Element type
					$(element).imagesLoaded(function () {
						$(element).isotope('appended', ele).isotope('layout');
					});
				}
			}

			function attachCallback(valueAccessor) {
				return function () {
					return {
						data: valueAccessor(),
						afterAdd: isotopeAppend
					};
				};
			}

			var data = ko.utils.unwrapObservable(valueAccessor());
			//extend foreach binding
			ko.bindingHandlers.foreach[method](element,
				attachCallback(valueAccessor), // attach 'afterAdd' callback
				allBindings, viewModel, bindingContext);

			if (method === 'init') {
				$(element).isotope({
					itemSelector: '.item',
					transitionDuration: transDuration,
					masonry: {
						columnWidth: '.sizer',
						percentPosition: true
					}
				});

				ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
					$(element).isotope("destroy");
				});
			}
		};
	}

	ko.bindingHandlers.scroll = {
		updating: true,

		init: function (element, valueAccessor, allBindingsAccessor) {
			var self = this;
			self.updating = true;
			ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
				$(window).off("scroll.ko.scrollHandler");
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
				$(window).on("scroll.ko.scrollHandler", function () {
					if ($(window).scrollTop() >= $(document).height() - $(window).height() - 300) {
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
				$(window).off("scroll.ko.scrollHandler");
				self.updating = false;
			}
		}
	};

	ko.bindingHandlers.profileisotope = {
		init: initOrUpdate('init'),
		update: initOrUpdate('update')
	};

	function Collection(data) {
		var self = this;
		$("div[role='main']").toggleClass("homepage", false);

		var mapping = {
			create: function (options) {
				var self = this;
				// use extend instead of map to avoid observables

				self = $.extend(self, options.data);

				self.title = findByLang(self.descriptiveData.label);
				self.thumbnail = ko.computed(function () {
					if (self.media && self.media[0] && self.media[0].Thumbnail) {
						var data = self.media[0].Thumbnail.url;
						if (data) {
							return data;
						} else {
							return "img/content/thumb-empty.png";
						}
					}
					return "img/content/thumb-empty.png";
				});

				self.type = ko.computed(function () {
					if (self.administrative) {
						if (self.administrative.collectionType.indexOf("Collection") != -1) {
							return "COLLECTION";
						} else if (self.administrative.collectionType.indexOf("Space") != -1) {
							return "SPACE";
						} else {
							return "EXHIBITION";
						}
					} else {
						return "";
					}
				});

				self.css = ko.computed(function () {
					if (self.administrative) {
						if (self.administrative.collectionType.indexOf("Collection") != -1) {
							return "item collection";
						} else if (self.administrative.collectionType.indexOf("Space") != -1) {
							return "item space";
						} else {
							return "item space";
						}
					} else {
						return "item exhibition";
					}
				});

				self.url = ko.computed(function () {
					if (self.administrative) {
						if (self.administrative.collectionType.indexOf("Collection") > -1) {
							return 'index.html#collectionview/' + self.dbId;
						} else if (self.administrative.collectionType.indexOf("Space") > -1) {
							return self.administrative.isShownAt;
						} else {
							return 'index.html#exhibitionview/' + self.dbId;
						}
					} else {
						return "";
					}
				});
				self.owner = ko.computed(function () {
					if (self.withCreatorInfo) {
						return self.withCreatorInfo.username;
					}
				});

				return self;
			}
		};


		var recmapping = {
			'dbId': {
				key: function (data) {
					return ko.utils.unwrapObservable(data.dbId);
				}
			}
		};
		self.isLoaded = ko.observable(false);
		self.records = ko.mapping.fromJS([], recmapping);


		self.data = ko.mapping.fromJS({"dbID": "","administrative": "","descriptiveData": ""}, mapping);

		self.load = function (data) {
			self.data = ko.mapping.fromJS(data, mapping);

		};


		if (data != undefined) {
			self.load(data);

		}
	}


	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	/* Custom bindingHandler for error message */
	ko.bindingHandlers.validationCore = {
		init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
			var span = document.createElement('SPAN');
			span.className = 'help-block';
			var parent = $(element).parent().closest(".input-group");
			if (parent.length > 0) {
				$(parent).after(span);
			} else {
				$(element).after(span);
			}
			ko.applyBindingsToNode(span, {
				validationMessage: valueAccessor()
			});
		}
	};

	function OrganizationViewModel(params) {
		var self = this;
		$("div[role='main']").toggleClass("homepage", false);
		this.route = params.route;
		document.body.setAttribute("data-page", "profile");
		// self.id = ko.observable(params.id);

		// Generic Parameters
		self.type = ko.observable('organization'); // Default is organization. If user gives a different parameter, that changes
		self.baseURL = ko.pureComputed(function () { // TODO: Dynamically get the address
			return window.location.origin + '/assets/index.html#' + self.type() + '/';
			// return 'www.with.image.ntua.gr/assets/index.html#' + self.type() + '/';
		});

		// UserGroup Fields
		self.id = ko.observable();
		self.creator = ko.observable();
		self.adminIds = {};
		self.username = ko.observable().extend({
			required: true
		});
		self.friendlyName = ko.observable().extend({
			required: true
		});
		// self.thumbnail = ko.observable();
		self.avatar = {
			Original: ko.observable(),
			Tiny: ko.observable(),
			Square: ko.observable(),
			Thumbnail: ko.observable(),
			Medium: ko.observable()
		};

		self.about = ko.observable().extend({
			required: true
		});

		self.validationModel = ko.validatedObservable({
			username: self.username,
			friendlyName: self.friendlyName,
			about: self.about
		});

		// Page Fields
		self.page = {
			address: ko.observable(),
			city: ko.observable(),
			country: ko.observable(),
			url: ko.observable(),
			cover: {
				Original: ko.observable(),
				Tiny: ko.observable(),
				Square: ko.observable(),
				Thumbnail: ko.observable(),
				Medium: ko.observable()
			},
			featuredCollections: ko.observableArray(),
			coordinates: {
				latitude: ko.observable(),
				longitude: ko.observable()
			}
		};

		// Display fields

		self.exhibitloaded = ko.observable(false);
		self.totalCollections = ko.observable(0);
		self.totalExhibitions = ko.observable(0);

		self.location = ko.computed(function () {
			if (self.page.city() === '' && self.page.country() === '') {
				return self.page.country() && self.page.city() ? self.page.city() + ', ' + self.page.country() : self.page.city() + self.page.country();
			} else {
				return '';
			}
		});
		self.coverThumbnail = ko.computed(function () {
			return self.page.coverThumbnail ? '/media/' + self.page.coverThumbnail() : null;
		});
		self.logo = ko.computed(function () {
			return self.thumbnail ? '/media/' + self.thumbnail() : false;
		});
		self.coords = ko.computed(function () {
			if (self.page.coordinates.latitude() && self.page.coordinates.longitude()) {
				return "https://www.google.com/maps/embed/v1/place?q=" + self.page.coordinates.latitude() + "," + self.page.coordinates.longitude() + "&key=AIzaSyAN0om9mFmy1QN6Wf54tXAowK4eT0ZUPrU";
			} else {
				return null;
			}
		});
		self.isCreator = ko.computed(function () {
			return isLogged() && app.currentUser._id() === self.creator();
		});

		self.isAdmin = ko.computed(function () {
			return isLogged() && $.inArray(app.currentUser._id(), self.adminIds);
		});

		self.backgroundImage = ko.computed(function () {
			if (self.page.cover.Original()) {
				return 'url(' + self.page.cover.Original() + ')';
			} else {
				return null;
			}
		});

		if (params.id !== undefined) {
			self.id(params.id);
		}
		if (params.type !== undefined) {
			self.type(params.type);
		}

		$('#imageupload').fileupload({
			type: "POST",
			url: '/media/create',
			acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
			maxFileSize: 50000,
			done: function (e, data) {
				// console.log(e);
				console.log(data);
				// var urlID = data.result.results[0].thumbnailUrl.substring('/media/'.length);
				// self.thumbnail(urlID);
				self.avatar.Original(data.result.original);
				self.avatar.Tiny(data.result.tiny);
				self.avatar.Square(data.result.square);
				self.avatar.Thumbnail(data.result.thumbnail);
				self.avatar.Medium(data.result.medium);
			},
			error: function (e, data) {
				$.smkAlert({
					text: 'Error uploading the file',
					type: 'danger',
					time: 10
				});
			}
		});

		$('#logoupdate').fileupload({
			type: "POST",
			url: '/media/create',
			acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
			maxFileSize: 50000,
			done: function (e, data) {
				// var urlID = data.result.results[0].thumbnailUrl.substring('/media/'.length);
				// self.thumbnail(urlID);

				var updateData = {
					avatar: {
						Original: data.result.original,
						Tiny: data.result.tiny,
						Square: data.result.square,
						Thumbnail: data.result.thumbnail,
						Medium: data.result.medium
					}
				};

				$.ajax({
					type: 'PUT',
					url: '/group/' + self.id(),
					contentType: 'application/json',
					dataType: 'json',
					processData: false,
					data: JSON.stringify(updateData),
					success: function (data, text) {
						self.avatar.Original(data.avatar.Original);
						self.avatar.Tiny(data.avatar.Tiny);
						self.avatar.Square(data.avatar.Square);
						self.avatar.Thumbnail(data.avatar.Thumbnail);
						self.avatar.Medium(data.avatar.Medium);
					},
					error: function (request, status, error) {
						// TODO: Show notification
					}
				});

			},
			error: function (e, data) {
				$.smkAlert({
					text: 'Error uploading the file',
					type: 'danger',
					time: 10
				});
			}
		});

		$('#coverupload').fileupload({
			type: "POST",
			url: '/media/create',
			acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
			maxFileSize: 500000,
			done: function (e, data) {
				self.page.cover.Original(data.result.original);
				self.page.cover.Tiny(data.result.tiny);
				self.page.cover.Square(data.result.square);
				self.page.cover.Thumbnail(data.result.thumbnail);
				self.page.cover.Medium(data.result.medium);
			},
			error: function (e, data) {
				$.smkAlert({
					text: 'Error uploading the file',
					type: 'danger',
					time: 10
				});
			}
		});

		$('#coverupdate').fileupload({
			type: "POST",
			url: '/media/create',
			acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
			maxFileSize: 500000,
			done: function (e, data) {
				var updateData = {
					page: {
						cover: {
							Original: data.result.original,
							Tiny: data.result.tiny,
							Square: data.result.square,
							Thumbnail: data.result.thumbnail,
							Medium: data.result.medium
						}
					}
				};

				$.ajax({
					type: 'PUT',
					url: '/group/' + self.id(),
					contentType: 'application/json',
					dataType: 'json',
					processData: false,
					data: JSON.stringify(updateData),
					success: function (data, text) {
						self.page.cover.Original(data.page.cover.Original);
						self.page.cover.Tiny(data.page.cover.Tiny);
						self.page.cover.Square(data.page.cover.Square);
						self.page.cover.Thumbnail(data.page.cover.Thumbnail);
						self.page.cover.Medium(data.page.cover.Medium);

						// $(".profilebar > .wrap").css('background-image', 'url(' + self.page.cover.Original() + ')');
					},
					error: function (request, status, error) {
						// TODO: Show notification
					}
				});

			},
			error: function (e, data) {
				$.smkAlert({
					text: 'Error uploading the file',
					type: 'danger',
					time: 10
				});
			}
		});

		// Getting the coordinates from Google Maps is done asynchronously, so we have to pass the create/update functions
		// as parameters to be used as callbacks
		self.getCoordinatesAndSubmit = function (submitFunc) {
			if (self.page.address && self.page.city && self.page.country) {
				var address = self.page.address() + ', ' + self.page.city() + ', ' + self.page.country();
				var geocoder = new google.maps.Geocoder();
				geocoder.geocode({
					'address': address
				}, function (results, status) {
					if (status == google.maps.GeocoderStatus.OK) {
						self.page.coordinates.latitude(results[0].geometry.location.lat());
						self.page.coordinates.longitude(results[0].geometry.location.lng());
					}

					submitFunc();
				});
			} else {
				submitFunc();
			}
		};

		// Create and Edit Functions
		self.submit = function (type) {
			if (self.validationModel.isValid()) {
				if (type === 'new') {
					self.getCoordinatesAndSubmit(self.create);
				} else if (type === 'update') {
					self.getCoordinatesAndSubmit(self.update);
				} else {
					console.log('Unknown type: ' + type);
				}
			} else {
				self.validationModel.errors.showAllMessages();
			}
		};

		self.create = function () {
			var data = {
				username: self.username,
				friendlyName: self.friendlyName,
				avatar: self.avatar,
				about: self.about,
				page: self.page
			};
			console.log("sending to db:");
			console.log(data);
			$.ajax({
				type: "POST",
				url: "/" + self.type() + "/create",
				contentType: 'application/json',
				dataType: 'json',
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					$.smkAlert({
						text: 'A new ' + self.type() + ' was created successfully!',
						type: 'success'
					});
					app.reloadUser();
					self.closeWindow();
				},
				error: function (request, status, error) {
					var err = JSON.parse(request.responseText);
					self.username.setError(err.error);
					self.username.isModified(true);
					self.validationModel.errors.showAllMessages();
				}
			});
		};

		self.update = function (data) {
			$.ajax({
				type: 'PUT',
				url: '/group/' + self.id(),
				contentType: 'application/json',
				dataType: 'json',
				processData: false,
				data: ko.toJSON(data),
				success: function (data, text) {
					$.smkAlert({
						text: 'Update successful!',
						type: 'success'
					});
				},
				error: function (request, status, error) {
					var err = JSON.parse(request.responseText);
					$.smkAlert({
						text: err.error,
						type: 'danger'
					});
					console.log(error);
				}
			});
		};

		self.manageMembersPopup = function () {
			app.showPopup('members-popup', params);
		};

		self.closeWindow = function () {
			app.closePopup();
		};

		// Display Functions
		self.revealItems = function (data) {
			if (data.length == 0) {
				loading(false); return;
			}

			for (var i in data) {
				var c = new Collection(
					data[i]
				);
				self.page.featuredCollections().push(c);
			}
			self.page.featuredCollections.valueHasMutated();
		};

		self.loadAll = function () {
			var promise = self.getProviderData();
			loading(true);
			$.when(promise).done(function (data, textStatus, jqXHR) {

				self.username(data.username);
				self.friendlyName(data.friendlyName);
				if (data.avatar) {
					self.avatar.Original(data.avatar.Original);
					self.avatar.Tiny(data.avatar.Tiny);
					self.avatar.Square(data.avatar.Square);
					self.avatar.Thumbnail(data.avatar.Thumbnail);
					self.avatar.Medium(data.avatar.Medium);
				}
				self.about(data.about);
				self.creator(data.creator);
				self.adminIds = data.adminIds;

				self.page.address(data.page.address);
				self.page.city(data.page.city);
				self.page.country(data.page.country);

				self.page.url = data.page.url;

				if (data.page.coordinates) {
					self.page.coordinates.longitude(data.page.coordinates.longitude);
					self.page.coordinates.latitude(data.page.coordinates.latitude);
				}
				if (data.page.cover) {
					self.page.cover.Original(data.page.cover.Original);
					self.page.cover.Tiny(data.page.cover.Tiny);
					self.page.cover.Square(data.page.cover.Square);
					self.page.cover.Thumbnail(data.page.cover.Thumbnail);
					self.page.cover.Medium(data.page.cover.Medium);
				}

				// if (self.page.cover.Original()) {
				// 	$(".profilebar > .wrap").css('background-image', 'url(' + self.page.cover.Original() + ')');
				// }

				if (!self.isCreator()) {
					$('.editable').editable('destroy');
				}

				var promise2 = self.getProfileCollections();
				$.when(promise2).done(function (data) {
					self.totalCollections(data.totalCollections);
					self.totalExhibitions(data.totalExhibitions);
					self.revealItems(data.collectionsOrExhibitions);
					initProfileScroll();
					loading(false);
				});

			}).fail(function (jqXHR, textStatus, errorThrown) {
				// window.location.href = "/assets/index.html";
				$.smkAlert({
					text: 'Page not found!',
					type: 'danger',
					time: 10
				});
				loading(false);
			});
		};

		self.getProfileCollections = function () {
			//call should be replaced with collection/list?isPublic=true&offset=0&count=20&isExhibition=false&directlyAccessedByGroupName=[{"orgName":self.username(), "access":"READ"}]
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				//TODO:add parent project filter
				data: "offset=0&count=20&collectionHits=true&directlyAccessedByUserOrGroup=" + JSON.stringify([{
					group: self.username(),
					rights: "OWN"
				}])
			}).success(function () {});
		};

		self.getProviderData = function () {
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/group/" + self.id(),
				processData: false

			}).success(function () {});
		};

		self.loadNext = function () {
			self.moreCollections();
		};


		self.loadNext = function () {
			if (loading() === false) {
				loading(true);
				var promise = self.moreCollections();
				$.when(promise).done(function (data) {
					self.revealItems(data.collectionsOrExhibitions);
					loading(false);
				})


			}
		};

		self.moreCollections = function () {
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				data: "count=20&offset=" + self.page.featuredCollections().length + "&directlyAccessedByUserOrGroup=" + JSON.stringify([{
					"group": self.username(),
					rights: "OWN"
				}])

			}).success (function () {
			});

		};

		// Load the page, if we have an ID
		if (self.id()) {
			self.loadAll();
		}

		self.loadCollectionOrExhibition = function (item) {
			if (item.isExhibition) {
				window.location = 'index.html#exhibitionview/' + item.id;
			} else {
				window.location = 'index.html#collectionview/' + item.id;
			}
		};

		self.filter = function (data, event) {
			var selector = event.currentTarget.attributes.getNamedItem("data-filter").value;
			$(event.currentTarget).siblings().removeClass("active");
			$(event.currentTarget).addClass("active");
			$(settings.mSelector).isotope({
				filter: selector
			});
			return false;
		};

	}

	return {
		viewModel: OrganizationViewModel,
		template: template
	};
});
