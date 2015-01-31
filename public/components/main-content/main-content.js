define(['knockout', 'text!./main-content.html'], function(ko, template) {

  function MainContentModel(params) {
	  this.route = params.route;
	  
	  require([
		'jquery',
		'masonry',
		'imagesloaded',
		], function($,masonry, imagesloaded) {
		  
	
		  
		  var msnryboxes = document.querySelectorAll('.js-masonry');

		  [].forEach.call(msnryboxes, function(msnrybox) {
			  imagesloaded( msnrybox, function() {
				  new masonry( msnrybox);
					})	  
		    
		  });
		  
		
	     });
	  
	
	 
  }
 
  return { viewModel: MainContentModel, template: template };
});