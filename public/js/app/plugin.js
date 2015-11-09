define(function () {
//  vars
var WITHApp = WITHApp || {};

var console = (window.console = window.console || {}),
debug = true;


// logger method
function logger( type, data ) {
	if( debug )  {
		console[ type ]( data );
	}
}

// UI interaction class
WITHApp.ui = function( custom ){

	// overwrite default settings
	var settings = $.extend({

		// page
		page  	  : 'default',

		// masonry
		mSelector : '.grid',
		mItem	  : '.item',
		mSizer	  : '.sizer',

		// mobile menu
		mobileSelector : '.mobilemenu',
		mobileMenu 	   : '.main .menu'
	}, 
	custom || {});

	// this
	this.init = function(){

		// log
		logger( 'info','plugins.js / init' );

		// init isotope
		initIsotope();

		// limit character description
		initCharacterLimiter();

		// init filter stick plugin
		initFilterStick();
	};
	
	this.initCharacterLimiter=function(){
		initCharacterLimiter();
	}
	

	// method to initialize isotope
	// dependency: js/vendor/isotope/
	var initIsotope = function(){

		// log
		logger( 'info','plugins.js / initIsotope' );

		// initialize masonry after window.load
		// safari seems to have problem when initialize on dom ready
		$(document).ready(function(){
			
			// check if grid exist
			if( $( settings.mSelector ).length > 0 ) {

				// init
				$( settings.mSelector ).imagesLoaded(function(){	
					
				$( settings.mSelector ).isotope({

					// options
					columnWidth		: settings.mSizer,
					itemSelector	: settings.mItem,
					percentPosition	: true
				});
			});
			}
		});
		/*$( window ).load(function(){

			// check if grid exist
			if( $( settings.mSelector ).length > 0 ) {

				// init
				$( settings.mSelector ).isotope({

					// options
					columnWidth		: settings.mSizer,
					itemSelector	: settings.mItem,
					percentPosition	: true
				});
			}
		});*/

		// init filter
		if( $( '.filter' ).length > 0 ) {

			// get list
			$( '.filter .nav li' ).each( function(){
				
				// list
				var $list = $( this ),
					data = $list.attr( 'data-filter' );

				// on click
				$( 'a', this ).on( 'click', function( e ){

					// prevent
					e.preventDefault();

					// filter
					$( settings.mSelector ).isotope({
						filter: data 
					});

					// reset
					$( '.filter .nav li' ).removeClass( 'active' );
					$list.addClass( 'active' );
				});
			});

		}
	};

	// method to limit character description
	var initCharacterLimiter = function(){

		// log
		logger( 'info','plugins.js / initCharacterLimiter' );

		// check
		if( $( '.featured .exhibition .description' ).length > 0 ) {

			// elem
			var $texts = $( '.featured .exhibition .description .text' );
			if ( $texts.text().length > 240 ) {
				var tmp = $texts.text().substr(0,240)+'...';
				$texts.text( tmp );
			}
		}
	};

	// method to initialize filter stick plugin
	// dependency sticker
	var initFilterStick = function(){

		// log
		logger( 'info','plugins.js / initSticky' );
 
		if( settings.page === 'home' ) {
		// check
		if ( $( '.filter' ).length !== 0 ){

			// init sticky
			$( '.filter' ).sticky({
				topSpacing: 74
			});

			// on scroll
			$( window ).on( 'scroll touchmove', function(){

				// var
				var offset = $( '.partners' ).offset(),
				topPos = offset.top - 74;

				// set class
				if ( $( document ).scrollTop() >= topPos ){

					$( '.filter' ).addClass('unstick');

				} else {

					$( '.filter' ).removeClass('unstick');

				}
			});
		}
	}};
};

return {
    
	WITHApp: WITHApp
	
  }
});