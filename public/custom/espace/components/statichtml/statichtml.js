define(['knockout', 'text!./statichtml.html','app','slick'], function(ko, template,app,slick) {


function staticHtmlViewModel(params) { 
	var self=this;
	self.route = params.route;
	self.templateName = ko.observable(params.page);
	
	switch (self.templateName()) {
	  case 'about':
	     document.body.setAttribute("data-page","about");
	    break;
	  case 'terms':
		  document.body.setAttribute("data-page","terms");
	    break;
	  case 'contact':
		  document.body.setAttribute("data-page","contact");
	    break;
	  case 'privacy':
		  document.body.setAttribute("data-page","privacy");
	    break;  
	  default:
		  document.body.setAttribute("data-page","about");
	    break;
	}
	

    self.animatePageChange= function() { $('div[role="main"]').hide(); $('div[role="main"]').fadeIn(3000); }
};



return { viewModel: staticHtmlViewModel, template: template };
});