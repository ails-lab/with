define(['knockout', 'text!./collection.html', 'selectize', 'app', 'knockout-validation','smoke'], function (ko, template, selectize, app) {

	
    self.saving=ko.observable(false);
	
	ko.validation.init({
		errorElementClass: 'error',
		errorMessageClass: 'errormsg',
		decorateInputElement: true
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
		self.params=params;
		self.route = params.route;
		self.templateName = ko.observable('');
		self.modal = ko.observable("3");
		self.record = ko.observable(false);
		self.collname = ko.observable('').extend({
			required: true
		});
		
		self.selectedCollection = ko.observable('');
		self.description = ko.observable('');
		self.collectionlist=ko.observableArray([]);
		self.isPublic=ko.observable(true);
		
		self.id = ko.observable(-1);
		self.ajaxConnections = 0;
		self.selected_items2 = ko.observableArray([]);
		self.validationModel = ko.validatedObservable({
			collname: self.collname()
		});

		self.findEditableCollections = function () {
			saving(true);
			var deferred = $.Deferred();
			self.collectionlist([]);
			if(isLogged()){
					var temparray=ko.utils.arrayMap(currentUser.editables(), function(item) {
		        	
		        	return({
						"id": item.dbId,
						"name": item.title
					});
		            
		        });
				self.collectionlist.push.apply(self.collectionlist, temparray);
				saving(false);
				deferred.resolve();
			}
			else{
			var promise=app.getEditableCollections();
			$.when(promise).done(function(){
				var temparray=ko.utils.arrayMap(currentUser.editables(), function(item) {
		        	
		        	return({
						"id": item.dbId,
						"name": item.title
					});
		            
		        });
				self.collectionlist.push.apply(self.collectionlist, temparray);
				saving(false);
				deferred.resolve();
			 })}
			return deferred.promise();
			
		};

		createNewCollection = function () {
			self.findEditableCollections();
			self.modal("2");
			self.templateName('collection_new');
			self.open();
		};
		
		collectInMultiple= function(){
			if (!isLogged()) {
				showLoginPopup();
			} else {
				saving(true);
				var promise=self.findEditableCollections();
				$.when(promise).done(function(){
				
					self.templateName("multiplecollect");
					saving(false);
					self.open();
				});
			}
		}

		collectionShow = function (record) {
			/*if(!ko.isObservable(record)){
				self.record(ko.mapping.fromJS(record));
			}
			else{*/
				self.record(record);
			//}
			var promise=self.findEditableCollections();
			$.when(promise).done(function(){
				if (self.collectionlist().length == 0) {
					self.modal("2");
					self.templateName('collection_new');
				} else {
					self.modal("3");
					self.templateName('additem');
				}
				self.open();});
		};

		self.open = function () {
			$( '.action' ).removeClass( 'active' );
			$( '.action.collect' ).addClass( 'active' );

		};

		self.close = function () {
			if (0 === self.ajaxConnections) {
				self.reset();
			}
		};

		self.addDescription=function(event){
			event.preventDefault();
			$('textarea').show();
			$(event.target).parent('a').hide();
		}
		
		self.save = function (formElement) {
			if (self.validationModel.isValid()) {

				/*var jsondata = JSON.stringify({
					ownerId: currentUser._id(),
					title: self.collname(),
					description: self.description(),
					isPublic: $("#publiccoll .active").data("value")
				});*/
				var jsondata = JSON.stringify({
					resourceType: "SimpleCollection",
					administrative: { access: {
				        isPublic: self.isPublic()},
				        },
				        descriptiveData : {
				        	 label : {
						            default : [self.collname()],
						            en : [self.collname()]
						        },
						     description : {
					            default : [self.description()],
					            en : [self.description()]
				            }
				        }
					
				});
				if (!self.record()) {
					/*new collection with no item saved inside, changed for mycolllections page*/
					self.saveCollection(jsondata, null);
				} else {

					self.saveCollection(jsondata, self.addRecord);
				}
				self.close();


			} else {
				self.validationModel.errors.showAllMessages();
			}
		};

		self.saveCollection = function (jsondata, callback) {
			saving(true);
			$.ajax({
				"beforeSend": function (xhr) {
					self.ajaxConnections++;
					 var utc = new Date().valueOf();
				        xhr.setRequestHeader('X-auth1', utc );
				        xhr.setRequestHeader('X-auth2', sign( document.location.origin, utc ));
				},
				"url": "/collection",
				"method": "post",
				"contentType": "application/json",
				"data": jsondata,
				"success": function (data) {
					self.id(data.dbId);
					self.selectedCollection(data.title);
					var temp = [];
					if(currentUser==undefined){
						$.smkAlert({text:'An error has occured. You are no longer logged in!', type:'danger', permanent: true});
					}
					else{
						currentUser.editables.push(data);
						$.smkAlert({text:'Collection created', type:'success'});
						self.collectionlist.push({
							"id": data.dbId,
							"name": data.title
						});
						getEditableCollections();
						//TODO: Bug fix - the route is mycollections only the first time new collection is called from mycollections?
						if(window.location.hash.indexOf("mycollection")!=-1){
				
						//if (self.params.request_ == "mycollections" || (self.params.route &&  self.params.route().request_=="mycollections")) {
							ko.contextFor(mycollections).$data.reloadCollection(data);
						}
						if (callback) {
							callback(data.dbId);
						}}
					self.ajaxConnections--;
					self.close();
				},

				"error": function (result) {
					saving(false);
					self.ajaxConnections--;
					//var r = JSON.parse(result.responseText);
					$.smkAlert({text:'An error occured', type:'danger', time: 10});
					self.close();
				}
			});
		};

		self.addToCollections = function () {
			/*will contain ids of collection and names for new collections so check each element if it is an id or a title for new collection*/
			self.selected_items2().forEach(function (item) {
				/* now find if item is one of collection ids*/
				if ($.inArray(item, self.collectionlist().map(function (x) {
						return x.id;
					})) != -1) {
					/* add item to collection with this id */
					self.addRecord(item);
				} else {
					/*otherwise save this collection and then add the item */	
					var jsondata = JSON.stringify({
						resourceType: "SimpleCollection",
						administrative: { access: {
				        isPublic: self.isPublic()},
				        },
				        descriptiveData : {
				        	 label : {
						            default : [item],
						            en : [item]
						        }
				        }});
					self.saveCollection(jsondata, self.addRecord);
				}
			});
		};

		
		self.addMultipleToCollections = function () {
			/*will contain ids of collection and names for new collections so check each element if it is an id or a title for new collection*/
			self.selected_items2().forEach(function (item) {
				/* now find if item is one of collection ids*/
				if ($.inArray(item, self.collectionlist().map(function (x) {
						return x.id;
					})) != -1) {
					/* add item to collection with this id */
					self.addRecords(item,true);
				} else {
					/*otherwise save this collection and then add the item */	
					var jsondata = JSON.stringify({
						resourceType: "SimpleCollection",
						administrative: { access: {
				        isPublic: self.isPublic()},
				        },
				        descriptiveData : {
				        	 label : {
						            default : [item],
						            en : [item]
						        }
				        }});
					self.saveCollection(jsondata, self.addRecords);
				}
			});
		};

		
		self.addRecord = function (collid, noDouble) {
			saving(true);
			 var jsondata = JSON.stringify({ 
				    provenance : [{ provider : self.record().source, 
					resourceId: self.record().externalId}]
					});
			if(self.record().data()){
				jsondata=JSON.stringify(self.record().data());
			}
			if (noDouble === undefined)
				noDouble = true;
			$.ajax({
				"beforeSend": function (xhr) {
					self.ajaxConnections++;
					 var utc = new Date().valueOf();
				        xhr.setRequestHeader('X-auth1', utc );
				        xhr.setRequestHeader('X-auth2', sign( document.location.origin, utc ));
				},
				"url": "/collection/" + collid + "/addRecord?noDouble=" + noDouble,
				"method": "post",
				"contentType": "application/json",
				"data": jsondata,
				"success": function (data) {
					saving(false);
					self.ajaxConnections--;
					if(window.location.hash.indexOf("collectionview/"+collid)!=-1){
						ko.contextFor(withcollection).$data.loadNext();
						ko.contextFor(withcollection).$data.reloadEntryCount();
					} 
					else if(window.location.hash.indexOf("mycollections")!=-1){
						var obj = null;
						ko.contextFor(mycollections).$data.reloadRecord(collid, jsondata);
					}

					$.smkAlert({text:'Item added!', type:'success'});
					self.close();
				},
				"error": function (result) {
					saving(false);
					self.ajaxConnections--;
					if (result.responseJSON && result.responseJSON.error === 'double') {
						$.smkConfirm({text:'Record already exists in collection. Do you want to add it again?', accept:'Yes', cancel:'No'}, function(e){if(e){
							self.addRecord(collid, false);
						}});
					}	
					else {
						$.smkAlert({text:'An error occured', type:'danger', time: 10});
						self.close();
					}
				}
			});
		};

		
		self.addRecords = function (collid,noDouble) {
			saving(true);
			var multipleselect=ko.dataFor(multiplecollect).multipleSelection();
			var jsonArray=[];
			for (i=0;i<ko.dataFor(multiplecollect).multipleSelection().length;i++){
			 var item=ko.dataFor(multiplecollect).multipleSelection()[i];
			 jsonArray.push({ 
				 descriptiveData : {
		        	 label : {
				            default : [item.title]
				        }
		           },
				    provenance : [{ provider : item.source, 
						resourceId: item.externalId}]
						});
			}
			if (noDouble === undefined)
				noDouble = true;
			var jsondata=JSON.stringify(jsonArray);
			$.ajax({
				"beforeSend": function (xhr) {
					self.ajaxConnections++;
					 var utc = new Date().valueOf();
				        xhr.setRequestHeader('X-auth1', utc );
				        xhr.setRequestHeader('X-auth2', sign( document.location.origin, utc ));
				},
				"url": "/collection/" + collid + "/addRecords?noDouble="+noDouble,
				"method": "post",
				"contentType": "application/json",
				"data": jsondata,
				"success": function (data) {
					saving(false);
					self.ajaxConnections--;
					if(window.location.hash.indexOf("collectionview/"+collid)!=-1){
						ko.contextFor(withcollection).$data.loadNext();
						ko.contextFor(withcollection).$data.reloadEntryCount();
					} 
					else if(window.location.hash.indexOf("mycollections")!=-1){
						var obj = null;
						ko.contextFor(mycollections).$data.reloadRecord(collid, jsondata);
					}

					$.smkAlert({text:'Items added!', type:'success'});
					
					self.close();
				},
				"error": function (result) {
					self.ajaxConnections--;
					saving(false);
					if (result.responseJSON && result.responseJSON.error === 'double') {
						$.smkConfirm({text:'One of the selected records already exists in collection. Do you want to add it again?', accept:'Yes', cancel:'No'}, function(e){if(e){
							self.addRecords(collid, false);
						}else{self.close();}});
					}	
					else {
						$.smkAlert({text:'An error occured', type:'danger', time: 10});
						self.close();
						
					}
					
				}
			});
		};
		
		self.reset = function () {
			self.collname('');
			self.description('');
			self.isPublic(true);
			self.id(-1);
			self.record();
			self.validationModel.errors.showAllMessages(false);
			self.selected_items2([]);
			$('textarea').hide();
			$('.add').show();
			$('#multiplecollect').removeClass('show');
			ko.dataFor(multiplecollect).multipleSelection([]);
			$('.grid').find('input[type=checkbox]:checked').removeAttr('checked');
			$('div.item').removeClass('selected');
			$( '.action' ).removeClass( 'active' );
			$( '.searchresults' ).removeClass( 'openfilter');
		};

		
	}

	return {
		viewModel: CollectionViewModel,
		template: template
	};
});
