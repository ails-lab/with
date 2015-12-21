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
		   
		   window.location.href=data;
		   event.preventDefault();
		   return false;
	}
	
	/* window.onhashchange = function () { alert(window.location.hash+" params route:"+self.route().page); }
	  
	  $(document).on('click', '.menu a', function (e) {
	      console.log("unload triggered:"+$(window).scrollTop()+" params route:"+self.route().page);
	      var scrollPosition = $(window).scrollTop();
	      if(self.route().page=="collection-view")
	      sessionStorage.setItem("collection-viewscroll"+self.route().id, scrollPosition);
	      else if(self.route().page=="home-page")
		      sessionStorage.setItem("homemasonryscroll", scrollPosition);
	      else if(self.route().page=="provider")
		      sessionStorage.setItem("providerview"+self.route().id, scrollPosition);
	   }); */
	  
	}

	return { viewModel: TopBarViewModel, template: template };
});
