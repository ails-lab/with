
//  vars
var EUSpaceApp = EUSpaceApp || {},
	EUSpaceUI;

// on ready
$( document ).ready(function(){

	// new app
	EUSpaceUI = new EUSpaceApp.ui({

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
	EUSpaceUI.init();
});