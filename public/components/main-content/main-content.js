define(['masonry','knockout', 'text!./main-content.html'], function(masonry,ko, template) {

  function MainContentModel(params) {
	  this.route = params.route;
	  
	  
	  $('.containermason').each(function(index,listitem){
		  /*this===listitem;
		  new masonry( listitem);
		  */
		  $(this).masonry({
			  columnWidth: '.littlesquare',
			  itemSelector: '.square',
			  gutter: 1
			});
			$(this).append("<span class='withsearch-view'><i class='fa fa-arrow-circle-right' title='see more'></i></span>");
			
		  
	  });
	  
	/*  var $container = $('#containermason');
	// initialize
	  //new masonry($container);
	$container.masonry({
	  columnWidth: '.littlesquare',
	  itemSelector: '.square',
	  gutter: 1
	});
	$container.append("<span class='withsearch-view'><i class='fa fa-arrow-circle-right' title='see more'></i></span>");
	//$container.masonry( 'layout' );
	 */
  }
 
  return { viewModel: MainContentModel, template: template };
});