define(['knockout', 'text!./exhibition-edit.html', 'jquery.ui','autoscroll','app', 'bootstrap-select', 'jquery.lazyload'], function(ko, template, jqueryUI, autoscroll, app, bootstrapSelect, jqueryLazyLoad) {


function MyCollection(collectionData) {
		this.title = ko.observable(collectionData.title);
		this.dbId = collectionData.dbId;
		this.description = ko.observable(collectionData.description);
		if (collectionData.thumbnail != null)
			this.thumbnail = ko.observable(collectionData.thumbnail);
		this.itemCount = collectionData.itemCount;
		this.isPublic = collectionData.isPublic;
		this.created = collectionData.created;
		this.lastModified = ko.observable(collectionData.lastModified);
		if (collectionData.category != null)
			this.category = ko.observable(collectionData.category);
		this.firstEntries = collectionData.firstEntries;
	}


var ExhibitionEditModel = function(params) {
    
    var self = this;
    self.route = params.route;	
    self.galleryName     = ko.observable('Add Title');
    self.gallerySubtitle = ko.observable("Add description"); 
    self.collectionItemsArray = ko.observableArray([]);
    
    var collections = [];
    var promise = app.getUserCollections();
    self.myCollections = ko.observableArray([]);
    $.when(promise).done(function() {
	if (sessionStorage.getItem('UserCollections') !== null) 
		collections = JSON.parse(sessionStorage.getItem("UserCollections"));
		if (localStorage.getItem('UserCollections') !== null) 
		  collections = JSON.parse(localStorage.getItem("UserCollections"));
		self.myCollections(ko.utils.arrayMap(collections, function(collectionData) {
		    return new MyCollection(collectionData);
	}));
		//then initialise select
	$('.selectpicker').selectpicker();
    });
    
    self.userSavedItemsArray = ko.observableArray([]);
    self.selectedCollection = ko.observable();
    self.currentItemSet = ko.observable(false);
    self.currentItem = ko.observable();
    self.selectedCollection.subscribe(function(newCollection){
	
	self.userSavedItemsArray(newCollection.firstEntries);
	self.currentItem(self.userSavedItemsArray()[0]);
	self.currentItemSet(true);
	}	
    );
    showViewModel = function() {
	
	console.log(ko.toJSON(self));
	console.log(self.myCollections());
    };
	
	
    self.xButtonClicked = function(item) {
        
        if ( self.collectionItemsArray.indexOf(item) > -1) {
            
            self.collectionItemsArray.remove(item);
        }
        else {
            
            self.userSavedItemsArray.remove(item);
        }
    }
    self.detailsEnabled =  ko.observable(false);
    self.enableDetails = function(data, event) {
        self.currentItem(data);
        self.detailsEnabled(true);
	//fix hover
	$(event.target).parent().addClass('box-Hover');
    }   
    self.disableDetails = function(data, event) {

	
        self.detailsEnabled(false);
	$(event.target).parent().removeClass('box-Hover');
    }
    self.showNewItem = function(elem) {  $(elem).hide().fadeIn(500);}
    self.removeItem = function(elem) {   $(elem).fadeOut(500);}
    
    self.showPopUp = function(data){
    	
	console.log('show popup');
	editItem(data, 'PopUpVideoMode');
	}	
	
    //custom binding
    var _draggedItem;
    var _bIsMoveOperation;
    ko.bindingHandlers.drag = {
        init: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
            var dragElement = $(element);
            _bIsMoveOperation = valueAccessor().move;                    
            var dragOptions = {
                start: function( event, ui ) {
                    
                    
                    $('#outer-bottom').css({"overflow":'visible'});
                    $('.bottom-box').removeClass("box-Hover");
                    console.log('dragging');
                    _draggedItem = ko.utils.unwrapObservable(valueAccessor().item);
                    _bIsMoveOperation = valueAccessor().move;
                    console.log('isMoving' + _bIsMoveOperation); 
                    ui.helper.css({"z-index":500});
                    if (_bIsMoveOperation) {
                        if (ui.helper.width() > 150 ) {
                        
                            var newAspectHeight = 150/ ui.helper.width() *  ui.helper.height();
                            ui.helper.css({"width":150});
                            ui.helper.css({"height":newAspectHeight});
                            ui.helper.css({opacity: 0.8});
                        }
                    }
                 },
                stop:function( event, ui ) {
                    $("#outer-bottom").css({overflow:"scroll"});
                    $("#outer-bottom").css({"overflow-y":"hidden"});
		    //un comment below statement
                    //$('.outer').autoscroll('destroy');
                    _draggedItem = undefined;
                }
            };
            if (_bIsMoveOperation) {//to fix the scroll beyond end of gallery
                dragOptions.appendTo = $('.left');
                dragOptions.helper = function() {
                
                        var $imageElementHelper = $(this).find('.itemImage').clone();
                        $imageElementHelper.css('margin', 0).css({'padding-top': 0});
                        //to fix the scroll issues remove it from div
                        return $imageElementHelper;
                }
                dragOptions.cursorAt = { top: 0, left: 75 };
                dragOptions.cursor = "move";
                dragOptions.revert = false;
            }
            else {

                dragOptions.helper = 'clone';
                dragOptions.cursor = 'pointer';
                dragOptions.revert = 'invalid';
            }
            dragElement.draggable(dragOptions).disableSelection();
        }
    };

    ko.bindingHandlers.drop = {
        init: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
            var dropElement = $(element);
            var dropOptions = {
                    hoverClass: "drop-hover",
                    tolerance: "intersect",
                    over: function( event, ui ) {
                        if (!_bIsMoveOperation) {
                         
                         $(this).find('#droppable-Children').css({display:"block"});
                         dropElement.animate({width:"150px"},200);   
                        }
                    },
                    out: function( event, ui ) {  
                        $(this).find('#droppable-Children').css({display:"none"});
                        dropElement.animate({width:"60px"},200);
                    } ,
                    drop: function(event, ui) {
                        
                        var indexNewItem =  ko.utils.unwrapObservable(valueAccessor().index);
                        //clone it
                        var newItem = JSON.parse(JSON.stringify(_draggedItem));
                        var arrayCollection = self.collectionItemsArray;
                        //dont do anything if it is moved to its direct left or right dashed box
                        if (_bIsMoveOperation) {
                            var indexDraggedItem = arrayCollection.indexOf(_draggedItem);
                            if (indexDraggedItem == indexNewItem || ( indexDraggedItem + 1 ) == indexNewItem) {
                               _draggedItem = undefined;
                               return;
                            }   
                        }
                        dropElement.animate({width:"60px"},200);
                        dropElement.find('#droppable-Children').css({display:"none"});
                        arrayCollection.splice(indexNewItem,0,newItem);
                        if (_bIsMoveOperation) {
                            arrayCollection.remove(_draggedItem);
                        }
                        _draggedItem = undefined;
                    }
            };
            dropElement.droppable(dropOptions);
        }
    };
    
//----knockout solution----//

    
    //for the side scrolling  
      $('.left').droppable({
            tolerance: "touch",
            over: function(event, ui){
                $('.outer').autoscroll({
                    direction: 'left',
                    step: 1000,
                    scroll: true
                });
            },
            out: function(event, ui){
                $('.outer').autoscroll('destroy');
            }
        });
    
     $('.right').droppable({
            tolerance: "touch",
            over: function(event, ui){
                $('.outer').autoscroll({
                    direction: 'right',
                    step: 1000,
                    scroll: true
                });
            },
            out: function(event, ui){
                $('.outer').autoscroll('destroy');
            }
    });
     //fix for hover issue
     self.showXbutton = function (data, event) {

	$('.bottom-box').removeClass('box-Hover');
	$(event.target).addClass('box-Hover');
     }
     //hide the nav bar
     $('nav').slideUp(1000);
};
 

	return {viewModel: ExhibitionEditModel, template: template};
});