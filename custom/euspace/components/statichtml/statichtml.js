define(['knockout', 'text!./statichtml.html','app'], function(ko, template,app) {


function staticHtmlViewModel(params) { 
	var self=this;
	self.route = params.route;
	self.templateName = ko.observable(params.page);
	console.log(self.templateName());
	
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
	setTimeout(function(){ EUSpaceUI.init(); }, 200);


    self.animatePageChange= function() { $('div[role="main"]').hide(); $('div[role="main"]').fadeIn(3000); }
};



return { viewModel: staticHtmlViewModel, template: template };
});