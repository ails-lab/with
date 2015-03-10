define(['knockout', 'text!./collection.html'], function(ko, template) {

  function CollectionViewModel(params) {
	  var self = this;

	  self.route = params.route;
	  
	  
	  collectionShow = function(record) {
	    	console.log(record);
	    	$('#modal-2').css('display', 'block');
	    	$('#modal-2').addClass('md-show');
	    	
	    	
	    	
	    }
	  
	  self.close= function(){
	    	$('#modal-2').removeClass('md-show');
	    	$('#modal-2').css('display', 'none');
	    	
	    }
	
	  $('.btn-toggle').click(function() {
		    $(this).find('.btn').toggleClass('active');  
		    
		    if ($(this).find('.btn-primary').size()>0) {
		    	$(this).find('.btn').toggleClass('btn-primary');
		    }
		   
		    
		    $(this).find('.btn').toggleClass('btn-default');
		       
		});

    
  }

  return { viewModel: CollectionViewModel, template: template };
});
