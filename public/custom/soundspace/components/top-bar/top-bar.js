define(['knockout', 'text!./top-bar.html', 'app', 'autocomplete'], function(ko, template, app, autocomplete) {
	
	

  function TopBarViewModel(params) {
	  this.route = params.route;
	  var self=this;
	  self.route=params.route;
	  self.openLogin=function(event){
		  console.log(window.location.href);
		  localStorage.setItem('withLoginPopup', window.location.href);
		  event.preventDefault();
		  $("#loginPopup").addClass("open");
		  
		 
	  }
	  
	  self.gotoWith=function(event){
		    event.preventDefault();
		    window.childwith=window.open('../../assets/index.html', 'with');
		    window.childwith.focus();
	}
	  
	 goToPage=function(data,event){
		 if(data=="#home" || data=="#"){
		      sessionStorage.removeItem("homemasonryscroll");
		      sessionStorage.removeItem("homemasonrycount");}
		   window.location.href=data;
		   event.preventDefault();
		   return false;
	}
	
	 startAnnotate = function() {
		  //self.getTestRecords();
		  self.randomRecords();
	  };
	  
	  self.addNextAnnot = function(randomList, inner, number) {
		  if (randomList.length > 0) {
			  initRecord = randomList[0];
			  initRecord.nextItemToAnnotate = inner;
			  initRecord.number = number;
			  randomList.splice(0, 1);
			  return self.addNextAnnot(randomList, initRecord, number+1);
		  }
		  else {
			  inner.number = number;
			  return inner;
		  }
	  }
	  
	  self.randomRecords = function() {
			$.ajax({
		    	"url": "/record/randomRecords?groupId="+WITHApp.projectId+"&batchCount=10",
		    	"method": "GET",
		    	"success": function( data, textStatus, jQxhr ){
		    		if (data.length > 0) {
			    		recordToAnnotate = self.addNextAnnot(data, {}, 0);
			    		itemShow(formatRecord(recordToAnnotate));
		    		}
				},
				"error": function (result) {
					$.smkAlert({ text: 'An error occured', type: 'danger', time: 10 });
				}         
		    });	
		}; 
	
	  
	}

	return { viewModel: TopBarViewModel, template: template };
});
