define(['knockout', 'text!./item.html'], function(ko, template) {

  function ItemViewModel(params) {
	  var self = this;

	  self.route = params.route;
      var thumb="";
		
	  self.title = ko.observable(false);
	  self.description=ko.observable(false);
	  self.thumb = ko.observable("");
	  self.fullres=ko.observable("");
	  self.view_url=ko.observable("");
	  self.creator=ko.observable("");
	  self.provider=ko.observable("");
	  self.apisource=ko.observable(true);
	  
	 
    itemShow = function(record) {
    	self.itemload(record);
    	
    	$('#modal-1').css('display', 'block');
    	
    	$('#modal-1').addClass('md-show');
    	$('body').css('overflow', 'hidden');
    }
    
    self.itemload = function(e) {
    	data=ko.toJS(e);
    	console.log(data);
    	if(data.title==undefined){
			self.title("No title");
		}else{self.title(data.title);}
		
		self.thumb(data.thumb);
		thumb=data.thumb;
		if(data.fullres!==undefined && data.fullres!=null && data.fullres[0].length>0 && data.fullres!="null"){
		  self.fullres(data.fullres[0]);}
		else{
			self.fullres(data.thumb);
			
		  }
		
		if(data.description==undefined){
			self.description(data.title);
		}
		else{
		self.description(data.description);}
		if(data.creator!==undefined){
			self.creator(data.creator);
		}
		if(data.provider!==undefined){
			self.provider(data.provider);
		}
		
		self.apisource(data.source);
		
		self.view_url(data.view_url);
		
	   
		
	};
   
    self.close= function(){
    	self.fullres('');
    	$("#modal-1").find("div[id^='modal-']").removeClass('md-show').css('display', 'none');
    	$('#modal-1').removeClass('md-show');
    	$('#modal-1').css('display', 'none');
    	
    	
    	
    	$('body').css('overflow', 'auto');
    }
    
    self.changeSource=function(item){
    	if(item.fullres!=item.thumb){
    		 $("#fullresim").attr('src',thumb);
    	}
    	else{
    		 $("#fullresim").attr('src',thumb);
    	}
    	
    }
    
    self.sourceImage = ko.pureComputed(function() {
		if(self.apisource() =="DPLA") return "images/logos/dpla.png";
		else if(self.apisource() == "Europeana") return "images/logos/europeana.jpeg";
		else if(self.apisource() == "NLA") return "images/logos/nla_logo.png";
		else if(self.apisource() == "DigitalNZ") return "images/logos/digitalnz.png";
		else if(self.apisource() == "DigitalNZ") return "images/logos/digitalnz.png";
		else if(self.apisource() == "EFashion") return "images/logos/eufashion.png";
		
		else return "";
	});
    
    self.collect=function(item){
    		collectionShow(item);
			
		
    }
    
  }

  return { viewModel: ItemViewModel, template: template };
});
