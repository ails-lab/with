define(['knockout', 'text!./collection-popup.html', 'selectize', 'app', 'knockout-validation','smoke'], function (ko, template, selectize, app) {

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
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
		self.templateName = ko.observable('collection_newpopup');
		self.modal = ko.observable("3");
		self.record = ko.observable(false);
		self.collname = ko.observable('').extend({
			required: true
		});
		self.selectedCollection = ko.observable('');
		self.description = ko.observable('');
		self.collectionlist=ko.observableArray([]);
		
		
		self.id = ko.observable(-1);
		self.ajaxConnections = 0;
		self.selected_items2 = ko.observableArray([]);
		self.validationModel = ko.validatedObservable({
			collname: self.collname
		});

		self.findEditableCollections = function () {
			var deferred = $.Deferred();
			self.collectionlist([]);
			var promise=app.getEditableCollections();
			$.when(promise).done(function(){
				var temparray=ko.utils.arrayMap(currentUser.editables(), function(item) {
		        	
		        	return({
						"id": item.dbId,
						"name": item.title
					});
		            
		        });
				self.collectionlist.push.apply(self.collectionlist, temparray);
				deferred.resolve();
			 })
			return deferred.promise();
			
		};

		createNewCollection = function () {
			self.findEditableCollections();
			self.modal("2");
			self.templateName('collection_newpopup');
			self.open();
		};

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
					self.templateName('collection_newpopup');
				} else {
					self.modal("3");
					self.templateName('additempopup');
				}
				self.open();});
		};

		self.open = function () {
			$('#modal-' + self.modal()).css('display', 'block');
			$('#modal-' + self.modal()).addClass('md-show');
		};

		self.close = function () {
			$('[id^="modal"]').removeClass('md-show').css('display', 'none');
			$("body").removeClass("modal-open");
			if (0 === self.ajaxConnections) {
				// this was the last Ajax connection, do the thing
				if ($('#myModal').find('h4.modal-title').is(':empty') == false)
					$("#myModal").modal('show');
				self.reset();
			}
		};

		self.save = function (formElement) {
			if (self.validationModel.isValid()) {

				/*var jsondata = JSON.stringify({
					ownerId: currentUser._id(),
					title: self.collname(),
					description: self.description(),
					isPublic: $("#publiccoll .active").data("value")
				});*/
				var jsondata = JSON.stringify({
					administrative: { access: {
				        isPublic: $("#publiccoll .active").data("value")},
				        collectionType: "SimpleCollection"},
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
						//TODO: Bug fix - the route is mycollections only the first time new collection is called from mycollections?
						if (self.params.request_ == "mycollections" || (self.params.route &&  self.params.route().request_=="mycollections")) {
							ko.contextFor(mycollections).$data.reloadCollection(data);
						}
						if (callback) {
							callback(data.dbId);
						}}
					self.ajaxConnections--;
					self.close();
				},

				"error": function (result) {
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
						administrative: { access: {
				        isPublic: $("#publiccoll .active").data("value")},
				        collectionType: "SimpleCollection"},
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

		self.addRecord = function (collid, noDouble) {
			 var jsondata = JSON.stringify({ 
				    provenance : [{ provider : self.record().source, 
					resourceId: self.record().externalId}]
					});
			if(self.record().data()){
				jsondata=JSON.stringify(self.record().data());
			}
			if (noDouble === undefined)
				noDouble = false;
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
					self.ajaxConnections--;
					if ((self.params.request_  && self.params.request_ .indexOf( "collectionview/" + collid)==0) ||( self.params.route &&  self.params.route().request_.indexOf("collectionview/" + collid))==0) {
						ko.contextFor(withcollection).$data.loadNext();
						ko.contextFor(withcollection).$data.reloadEntryCount();
					} else if (self.params.request_ == "mycollections" ||( self.params.route &&  self.params.route().request_ == "mycollections" )) {
						var obj = null;
						ko.contextFor(mycollections).$data.reloadRecord(collid, jsondata);
					}

					$.smkAlert({text:'Item added!', type:'success'});
					self.close();
				},
				"error": function (result) {
					self.ajaxConnections--;
					if (result.responseJSON.error === 'double') {
						app.showInfoPopup("Record already exists in collection.",
								"Do you want to add it again?", function() {
							self.addRecord(collid, false);
						});
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
			self.id(-1);
			self.record(false);
			self.validationModel.errors.showAllMessages(false);
			self.selected_items2([]);
		};

		self.privateToggle = function (e, arg) {
			$(arg.currentTarget).parent().find('.btn').toggleClass('active');

			if ($(arg.currentTarget).parent().find('.btn-primary').size() > 0) {
				$(arg.currentTarget).parent().find('.btn').toggleClass('btn-primary');
			}

			$(arg.currentTarget).parent().find('.btn').toggleClass('btn-default');
		};
		

	}

	return {
		viewModel: CollectionViewModel,
		template: template
	};
});
