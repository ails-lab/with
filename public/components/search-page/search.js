define(['knockout', 'text!./search.html'], function(ko, template) {

  function SearchModel(params) {
	  this.route = params.route;
	  
	  var withsearch = $( '#withsearchid' ),
	  input =$("input.withsearch-input");
	  ctrlClose =$("span.withsearch-close");
	  isOpen = false,
		// show/hide search area
		toggleSearch = function(evt) {
			// return if open and the input gets focused
			if( evt === 'focus' && isOpen ) return false;

			if( isOpen ) {
				withsearch.removeClass("open");
				
				input.blur();
			}
			else {
				withsearch.addClass('open');
				
			}
			isOpen = !isOpen;
		};

	// events
		
	input.focus(function(){toggleSearch('focus');});	
	$( document ).on( 'keypress', function( ev ) {
	    var nodeName = ev.target.nodeName;

	    
	    $('#searchlink').trigger( 'click' );
	    input.focus();
	    });
		
	ctrlClose.on('click',function(){
		input.val('');
		toggleSearch('click');
		}
	);
	// esc key closes search overlay
	// keyboard navigation events
	
	$(document).keyup(function(e) {

		  if (e.keyCode == 27 && isOpen ) { ctrlClose.click(); }   // esc
		});
	
	
	 
  }
 
  return { viewModel: SearchModel, template: template };
});