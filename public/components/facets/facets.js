define(['knockout', 'text!./facets.html','inputtags'], function(ko, template,tagsinput) {
	
	
	ko.bindingHandlers.dofacets = {

			  updating: true,

			  init: function(element, valueAccessor, allBindingsAccessor) {
				  var self=this;
				  
				  
			  },
	 update: function(element, valueAccessor, allBindingsAccessor){
		    var load = ko.utils.unwrapObservable(valueAccessor());
		    var self = this;

		  
		  }
    }

  function FacetsViewModel(params) {
	 
   
    this.route = params.route;
   
    var self=this;
    self.visiblePanel=ko.observable(false);
    self.filters=ko.observableArray([]);
    
    $('#facet_tags').tagsinput({
        allowDuplicates: false,
          itemValue: 'id',  // this will be used to set id of tag
          itemText: 'label' // this will be used to set text of tag
      });
    
    
    self.initFacets=function(){
    	self.filters.removeAll();
    	$("#facet_tags").tagsinput('removeAll');
    	var selsources=ko.contextFor(withsearchid).$data.sources();
    	for(var i=0;i<selsources.length;i++){
    		var jsontoadd={itemValue: 'source_'+i,itemText:selsources[i]};
    	    $("#facet_tags").tagsinput('add',{ id: 'source_'+i, label: selsources[i] });
    		//$("#facet_tags").tagsinput('add',selsources[i]);
    	}
    	var obj = {};
    	obj['sources']=selsources;
    	obj['filters']=[];
    	self.filters().push(obj);
    		
    }
    
    self.showfacets= function(){
    	if(self.visiblePanel()==false)
    	    self.visiblePanel(true);
    	else
    		self.visiblePanel(false);
    }
    
    $('#facet_tags').on('itemRemoved', function(event) {
    	
    	if(event.item.id.indexOf("source_")>-1){
    	  ko.contextFor(this).$parent.sources.remove(event.item.label);
    	  ko.contextFor(this).$parent.filterselect(true);
      	  ko.contextFor(this).$parent.filtersearch();
    	
    	}
    	else{
    		var id=event.item.id.substring(0,event.item.id.indexOf("_"));
    		
    		var filterfound=ko.utils.arrayFirst(self.filters()[0].filters, function(item) {
        		
    		    if(item.filterID===id){
    		    	var index=$.inArray(event.item.label, item.values);
    		    	if(index>-1){
    		    		item.values.splice(index,1);
    		    		if(item.values.length==0){
    		    			//remove filter id and values
    		    			self.filters()[0].filters.splice(self.filters()[0].filters.indexOf(item),1);
    		    			
    		    		}
    		    		return true;
    		    		
    		    	}
    		    	
    		    }
    	        return false;
           
    		});
    		if(filterfound){
    			
            
    			self.visiblePanel(false);	 
    	     	
    	       
    		}
    		
    	}
    	
    	 self.setFilters();
    	});
    
    
    self.listClick=function(data, event){
    	if(event.target.checked){
    		//new filter added
    		var id=$(event.target).parents('div.active').attr('id');
    		if(id=='datasources'){
    				self.initFacets();
    			
    		}
    		
    	}
    	if(!event.target.checked){
    		//filter remove
    		var id=$(event.target).parents('div.active').attr('id');
    		if(id=='datasources'){
    			self.initFacets();
    			
    		}
    		
    	}
    	 self.setFilters();
    	return true;
    }
   
    
    self.listSelect=function(id,newvalue){
    	var exists=false;
    	var filterfound=ko.utils.arrayFirst(self.filters()[0].filters, function(item) {
    		
    		    if(item.filterID===id){
    		    	if($.inArray(newvalue, item.values)>-1){
    		    		//value is already there 
    		    		exists=true;
    		    	}
    		    	else{
    		    		$("#facet_tags").tagsinput('add',{ id: id+'_'+item.values.length, label: newvalue });
    		        	
    		    		item.values.push(newvalue);
    		    	}
    		    	return true;
    		        
    		    }
    	        
           
        });
    	if(!filterfound){
    		$("#facet_tags").tagsinput('add',{ id: id+'_0', label: newvalue });
        	var obj={};
    		obj['filterID']=id;
    		obj['values']=new Array(newvalue);
    		self.filters()[0].filters.push(obj);
    	}
    	if(!exists){
    	  self.setFilters();}
          return; 	 
    }
    
    
    self.setFilters=function(){
    	self.visiblePanel(false);
    	ko.contextFor(withsearchid).$data.filterselection([]);
    	ko.contextFor(withsearchid).$data.filterselection().push.apply(ko.contextFor(withsearchid).$data.filterselection(),self.filters()[0].filters);
    	ko.contextFor(withsearchid).$data.filtersearch();
    }
    
    self.initFacets();
    
    
    
    
  }

  return { viewModel: FacetsViewModel, template: template };
});
