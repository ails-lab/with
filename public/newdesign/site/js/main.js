
//  vars
var WITHApp = WITHApp || {},
	WITHUI;

// on ready
$( document ).ready(function(){

	// new app
	WITHUI = new WITHApp.ui({

		// page name
		page  	  : $( 'body' ).attr( 'data-page' ),

		// masonry
		mSelector : '.grid',
		mItem	  : '.item',
		mSizer	  : '.sizer',

		// mobile menu
		mobileSelector : '.mobilemenu',
		mobileMenu 	   : '.main .menu'
	});

	// init
	WITHUI.init();
});