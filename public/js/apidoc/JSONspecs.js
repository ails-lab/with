


function getSpecs() {


	var jsonSpec = 


/////////////////////////////////////////////////////////
//
//		PASTE JSON FROM SWAGGER EDITOR
//			HERE :
//
////////////////////////////////////////////////////////






{
    "swagger": "2.0",
    "info": {
        "version": "v1",
        "title": "WITH API",
        "description": "Welcome to the WITH API documentation! \nWe are still in a development phase, so expect frequent changes. We will keep this documentation updated and this text will include a memo of the latest changes.\n You can read the full log here:\n```\nhttp://WITH.com\n```\n"
    },
    "paths": {
        "/api/search": {
            "post": {
                "tags": [
                    "Search"
                ],
                "summary": "General search in external resources and the WITH database.",
                "parameters": [
                    {
                        "in": "body",
                        "name": "body",
                        "description": "Search parameters.",
                        "schema": {
                            "type": "object",
                            "properties": {
                                "searchTerm": {
                                    "type": "string"
                                },
                                "page": {
                                    "type": "integer"
                                },
                                "pageSize": {
                                    "type": "integer"
                                },
                                "source": {
                                    "type": "string"
                                }
                            }
                        }
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Record"
                        }
                    }
                }
            }
        },
        "/collection/list": {
            "get": {
                "parameters": [
                    {
                        "name": "offset",
                        "in": "query",
                        "description": "offset",
                        "type": "integer"
                    },
                    {
                        "name": "count",
                        "in": "query",
                        "description": "count (default 10)",
                        "type": "integer"
                    },
                    {
                        "name": "access",
                        "in": "query",
                        "description": "access (default is owned)",
                        "type": "string"
                    },
                    {
                        "name": "filterByUser",
                        "in": "query",
                        "description": "Owned by this user (username)",
                        "type": "string"
                    },
                    {
                        "name": "filterByUserId",
                        "in": "query",
                        "description": "Owned by this user (user ID)",
                        "type": "string"
                    },
                    {
                        "name": "filterByUserEmail",
                        "in": "query",
                        "description": "Owned by this user (user email)",
                        "type": "string"
                    }
                ],
                "summary": "Show a collection",
                "tags": [
                    "Collection"
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Collection"
                        }
                    }
                }
            }
        },
        "/collection/create": {
            "post": {
                "tags": [
                    "Collection"
                ],
                "summary": "Create a new collection.",
                "parameters": [
                    {
                        "in": "body",
                        "name": "body",
                        "description": "Collection metadata.",
                        "required": true,
                        "schema": {
                            "type": "object",
                            "required": [
                                "ownerId",
                                "title",
                                "isPublic"
                            ],
                            "properties": {
                                "ownerId": {
                                    "type": "string"
                                },
                                "title": {
                                    "type": "string"
                                },
                                "description": {
                                    "type": "string"
                                },
                                "isPublic": {
                                    "type": "boolean"
                                },
                                "rights": {
                                    "type": "string"
                                }
                            }
                        }
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Collection"
                        }
                    }
                }
            }
        },
        "/collection/{collectionId}/addRecord": {
            "parameters": [
                {
                    "name": "collectionId",
                    "in": "path",
                    "description": "collection id",
                    "type": "string"
                }
            ],
            "post": {
                "description": "Adds a record to a collection, creating a new record that containts the specified metadata.",
                "summary": "Add a record",
                "tags": [
                    "Collection"
                ],
                "parameters": [
                    {
                        "in": "body",
                        "name": "body",
                        "description": "Record json.",
                        "schema": {
                            "$ref": "#/definitions/Record"
                        }
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Record"
                        }
                    }
                }
            }
        },
        "/collection/{collectionId}/removeRecord": {
            "parameters": [
                {
                    "name": "collectionId",
                    "in": "path",
                    "description": "collection Id",
                    "type": "string"
                }
            ],
            "delete": {
                "description": "Removes a record from a collection",
                "summary": "Remove a record",
                "tags": [
                    "Collection"
                ],
                "parameters": [
                    {
                        "name": "recordId",
                        "in": "query",
                        "description": "ID of record to be removed.",
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK"
                    }
                }
            }
        },
        "/collection/{id}/list": {
            "parameters": [
                {
                    "name": "id",
                    "in": "path",
                    "description": "id",
                    "type": "string"
                }
            ],
            "get": {
                "description": "Retrieves all records from a collection.",
                "summary": "Retrieve all records",
                "tags": [
                    "Collection"
                ],
                "parameters": [
                    {
                        "name": "start",
                        "in": "query",
                        "description": "offset",
                        "type": "integer"
                    },
                    {
                        "name": "count",
                        "in": "query",
                        "description": "count (default 10)",
                        "type": "integer"
                    },
                    {
                        "name": "format",
                        "in": "query",
                        "description": "One of the following JSON_UNKNOWN, JSONLD_UNKNOWN,XML_UNKNOWN,JSON_EDM, JSONLD_EDM, XML_EDM, JSONLD_DPLA, JSON_NLA, XML_NLA, JSON_DNZ,XML_DNZ, JSON_YOUTUBE, “UKNOWN”, “all”. If not specified, no content is returned, only basic collection fields.",
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Record"
                        }
                    }
                }
            }
        },
        "/collection/{collectionId}": {
            "parameters": [
                {
                    "name": "collectionId",
                    "in": "path",
                    "description": "Internal id of the collection/exhibition",
                    "type": "string"
                }
            ],
            "get": {
                "summary": "Retrieves collection metadata.",
                "tags": [
                    "Collection"
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Collection"
                        }
                    },
                    "403": {
                        "description": "No read-access"
                    },
                    "500": {
                        "description": "Database error"
                    }
                }
            },
            "post": {
                "summary": "Updates collection metadata.",
                "tags": [
                    "Collection"
                ],
                "parameters": [
                    {
                        "in": "body",
                        "name": "body",
                        "description": "New collection metadata.",
                        "required": false,
                        "schema": {
                            "type": "object",
                            "properties": {
                                "title": {
                                    "type": "string"
                                },
                                "isPublic": {
                                    "type": "boolean"
                                },
                                "description": {
                                    "type": "string"
                                }
                            }
                        }
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Collection"
                        }
                    },
                    "400": {
                        "description": "Wrong JSON fields"
                    },
                    "403": {
                        "description": "No write-access"
                    },
                    "500": {
                        "description": "Database error"
                    }
                }
            },
            "delete": {
                "summary": "Deletes the collection.",
                "tags": [
                    "Collection"
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Collection"
                        }
                    },
                    "403": {
                        "description": "No read-access"
                    },
                    "500": {
                        "description": "Database error"
                    }
                }
            }
        },
        "/record/{id}": {
            "parameters": [
                {
                    "name": "id",
                    "in": "path",
                    "description": "The id of the record",
                    "type": "string"
                }
            ],
            "get": {
                "summary": "Retrieves a record.",
                "tags": [
                    "Record"
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Record"
                        }
                    }
                }
            },
            "post": {
                "summary": "Updates a record.",
                "tags": [
                    "Record"
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Record"
                        }
                    }
                }
            },
            "delete": {
                "summary": "Removes a record.",
                "tags": [
                    "Record"
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Record"
                        }
                    }
                }
            }
        },
        "/rights/{colId}/{right}": {
            "parameters": [
                {
                    "name": "colId",
                    "in": "path",
                    "description": "Internal Id of the object you wish to share (or unshare)",
                    "type": "string"
                },
                {
                    "name": "right",
                    "in": "path",
                    "description": "none (withdraw previously given right), read, write, own",
                    "type": "string"
                }
            ],
            "post": {
                "summary": "Set rights",
                "tags": [
                    "Rights"
                ],
                "parameters": [
                    {
                        "name": "username",
                        "in": "query",
                        "description": "username of user to give rights to (or take away from)",
                        "type": "string"
                    },
                    {
                        "name": "email",
                        "in": "query",
                        "description": "another way of specifying the user",
                        "type": "string"
                    },
                    {
                        "name": "userId",
                        "in": "query",
                        "description": "another way of specifying the user",
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK"
                    }
                }
            }
        },
        "/exhibition/create": {
            "post": {
                "tags": [
                    "Exhibition"
                ],
                "description": "Create a new exhibition with dummy title",
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/Exhibition"
                        }
                    },
                    "400": {
                        "description": "Invalid JSON"
                    },
                    "500": {
                        "description": "Database Error"
                    }
                }
            }
        },
        "/exhibition/list": {
            "get": {
                "tags": [
                    "Exhibition"
                ],
                "summary": "Get all user exhibitons",
                "parameters": [
                    {
                        "name": "offset",
                        "description": "offset (default is 0)",
                        "in": "query",
                        "type": "integer"
                    },
                    {
                        "name": "count",
                        "description": "number of results (default is 10)",
                        "in": "query",
                        "type": "integer"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "type": "array",
                            "items": {
                                "$ref": "#/definitions/Exhibition"
                            }
                        }
                    },
                    "500": {
                        "description": "Database Error"
                    }
                }
            }
        },
        "/user/register": {
            "post": {
                "tags": [
                    "User"
                ],
                "description": "Creates a new user and stores at the database.",
                "summary": "Create new user",
                "produces": [
                    "application/json",
                    "application/xml"
                ],
                "parameters": [
                    {
                        "in": "body",
                        "name": "body",
                        "description": "Contains JSON of the user to create.",
                        "required": false,
                        "schema": {
                            "type": "object",
                            "properties": {
                                "firstName": {
                                    "type": "string"
                                },
                                "lastName": {
                                    "type": "string"
                                },
                                "username": {
                                    "type": "string"
                                },
                                "email": {
                                    "type": "string"
                                },
                                "password": {
                                    "type": "string"
                                },
                                "gender": {
                                    "type": "string"
                                },
                                "facebookId": {
                                    "type": "string"
                                },
                                "googleID": {
                                    "type": "string"
                                },
                                "about": {
                                    "type": "string"
                                },
                                "location": {
                                    "type": "string"
                                }
                            }
                        }
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/User"
                        }
                    },
                    "400": {
                        "description": "Bad Request"
                    }
                }
            }
        },
        "/user/login": {
            "post": {
                "tags": [
                    "User"
                ],
                "summary": "User login",
                "parameters": [
                    {
                        "in": "body",
                        "name": "body",
                        "description": "Email or username and password.",
                        "required": true,
                        "schema": {
                            "type": "object",
                            "properties": {
                                "email": {
                                    "type": "string"
                                },
                                "password": {
                                    "type": "string"
                                }
                            }
                        }
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK status, login cookie, user metadata JSON including userID.",
                        "schema": {
                            "$ref": "#/definitions/User"
                        }
                    },
                    "400": {
                        "description": "Error status, problem description JSON."
                    }
                }
            }
        },
        "/user/logout": {
            "get": {
                "description": "Browser cookie is removed (all session information is kept in cookie, nothing is stored on server).",
                "summary": "logout user",
                "tags": [
                    "User"
                ],
                "responses": {
                    "200": {
                        "description": "OK"
                    }
                }
            }
        },
        "/user/emailAvailable": {
            "get": {
                "tags": [
                    "User"
                ],
                "summary": "Checks email availability.",
                "parameters": [
                    {
                        "name": "email",
                        "in": "query",
                        "description": "User email.",
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK"
                    },
                    "400": {
                        "description": "Not available"
                    }
                }
            }
        },
        "/user/{userId}": {
            "parameters": [
                {
                    "name": "userId",
                    "in": "path",
                    "description": "Internal ID of a user.",
                    "type": "string"
                }
            ],
            "get": {
                "summary": "Returns the complete user entry.",
                "tags": [
                    "User"
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/User"
                        }
                    }
                }
            },
            "put": {
                "tags": [
                    "User"
                ],
                "summary": "Updates user entry.",
                "parameters": [
                    {
                        "in": "body",
                        "name": "body",
                        "description": "User entry.",
                        "required": true,
                        "schema": {
                            "$ref": "#/definitions/User"
                        }
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/User"
                        }
                    }
                }
            },
            "delete": {
                "tags": [
                    "User"
                ],
                "summary": "Deletes the user.",
                "responses": {
                    "200": {
                        "description": "OK"
                    }
                }
            }
        },
        "/user/resetPassword/{emailOrUserName}": {
            "parameters": [
                {
                    "name": "emailOrUserName",
                    "in": "path",
                    "description": "Username or email",
                    "type": "string"
                }
            ],
            "get": {
                "tags": [
                    "User"
                ],
                "summary": "Reset password email.",
                "responses": {
                    "200": {
                        "description": "OK"
                    }
                }
            }
        },
        "/user/changePassword": {
            "post": {
                "tags": [
                    "User"
                ],
                "summary": "Change password.",
                "responses": {
                    "200": {
                        "description": "OK"
                    }
                }
            }
        },
        "/user/apikey/{email}": {
            "parameters": [
                {
                    "name": "email",
                    "in": "path",
                    "description": "e-mail",
                    "type": "string"
                }
            ],
            "post": {
                "tags": [
                    "User"
                ],
                "summary": "Get an API key.",
                "responses": {
                    "200": {
                        "description": "OK"
                    }
                }
            }
        }
    },
    "definitions": {
        "User": {
            "type": "object",
            "required": [
                "firstName",
                "lastName",
                "username",
                "email",
                "password",
                "about",
                "location"
            ],
            "properties": {
                "firstName": {
                    "type": "string"
                },
                "lastName": {
                    "type": "string"
                },
                "username": {
                    "type": "string"
                },
                "email": {
                    "type": "string"
                },
                "password": {
                    "type": "string"
                },
                "gender": {
                    "type": "string"
                },
                "facebookId": {
                    "type": "string"
                },
                "googleID": {
                    "type": "string"
                },
                "about": {
                    "type": "string"
                },
                "location": {
                    "type": "string"
                },
                "userId": {
                    "type": "string"
                }
            }
        },
        "Collection": {
            "type": "object",
            "properties": {
                "id": {
                    "type": "object",
                    "properties": {
                        "value": {
                            "type": "string"
                        }
                    }
                },
                "ownerId": {
                    "type": "object",
                    "properties": {
                        "value": {
                            "type": "string"
                        }
                    }
                },
                "className": {
                    "type": "string"
                },
                "title": {
                    "type": "string"
                },
                "description": {
                    "type": "string"
                },
                "itemCount": {
                    "type": "integer"
                },
                "isPublic": {
                    "type": "boolean"
                },
                "rights": {
                    "type": "object",
                    "properties": {
                        "value": {
                            "type": "string"
                        }
                    }
                }
            }
        },
        "Exhibition": {
            "type": "object",
            "properties": {
                "id": {
                    "type": "object",
                    "properties": {
                        "value": {
                            "type": "string"
                        }
                    }
                },
                "ownerId": {
                    "type": "object",
                    "properties": {
                        "value": {
                            "type": "string"
                        }
                    }
                },
                "className": {
                    "type": "string"
                },
                "title": {
                    "type": "string"
                },
                "description": {
                    "type": "string"
                },
                "itemCount": {
                    "type": "integer"
                },
                "isPublic": {
                    "type": "boolean"
                },
                "rights": {
                    "type": "object",
                    "properties": {
                        "value": {
                            "type": "string"
                        }
                    }
                },
                "isExhibition": {
                    "type": "boolean"
                },
                "exhibition": {
                    "type": "object",
                    "properties": {
                        "intro": {
                            "type": "string"
                        }
                    }
                }
            }
        },
        "Record": {
            "type": "object",
            "required": [
                "title"
            ],
            "properties": {
                "dbId": {
                    "type": "string"
                },
                "externalId": {
                    "type": "string"
                },
                "isPublic": {
                    "type": "boolean"
                },
                "source": {
                    "type": "string"
                },
                "thumbnailUrl": {
                    "type": "string"
                },
                "title": {
                    "type": "string"
                },
                "creator": {
                    "type": "string"
                },
                "description": {
                    "type": "string"
                },
                "provider": {
                    "type": "string"
                },
                "sourceId": {
                    "type": "string"
                },
                "sourceUrl": {
                    "type": "string"
                },
                "exhibition": {
                    "type": "object",
                    "properties": {
                        "anotation": {
                            "type": "string"
                        },
                        "audioUrl": {
                            "type": "string"
                        },
                        "videoUrl": {
                            "type": "string"
                        }
                    }
                }
            }
        }
    }
}








