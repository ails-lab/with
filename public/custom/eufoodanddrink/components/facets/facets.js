define(['bridget','knockout', 'text!./facets.html','inputtags','liveFilter', 'barchart'], function(bridget, ko, template,tagsinput,liveFilter,horizBarChart) {
	
	

  function FacetsViewModel(params) {
	 
   
    this.route = params.route;
   
    var self=this;
    self.visiblePanel=ko.observable(false);
    self.filters=ko.observableArray([]);
    self.yaxis=ko.observableArray([]);
    self.barheights=ko.observableArray([]);
    self.yaxislabels=ko.observableArray([]);
    self.numvalues=ko.observableArray([]);
    $('#facet_tags').tagsinput({
        allowDuplicates: false,
          itemValue: 'id',  // this will be used to set id of tag
          itemText: 'label' // this will be used to set text of tag
      });
    
    
    self.initFacets=function(){
    	$('#facet_tags').tagsinput({
            allowDuplicates: false,
              itemValue: 'id',  // this will be used to set id of tag
              itemText: 'label' // this will be used to set text of tag
          });
    	self.visiblePanel(false);
    	self.filters.removeAll();
    	$("#facet_tags").tagsinput('removeAll');
    	var selsources=ko.contextFor(withsearchid).$data.sources();
    	for(var i=0;i<selsources.length;i++){
    		var jsontoadd={itemValue: 'source#'+i,itemText:selsources[i]};
    	    $("#facet_tags").tagsinput('add',{ id: 'source#'+i, label: selsources[i] });
    		
    	}
    	var obj = {};
    	obj['sources']=selsources;
    	obj['filters']=[];
    	self.filters().push(obj);
    	self.sourceBind();
    	//do date calculations for bar graph
    	self.calcdates(); 
    }
    
    
    
    self.recalcFacets=function(){
    	
    	self.calcdates(); 
    }
    
    
    self.showfacets= function(data,e){
    	e.preventDefault();

		// on
    	$(e.target).parents("div.settings").toggleClass( 'active' );
    //	e.preventDefault();

		// on
    	//$(e.target).parents("div.settings").toggleClass( 'active' );
    	if($(e.target).parents("div.settings").hasClass("active")){
    	    $('.chart').horizBarChart({
  	          selector: '.bar',
  	          speed: 1000
  	        });
    	    self.visiblePanel(true);
    	    return;
  		
    	}
    	else
    		self.visiblePanel(false);
    		return;
    }
    
    
    self.calcdates=function(){
    	self.yaxis([]);
    	self.barheights([]);
    	self.yaxislabels([]);
    	self.numvalues([]);
    	var filterfound=ko.utils.arrayFirst(ko.contextFor(withsearchid).$data.filters(), function(item) {
    		
		    if(item.filterID==="dates"){
		    	var years=item.suggestedvalues;
		    	
		       /*get rid of crap data , values larger than 3000l, or non numeric values*/	
		    	var filtered = item.suggestedValues.filter(filterTooBig(3000));	
		    
		    	
		    	min=arrayMin(filtered);
		    	max=arrayMax(filtered);
		    	var resultaxis=self.makeYaxis(min,max,20);
		    	if(resultaxis[0]<min && resultaxis[1]>min){resultaxis[0]=min;}
		    	if(resultaxis[length-1]>max){resultaxis[length-1]=max;}
		    	for(i=0;i<resultaxis.length-1;i++){
		    		var label1=""+resultaxis[i];
		    		if(resultaxis[i]<0){
		    			label1=Math.abs(resultaxis[i])+ " BC";
		    		}
		    		else if(resultaxis[i]>0 && resultaxis[i]<1000){
		    			label1=Math.abs(resultaxis[i])+ " AD";
		    		}
		    		self.yaxislabels.push(label1);
		    		var label2="";
		    		if(i+1<resultaxis.length){
		    			if((resultaxis[i+1]-1)<0){
		    				label2=Math.abs(resultaxis[i+1]-1)+ " BC";
		    			}
		    			else if(resultaxis[i+1]-1>0 && resultaxis[i+1]-1<1000){
		    				label2=Math.abs(resultaxis[i+1]-1)+ " AD";
		    			}
		    			else{label2=""+(resultaxis[i+1]-1);}
		    			self.yaxis.push(label1+" - "+label2);
		    		}
		    		else if(i+1==resultaxis.length){
		    			if(resultaxis[i+1]<0){
		    				label2=Math.abs(resultaxis[i+1])+ " BC";
		    			}
		    			else if(resultaxis[i+1]>0 && resultaxis[i+1]<1000){
		    				label2=Math.abs(resultaxis[i+1])+ " AD";
		    			}
		    			else{label2=""+resultaxis[i+1];}
		    			self.yaxis.push(label1+" - "+label2);
		    		}
		    		else{
		    			self.yaxis.push(label1);
		    		}
		    		
		    		
		    	}
		    	
		    	var sortedyears=filtered.slice().sort(self.sortFunction);
		    	
		    	var index=0;
		    	var newaxis=[];
		    		for(j=1;j<resultaxis.length;j++){
		    			var val=resultaxis[j];
		    			var count=0;
		    			
		    			for(i=index;i<sortedyears.length;i++){
		    				if(sortedyears[i].value<val){
		    					count+=sortedyears[i].count;
		    					index=i+1;
		    				}
		    				else{break;}
		    			}
		    			self.barheights.push(count);
		    			self.numvalues.push("("+count+")");
		    		}
		    	
		    	/*now build the bar graph*/
		    	
		    		$('.chart').horizBarChart({
		    	          selector: '.bar',
		    	          speed: 1000
		    	        });
		    		
		    	return true;
		    }
	        return false;
       
		});
    }
    
    self.sortFunction = function(a, b) {
        return Number(a.value) > Number(b.value) ? 1 : -1;  
    }

    
 
    
    $('#facet_tags').on('itemRemoved', function(event) {
    	
    	if(event.item.id.indexOf("source#")>-1){
    	 if(ko.contextFor(this).$data.sources().length>=2)	{
    		 ko.contextFor(this).$data.sources.remove(event.item.label);
    		 ko.contextFor(this).$data.filterselect(true);
    		 ko.contextFor(this).$data.filtersearch();}
    	 else{
    		 $("#facet_tags").tagsinput('add',{ id: event.item.id, label: event.item.label });
    		 return false;}
    	
    	}
    	else{
    		var id=event.item.id.substring(0,event.item.id.indexOf("#"));
    		id=id.replace(/\_/g, '.');
    		var filterfound=ko.utils.arrayFirst(self.filters()[0].filters, function(item) {
        	  if(item!=undefined)	{
    		    if(item.filterID===id && id!="dates"){
    		    	var index=$.inArray(event.item.label, item.values);
    		    	if(index>-1){
    		    		item.values.splice(index,1);
    		    		if(item.values.length==0){
    		    			//remove filter id and values
    		    			self.filters()[0].filters.splice(self.filters()[0].filters.indexOf(item),1);
    		    			
    		    		}
    		    		return true;
    		    		
    		    	}
    		    	
    		    }else if(item.filterID===id && id=="dates"){
    		    	self.filters()[0].filters.splice(self.filters()[0].filters.indexOf(item),1);
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
    		
    		    if(item.filterID===id && id!="dates"){
    		    	if($.inArray(newvalue, item.values)>-1){
    		    		//value is already there 
    		    		exists=true;
    		    	}
    		    	else{
    		    		$("#facet_tags").tagsinput('add',{ id: id+'#'+item.values.length, label: newvalue });
    		        	
    		    		item.values.push(newvalue);
    		    	}
    		    	return true;
    		        
    		    }
    		    
    		    else if(item.filterID===id && id=="dates"){
    		    	//break new value to two values if it is range
    		    	var val1=newvalue;
    		    	var val2="";
    		    	if(newvalue.indexOf("-">0)){
    		    		val1=newvalue.substring(0,newvalue.indexOf("-")-1).trim().replace(' AD','');
    		    		val2=newvalue.substring(newvalue.indexOf("-")+2,newvalue.length).trim().replace(' AD','');
    		    		
    		    		if(val1.indexOf("BC")>0){
    		    			val1=-Math.abs(val1.replace(' BC',''));
    		    		}
    		    		if(val2.indexOf("BC")>0){
    		    			val2=-Math.abs(val2.replace(' BC',''));
    		    		}
    		    		
    		    	
    		    	}
    		    	
    		    	//now empty any previous year ranges
    		    	    
    		    	    
    		    	    var label="";
    		    	    if(Number(item.values[0])<0){
    		    	    	label=Math.abs(item.values[0])+" BC";
    		    	    }
    		    	    else if(Number(item.values[0])>0 && Number(item.values[0])<1000){
    		    	    	label=item.values[0]+" AD";
    		    	    }
    		    	    else{
    		    	    	label=item.values[0];
    		    	    }
    		    	    if(Number(item.values[1])<0){
    		    	    	label+=" - "+Math.abs(item.values[1])+" BC";
    		    	    }
    		    	    else if(Number(item.values[1])>0 && Number(item.values[1])<1000){
    		    	    	label+=" - "+item.values[1]+" AD";
    		    	    }
    		    	    else{
    		    	    	label+=" - "+item.values[1];
    		    	    }
    		    	    item.values=[];
    		    	    $('span.tag:contains("'+label+'")').html(newvalue+'<span data-role="remove"></span>');
    		    		item.values.push(val1.toString());
    		    		if(val2.toString().length>0)
    		    		item.values.push(val2.toString());
    		    		return true;
    		    	}
    		    	
    		        
    		    
    	        
           
        });
    	if(!filterfound){
    		
    		$("#facet_tags").tagsinput('add',{ id: id+'#0', label: newvalue });
        	var obj={};
    		obj['filterID']=id;
    		if(id!="dates")
    		  obj['values']=new Array(newvalue);
    		else if(id=="dates"){
    			var val2="";
		    	if(newvalue.indexOf("-">0)){
		    		val1=newvalue.substring(0,newvalue.indexOf("-")-1).trim().replace(' AD','');
		    		if(val1.indexOf("BC")>0){
		    			val1=-Math.abs(val1.replace(' BC',''));
		    		}
		    		val2=newvalue.substring(newvalue.indexOf("-")+2,newvalue.length).trim().replace(' AD','');
		    		if(val2.indexOf("BC")>0){
		    			val2=-Math.abs(val2.replace(' BC',''));
		    		}
		    	}
		    	obj['values']=[val1.toString()];
		    	if(val2.toString().length>0)
		    		obj['values']=[val1.toString(),val2.toString()];
		    	
    		}
    		self.filters()[0].filters.push(obj);
    	}
    	if(!exists){
    	  self.setFilters();
    	  }
          	 
    }
    
    
    self.setFilters=function(){
    	self.visiblePanel(false);
    	ko.contextFor(withsearchid).$data.filterselection([]);
    	var searchfacets=[];
    	for(var i=0;i<self.filters()[0].filters.length; i++){
    		 var filter=self.filters()[0].filters[i];
    		 filter.filterID=filter.filterID.replace(/\_/g, '.');
	         searchfacets.push(filter);
    	}
    	ko.contextFor(withsearchid).$data.filterselection().push.apply(ko.contextFor(withsearchid).$data.filterselection(),searchfacets);
    	ko.contextFor(withsearchid).$data.filtersearch();
    }
    
     self.listBind=function(e){
    	var link = $(e.target);
    	$('#f_search').val('');$('#f_search').removeAttr('value');
    	$('#'+e.filterID).liveFilter('#f_search', 'li', {
    		  filterChildSelector: 'a'
    		});
    	
    }
    
    self.sourceBind=function(e){
    	$('#f_search').val('');$('#f_search').removeAttr('value');
    	$('#datasources').liveFilter('#f_search', 'li', {
    		  filterChildSelector: 'span'
    		});
    	
    }
    
       
	
    self.makeYaxis=function(yMin, yMax, ticks  /*10*/)
    {
		     if(typeof ticks === "undefined"){
		    			ticks = 10;
		    }
		      var result = [];
		      // If yMin and yMax are identical, then
		      // adjust the yMin and yMax values to actually
		      // make a graph. 
		      if(yMin == yMax)
		      {
		        yMin = yMin - 10;   // some small value
		        yMax = yMax + 10;   // some small value
		      }
		      // Determine Range
		      var range = yMax - yMin;
		      // Adjust ticks if needed
		      if(ticks < 2)
		        ticks = 2;
		      else if(ticks > 2)
		        ticks -= 2;
		      // Get raw step value
		      var tempStep = range/ticks;
		      // Calculate pretty step value
		      var mag = Math.floor(Math.log10(tempStep));
		      var magPow = Math.pow(10,mag);
		      var magMsd = (tempStep/magPow + 0.5);
		      var stepSize = magMsd*magPow;
		
		      // build Y label array.
		      // Lower and upper bounds calculations
		      var lb = stepSize * Math.floor(yMin/stepSize);
		      var ub = stepSize * Math.ceil((yMax/stepSize));
		      // Build array
		      var val = lb;
		     // var result=[];
		      while(1)
		      {
		    	  
		        result.push(Math.floor(val));
		        val += stepSize;
		        
		        if(val > ub || isNaN(val))
		          break;
		      }
		     
		      return result;
		    }
		
		   
    
  }
  
  self.arrayMin=function(arr) {
	  var len = arr.length, min = Infinity;
	  while (len--) {
		  if (Number(arr[len].value) < min) {
		      min = Number(arr[len].value);
		    }
	  }
	  return min;
	};

	self.arrayMax=function(arr) {
	  var len = arr.length, max = -Infinity;
	  while (len--) {
		    if (Number(arr[len].value) > max) {
		      max = Number(arr[len].value);
		    }
		  }
	  return max;
	};

	
	self.filterTooBig=function(value) {
		if(!isNaN(parseFloat(value)) && isFinite(value))
		  return function(element, index, array) {
		  if(element.count>0)
		    return (element.value < value);
		  }
		}
	
	
	

	
  return { viewModel: FacetsViewModel, template: template };
});
