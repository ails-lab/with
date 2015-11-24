define("app", ['knockout'], function (ko) {

	// overwrite default settings*/
	var self = this;

	self.WITHApp="";
	
	
	require(["./js/plugin",'./js/params'], function(plugin,params) {
		 self.WITHApp=new plugin.EUSpaceApp.ui({

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

		 self.WITHApp.projectName = params.SPACEPARAMS.projectName;
		 self.WITHApp.projectId = params.SPACEPARAMS.projectId;
		 self.WITHApp.featuredExhibition=params.SPACEPARAMS.featuredExhibition;
		 setTimeout(function(){ WITHApp.init(); }, 1000);
		
		 return {
				WITHApp: self.WITHApp
			};
		 
			});
	
	
	
	
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
	
	
	self.currentUser = {
			"_id": ko.observable(""),
			"email": ko.observable(),
			"username": ko.observable(),
			"firstName": ko.observable(),
			"lastName": ko.observable(),
			"gender": ko.observable(),
			"facebookId": ko.observable(),
			"googleId": ko.observable(),
			"image": ko.observable(),
			"recordLimit": ko.observable(),
			"collectedRecords": ko.observable(),
			"storageLimit": ko.observable(),
			"favorites": ko.observableArray(),
			"favoritesId": ko.observable(),
			"usergroups": ko.observableArray(),
			"organizations": ko.observableArray(),
			"projects": ko.observableArray(),
			"editables":ko.observableArray([])
		};
	
	self.isLogged=function(){
		var user=null;
		var usercookie=readCookie("PLAY_SESSION");
		if(usercookie)
		usercookie.replace(/\"/g, "");
		if(usercookie!=null){
		   var keys=ExtractQueryString(usercookie);	
		   if(self.currentUser._id()==undefined || self.currentUser._id().length==0){
		     if(keys["user"]){self.currentUser._id(keys["user"]);console.log(self.currentUser._id());self.getUser();}}
		    return (keys["user"]==undefined ? false : true);
		}else{return false;}
		
	};
	
	
	self.gotoWith=function(){
		    window.childwith=window.open('../assets/index.html#mycollections', 'with');
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
	    var aQueryString = cookie.replace(/\"/g, "").split("&");
	    for (var i = 0; i < aQueryString.length; i++) {
	        var aTemp = aQueryString[i].split("=");
	        if (aTemp[1].length > 0) {
	            oResult[aTemp[0]] = unescape(aTemp[1]);
	        }
	    }
	    return oResult;
	} 
	
	self.getUser = function() {
		
		$.ajax({
			url: '/user/' + self.currentUser._id(),
			type: 'GET',
			success: function(data, text) {
				loadUser(data, true);
			}
		});
	};
	
	loadUser = function (data, loadCollections) {
		self.currentUser._id(data.dbId);
		self.currentUser.email(data.email);
		self.currentUser.username(data.username);
		self.currentUser.firstName(data.firstName);
		self.currentUser.lastName(data.lastName);
		self.currentUser.gender(data.gender);
		self.currentUser.facebookId(data.facebookId);
		self.currentUser.googleId(data.googleId);
		self.currentUser.recordLimit(data.recordLimit);
		self.currentUser.collectedRecords(data.collectedRecords);
		self.currentUser.storageLimit(data.storageLimit);
		self.currentUser.image(data.image);
		self.currentUser.favoritesId(data.favoritesId);
		self.currentUser.usergroups(data.usergroups);
		self.currentUser.organizations(data.organizations);
		self.currentUser.projects(data.projects);
        self.loadFavorites();
		if (typeof (loadCollections) === 'undefined' || loadCollections === true) {
			return [self.getEditableCollections()]; 
		}
	};

	
	self.loadFavorites = function () {
		$.ajax({
				url: "/collection/favorites",
				type: "GET",
			})
			.done(function (data, textStatus, jqXHR) {
				self.currentUser.favorites(data);
				for(var i in data) {
					if($("#" + data[i])){
						$("#" + data[i]).addClass('active');
					}
				}
			})
			.fail(function (jqXHR, textStatus, errorThrown) {
				$.smkAlert({text:'Error loading Favorites', type:'danger', time: 10});
				console.log("Error loading favorites!");
		});
	};

	
	self.getEditableCollections = function () {
		return $.ajax({
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			url: "/collection/list",
			processData: false,
			data: "offset=0&count=500&isExhibition=false&directlyAccessedByUserName="+JSON.stringify([{user:self.currentUser.username(),rights:"WRITE"}]),
		}).done(
			function (data) {
				var array = JSON.parse(JSON.stringify(data.collectionsOrExhibitions));
				self.currentUser.editables.removeAll();
				array.forEach(function (item) {
					self.currentUser.editables.push({
						title: item.title,
						dbId: item.dbId
					});
				});
				console.log(self.currentUser.editables());
			}).fail(function (request, status, error) {
		});
	};
	
    
	
	likeItem = function (record, update) {
		var id, data;
		if (ko.isObservable(record.externalId)) {
			id = record.externalId();
			data = {
				source: record.source(),
				sourceId: record.recordId(),
				title: record.title(),
				provider: record.provider(),
				creator: record.creator(),
				description: record.description(),
				rights: record.rights(),
				type: '',
				thumbnailUrl: record.thumb(),
				sourceUrl: record.view_url(),
				collectionId: self.currentUser.favoritesId(),
				externalId: record.externalId()
			};
		}
		else {
			id = record.externalId;
			data = {
				source: record.source,
				sourceId: record.recordId,
				title: record.title,
				provider: record.provider,
				creator: record.creator,
				description: record.description,
				rights: record.rights,
				type: '',
				thumbnailUrl: record.thumb,
				sourceUrl: record.view_url,
				collectionId: self.currentUser.favoritesId,
				externalId: record.externalId
			};
		}
		if (!self.isLiked(id)) {	// Like
			$.ajax({
				type: "POST",
				url: "/collection/liked",
				data: JSON.stringify(data), //ko.toJSON(record),
				contentType: "application/json",
				success: function (data, textStatus, jqXHR) {
					self.currentUser.favorites.push(id);
					update(true);
				},
				error: function (jqXHR, textStatus, errorThrown) {
					$.smkAlert({text:'An error has occured. Please try again.', type:'danger', time: 10});
					console.log(errorThrown);
				}
			});
		} else {	// Unlike
			$.ajax({
				type: "DELETE",
				url: "/collection/unliked/" + id,
				success: function (data, textStatus, jqXHR) {
					self.currentUser.favorites.remove(id);
					update(false);
				},
				error: function (jqXHR, textStatus, errorThrown) {
					$.smkAlert({text:'An error has occured. Please try again.', type:'danger', time: 10});
					console.log(errorThrown);
				}
			});
		}
	};
	
	self.isLiked = function (id) {
		return self.currentUser.favorites.indexOf(id) < 0 ? false : true;
	};
	
	 /* for all isotopes binding */
	 function initOrUpdate(method) {
			return function (element, valueAccessor, allBindings, viewModel, bindingContext) {
				function isotopeAppend(ele) {
					if (ele.nodeType === 1 && ele.className.indexOf ("item")>-1) { // Element type
						$(ele).css("display","none");
							
						$(element).imagesLoaded(function () {
							if(ko.contextFor(ele).$parent.loading!=undefined){
							  ko.contextFor(ele).$parent.loading(false);
							  ko.contextFor(ele).$data.isLoaded(true); 
							}
							$(element).isotope('appended', ele).isotope('layout');
							$(ele).css("display","");
							
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
					/* this is very important, when hitting back button this makes it scroll to correct position*/
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
			scroll: scroll,
			likeItem: likeItem,
			isLiked: isLiked,
			isLogged: isLogged
	 }
	 
	 
	
});
