define(['knockout', 'text!./top-bar.html', 'app', 'autocomplete'], function(ko, template, app, autocomplete) {
	
	

  function TopBarViewModel(params) {
	  this.route = params.route;
	  var self=this;
	  self.route=params.route;
	  self.openLogin=function(event){
		  event.preventDefault();
		  $("#loginPopup").addClass("open");
		  
		 
	  }
	  
	  self.gotoWith=function(event){
		    event.preventDefault();
		    window.childwith=window.open('../../assets/index.html', 'with');
		    window.childwith.focus();
	}
	  
	 goToPage=function(data,event){
		 if(data=="#home" || data=="#"){
		      sessionStorage.removeItem("homemasonryscroll");
		      sessionStorage.removeItem("homemasonrycount");}
		   window.location.href=data;
		   event.preventDefault();
		   return false;
	}
	
	  
	
	  
	}

	return { viewModel: TopBarViewModel, template: template };
});
