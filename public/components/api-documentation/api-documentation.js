define(['knockout', 'text!./api-documentation.html'], function (ko, template) {

	
	
	
function ApiDocuModel(params) {
	

		this.details = ko.observable();
		
        this.details("<em>For further details, view the report <a href='report.html'>here</a>.</em>"); // HTML content appears
        
        //this.details = swagerUi;
        
		this.route = params.route;
	}

	


	


	return { viewModel: ApiDocuModel, template: template };
	
	
});
