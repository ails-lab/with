
define(function () {
 
//  vars
var EUSpaceApp = EUSpaceApp || {};
var console = (window.console = window.console || {}),
	debug = true;

var inited=false;

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
     settings.page=$( "body" ).attr( 'data-page' );
	 if(!inited){	
		inited=true;
		
		// log
		logger( 'info','plugins.js / init' );

		// init mobile menu
		initMobileMenu();

				// initialize media tooltip
		initTooltip();
		
		
		// initialize inline viewer for item
		//initInlineViewer();

		
	 }
	// initialize script on homepage only
		if( settings.page === 'home' ) {

			// initialize homepage header minimisation
			initHomeScroll();
			initIsotope();
		}

		else if(settings.page === 'about') {

			// init carousel
			initCarousel();
		}
		else if( settings.page === 'exhibition') {

			// init carousel
			/*initCarousel();

			// init expand on exhibition
			initExpandExhibitionText();

			
			// init search setting bar
			//initSearchSetting();

			// initialize media tooltip
			initTooltip();
				// init exhibition image zoom
			initImageZoom();*/
			
			
			
		}
		// initialize script on homepage only
		else if( settings.page === 'profile' ) {

			// initialize homepage header minimisation
			initProfileScroll();
			initIsotope();
		}

		
		else if( settings.page === 'search' ) {

			// initialize homepage header minimisation
			//initSearchViewToggle();
			initIsotope();
			//initInlineViewer();
		}
		
		
		else if( settings.page === 'collection' ) {

			// initialize homepage header minimisation
			initCollectionScroll();
			initIsotope();
			//initInlineViewer();
		}
	 
	};

	this.initSearch=function(){
		/*these are needed after search page has been loaded*/
		// init search setting bar
			// initialize search view toggle button
		//initSearchViewToggle();

		// init adjustment for search column view
		initSearchColumnAdjustment();
	}
	
	this.initTooltip=function(){
		initTooltip();
	}
	
	this.initSearchColumnAdjustment=function(){
		initSearchColumnAdjustment();
	}
	
	
	this.resetMobileMenu=function(){
		var $menu = $( settings.mobileMenu );
		
          if($menu.hasClass("visible")){
        	  $menu.toggleClass( 'visible' );

				// toggle button
				$( settings.mobileSelector ).toggleClass( 'active' );
        	  
          }
		
	}
	
	this.initProfile=function(){
		initProfileScroll();
	}
	
	this.initIsotope=function(){
		initIsotope();
	}

	this.initCarousel=function(){
		initCarousel();
	}

	this.initCharacterLimiter=function(){
		initCharacterLimiter();
	}
	
	this.initExpandExhibitionText=function(){
		initExpandExhibitionText();
	}

	this.initMobileMenu=function(){
		initMobileMenu();
	}

	this.initImageZoom=function(){
		initImageZoom();
	}
	
	this.initInlineViewer=function(){
		initInlineViewer();
	}
	
	this.initHomeScroll=function(){
		this.initHomeScroll();
	}
	
	this.initChart=function(dataPercent){
		initChart(dataPercent);
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
				$( settings.mSelector ).isotope("layout");
				});
				
			}
		});

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
		// limit character description
		
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
				'prevArrow' : '<a class="slick-prev"><img src="img/ic-left-arrow.png"></a>',
				'nextArrow' : '<a class="slick-next"><img src="img/ic-right-arrow.png"></a>'
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
		/*if( $( '.searchbar .settings' ).length > 0 ) {

			// attach
			$( '.searchbar .settings > a' ).on( 'click', function( e ){

				// e
				e.preventDefault();

				// on
				$( this ).parent().toggleClass( 'active' );

			});
		}	*/	
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
		// popover
		if( $('[data-toggle="popover"]').length > 0 ) {

			// init
			$('[data-toggle="popover"]').popover();
		}

	};

	// method to initialize exhibition image zoom
	var initImageZoom = function(){

		// log
		logger( 'info','plugins.js / initImageZoom' );

		// check
		if( $( '.imagezoom' ).length > 0 ) {

			// init
			$( '.imagezoom' ).magnificPopup({ type:'image' });
		}

		// check
		if( $( '.iframezoom' ).length > 0 ) {

			// init
			$( '.iframezoom' ).magnificPopup({ type:'iframe' });
		}
	};

	// method to initialize search view toggle
	var initSearchViewToggle = function(){

		// log
		logger( 'info','plugins.js / initSearchViewToggle' );

		// check
		if( $( '.searchbar' ).length > 0 ) {

			// set
			$( '.searchbar .view li a' ).on( 'click', function( e ){

				// prevent
				e.preventDefault();

				// get view
				var view = $( this ).attr( 'data-view' );

				// set
				$( '.searchlist' ).hide();
				$( '#'+view+'list' ).show();

				// set button
				$( '.searchbar .view li').removeClass( 'active' );
				$( this ).parent().addClass( 'active' );

				// check
				if( view === 'grid' ) {

					// reload
					$( settings.mSelector ).isotope();
				}
			});
		}
	};

	// init inline viewer
	var initInlineViewer = function(){

		// log
		logger( 'info','plugins.js / initInlineViewer' );

		// get data-item attr
		$( '[data-view="inline"]' ).each(function(){

			// click
			$( this ).on( 'click', function(e){

				// show 
				e.preventDefault();
				$( '.itemview' ).fadeIn();
				$('[role="main"]').addClass('itemopen');
				//$('body').css('overflow','hidden');
				adjustHeight();
			});
		});

		

		// close event
		$( '.itemview .menubar .action .close a' ).on( 'click', function( e ){

			
			// show 
			$( '.itemview' ).fadeOut();

			$('body').css('overflow','visible');
		
			// enable scroll on main
			//$('[role="main"]').removeClass('itemopen');
		});

		// function
		function adjustHeight() {

			// vars 
			var wHeight = $( window ).height(),
				wWidth = $( window ).width(),
				itemHeight = wHeight - 70;

			// check
			if( wWidth >= 1200 ) {

				// set height
				$( '.itemopen .itemview' ).css({
					height : itemHeight+"px"
				});
			}
		}
	};

	// method to initialize search column width
	var initSearchColumnAdjustment = function(){

		// log
		logger( 'info','plugins.js / initSearchColumnAdjustment' );

		// internal method
		function adjustColumnSize() {

			// reset
			$( '#columnlist .row .column' ).attr( 'style','');

			// get the screen width
			var screenWidth = $( window ).width(),
				columnCount = $( '#columnlist .row .column' ).length,
				columnWidth = $( '#columnlist .row .column' ).first().width();

			// check
			if( (screenWidth > (columnCount * columnWidth)) && (screenWidth > 768) ) {
				
				var newWidth = 100 / columnCount;
				$( '#columnlist .row .column' ).css({
					'width' : newWidth+'%'
				});
			}
		}

		// initial adjustment
		adjustColumnSize();

		// window resize
		$( window ).resize(function(){
			
			// initial adjustment
			adjustColumnSize();
		});
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
	
	// method to initialize chart
	var initChart = function(dataPercent){
		// log
		logger( 'info','plugins.js / initChart' );
		// check
		if( $( '.chart' ).length > 0 ) {

			// each
			$( '.chart' ).each( function(){

				// pie chart
				$( this ).easyPieChart({
					'scaleColor' : false,
					'size': 160,
					'lineCap' : 'round',
					'lineWidth' : 6,
					'barColor' : '#6fa130'
				});
			});
			 //update instance after 5 sec
		    setTimeout(function() {
		        $('.chart').data('easyPieChart').update(dataPercent);
		    }, 5);
		}
	};
	
	 
};

return {
    
	EUSpaceApp: EUSpaceApp
	
  }
});
