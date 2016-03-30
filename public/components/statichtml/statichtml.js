define(['knockout', 'text!./statichtml.html', 'app', 'slick'], function (ko, template, app, slick) {

	$.fn.scrollView = function () {
		return this.each(function () {
			$('html, body').animate({
				scrollTop: $(this).offset().top
			}, 500);
		});
	};

	function staticHtmlViewModel (params) {
		var self = this;
		self.route = params.route;
		self.templateName = ko.observable(params.page);


		self.scrollTo = function (anchor) {
			window.history.pushState(null, anchor, '#about/' + anchor);
			$('#' + anchor).scrollView();
		};

		self.animatePageChange = function () {
			$('div[role="main"]').hide();
			$('div[role="main"]').fadeIn(3000);
		};

		switch (self.templateName()) {
		case 'about':
			document.body.setAttribute("data-page", "about");
			if (typeof params.anchor !== 'undefined') {
				setTimeout(function () {	// Don't use the function, to avoid duplicate states
					$('#' + params.anchor).scrollView();
				}, 500);
			}
			break;
		case 'terms':
			document.body.setAttribute("data-page", "terms");
			break;
		case 'contact':
		case 'feedback':
			document.body.setAttribute("data-page", "contact");
			break;
		case 'privacy':
			document.body.setAttribute("data-page", "privacy");
			break;
		case 'faq':
			document.body.setAttribute("data-page", "faq");
			break;
		default:
			document.body.setAttribute("data-page", "about");
			break;
		}
	}

	return {
		viewModel: staticHtmlViewModel,
		template: template
	};
});