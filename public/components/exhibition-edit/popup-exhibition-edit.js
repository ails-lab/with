define(['knockout', 'text!./popup-exhibition-edit.html', 'app'], function(ko, template, app) {

function ItemEditViewModel(params) {
    
	  var self = this;
	  self.route = params.route;
          self.title = ko.observable('');
          self.placeholder = ko.observable('');
          self.popUpMode = '';
          self.videoUrl = ko.observable('');
          self.thumbnailUrl = ko.observable('');
          self.videoAdded = ko.observable(false);
          self.textAdded = ko.observable(false);
          self.primaryButtonTitle = ko.observable('check');
          self.cancelButtonTitle = ko.observable('cancel');
          self.exhibitionItem = {};
          
          editItem = function(exhibitionItem, editMode) {
          
            console.log(exhibitionItem);
            self.exhibitionItem = exhibitionItem;
            self.setUpPopUp(exhibitionItem, editMode);
            $('#myModal').modal('show');
          };

          self.setUpPopUp = function(exhibitionItem, popUpMode) {
                
            self.popUpMode = popUpMode;
            if (self.popUpMode === 'PopUpVideoMode') {
             
                self.title('Add a youtube video');
                self.placeholder('Enter youtube video url'); 
                if ( self.videoAdded() ) {
                
                    self.cancelButtonTitle('delete');
                    self.primaryButtonTitle ('save');
                    //self.videoUrl(exhibitionItem.videoUrl);
                }
                else {
                    
                    self.cancelButtonTitle('cancel');
                    self.primaryButtonTitle ('check');
                } 
            }
            else {
                
                self.title('Add some text');
                self.placeholder('enter your description');    
            }
          }
          
          //setup button actions
          self.rightButtonTapped = function () {
            
            var methodName = self.primaryButtonTitle();
            methodName = methodName + self.popUpMode;
            console.log(methodName);
            self[methodName](); 
          };
          
           self.leftButtonTapped = function () {
            
            var methodName = self.cancelButtonTitle();
            methodName = methodName + self.popUpMode;
            self[methodName]();  
          };
          
          //right button actions
          self.checkPopUpVideoMode = function () {
            
            self.videoAdded(true);
            var youtube_video_id = self.videoUrl().match(/youtube\.com.*(\?v=|\/embed\/)(.{11})/).pop();
            var thumbnailPath = '//img.youtube.com/vi/'+youtube_video_id+'/0.jpg';
            var embeddedVideoPath = 'https://www.youtube.com/embed/'+youtube_video_id;
            self.thumbnailUrl(thumbnailPath);
            self.videoUrl(embeddedVideoPath);
            self.primaryButtonTitle('save');
          }
          self.savePopUpVideoMode = function () {
            
           $('#myModal').modal('hide');
          }
          //left button actions
          self.cancelPopUpVideoMode = function () {
            
             
             $('#myModal').modal('hide');
          }
          self.deletePopUpVideoMode = function () {
            
             self.videoUrl('');
             self.videoAdded(false);
             $('#myModal').modal('hide');
          }          
  }

  return { viewModel: ItemEditViewModel, template: template };
});
