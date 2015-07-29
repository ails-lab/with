'use strict';

SwaggerUi.Views.ParameterView = Backbone.View.extend({
  initialize: function(){
    Handlebars.registerHelper('isArray', function(param, opts) {
      if (param.type.toLowerCase() === 'array' || param.allowMultiple) {
        opts.fn(this);
      } else {
        opts.inverse(this);
      }
    });
  },

  render: function() {
    var type = this.model.type || this.model.dataType;

    if (typeof type === 'undefined') {
      var schema = this.model.schema;
      if (schema && schema.$ref) {
        var ref = schema.$ref;
        if (ref.indexOf('#/definitions/') === 0) {
          type = ref.substring('#/definitions/'.length);
        } else {
          type = ref;
        }
      }
    }

    this.model.type = type;
    this.model.paramType = this.model.in || this.model.paramType;
    this.model.isBody = this.model.paramType === 'body' || this.model.in === 'body';
    this.model.isFile = type && type.toLowerCase() === 'file';
    this.model.default = (this.model.default || this.model.defaultValue);

    if(this.model.format === 'password') {
        this.model.inputType = 'password';
    } else {
        this.model.inputType = 'text';
    }

    if (this.model.allowableValues) {
      this.model.isList = true;
    }

    var template = this.template();
    $(this.el).html(template(this.model));

    var isParam = false;

    if (this.model.isBody) {
      isParam = true;
    }

    var contentTypeModel = {
      isParam: isParam
    };

    contentTypeModel.consumes = this.model.consumes;

    if (isParam) {
      var parameterContentTypeView = new SwaggerUi.Views.ParameterContentTypeView({model: contentTypeModel});
      $('.parameter-content-type', $(this.el)).append(parameterContentTypeView.render().el);
    }

    else {
      var responseContentTypeView = new SwaggerUi.Views.ResponseContentTypeView({model: contentTypeModel});
      $('.response-content-type', $(this.el)).append(responseContentTypeView.render().el);
    }

    return this;
  },

  // Return an appropriate template based on if the parameter is a list, readonly, required
  template: function(){
    if (this.model.isList) {
      return Handlebars.templates.param_list;
    } else {
      if (this.options.readOnly) {
        if (this.model.required) {
          return Handlebars.templates.param_readonly_required;
        } else {
          return Handlebars.templates.param_readonly;
        }
      } else {
        if (this.model.required) {
          return Handlebars.templates.param_required;
        } else {
          return Handlebars.templates.param;
        }
      }
    }
  }
});