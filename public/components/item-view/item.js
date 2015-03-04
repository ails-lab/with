define(['knockout', 'text!./item.html'], function(ko, template) {

  function ItemViewModel(params) {
	  var self = this;

	  self.route = params.route;
      var thumb="";
		
	  self.title = ko.observable(false);
	  self.description=ko.observable(false);
	  self.thumb = ko.observable(false);
	  self.fullres=ko.observable(false);
	  self.view_url=ko.observable("");
	  self.apisource=ko.observable(true);
	  self.thumbshow=ko.observable(false);
     
    itemShow = function(record) {
    	self.itemload(record);
    	$('#modal-1').css('display', 'block');
    	$('#modal-1').addClass('md-show');
    	
    	
    	$('.withsearch').css('position','fixed');
    	$('.withsearch').css('overflow','hidden');
    }
    
    self.itemload = function(e) {
    	data=ko.toJS(e);
    	if(data.title==undefined){
			self.title("No title");
		}else{self.title(data.title);}
		
		self.thumb(data.thumb);
		thumb=data.thumb;
		if(data.fullres!==undefined && data.fullres!=null && data.fullres[0].length>0)
		self.fullres(data.fullres[0]);
		else{
			self.fullres(data.thumb);
			self.thumbshow(true);
		  }
		
		if(data.description==undefined){
			self.description(data.title);
		}
		else{
		self.description(data.description);}
		
		self.apisource(data.source);
		self.view_url(data.view_url);
		
	};
   
    self.close= function(){
    	$('#modal-1').removeClass('md-show');
    	$('#modal-1').css('display', 'none');
    	$('.withsearch').css('position','absolute');
    	$('.withsearch').css('overflow','scroll');
    }
    
    self.changeSource=function(){
    	if(self.thumbshow()==false){
    	  $("#fullresim").attr('src',thumb);
    	  self.thumbshow(true);
    	}
    	else{
    		$("#fullresim").attr('src','images/no_image.jpg');
    	}
    }
   
    
  }

  return { viewModel: ItemViewModel, template: template };
});
