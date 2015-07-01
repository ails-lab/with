define(['knockout', 'text!./popup-exhibition-edit.html', 'app'], function(ko, template, app) {

function ItemEditViewModel(params) {
    
	  var self = this;
	  self.route = params.route;
          self.title = ko.observable('');
          self.placeholder = ko.observable('');
          self.popUpMode = '';
          self.modeIsVideo = ko.observable(false);
          self.videoUrl = ko.observable('');
          self.thumbnailUrl = ko.observable('');
          self.videoAdded = ko.observable(false);
          self.textAdded = ko.observable(false);
          self.textInput = ko.observable('');
          self.primaryButtonTitle = ko.observable('check');
          self.cancelButtonTitle  = ko.observable('cancel');
          self.exhibitionItem = {};
          
          editItem = function(exhibitionItem, editMode) {
          
            self.modeIsVideo(false);
            self.exhibitionItem = exhibitionItem;
            self.setUpPopUp(exhibitionItem, editMode);
            $('#myModal').modal('show');
          };

          self.setUpPopUp = function(exhibitionItem, popUpMode) {
                
            console.log(popUpMode); 
            self.popUpMode = popUpMode;
            console.log('mode is video : ' + self.popUpMode === 'PopUpVideoMode');
            if (self.popUpMode === 'PopUpVideoMode') {
             
                self.modeIsVideo(true);
                self.videoUrl(exhibitionItem.videoUrl());
                self.title('Add a youtube video');
                self.title('Add a youtube video');
                self.placeholder('Enter youtube video url'); 
                if ( self.videoAdded() ) {
                
                    self.cancelButtonTitle('delete');
                    self.primaryButtonTitle ('save');
                    //self.videoUrl(exhibitionItem.videoUrl);
                }
                else {
                    
                    self.cancelButtonTitle('cancel');
                    self.primaryButtonTitle ('embed');
                } 
            }
            else {
                
                self.videoAdded(false);
                self.title('Add text');
                self.placeholder('');
                self.textInput(exhibitionItem.additionalText());
                self.primaryButtonTitle ('save');
                if ( self.exhibitionItem.containsText()) {
                
                    self.cancelButtonTitle('delete');
                }
                else {
                    
                    self.cancelButtonTitle('cancel');
                }
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
          self.embedPopUpVideoMode = function () {
            
            if (self.videoUrl().match(/youtube\.com.*(\?v=|\/embed\/)(.{11})/) === null) {
                
                return;
            }
            self.videoAdded(true);
            var youtube_video_id = self.videoUrl().match(/youtube\.com.*(\?v=|\/embed\/)(.{11})/).pop();
            var thumbnailPath = '//img.youtube.com/vi/'+youtube_video_id+'/0.jpg';
            var embeddedVideoPath = 'https://www.youtube.com/embed/'+youtube_video_id;
            self.thumbnailUrl(thumbnailPath);
            self.videoUrl(embeddedVideoPath);
            self.primaryButtonTitle('save');
          }
          self.savePopUpVideoMode = function () {
            
           self.exhibitionItem.videoUrl(self.videoUrl());
           self.exhibitionItem.containsVideo(true);
            $('#myModal').modal('hide');
          }
          //left button actions
          self.cancelPopUpVideoMode = function () {
            
             $('#myModal').modal('hide');
          }
          self.deletePopUpVideoMode = function () {
           
             self.exhibitionItem.videoUrl('');
             self.exhibitionItem.containsVideo(false); 
             $('#myModal').modal('hide');
          }
          
          
          self.savePopUpTextMode = function () {
            
           self.exhibitionItem.additionalText(self.textInput());
           self.exhibitionItem.containsText(true);
           $('#myModal').modal('hide');
          }
          //left button actions
          self.cancelPopUpTextMode = function () {
            
             $('#myModal').modal('hide');
          }
          self.deletePopUpTextMode = function () {
           
             self.exhibitionItem.additionalText('');
             self.exhibitionItem.containsText(false); 
             $('#myModal').modal('hide');
          }
          
          
  }

  return { viewModel: ItemEditViewModel, template: template };
});
