define(['knockout', 'text!./facets.html'], function(ko, template) {

  function FacetsViewModel(params) {

    // This viewmodel doesn't do anything except pass through the 'route' parameter to the view.
    // You could remove this viewmodel entirely, and define 'nav-bar' as a template-only component.
    // But in most apps, you'll want some viewmodel logic to determine what navigation options appear.

    this.route = params.route;
    if(params.length==0){
    	
    }
    var self=this;
    self.visiblePanel=ko.observable(false);
    
    self.showfacets= function(){
    	if(self.visiblePanel()==false)
    	    self.visiblePanel(true);
    	else
    		self.visiblePanel(false);
    }
    
    self.changeSearch=function(){
    	//ko.contextFor(withsearchid).$data.search();
    }
    
    
  }

  return { viewModel: FacetsViewModel, template: template };
});
