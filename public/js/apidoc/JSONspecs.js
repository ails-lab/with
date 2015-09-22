


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
		        "description": "Welcome to the WITH API documentation! \nWe are still in a development phase, so expect frequent changes. We will keep this documentation updated and this text will include a memo of the latest changes. \n"
		    },
		    "paths": {
		        "/api/search": {
		            "post": {
		                "tags": [
		                    "Search"
		                ],
		                "summary": "General search in external resources and the WITH database.",
		                "description": "Body contains search parameters, response is a JSON array of records that match the search term. Boolean search supports use of AND, OR and NOT operators. Terms seperated without an operator (using a space) are treated as an AND. Use of quotes will perform exact term or phrase searches. For example, ```\"Olympian Zeus\"``` will search for the exact phrase, whereas ```Olympian Zeus``` will equate to ```Olympian AND Zeus```. For more search options use advanced search (coming soon).",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "required": true,
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
		                            "type": "array",
		                            "items": {
		                                "$ref": "#/definitions/Record"
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Bad request",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
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
		                        "description": "Offset",
		                        "type": "integer"
		                    },
		                    {
		                        "name": "count",
		                        "in": "query",
		                        "description": "Count (default 10)",
		                        "type": "integer"
		                    },
		                    {
		                        "name": "access",
		                        "in": "query",
		                        "description": "Type of access (read, write, owned - default is owned)",
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
		                "summary": "Get a list of collections.",
		                "description": "Using the parameter filters, you can get the collections associated with a specific user. You only need to provide one user filter with each call. The access filter refers to the access rights you have to another user's collections. If users are not provided then all publicly available collections are returned.",
		                "tags": [
		                    "Collection"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "type": "array",
		                            "items": {
		                                "$ref": "#/definitions/Collection"
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
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
		                "description": "This creates a new collection and stores it in the database. You can add records to it later with  ```/collection/{collectionId}/addRecord```. Fields with asterisk are required. Note that calls to this path can also be used for exhibitions. At least one user must be logged in for the session and will be automatically retrieved. The owner of the collection will be the first user.",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "Collection metadata",
		                        "required": true,
		                        "schema": {
		                            "type": "object",
		                            "required": [
		                                "title",
		                                "isPublic"
		                            ],
		                            "properties": {
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
		                        "description": "Bad Request",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "description": "invalid json, constraint violations",
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbidden",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "description": "cannot find user or unspecified",
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "description": "title already exists, cannot save to database",
		                                    "type": "string"
		                                }
		                            }
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
		                    "description": "Id of the collection or exhibition",
		                    "type": "string"
		                }
		            ],
		            "post": {
		                "description": "Adds a record to the collection specified in the path, creating a new record that containts the specified metadata. You will need to have write access or be the owner of the collection to add records to it. Note that calls to this path can also be used for exhibitions. Position is a Mandatory field for exhibitions, the default is 0.",
		                "summary": "Add a record to a collection.",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "Record JSON schema",
		                        "schema": {
		                            "type": "object",
		                            "required": [
		                                "ownerId",
		                                "title",
		                                "isPublic"
		                            ],
		                            "properties": {
		                                "source": {
		                                    "type": "string"
		                                },
		                                "sourceId": {
		                                    "type": "string"
		                                },
		                                "title": {
		                                    "type": "string"
		                                },
		                                "description": {
		                                    "type": "string"
		                                },
		                                "thymbnailUrl": {
		                                    "type": "string"
		                                },
		                                "sourceUrl": {
		                                    "type": "string"
		                                },
		                                "position": {
		                                    "type": "integer"
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
		                    },
		                    "400": {
		                        "description": "Bad Request (no position, constraint violation)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbidden (no permission to edit collection)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error (cannot save to database)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
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
		                    "description": "Id of the collection or exhibition from which to remove the record",
		                    "type": "string"
		                }
		            ],
		            "delete": {
		                "description": "Removes the specified record (parameter) from a specified collection (path). Note that calls to this path can also be used for exhibitions.",
		                "summary": "Remove a record from a collection.",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "parameters": [
		                    {
		                        "name": "recordId",
		                        "in": "query",
		                        "description": "Id of record to be removed",
		                        "type": "string"
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK (collection Id in string)",
		                        "schema": {
		                            "properties": {
		                                "message": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbidden (no permission to edit collection)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error (no record Id, cannot delete from database, exception error)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        },
		        "/collection/{collectionId}/list": {
		            "parameters": [
		                {
		                    "name": "collectionId",
		                    "in": "path",
		                    "description": "Id of the collection",
		                    "type": "string"
		                }
		            ],
		            "get": {
		                "description": "Retrieves all records from the collection specified in the path and returns an array of record objects. The format parameter defines the serialization in the records array field of the JSON response.",
		                "summary": "Retrieve all records in a collection.",
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
		                        "description": "One of the following:  JSON_UNKNOWN, JSONLD_UNKNOWN, XML_UNKNOWN, JSON_EDM, JSONLD_EDM, XML_EDM, JSONLD_DPLA, JSON_NLA, XML_NLA, JSON_DNZ, XML_DNZ, JSON_YOUTUBE, “UKNOWN”, “all”. If not specified, no content is returned, only basic collection fields.",
		                        "type": "string"
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK (JSON contains the serialization specified)",
		                        "schema": {
		                            "type": "object",
		                            "properties": {
		                                "itemCount": {
		                                    "type": "integer"
		                                },
		                                "records": {
		                                    "type": "array",
		                                    "items": {
		                                        "$ref": "#/definitions/Record"
		                                    }
		                                }
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbiden (invalid collection id, no read access)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error (cannot retrieve records from database)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
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
		                    "description": "Internal id of the collection or exhibition",
		                    "type": "string"
		                }
		            ],
		            "get": {
		                "summary": "Retrieve collection metadata.",
		                "description": "Returns the metadata of the collection specified in path. Note that calls to this path can also be used for exhibitions.",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "properties": {
		                                "owner": {
		                                    "type": "string"
		                                },
		                                "access": {
		                                    "type": "string"
		                                },
		                                "collection": {
		                                    "$ref": "#/definitions/Collection"
		                                }
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbidden (no read-access)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error(database error)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            },
		            "post": {
		                "summary": "Update metadata in a collection.",
		                "description": "Use this call to change the stored metadata of a collection. Note that calls to this path can also be used for exhibitions.",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "New collection/exhibtion metadata. Only provide the fields you wish to be changed!",
		                        "required": false,
		                        "schema": {
		                            "$ref": "#/definitions/Collection"
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "properties": {
		                                "owner": {
		                                    "type": "string"
		                                },
		                                "access": {
		                                    "type": "string"
		                                },
		                                "collection": {
		                                    "$ref": "#/definitions/Collection"
		                                }
		                            }
		                        }
		                    },
		                    "400": {
		                        "description": "Bad Request (null/invalid JSON, duplicate title, wrong JSON fields)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbidden (no read-access)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error (database error)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            },
		            "delete": {
		                "summary": "Delete a collection.",
		                "description": "Removes a collection from the database. Records that were created into this collection will also be deleted. Note that calls to this path can also be used for exhibitions.",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Collection"
		                        }
		                    },
		                    "403": {
		                        "description": "Forbiden (no read-access)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error (database error)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        },
		        "/record/{recordId}": {
		            "parameters": [
		                {
		                    "name": "recordId",
		                    "in": "path",
		                    "required": true,
		                    "description": "The id of the record",
		                    "type": "string"
		                }
		            ],
		            "get": {
		                "parameters": [
		                    {
		                        "name": "format",
		                        "in": "query",
		                        "description": "The serialization of the response. One of the following:  JSON_UNKNOWN, JSONLD_UNKNOWN, XML_UNKNOWN, JSON_EDM, JSONLD_EDM, XML_EDM, JSONLD_DPLA, JSON_NLA, XML_NLA, JSON_DNZ, XML_DNZ, JSON_YOUTUBE, “UKNOWN”, “all”. If not specified, no content is returned, only basic collection fields.",
		                        "type": "string"
		                    }
		                ],
		                "summary": "Retrieve a record.",
		                "description": "Retrieve a JSON with the metadata of the record specified in the path. The format parameter defines the serialization in the record field of the JSON response.",
		                "tags": [
		                    "Record"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Record"
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error (database error)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            },
		            "post": {
		                "summary": "Update a record.",
		                "description": "Update the metadata of an existing record, specified by its id in the path. You only need to provide the fields you want updated in the record body.",
		                "parameters": [
		                    {
		                        "name": "format",
		                        "in": "query",
		                        "description": "The serialization of the response. One of the following:  JSON_UNKNOWN, JSONLD_UNKNOWN, XML_UNKNOWN, JSON_EDM, JSONLD_EDM, XML_EDM, JSONLD_DPLA, JSON_NLA, XML_NLA, JSON_DNZ, XML_DNZ, JSON_YOUTUBE, “UKNOWN”, “all”. If not specified, no content is returned, only basic collection fields.",
		                        "type": "string"
		                    },
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "A JSON with the updated metadata",
		                        "schema": {
		                            "$ref": "#/definitions/Record"
		                        }
		                    }
		                ],
		                "tags": [
		                    "Record"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "properties": {
		                                "message": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "400": {
		                        "description": "Bad Request (invalid json)",
		                        "schema": {
		                            "properties": {
		                                "message": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbiden (no edit permissions)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error (database error)",
		                        "schema": {
		                            "properties": {
		                                "message": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            },
		            "delete": {
		                "summary": "Remove a record.",
		                "description": "Remove the whole record specified in the path from the database, or just a single format.",
		                "tags": [
		                    "Record"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "properties": {
		                                "message": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error",
		                        "schema": {
		                            "properties": {
		                                "message": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        },
		        "/rights/{collectionId}/{right}": {
		            "parameters": [
		                {
		                    "name": "collectionId",
		                    "in": "path",
		                    "required": true,
		                    "description": "Internal Id of the collection whose rights you wish to change",
		                    "type": "string"
		                },
		                {
		                    "name": "right",
		                    "in": "path",
		                    "required": true,
		                    "description": "\"none\" (withdraws previously given rights), \"read\", \"write\", \"own\"",
		                    "type": "string"
		                }
		            ],
		            "post": {
		                "summary": "Change access rights to a collection.",
		                "description": "Changes access rights: \"none\" (withdraws previously given rights), \"read\", \"write\", \"own\", of a specified user (parameter) for a specifed collection (in path). Only the owner of a collection can use this call (you need to be loged in). Just one of username, email or userId needs to be provided.",
		                "tags": [
		                    "Rights",
		                    "Collection"
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
		                        "description": "OK",
		                        "schema": {
		                            "properties": {
		                                "message": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "400": {
		                        "description": "Bad Request (no user specified)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbidden (no owner rights)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Interal Server Error (read/write database error)",
		                        "schema": {
		                            "properties": {
		                                "message": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        },
		        "/exhibition/list": {
		            "get": {
		                "tags": [
		                    "Exhibition"
		                ],
		                "summary": "Get all your exhibitons.",
		                "description": "Returns an array of collection JSON objects, for all exhibitions owned by the currently logged in user. See ```/user/login```.",
		                "parameters": [
		                    {
		                        "name": "offset",
		                        "description": "Offset (default is 0)",
		                        "in": "query",
		                        "type": "integer"
		                    },
		                    {
		                        "name": "count",
		                        "description": "Number of results (default is 10)",
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
		                                "$ref": "#/definitions/Collection"
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbidden (no user specified)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Interal Server Error (exception error)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
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
		                "summary": "Create new user.",
		                "produces": [
		                    "application/json",
		                    "application/xml"
		                ],
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "Contains JSON of the user to create",
		                        "required": true,
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
		                        "description": "Bad Request (json object describes all errors)",
		                        "schema": {
		                            "type": "object"
		                        }
		                    }
		                }
		            }
		        },
		        "/user/login": {
		            "post": {
		                "tags": [
		                    "User"
		                ],
		                "summary": "User login.",
		                "description": "Log an user in (create a browser cookie). Some API calls do not take the user as a parameter and you need to be logged in first. You can log in with your google or facebook id. The email parameter can be a username.",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "Email or username and password",
		                        "required": true,
		                        "schema": {
		                            "type": "object",
		                            "properties": {
		                                "email": {
		                                    "type": "string"
		                                },
		                                "password": {
		                                    "type": "string"
		                                },
		                                "googleId": {
		                                    "type": "string"
		                                },
		                                "facebokId": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK (creates login browser cookie, returns user metadata JSON including userID.)",
		                        "schema": {
		                            "$ref": "#/definitions/User"
		                        }
		                    },
		                    "400": {
		                        "description": "Bad Request (error status, problem description JSON object)",
		                        "schema": {
		                            "type": "object"
		                        }
		                    }
		                }
		            }
		        },
		        "/user/logout": {
		            "get": {
		                "description": "Browser cookie is removed, user is logged out (all session information is kept in cookie, nothing is stored on server).",
		                "summary": "User logout.",
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
		                "summary": "Check email availability.",
		                "description": "Used when registering a new user, checks if there has been another user with the same email already stored in the database.",
		                "parameters": [
		                    {
		                        "name": "email",
		                        "in": "query",
		                        "description": "Proposed email address",
		                        "required": true,
		                        "type": "string"
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK"
		                    },
		                    "400": {
		                        "description": "Bad Request (not available)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        },
		        "/user/{userId}": {
		            "parameters": [
		                {
		                    "name": "userId",
		                    "in": "path",
		                    "description": "Internal ID of a user",
		                    "required": true,
		                    "type": "string"
		                }
		            ],
		            "get": {
		                "summary": "Get user details.",
		                "description": "Returns the complete entry of a user specified by the id provided in the path.",
		                "tags": [
		                    "User"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/User"
		                        }
		                    },
		                    "400": {
		                        "description": "Bad Request (user does not exist, exception error)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            },
		            "put": {
		                "tags": [
		                    "User"
		                ],
		                "summary": "Update a user entry.",
		                "description": "Updates the stored info of the user specified by the id provided in the path.",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "New user entry",
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
		                    },
		                    "400": {
		                        "description": "Bad Request (error status, problem description JSON object)",
		                        "schema": {
		                            "type": "object"
		                        }
		                    }
		                }
		            },
		            "delete": {
		                "tags": [
		                    "User"
		                ],
		                "summary": "Deletes the user.",
		                "description": "Removes a user from the database. (This call might not function currently at the moment.)",
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "properties": {
		                                "message": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "400": {
		                        "description": "Bad Request (user does not exist)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
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
		                "summary": "Send a reset password email.",
		                "description": "Sends an email to the user provided in the path. The email contains a link to a webpage where the user can provide a new password.",
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "properties": {
		                                "mesage": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "400": {
		                        "description": "Bad Request (invalid username or email, could not send email)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "404": {
		                        "description": "Not Found (user email not found - if user had originally registered with google or facebook account))",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        },
		        "/user/apikey/create": {
		            "post": {
		                "tags": [
		                    "User"
		                ],
		                "summary": "Get an API key.",
		                "description": "Automatically sends an API key to the stored email address of the logged in user.",
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "properties": {
		                                "email": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Bad Request (no user logged in, email already sent in past, email exception error)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error ( could not create API key)",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        }
		    },
		    "definitions": {
		        "User": {
		            "description": "Describes a registered user in the database.",
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
		            "description": "Collection metadata. The fields isExhibition and exhibition are only used in calls for exhibitions.",
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
		            "description": "An individual record description and metadata. The fields exhibition and position are used only for records in exhibitions.",
		            "type": "object",
		            "required": [
		                "title",
		                "position"
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
		                },
		                "position": {
		                    "description": "default is 0",
		                    "type": "integer"
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















function apiKeyClick(){
	
	$.ajax({
		type    : "get",
		url     : "/user/apikey/create",
		success : function(data) {
			// Show message that an email was sent
			$("#myModal").find("h4").html("API key requested!");
			$("#myModal").find("#popupText").html("<p>An email was successfuly sent. " +
					"Follow the instructions to create a new password.</p>");
			$("#myModal").modal('show');
		},
		error   : function(request, status, error) {
			//var err = JSON.parse(request.responseText, function(k,v){
			//	alert(k);
			//	alert(v);
			//			});

			var err = JSON.parse(request.responseText);
			$("#myModal").find("h4").html("Email not sent");
			$("#myModal").find("#popupText").html("<p>" + err.error.error + "</p>");
			$("#myModal").find("h4").html("Email not sent");
			$("#myModal").modal('show');

		}
	});

    return false;

}


