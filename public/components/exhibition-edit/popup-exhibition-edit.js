define(['knockout', 'text!./popup-exhibition-edit.html', 'app'], function(ko, template, app) {

function ItemEditViewModel(params) {
	  var self = this;
	  
	  self.route = params.route;
          self.title = ko.observable('');         
          editItem = function(exhibitionItem) {
            
            console.log(exhibitionItem);
            self.title(exhibitionItem.title);
            $('#myModal').modal('show');
          };
          
  }

  return { viewModel: ItemEditViewModel, template: template };
});