///////////////////////////////////



///////////////////////////////////
;

return jsonSpec;

}











function getAPIkeyButt(){
	
	$.ajax({
		type    : "get",
		url     : "/user/apikey/create",
		data	: {email:null},
		success : function(data) {
			// Show message that an email was sent
			$("#myModal").find("h4").html("API key requested!");
			$("#myModal").find("#popupText").html("<p>An email was automatically sent to the address you" +
					" have registered with. If you want an API key for a different email, provide it and press \"Send\".</p>");
			$("#myModal").modal('show');
		},
		error   : function(request, status, error) {
			//do nothing
		}
	});

	//alert("here?");
    return false;

}








function apiKeyClick(){
	
	var value = $(".form-control").val();
	
	//alert(value);
	$.ajax({
		type    : "get",
		url     : "/user/apikey/create",
		data	: {'email':value},
		success : function(data) {
			// Show message that an email was sent
			$("#myModal").find("h4").html("API key requested!");
			$("#myModal").find("#popupText").html("<p>An email was successfuly sent. Follow the instructions to create a new password</p>");
			$("#myModal").modal('show');
		},
		error   : function(request, status, error) {
			//var err = JSON.parse(request.responseText, function(k,v){
			//	alert(k);
			//	alert(v);
			//			});

			var err = JSON.parse(request.responseText);
			$("#myModal").find("h4").html("Couldn't send email");
			$("#myModal").find("#popupText").html("<p>" + err.APIKey + "</p>");
			$("#myModal").modal('show');

		}
	});

	//alert("here?");
    return false;

}


