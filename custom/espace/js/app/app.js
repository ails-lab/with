define("app", ['knockout'], function (ko) {

	// overwrite default settings*/
	var self = this;
	//project id goes here
	
	self.settings = $.extend({

		// page
		page  	  : 'default',

		// masonry
		mSelector : '.grid',
		mItem	  : '.item',
		mSizer	  : '.sizer',

		// mobile menu
		mobileSelector : '.mobilemenu',
		mobileMenu 	   : '.main .menu'
	});
	self.transDuration = '0.4s';
	var isFirefox = typeof InstallTrigger !== 'undefined'; // Firefox 1.0+
	if (isFirefox) {
		self.transDuration = 0;
	}

	 require(["./js/plugin"], function(EUSpaceApp) {
		 window.EUSpaceUI=new EUSpaceApp.EUSpaceApp.ui({

		 		// page name
		 		page  	  : $( 'body' ).attr( 'data-page' ),

		 		// masonry
		 		mSelector : '.grid',
		 		mItem	  : '.item',
		 		mSizer	  : '.sizer',

		 		// mobile menu
		 		mobileSelector : '.mobilemenu',
		 		mobileMenu 	   : '.main .menu'
		 	})

		 window.EUSpaceUI.init();
			});
	
	
	
});
