define(['knockout', 'text!./collection.html','selectize', 'app','knockout-validation'], function(ko, template, selectize, app) {

	
	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block'
		
	});

	var inject_binding = function (allBindings, key, value) {
	    return {
	        has: function (bindingKey) {
	            return (bindingKey == key) || allBindings.has(bindingKey);
	        },
	        get: function (bindingKey) {
	            var binding = allBindings.get(bindingKey);
	            if (bindingKey == key) {
	                binding = binding ? [].concat(binding, value) : value;
	            }
	            return binding;
	        }
	    };
	}

	ko.bindingHandlers.selectize = {
	    init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
	        if (!allBindingsAccessor.has('optionsText'))
	            allBindingsAccessor = inject_binding(allBindingsAccessor, 'optionsText', 'name');
	        if (!allBindingsAccessor.has('optionsValue'))
	            allBindingsAccessor = inject_binding(allBindingsAccessor, 'optionsValue', 'id');
	        if (typeof allBindingsAccessor.get('optionsCaption') == 'undefined')
	            allBindingsAccessor = inject_binding(allBindingsAccessor, 'optionsCaption', 'Choose...');

	        ko.bindingHandlers.options.update(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext);

	        var options = {
	            valueField: allBindingsAccessor.get('optionsValue'),
	            labelField: allBindingsAccessor.get('optionsText'),
	            searchField: allBindingsAccessor.get('optionsText')
	        }

	        if (allBindingsAccessor.has('options')) {
	            var passed_options = allBindingsAccessor.get('options')
	            for (var attr_name in passed_options) {
	                options[attr_name] = passed_options[attr_name];
	            }
	        }

	        var $select = $(element).selectize(options)[0].selectize;

	        if (typeof allBindingsAccessor.get('value') == 'function') {
	            $select.addItem(allBindingsAccessor.get('value')());
	            allBindingsAccessor.get('value').subscribe(function (new_val) {
	                $select.addItem(new_val);
	            })
	        }

	        if (typeof allBindingsAccessor.get('selectedOptions') == 'function') {
	            allBindingsAccessor.get('selectedOptions').subscribe(function (new_val) {
	                // Removing items which are not in new value
	                var values = $select.getValue();
	                var items_to_remove = [];
	                for (var k in values) {
	                    if (new_val.indexOf(values[k]) == -1) {
	                        items_to_remove.push(values[k]);
	                    }
	                }

	                for (var k in items_to_remove) {
	                    $select.removeItem(items_to_remove[k]);
	                }

	                for (var k in new_val) {
	                    $select.addItem(new_val[k]);
	                }

	            });
	            var selected = allBindingsAccessor.get('selectedOptions')();
	            for (var k in selected) {
	                $select.addItem(selected[k]);
	            }
	        }


	        if (typeof init_selectize == 'function') {
	            init_selectize($select);
	        }

	        if (typeof valueAccessor().subscribe == 'function') {
	            valueAccessor().subscribe(function (changes) {
	                // To avoid having duplicate keys, all delete operations will go first
	                var addedItems = new Array();
	                changes.forEach(function (change) {
	                    switch (change.status) {
	                        case 'added':
	                            addedItems.push(change.value);
	                            break;
	                        case 'deleted':
	                            var itemId = change.value[options.valueField];
	                            if (itemId != null) $select.removeOption(itemId);
	                    }
	                });
	                addedItems.forEach(function (item) {
	                    $select.addOption(item);
	                });

	            }, null, "arrayChange");
	        }

	    },
	    update: function (element, valueAccessor, allBindingsAccessor) {

	        if (allBindingsAccessor.has('object')) {
	            var optionsValue = allBindingsAccessor.get('optionsValue') || 'id';
	            var value_accessor = valueAccessor();
	            var selected_obj = $.grep(value_accessor(), function (i) {
	                if (typeof i[optionsValue] == 'function')
	                    var id = i[optionsValue]
	                else
	                    var id = i[optionsValue]
	                return id == allBindingsAccessor.get('value')();
	            })[0];

	            if (selected_obj) {
	                allBindingsAccessor.get('object')(selected_obj);
	            }
	        }
	    }
	}


  function CollectionViewModel(params) {
	  var self = this;

	  self.route = params.route;
	  self.templateName=ko.observable('collection_new');
	  self.modal=ko.observable("3");
	  self.record=ko.observable(true);
	  self.collname=ko.observable('').extend({ required: true });
	  
	  var nocollection=true; /*picked up from browser session storage : should be stored upon login*/
	  
	  self.validationModel = ko.validatedObservable({
			collname : self.collname
		});

	  var collections;
		if (sessionStorage.getItem('UserCollections') !== null) 
			collections = JSON.parse(sessionStorage.getItem("UserCollections"));
		else if (localStorage.getItem('UserCollections') !== null) 
			collections = JSON.parse(localStorage.getItem("UserCollections"));

		/*load these from db and put on session storage upon login . For now use static array*/
	/*  self.collectionitems = ko.observableArray([
	                           		{'id': 1, 'name': ' Collection One'},
	                           		{'id': 2, 'name': ' Collection Two'}
	                           		]);
	*/

	  self.selected_items2 = ko.observableArray();

	collectionShow = function(record) {
	    	if(nocollection){self.modal("2");self.templateName('collection_new');}
	    	else{self.modal("3");self.templateName('additem');}
	    	self.open();
	    }

	  self.open=function(){
		  $('#modal-'+self.modal()).css('display', 'block');
	      $('#modal-'+self.modal()).addClass('md-show');

	  }

	  self.close= function(){
	    	$('#modal-'+self.modal()).removeClass('md-show');
	    	$('#modal-'+self.modal()).css('display', 'none');

	    }

	  self.save=function(formElement){
		  if (self.validationModel.isValid()) {
			  console.log(app.currentUser._id.$oid);
			  var jsondata=JSON.stringify({
					ownerId: app.currentUser._id(),
					title: self.collname(),
					description:formElement.elements['details'].value,
					public: $("#publiccoll .active").data("value")
				});
			  var collectionId=-1;
			  $.ajax({
					"url": "/collection/create",
					"method": "post",
					"contentType": "application/json",
					"data": jsondata,
					"success": function(data) {
						console.log(data);
					},
					
					"error":function(result) {
				        alert(result.statusText);
						//$("#results").text('Status:'+result.status+', Error: ' +result.responseText);
			             
				     }});
			  
		  }
		  else{
			  self.validationModel.errors.showAllMessages();
		  }
	   
	  }
	  
	  self.addRecord=function(collid){
		  
	  }
	  
	  self.privateToggle=function(e,arg){
		  $(arg.currentTarget).parent().find('.btn').toggleClass('active');

		    if ($(arg.currentTarget).parent().find('.btn-primary').size()>0) {
		    	$(arg.currentTarget).parent().find('.btn').toggleClass('btn-primary');
		    }


		    $(arg.currentTarget).parent().find('.btn').toggleClass('btn-default');
	  }



  }



  return { viewModel: CollectionViewModel, template: template };
});
