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
	
	self.isLogged=function(){
		var user=null;
		var usercookie=readCookie("PLAY_SESSION")
		if(usercookie)
		usercookie.replace(/\"/g, "");
		if(usercookie!=null){
		   var keys=ExtractQueryString(usercookie);	
		  
         
		    return (keys["user"]==undefined ? false : true);
		}else{return false;}
		
	};
	
	self.gotoWith=function(){
		
		    window.childwith=window.open('http://with.image.ntua.gr/assets/index.html#mycollections', 'with');
		  
		    window.childwith.focus();
		   		
	}
	
	function readCookie(name) {
	    var nameEQ = encodeURIComponent(name) + "=";
	    var ca = document.cookie.split(';');
	    for (var i = 0; i < ca.length; i++) {
	        var c = ca[i];
	        while (c.charAt(0) === ' ') c = c.substring(1, c.length);
	        if (c.indexOf(nameEQ) === 0) return decodeURIComponent(c.substring(nameEQ.length, c.length));
	    }
	    return null;
	}
	  
	function ExtractQueryString(cookie) {
	    var oResult = {};
	    var aQueryString = cookie.split("&");
	    for (var i = 0; i < aQueryString.length; i++) {
	        var aTemp = aQueryString[i].split("=");
	        if (aTemp[1].length > 0) {
	            oResult[aTemp[0]] = unescape(aTemp[1]);
	        }
	    }
	    return oResult;
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
	
    require(["js/vendor/slick.js/slick/slick.min","js/plugin"], function(slick,EUSpaceApp) {
		 var EUSpaceUI=new EUSpaceApp.EUSpaceApp.ui({

		 		// page name
		 		page  	  : document.body.getAttribute("data-page"),

		 		// masonry
		 		mSelector : '.grid',
		 		mItem	  : '.item',
		 		mSizer	  : '.sizer',

		 		// mobile menu
		 		mobileSelector : '.mobilemenu',
		 		mobileMenu 	   : '.main .menu'
		 	})

		 EUSpaceUI.init();
			});
  
	
});
