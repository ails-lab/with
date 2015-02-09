define(['knockout', 'text!./search.html'], function(ko, template) {
	

  function SearchModel(params) {
	  this.route = params.route;
	  
	  var withsearch = $( '#withsearchid' );
	  var withinput =$("input.withsearch-input");
	  ctrlClose =$("span.withsearch-close");
	  isOpen = false,
		// show/hide search area
		toggleSearch = function(evt,char) {
			// return if open and the input gets focused
			if(  evt === 'focus' && isOpen ) return false;

			if( isOpen ) {
				withsearch.removeClass("open");
				withinput.blur();
			}
			else {
				var isOpera = !!window.opera || navigator.userAgent.indexOf(' OPR/') >= 0;
			    // Opera 8.0+ (UA detection to detect Blink/v8-powered Opera)
			    var isFirefox = typeof InstallTrigger !== 'undefined';   // Firefox 1.0+

				withsearch.addClass('open');
				withinput.focus();
				if(isOpera || isFirefox)
				withinput.val(char);
				
			}
			isOpen = !isOpen;
		};

		
	    $(document).keyup(function(e) {

  		  if (e.keyCode == 27 && isOpen ) { 
  			withinput.val('');
  			toggleSearch(e,'');
  			  
  			  }   // esc
  		});
        ctrlClose.on('click',function(event){
    		withinput.val('');
    		
    		toggleSearch(event,'');
    		}
    	);
		

	
	 
  }
 
  return { viewModel: SearchModel, template: template };
});
