define(["knockout", "text!./home2.html","flip"], function(ko, homeTemplate,flip) {

	
	
	function HomeViewModel(params) {
		
		$( document ).on( 'keypress', function( event ) {
		     if (event.which == null) {
		    	 var char=String.fromCharCode(event.which);
		    	 toggleSearch("focus",char);
		    	 
		     } else if (event.which!=0 && event.charCode!=0) {
		    	 var char=String.fromCharCode(event.which);
		    	 toggleSearch("focus",char);
		       } else {
		    		    return;
		     }
		    
		    
		    
		});

		
		
		
		$(".cardFlip").flip({
	        axis: "y", // y or x
	        reverse: false, // true and false
	        trigger: "manual", // click or hover
	        speed: 500
	      });
		
		$(".cardFlip").bind("click",function(){
			
			 var elem = $(this);
			 var fig=elem.find('figure');
			 var imholder=elem.find('div.imholder');
			 //var back=elem.find(".back");
			 if(fig.css('display') == 'block'){
				 var image = fig.children("img");
                 image.remove();
                 image.css('width','100%').css('opacity','0.3').addClass('img-responsive');
                 
                 imholder.css('background-image','url('+image.attr('src')+')'); 
                 imholder.css('display','block');
                 if(image.attr('src').indexOf('create')==-1){imholder.css('height',175);}
                 else{imholder.css('height',350);}
			     elem.flip(true);
			    
			    fig.css('display','none');
			    return;
			 }
			 else{
				 var img = $('<img>'); 
				 var url= imholder.css('background-image');
				 url = url.replace(/^url\(["']?/, '').replace(/["']?\)$/, '');
				
				 img.attr('src', url);
				 
				 imholder.css('display','none');
				 fig.append(img);
				
				 elem.flip(false);
				 fig.css('display','block');
				 return;
			 }
			 
		});
		
		
		this.route = params.route;
	}

	return { viewModel: HomeViewModel, template: homeTemplate };
});
