define(['knockout', 'text!./top-bar.html', 'app', 'autocomplete'], function(ko, template, app, autocomplete) {

  function TopBarViewModel(params) {
	  this.route = params.route;
	  self.openLogin=function(){
		  $("#loginPopup").addClass("open");
		  
		 
	  }
	  
	  self.gotoWith=function(){
		    window.childwith=window.open('../../assets/index.html#', 'with');
		    window.childwith.focus();
	}
	
	  
	}

	return { viewModel: TopBarViewModel, template: template };
});
