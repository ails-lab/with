
define(function () {
 
  
 
 
//  vars
var EUSpaceApp = EUSpaceApp || {};
var console = (window.console = window.console || {}),
	debug = true;

// logger method
function logger( type, data ) {
	if( debug )  {
		console[ type ]( data );
	}
}

// UI interaction class
EUSpaceApp.ui = function( custom ){

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

		// init mobile menu
		initMobileMenu();

		// init masonry
		//initMasonry();

		// initialize script on homepage only
		if( settings.page === 'home' ) {

			// initialize homepage header minimisation
			initHomeScroll();
		}

		// initialize script on homepage only
		if( settings.page === 'profile' ) {

			// initialize homepage header minimisation
			initProfileScroll();
		}

		// init carousel
		initCarousel();

		// init expand on exhibition
		initExpandExhibitionText();

		// init collection titlebar shrink
		initCollectionScroll();

		// init search setting bar
		initSearchSetting();

		// initialize media tooltip
		initTooltip();
	};

	// method to initialize masonry
	// dependency: js/vendor/masonry/
	var initMasonry = function(){

		// log
		logger( 'info','plugins.js / initMasonry' );

		// initialize masonry after window.load
		// safari seems to have problem when initialize on dom ready
		$( window ).load(function(){

			// check if grid exist
			if( $( settings.mSelector ).length > 0 ) {

				// init
				$( settings.mSelector ).masonry({

					// options
					columnWidth		: settings.mSizer,
					itemSelector	: settings.mItem,
					percentPosition	: true
				});
			}
		});
	};

	// method to initialize mobile menu
	var initMobileMenu = function(){

		// log
		logger( 'info','plugins.js / initMobileMenu' );

		// on click
		$( settings.mobileSelector ).on( 'touchstart click', function( e ) {

			// prevent default
			e.preventDefault();

			// toggle on menu
			var $menu = $( settings.mobileMenu );
			$menu.toggleClass( 'visible' );

			// toggle button
			$( settings.mobileSelector ).toggleClass( 'active' );
		});
	};

	// method to minimize header on homepage
	var initHomeScroll = function(){

		// log
		logger( 'info','plugins.js / initHomeHeader' );

		// windows scroll event
		$( window ).on( 'scroll touchmove', function(){

			// set class
			toggleHomeClasses();
		});

		// function init
		function toggleHomeClasses() {

			// toggle class
		    $( 'header' ).toggleClass( 'minimize', $( document ).scrollTop() > 40 );

		    // check window height
		    if( $( window ).height() > 600  && $( window ).width() > 767 ) {

		    	// stick part of the banner on top
				if( $( document ).scrollTop() >= 430 ) {
					$( 'header .hero' ).addClass( 'fixed' );
				} else {
					if( $( 'header .hero' ).hasClass( 'fixed' ) ) {
						$( 'header .hero' ).removeClass( 'fixed' );
					}
				}

				// check
			    if( $( '.filter' ).length > 0 ) {

			    	// vars
			    	var offset = $('.filter').offset(),
			    		topPos = parseInt( offset.top ) - 170;

			    	// stick part of the banner on top
					if( $( document ).scrollTop() >= topPos ) {
						$( '.filter' ).addClass( 'fixed' );
					} else {
						if( $( '.filter' ).hasClass( 'fixed' ) ) {
							$( '.filter' ).removeClass( 'fixed' );
						}
					}
			    }
		    }
		}

		// set on init
		toggleHomeClasses();
	};

	// method to minimize header on collection
	var initCollectionScroll = function(){

		// log
		logger( 'info','plugins.js / initCollectionScroll' );

		// windows scroll event
		$( window ).on( 'scroll touchmove', function(){

			// set class
			toggleCollectionTitlebar();
		});

		// function init
		function toggleCollectionTitlebar() {

			// toggle class
		    $( '.double.titlebar' ).toggleClass( 'minimize', $( document ).scrollTop() > 40 );
		}

		// set on init
		toggleCollectionTitlebar();
	};

	// method to minimize header on profile page
	var initProfileScroll = function(){

		// log
		logger( 'info','plugins.js / initProfileScroll' );

		// windows scroll event
		$( window ).on( 'scroll touchmove', function(){

			// set class
			toggleProfileClasses();
		});

		// function init
		function toggleProfileClasses() {

		    // check window height
		    if( $( window ).height() > 600  && $( window ).width() > 767 ) {

		    	// stick part of the banner on top
				if( $( document ).scrollTop() >= 226 ) {
					$( '.profilebar' ).addClass( 'fixed' );
				} else {
					if( $( '.profilebar' ).hasClass( 'fixed' ) ) {
						$( '.profilebar' ).removeClass( 'fixed' );
					}
				}

				// check
			    if( $( '.filter' ).length > 0 ) {

			    	// vars
			    	var offset = $('.filter').offset(),
			    		topPos = parseInt( offset.top ) - 226;

			    	// stick part of the banner on top
					if( $( document ).scrollTop() >= topPos ) {
						$( '.filter' ).addClass( 'fixed' );
					} else {
						if( $( '.filter' ).hasClass( 'fixed' ) ) {
							$( '.filter' ).removeClass( 'fixed' );
						}
					}
			    }
		    }
		}

		// set on init
		toggleProfileClasses();
	};

	// method to minimize header on homepage
	var initCarousel = function(){

		// log
		logger( 'info','plugins.js / initCarousel' );

		// check
		if( $( '.carousel' ).length > 0 ) {

			// init
			$( '.carousel' ).slick({
				'dots'   : true,
				'arrows' : false,
				'autoplay' : true,
				'autoplayspeed' : 5000
			});
		}

		// check
		if( $( '.carouselexhibition' ).length > 0 ) {

			// init
			$( '.carouselexhibition' ).slick({
				'accessibility' : true,
				'infinite' : false,
				'dots'   : true,
				'arrows' : true,
				'prevArrow' : '<a href="#" class="slick-prev"><img src="img/ic-left-arrow.png"></a>',
				'nextArrow' : '<a href="#" class="slick-next"><img src="img/ic-right-arrow.png"></a>'
			});

			// on btn click
			$( '.carouselexhibition .main .btn').click( function( e ){

				// prevent
				e.preventDefault();

				// click
				$( '.carouselexhibition' ).slick( 'slickNext' );
			});
		}
	};

	// method to expand text on exhibition
	var initExpandExhibitionText = function(){

		// log
		logger( 'info','plugins.js / initExpandExhibitionText' );

		// check
		if( $( '.exhibitionplayer .expand' ).length > 0 ) {

			// each
			$( '.exhibitionplayer .expand a').on( 'click', function( e ){

				// prevent
				e.preventDefault();

				// set
				$( this ).parent().parent().parent().toggleClass( 'expanded' );
			});
		}
	};

	// method to initialize search setting
	var initSearchSetting = function(){

		// log
		logger( 'info','plugins.js / initSearchSetting' );

		// check
		if( $( '.searchbar .settings' ).length > 0 ) {

			// attach
			$( '.searchbar .settings > a' ).on( 'click', function( e ){

				// e
				e.preventDefault();

				// on
				$( this ).parent().toggleClass( 'active' );

			});
		}		
	};

	// method to initialize tooltip
	var initTooltip = function(){

		// log
		logger( 'info','plugins.js / initTooltip' );

		// check
		if( $('[data-toggle="tooltip"]').length > 0 ) {

			// init
			$('[data-toggle="tooltip"]').tooltip();
		}
	};
};

return {
    
	EUSpaceApp: EUSpaceApp
  }
});
