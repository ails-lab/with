define(['masonry','knockout', 'text!./main-content.html'], function(masonry,ko, template) {

  function MainContentModel(params) {
	  this.route = params.route;
	  
	  var msnryboxes=$('.js-masonry');
	  
	  
	  $('.js-masonry').each(function(index,listitem){
		  this===listitem;
		  new masonry( listitem);
	  });
	
	 
  }
 
  return { viewModel: MainContentModel, template: template };
});