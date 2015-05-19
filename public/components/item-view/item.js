define(['knockout', 'text!./item.html', 'app'], function(ko, template, app) {

	function Record(data) {
		var self = this;
		
		self.recordId = ko.observable("");
		self.title = ko.observable(false);
		self.description=ko.observable(false);
		self.thumb = ko.observable(false);
		self.fullres=ko.observable(false);
		self.view_url=ko.observable(false);
		self.source=ko.observable(false);
		self.creator=ko.observable("");
		self.provider=ko.observable("");
		self.url=ko.observable("");
		self.id=ko.observable("");
		self.load = function(data) {
			if(data.title==undefined){
				self.title("No title");
			}else{self.title(data.title);}
			
			self.url("#item/"+data.id);
			self.view_url(data.view_url);
			self.thumb(data.thumb);
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

			
			
			
			self.source(data.source);
			
			self.recordId(data.recordId);
		};

		 self.sourceImage = ko.pureComputed(function() {
				if(self.source() =="DPLA") return "images/logos/dpla.png";
				else if(self.source() == "Europeana") return "images/logos/europeana.jpeg";
				else if(self.source() == "NLA") return "images/logos/nla_logo.png";
				else if(self.source() == "DigitalNZ") return "images/logos/digitalnz.png";
				else if(self.source() == "DigitalNZ") return "images/logos/digitalnz.png";
				else if(self.source()== "EFashion") return "images/logos/eufashion.png";
				else if(self.source() == "YouTube") return "images/logos/youtube.jpg";
				else return "";
			});
		if(data != undefined) self.load(data);
	}
	
	
	
  function ItemViewModel(params) {
	  var self = this;
	  
	  self.route = params.route;
      var thumb="";
      self.record=ko.observable(new Record());
    
	  
    itemShow = function(e) {
    	data=ko.toJS(e);
    	self.record(new Record(data));
    	self.open();
    	
    }
    
    
    self.open=function(){
     $("body").addClass("modal-open");
    	$('#modal-1').css('display', 'block');

    	$('#modal-1').addClass('md-show');
	  }

	 

    

    self.close= function(){
    	
    	self.record().fullres('');
    	 $("body").removeClass("modal-open");
    	$("#modal-1").find("div[id^='modal-']").removeClass('md-show').css('display', 'none');
    	$('#modal-1').removeClass('md-show');
    	$('#modal-1').css('display', 'none');
    	$("#myModal").modal('hide'); 


    }

    
   
    
    self.changeSource=function(item){
    	if(item.record().fullres()!=item.record().thumb()){
    		 $("#fullresim").attr('src',item.record().thumb());
    	}
    	else{
    		 $("#fullresim").attr('src',item.record().thumb());
    	}

    }

   

    self.collect = function(item){
    	
		if (!isLogged()) {
			showLoginPopup(self.record());
		}
		else {
			collectionShow(self.record());
		}
    }

    self.recordSelect= function (e){
		
		itemShow(e);
		
		
	}
    
  }

  return { viewModel: ItemViewModel, template: template };
});
