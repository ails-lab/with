define(['knockout', 'text!./top-bar.html', 'app', 'autocomplete'], function(ko, template, app, autocomplete) {

  function TopBarViewModel(params) {


		$( document ).on( 'keypress', function( event ) {

			if(event.target.nodeName != 'INPUT') {

			 if (event.which == null) {
		    	 var char=String.fromCharCode(event.which);
		    	 toggleSearch("focus",char);

		     } else if (event.which!=0 && event.charCode!=0) {
		    	 var char=String.fromCharCode(event.which);
		    	 toggleSearch("focus",char);
		       } else {
		    		    return;
		     }
			}else{return;}

			
		this.route = params.route;

		var self = this;
		self.logout = function() {
			$.get('api/logout', function() {
				app.currentUser();
			});
		}

		self.username = app.currentUser().username;

	}

	return { viewModel: TopBarViewModel, template: template };
});
