define(['knockout', 'text!./top-bar.html', 'app', 'autocomplete', 'knockout-switch-case'], function(ko, template, app, autocomplete) {

  function TopBarViewModel(params) {
		this.route         = params.route;
		var self           = this;
		self.notifications = ko.observableArray();
		self.templateName = ko.observable('top-bar'); 
		
		ko.computed(function () {
			var tmp = app.currentUser.notifications.userNotifications().concat(app.currentUser.notifications.groupNotifications());
			tmp.sort(function(a, b) {
				return b.openedAt - a.openedAt;
			});
			self.notifications(tmp.splice(0, 5));
		});
		
		if(localStorage.getItem('logged_in')=="true"){
			self.templateName('dashboard-bar');
			
		}
		
		goToPage=function(data,event){
			 if(data=="#index" || data=="#"){
			      sessionStorage.removeItem("homemasonryscroll");
			      sessionStorage.removeItem("homemasonrycount");}
			   window.location.href=data;
			   event.preventDefault();
			   return false;
		}

		
		
		self.username      = app.currentUser.username;
		
		self.profileImage  = ko.computed(function() { return app.currentUser.avatar.Square() ? app.currentUser.avatar.Square() : 'images/user.png'; });
		self.organizations = app.currentUser.organizations;
		self.projects      = app.currentUser.projects;
		self.usergroups    = app.currentUser.usergroups;
		self.noticount     = ko.pureComputed(function() {
			return app.currentUser.notifications.unread() === 0 ? '' : app.currentUser.notifications.unread();
		});

		logout             = function() { app.logout(); };
		
		
		self.removeFlyouts=function() {
		
			$( '.action' ).removeClass( 'active' );

		};
	}

	return { viewModel: TopBarViewModel, template: template };
});
