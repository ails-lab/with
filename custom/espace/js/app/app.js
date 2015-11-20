define("app", ['knockout'], function (ko) {

	// overwrite default settings*/
	var self = this;
	
	require(["./js/plugin","js/vendor/slick.js/slick/slick.min"], function(EUSpaceApp,slick) {
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

		// window.EUSpaceUI.init();
		 return {
				EUSpaceUI: window.EUSpaceUI
			};
		 
			});
	
	
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
	
	
	
    
	 /* for all isotopes binding */
	 function initOrUpdate(method) {
			return function (element, valueAccessor, allBindings, viewModel, bindingContext) {
				function isotopeAppend(ele) {
					if (ele.nodeType === 1 && ele.className.indexOf ("item")>-1) { // Element type
						$(ele).css("display","none");
							
						$(element).imagesLoaded(function () {
							if(ko.contextFor(ele).$parent.loading!=undefined)
							  ko.contextFor(ele).$parent.loading(false);
							$(element).isotope('appended', ele).isotope('layout');
							
						});
						
					}
					
				}

				
				
				function attachCallback(valueAccessor) {
					return function() {
						return {
							data: valueAccessor(),
							afterAdd: isotopeAppend
						};
					};
				}

				var data = ko.utils.unwrapObservable(valueAccessor());
				//extend foreach binding
				ko.bindingHandlers.foreach[method](element,
					 attachCallback(valueAccessor), // attach 'afterAdd' callback
					 allBindings, viewModel, bindingContext);

				if (method === 'init') {
					/* this is very important, when hiting back button this makes it scroll to correct position*/
					var height = $(element).height();

					if( height > 0 ) { // or some other number
					    $(element).height( height );
					}
					
					/* finished back button fix*/
					 $(element).imagesLoaded(function () {
						$(element).isotope({
							itemSelector: '.item',
							transitionDuration: transDuration,
							masonry: {
								columnWidth		: '.sizer',
								percentPosition	: true
							}
						});
						
					});

					ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
						$(element).isotope("destroy");
					});
					
				} 
				
			};
		}
	 
	 /* scroll binding for infinite load*/
	 ko.bindingHandlers.scroll = {
				updating: true,

				init: function (element, valueAccessor, allBindingsAccessor) {
					var self = this;
					self.updating = true;
					ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
						$(window).off("scroll.ko.scrollHandler");
						self.updating = false;
					});
				},

				update: function (element, valueAccessor, allBindingsAccessor) {
					var props = allBindingsAccessor().scrollOptions;
					var offset = props.offset ? props.offset : "0";
					var loadFunc = props.loadFunc;
					var load = ko.utils.unwrapObservable(valueAccessor());
					var self = this;

					if (load) {
						$(window).on("scroll.ko.scrollHandler", function () {
							if ($(window).scrollTop() >= $(document).height() - $(window).height() - 300) {
								if (self.updating) {
									loadFunc();
									self.updating = false;
								}
							} else {
								self.updating = true;
							}
							
							 if ($(window).scrollTop() > 100) {
									$('.scroll-top-wrapper').addClass('show');
								} else {
									$('.scroll-top-wrapper').removeClass('show');
								}
						});
					} else {
						element.style.display = "none";
						$(window).off("scroll.ko.scrollHandler");
						self.updating = false;
					}
				}
			};


	 
	 return {
			initOrUpdate: initOrUpdate,
			scroll: scroll
	 }
	 
	 
	
});
