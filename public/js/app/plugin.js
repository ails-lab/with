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

			// mobile
		mobile : false,

		// mobile menu
		mobileTrigger : '.mobile-trigger'
	}, 
	custom || {});

   // moblie
	if ( $( window ).width() < 767 ) {

		settings.mobile = true;
	}
   
	// this
	this.init = function(){

		// log
		logger( 'info','plugins.js / init' );
		settings.page=$( "body" ).attr( 'data-page' );
		 

		// init isotope
		initIsotope();

		// limit character description
		//only applied to home page so moved to maincontent component ->initCharacterLimiter();

		// init filter stick plugin
		//only applied to home page so moved to maincontent component ->initFilterStick();
		
		//need to figure out where these should be called.sme might go to individual components
		
		// init mobile main menu trigger
		initMobileMenu();

		// init wrapper for moblie filter
		initMobileFilter();

		// init tooltip
		initTooltip();

		// hide message function
		hideMessage();

		// toggle list function
		changeList();

		// init all action in collection page
		/*this call is moved to every component that require the flyouts*/
		//tabAction();

		// initialize nicescroll plugin
		//initNicescroll();

		// init media viewer
		initMediaViewer();

		// init carousel
		initCarousel();

		

		initSearchColumnAdjustment();

		// init show more info 
		/*this is probably for item view only when we have more data to show*/
		//initShowMore();

		// init itemselect on search grid to be implemented after event
		//initMultipleSelect();

		// init smoothscroll
		initSmoothScroll();

		// init expand on exhibition 
		initExpandExhibitionText();
				
	};
	
	this.initCharacterLimiter=function(){
		initCharacterLimiter();
	}
	
	this.initSearchColumnAdjustment=function(){
		initSearchColumnAdjustment();
	}
	
	this.initIsotope=function(){
		initIsotope();
	}
	
	this.initCarousel=function(){
		initCarousel();
	}

	this.initExpandExhibitionText=function(){
		initExpandExhibitionText();
	}
	
	this.tabAction=function(){
		tabAction();
	}
	
	this.changeList=function(){
		changeList();
	}
	
	this.initImageZoom=function(){
		initImageZoom();
		
	}
	
	this.initTooltip=function(){
		initTooltip();
	}
	
	this.initChart=function(dataPercent){
		initChart(dataPercent);
	}
	
	// method to toggle search view mode
	var initSearchViewToggle = function(){

		// log
		logger( 'info','plugins.js / initSearchViewToggle' );

		if ( $( '.searchbar .view' ).length !== 0 ) {

			$( '.searchbar .view a' ).on('click', function( e ) {

				e.preventDefault();
				e.stopPropagation();

				$( '.searchbar .view li' ).removeClass( 'active' );
				$( this ).parent().addClass( 'active' );

				var data = $( this ).attr( 'data-view' );

				$( '.searchresults').find( 'section' ).removeClass( 'active' );
				$( '.searchresults #'+ data + 'list' ).toggleClass( 'active' );

				if ( data === 'grid' ) {
					console.log( 'test');
					$( settings.mSelector ).isotope();
				}
			})
		}
	}

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

	// method to initialize search setting
	var initSearchSetting = function(){

		// log
		logger( 'info','plugins.js / initSearchSetting' );

		// handled by component
		/*if( $( '.searchbar .settings' ).length > 0 ) {

			// attach
			$( '.searchbar .settings > a' ).on( 'click', function( e ){

				// e
				e.preventDefault();

				// on
				$( this ).parent().toggleClass( 'active' );

			});
		}*/		
	};

	// method to initialize nicescroll plugin
	// dependency nicescroll.js
	var initNicescroll = function() {

		// log
		logger( 'info','plugin.js / initNicescroll' );

		$( 'body' ).niceScroll({
			autohidemode: false
		});	

		// check action
		if ( $( 'section.action' ).length !== 0 ) {

			$( 'section.action' ).niceScroll();			
		}
	};

	// method to init all the action in the collection page
	var tabAction = function() {

		// log
		logger( 'info','plugin.js / Collection Action' );

		// close
		
			$( '.cancel' ).on("click", function( e ) {
				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.searchresults' ).removeClass( 'openfilter');
				// check
				/*if( $( '.searchresults' ).length > 0 ) {
					if( $( '.searchresults' ).hasClass( 'openfilter' ) ) {
						$( '.searchresults' ).removeClass( 'openfilter' );
					}
				}*/

			});
		
		// open edit & access
		if ( $( '.action-group' ).length !== 0 ) {

			// edit
			$( '.editaction' ).on("click", function( e ) {
				e.preventDefault();

				$( '.action' ).removeClass( 'active' );
				$( '.action.edit' ).addClass( 'active' );

			});

			// access
			$( '.fa-paper-plane-o' ).on("click", function( e ) {
				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.access' ).addClass( 'active' );

			});

			// access
			$( '.members' ).on("click", function( e ) {
				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.access' ).addClass( 'active' );

			});
			/*$( '.collectbutton' ).on("click", function( e ) {
				// e
				e.preventDefault();

				$( '.action' ).removeClass( 'active' );
				$( '.action.collect' ).addClass( 'active' );

			});*/

			/*$( '.fa-download' ).on("click", function( e ) {

				$( '.action' ).removeClass( 'active' );
				$( '.action.collect' ).addClass( 'active' );

			});*/
		}

		
		// credit
		// open upload
		if ( $( 'a.creditbutton' ).length !== 0 ) {

			$( 'a.creditbutton' ).click( function( e ) {

				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.creditsection' ).addClass( 'active' );				
			});

		}

		// credit
		// open upload
		if ( $( 'a.addmediabutton' ).length !== 0 ) {
			

			$( 'a.addmediabutton' ).click( function( e ) {


				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.addmediasection' ).addClass( 'active' );				
			});

		}
		// open upload
		if ( $( 'a.upload' ).length !== 0 ) {

			$( 'a.upload' ).on("click", function( e ) {

				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.upload' ).addClass( 'active' );				
			});

		}
		
		
		
		// open uploadimage
		if ( $( 'a.uploadimage' ).length !== 0 ) {

			$( 'a.uploadimage' ).click( function( e ) {

				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.upload' ).addClass( 'active' );				
			});

		}

		// open new
		if ( $( 'a.new' ).length !== 0 ) {

			$( 'a.new' ).on("click", function( e ) {

				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.new' ).addClass( 'active' );				
			});

		}
		
		// open thesaurus
		if ( $( 'a.thesaurus' ).length !== 0 ) {

			$( 'a.thesaurus' ).on("click", function( e ) {

				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.thesaurus' ).addClass( 'active' );				
			});

		}

		// open annotate collection
		if ( $( 'a.annotate-col' ).length !== 0 ) {

			$( 'a.annotate-col' ).on("click", function( e ) {

				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.annotate-col' ).addClass( 'active' );				
			});

		}

		// open detail area
		if ( $( 'a.detail-control' ).length !== 0 ) {

			$( 'a.detail-control' ).on("click", function( e ) {

				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.api' ).addClass( 'active' );				
			});
		}

		// open members acces
		if ( $( '.members-access' ).length !== 0 ) {

			$( '.members-access' ).on("click", function( e ) {

				e.preventDefault();
				$( '.action' ).removeClass( 'active' );
				$( '.action.members' ).addClass( 'active' );
			});
		}

		// add textarea
		if ( $( '.action' ).length !== 0 ) {

			var textarea = '<textarea rows="10" placeholder="Describe your collection"></textarea>';

			$( '.action .add' ).on("click", function( e ) {

				e.preventDefault();
				$( this ).hide();
				$( this ).first().after( textarea );

			});
		}
		
		// search settings
		/*this might need to be removed
		$( '.searchbar .settings > a' ).click( function( e ) {

			// prevent
			e.preventDefault(); 

			$( '.action' ).removeClass( 'active' );
			$( '.action.searchfilter' ).addClass( 'active' );
			$( '.searchresults' ).addClass( 'openfilter');
		});*/

		// profile
		$( '.showprofile' ).on('click', function(e) {

			// prevent
			e.preventDefault();

			$( '.action' ).removeClass( 'active' );
			$( '.action.profile' ).addClass( 'active' );

		});
		
		// profile
		$( '.editbox' ).on('click', function(e) {
			// prevent
			e.preventDefault();
			$( '.action' ).removeClass( 'active' );
			$( '.action.editsection' ).addClass( 'active' );

		});
		
		$( '.videobox' ).on('click', function(e) {
			e.preventDefault();
			$( '.action' ).removeClass( 'active' );
			$( '.action.editvideo' ).addClass( 'active' );
		});

		// multiple select
		$( '#multiplecollect' ).on('click', function(e) {
			// prevent
			e.preventDefault();

			$( '.action' ).removeClass( 'active' );
			$( '.action.collect' ).addClass( 'active' );
		});
		
		initTooltip();
		
	};
	
	// method to initialize isotope
	// dependency: js/vendor/isotope/
	var initIsotope = function(){

		// log
		logger( 'info','plugins.js / initIsotope' );

		// initialize masonry after window.load
		// safari seems to have problem when initialize on dom ready

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
					/*,
					  onLayout: function( $elems, instance ) {
						    // `this` refers to jQuery object of the container element
						  console.log("onLayout");
						    console.log( this.height() );
						    var height = $( settings.mSelector ).height();

							if( height > 0 ) { // or some other number
								$( settings.mSelector ).height( height );
							}
				}*/
				
			});
				$( settings.mSelector ).isotope("layout");
				});
				
			}
		});
		

		// init filter
		
	/*	if( $( '.filter' ).length > 0 ) {

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

		}*/
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
				if( $( "body" ).attr( 'data-page' )=== 'home' ) {
				var offset = $( '.partners' ).offset(),
				topPos = offset.top - 74;

				// set class
				if ( $( document ).scrollTop() >= topPos ){

					$( '.filter' ).addClass('unstick');

				} else {

					$( '.filter' ).removeClass('unstick');

				}
				}
			});
		}
	}};
	
	// method to initialize main navigation in mobile view
	var initMobileMenu = function(){

		// log
		logger( 'info', 'plugin.js / initMobileMenu');

		// check
		if ( settings.mobile && $( settings.mobileTrigger ).length !== 0 ) {

			$( settings.mobileTrigger ).click( function( e ){

				$( this ).toggleClass( 'active' );
				$( 'nav' ).toggleClass( 'active' );
			});
		}
	};

	// method to wrap filter with dropdown menu in mobile view
	var initMobileFilter = function(){

		// log
		logger( 'info', 'plugin.js / initMobileFilter');

		// check
		if ( settings.mobile && $( '.filter .nav').length !== 0 ) {

			// var
			var dropdownHTML = '<a href="#" class="dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">Filter <span></span></a>';

			$( '.filter .nav' ).addClass( 'dropdown-menu' ).wrap('<div class="select-filter dropdown"></div>');
			$( '.select-filter' ).prepend( dropdownHTML );
		}
	};

	// method to initialize tooltip funciton
	// dependency bootstrap.js
	var initTooltip = function() {
		// init
		$('[data-toggle="tooltip"]').tooltip(); 
	};

	// method to initialize hide message function
	var hideMessage = function() {

		// log
		logger( 'info', 'plugin.js / hideMessage');

		// check
		if ( $( 'section.message' ).length !== 0 ) {

			$( '.message-body a' ).on('click', function( e ) {

				e.preventDefault();
				$( this ).closest( '.message' ).addClass( 'close' );

			});
		}
	};

	// method to toggle collection listing from grid to list
	var changeList = function() {
		// check
		if ( $( '.list-control' ).length !== 0 ) {

			$( '.list-control .fa-th' ).on('click', function(e) {
				e.preventDefault();

				$( this ).toggleClass( 'active' );
				$( this ).parent().find( '.fa-bars' ).toggleClass( 'active' );
				$( '.list-body' ).toggleClass('grids');
				$( '.list-body' ).toggleClass('list');

			});

			$( '.list-control .fa-bars' ).on('click', function( e ) {
				e.preventDefault();

				$( this ).toggleClass( 'active' );
				$( this ).parent().find( '.fa-th' ).toggleClass( 'active' );
				$( '.list-body' ).toggleClass('grids');
				$( '.list-body' ).toggleClass('list');
			});

		}
	};

	// method to initialize media viewer
	var initMediaViewer = function(){

		// log
		logger( 'info', 'plugin.js / initMediaViewer');

		// check
		if( $( '.mediaviewer' ).length > 0 ) {

			// on click
			$( '.mediaviewer' ).on( 'click', function( e ){

				//
				e.preventDefault();

				// disable scroll on main
				$('body').css('overflow','hidden');

				// open
				$( '.itemview').fadeIn();
			});

			// on click
			$( '.closemedia' ).on( 'click', function( e ){

				//
				e.preventDefault();

				// open
				$( '.itemview').fadeOut();

				// disable scroll on main
				$('body').css('overflow','visible');
			});
		}

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
				'prevArrow' : '<a class="slick-prev"><img src="img/ui/ic-left-arrow.png"></a>',
				'nextArrow' : '<a class="slick-next"><img src="img/ui/ic-right-arrow.png"></a>'
			});

			// on btn click
			$( '.carouselexhibition .main .btn').on('click', function( e ){

				// prevent
				e.preventDefault();

				// click
				$( '.carouselexhibition' ).slick( 'slickNext' );
			});
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

	
	// method to initialize more info on item page
	var initShowMore = function(){

		// log
		logger( 'info','plugins.js / initShowMore' );

		// check
		if( $( '.listmore' ).length > 0 ) {

			// each
			$( '.listmore' ).each( function(){

				// get target
				var target = $( this ).attr( 'data-target' );

				// click
				$( this ).on( 'click', function( e ){

					// e
					e.preventDefault();

					// check
					if( $('#'+target).hasClass( 'show' )) {

						// remove
						$('#'+target).removeClass( 'show' )
						$( this ).html( 'SHOW MORE INFO' );
					} else {
						// add
						$('#'+target).addClass( 'show' )
						$( this ).html( 'SHOW LESS INFO' );
					}
				});
			});
		}	
	};
	// method to initialize multiple select on search
	var initMultipleSelect = function(){

		// log
		logger( 'info','plugins.js / initShowMore' );

		// check
		if( $( '.selectitem' ).length > 0 ) {

			// on click
			$( '.selectitem' ).each( function(){

				// on click
				$( this ).click( function() {

					// save
					var $this = $(this);

					// check 
					if ($this.is(':checked')) {
						
						// set selected
						$this.closest( '.item' ).addClass( 'selected' );

						// show
						showHideCollectButton();
					} else {
						// set selected
						$this.closest( '.item' ).removeClass( 'selected' );

						// show
						showHideCollectButton();
					}
				});
			});
		}

		// function
		function showHideCollectButton() {

			// check
			if( $( '.item.selected').length > 0 ) {
				$( '#multiplecollect').addClass( 'show' );
			} else {
				$( '#multiplecollect').removeClass( 'show' );
			}
		}
	};

	// init smoothscroll
	var initSmoothScroll = function(){

		// log
		logger( 'info','plugins.js / initSmoothScroll' );

		// check
		if( $( '.about' ).length > 0 ) {
			// init
			$('a[href*="#"]:not([href="#"])').click(function() {
				if (location.pathname.replace(/^\//,'') == this.pathname.replace(/^\//,'') && location.hostname == this.hostname) {
					var target = $(this.hash);
					target = target.length ? target : $('[name=' + this.hash.slice(1) +']');
					if (target.length) {
						$('html, body').animate({
							scrollTop: target.offset().top - 72
						}, 1000);
						return false;
					}
				}
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
    
	WITHApp: WITHApp
	
  }
});
