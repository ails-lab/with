define(['knockout', 'text!./side-bar.html', 'app'], function(ko, template, app) {

  function SideBarViewModel(params) {

		// This viewmodel doesn't do anything except pass through the 'route' parameter to the view.
		// You could remove this viewmodel entirely, and define 'side-bar' as a template-only component.
		// But in most apps, you'll want some viewmodel logic to determine what navigation options appear.

		this.route = params.route;
		$('[data-toggle=offcanvas]').click(function() {
			$(this).toggleClass('visible-xs text-center');
			$(this).find('i').toggleClass('glyphicon-chevron-right glyphicon-chevron-left');
			$('.row-offcanvas').toggleClass('active');
			$('#lg-menu').toggleClass('hidden-xs').toggleClass('visible-xs');
			$('#xs-menu').toggleClass('visible-xs').toggleClass('hidden-xs');
			$('#btnShow').toggle();
		});
	}

	return { viewModel: SideBarViewModel, template: template };
});
