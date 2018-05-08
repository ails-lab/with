define(['bridget','knockout', 'text!./annotations-end.html','isotope','imagesloaded','app', 'knockout-else', 'easypiechart'], function(bridget,ko, template,Isotope,imagesLoaded,app,KnockoutElse) {
	
	
	$.bridget('isotope', Isotope);
	ko.bindingHandlers.annotationsIsotope = {
			init: app.initOrUpdate('init'),
			update: app.initOrUpdate('update')
		};
	
	self.loading=ko.observable(false);
	
	var recmapping={
			'dbId': {
				key: function(data) {
		            return ko.utils.unwrapObservable(data.dbId);
		        }
			 }};			
		

  function AnnotationsEndModel(params) {
	  this.route = params.route;
	  var self = this;
	  //document.body.setAttribute("data-page","home");
	  self.hash=window.location.hash;
	  setTimeout(function(){ WITHApp.init(); }, 300);
	  self.batchItemsAnnotated = ko.observableArray();
	  self.batchAnnotationCount = ko.observable(0);
	  self.userTotalAnnotationCount = ko.observable(0);
	  self.userTotalAnnotatedRecordsCount = ko.observable(0);
	  self.badgeImg = ko.observable("img/ui/rookie.png");
	  self.badgeName = ko.observable('Rookie');
	  WITHApp.initIsotope();
	  
	  showEndOfAnnotations = function (batchItemsAnnotated, batchAnnotationCount) {
		  self.batchItemsAnnotated(batchItemsAnnotated);
		  self.batchAnnotationCount(batchAnnotationCount);
		  $("#main-content").hide();
		  document.body.setAttribute("data-page","home");
		  $("#annotatehero").hide();
		  $("#annotations-end").show();
		  WITHApp.initIsotope();
		  dispatchDocumentEvent('Pundit.hide');
		  self.loadAnnotations();
	  };
	  
	  self.loadAnnotations = function () {
			$.ajax({
				"url": '/user/annotations',
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					if (data.annotationCount) {
						self.userTotalAnnotationCount(data.annotationCount);
					}
					if (data.annotationCount > 10 && data.annotationCount < 25) {
						self.badgeImg('img/ui/ic-badge-bronze.png');
						self.badgeName('Bronze');
					} else if(data.annotationCount >= 25 &&  data.annotationCount < 50) {
						self.badgeImg('img/ui/ic-badge-silver.png');
						self.badgeName('Silver');
					}
					else if(data.annotationCount >= 50) {
						self.badgeImg('img/ui/ic-badge-gold');
						self.badgeName('Golden');
					}
					if (data.annotatedRecordsCount) {
						self.userTotalAnnotatedRecordsCount(data.annotatedRecordsCount);
					}
				},
				"error": function (result) {
					self.loading(false);
					$.smkAlert({
						text: 'An error has occured',
						type: 'danger',
						permanent: true
					});
				}
			});
		};
		
		self.returnHomepage = function() {
			window.location.href = "../../custom/soundspace/index.html";
			//if next lines, scroll down in page does not work
			/*$("#annotatehero").show();
			$("#main-content").show();
			$("#annotations-end").hide();*/
		}
		
		self.itemShow = function(item) {
			itemShow(item);
		}
	
  }
  
 
 
  return { viewModel: AnnotationsEndModel, template: template };
});
