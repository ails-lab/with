define(['bridget','knockout', 'text!./annotations-end.html','isotope','imagesloaded','app', 'knockout-else', 'easypiechart'], function(bridget,ko, template,Isotope,imagesLoaded,app,KnockoutElse) {
	
	
	$.bridget('isotope', Isotope);
		
	
	self.loading=ko.observable(false);
	
	var recmapping={
			'dbId': {
				key: function(data) {
		            return ko.utils.unwrapObservable(data.dbId);
		        }
			 }};			
		

  function AnnotationsEndModel(params) {
	  this.route = params.route;
	  var self = this;
	  setTimeout(function(){ WITHApp.init(); }, 300);
	  self.batchItemsAnnotated = ko.observable();
	  self.hash=window.location.hash;
	  
	  showEndOfAnnotations = function (batchItemsAnnotated) {
		  document.body.setAttribute("data-page","annotationsend");
		  alert(batchItemsAnnotated);
	  };  
	
  }
  
 
 
  return { viewModel: AnnotationsEndModel, template: template };
});
