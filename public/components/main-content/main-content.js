define(['bridget','knockout', 'text!./main-content.html','masonry'], function(bridget,ko, template,masonry) {

	 $.bridget( 'masonry', masonry );	
	
  function MainContentModel(params) {
	  this.route = params.route;
	  
	  
	  $('.containermason').each(function(index,listitem){
		 
		  var url="";
		  if($(this).find('div.bigsquare').attr('id')=='omeka'){
			  url='http://digitalgallery.promoter.it/';
		  }
		  
		 
		  $(this).masonry({
			  columnWidth: '.littlesquare',
			  itemSelector: '.square',
			  gutter: 1
			});
		  if(url.length==0)
			$(this).append("<span class='withsearch-view'><i class='fa fa-arrow-circle-right' title='see more'></i></span>");
		  else{
			  $(this).append("<span class='withsearch-view'>"+"<a href=\'"+url+"\' target='_blank'><i class='fa fa-arrow-circle-right' title='see more'></i></a></span>");
		  }	
			
		  
	  });
	  
	
  }
 
  return { viewModel: MainContentModel, template: template };
});