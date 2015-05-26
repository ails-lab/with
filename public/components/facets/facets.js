define(['knockout', 'text!./facets.html'], function(ko, template) {

  function FacetsViewModel(params) {

   
    this.route = params.route;
   
    var self=this;
    self.visiblePanel=ko.observable(false);
    
    self.showfacets= function(){
    	if(self.visiblePanel()==false)
    	    self.visiblePanel(true);
    	else
    		self.visiblePanel(false);
    }
    
   
    
  }

  return { viewModel: FacetsViewModel, template: template };
});
