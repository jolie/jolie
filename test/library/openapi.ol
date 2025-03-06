/*

 * Copyright (c) 2025 Claudio Guidi <guidiclaudio@gmail.com>

 *   This program is free software; you can redistribute it and/or modify 
 *   it under the terms of the GNU Library General Public License as      
 *   published by the Free Software Foundation; either version 2 of the   
 *   License, or (at your option) any later version.                      
 *                                                                        
 *   This program is distributed in the hope that it will be useful,      
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of       
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 *   GNU General Public License for more details.                         
 *                                                                        
 *   You should have received a copy of the GNU Library General Public    
 *   License along with this program; if not, write to the                
 *   Free Software Foundation, Inc.,                                      
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            
 *                                                                        
 *   For details about the authors of this software, see the AUTHORS file.
*/

from ..test-unit import TestUnitInterface
from json-schema import JsonSchema
from metajolie import MetaJolie
from console import Console
from string-utils import StringUtils
from json-utils import JsonUtils
from file import File
from openapi import OpenApi


service Main {

    embed JsonSchema as JsonSchema
    embed MetaJolie as MetaJolie
    embed Console as Console
    embed StringUtils as StringUtils
    embed JsonUtils as JsonUtils
    embed File as File
    embed OpenApi as OpenApi

	inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

    main {
        test()() {

            readFile@File( { filename ="library/private/openapi-schema2_0.json" })( openapi_schema2 )
            readFile@File( { filename ="library/private/openapi-schema3_0.json" })( openapi_schema3 )

            getInputPortMetaData@MetaJolie( { filename = "./library/private/openapi-test.ol" } )( meta_description )
            for ( i in meta_description.input.interfaces ) {
                for( t in i.types ) {
                    hashmap.( t.name ) << t.type
                    request.types[ #request.types ] << t
                }
            }

        
            request << {
                 info << {
                    description ="This is a sample server Petstore server.  You can find out more about..."
                    version = "1.0.0"
                    title = "Swagger Petstore 2.0"
                    termsOfService = "http://swagger.io/terms/"
                    contact.email = "apiteam@swagger.io"
                    license << {
                        name = "Apache 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"
                    }
                }
                version = "2.0"
                servers << {
                    host = "petstore.swagger.io"
                    basePath = "/v2"
                    schemes[ 0 ] = "http"
                    schemes[ 1 ] = "https"
                }
                tags[ 0 ] << {
                    name = "pet"
                    description = "Everything about your Pets"
                    externalDocs << {
                        description = "Find out more"
                        url = "http://swagger.io"
                    }
                }
                tags[ 1 ] << {
                    name = "store"
                    description ="Access to Petstore orders"
                }
                tags[ 2 ] << {
                    name = "user"
                    description = "Operations about user"
                    externalDocs << {
                        description = "Find out more about our store"
                        url = "http://swagger.io"
                    }
                }
                paths[ 0 ] << "/pet" {
                    post << {
                        tags[ 0 ] = "pet"
                        summary = "Add a new pet to the store"
                        description = ""
                        operationId = "addPet"
                        consumes[ 0 ] = "application/json"
                        consumes[ 1 ] = "application/xml"
                        produces[ 0 ] = "application/xml"
                        produces[ 1 ] = "application/json"
                        parameters << {
                            name = "body"
                            description = "Pet object that needs to be added to the store"
                            required = true
                            in << {
                                body.schema_ref = "Pet"
                            }
                        }
                        responses[ 0 ] << {
                            status = 400
                            description = "Ivalid Input"
                        }
                        responses[ 1 ] << {
                            status = 422
                            description = "Validation Exception"
                        }
                    }
                    put << {
                        tags[ 0 ] = "pet"
                        summary = "Update an existing pet"
                        description = ""
                        operationId = "updatePet"
                        consumes[ 0 ] = "application/json"
                        consumes[ 1 ] = "application/xml"
                        produces[ 0 ] = "application/xml"
                        produces[ 1 ] = "application/json"
                        parameters << {
                            name = "body"
                            description = "Pet object that needs to be added to the store"
                            required = true
                            in << {
                                body.schema_ref = "Pet"
                            }
                        }
                        responses[ 0 ] << {
                            status = 400
                            description = "Ivalid ID supplied"
                        }
                        responses[ 1 ] << {
                            status = 404
                            description = "Pet not found"
                        }
                        responses[ 2 ] << {
                            status = 422
                            description = "Validation Exception"
                        }
                    }
                }
                paths[ 1 ] << "/pet/findByStatus" {
                    get << {
                        tags[ 0 ] = "pet"
                        summary = "Finds Pets by status"
                        description = "Multiple status values can be provided with comma separated strings"
                        operationId = "findPetsByStatus"
                        produces[ 0 ] = "application/xml"
                        produces[ 1 ] = "application/json"
                        parameters << {
                            name = "status"
                            description = "Status values that need to be considered for filter"
                            required = true
                            in << {
                                other << "query" {
                                    nativeType.string_type = true
                                }
                            }
                        }
                        responses[ 0 ] << {
                            status = 200
                            description = "successful operation"
                            schema << hashmap.Pet
                        }
                        responses[ 1 ] << {
                            status = 400
                            description = "Invalid status value"
                        }
                    }
                }
                paths[ 2 ] << "/pet/{petId}" {
                    get << {
                        tags[ 0 ] = "pet"
                        summary = "Find pet by ID"
                        description = "Returns a single pet"
                        operationId = "getPetById"
                        produces[ 0 ] = "application/xml"
                        produces[ 1 ] = "application/json"
                        parameters <<  {
                            name = "petId"
                            description = "ID of pet to return"
                            required = true
                            in << {
                                other << "path" {
                                    nativeType.int_type = true
                                }
                            } 
                        }
                        responses[ 0 ] << {
                            status = 200
                            description = "successful operation"
                            schema << hashmap.Pet
                        }
                        responses[ 1 ] << {
                            status = 400
                            description = "Invalid ID supplied"
                        }
                        responses[ 2 ] << {
                            status = 404
                            description = "Pet not found"
                        }
                    }
                    post << {
                        tags = "pet"
                        summary = "Updates a pet in the store with form data"
                        description = ""
                        operationId = "updatePetWithForm"
                        consumes[ 0 ] = "application/x-www-form-urlencoded"
                        produces[ 0 ] = "application/xml"
                        produces[ 1 ] = "application/json"
                        parameters[ 0 ] << {
                            name = "petId"
                            required = true
                            description = "ID of pet that needs to be updated"
                            in << {
                                other << "path" {
                                    nativeType.int_type = true
                                }
                            } 
                        }
                        parameters[ 1 ] << {
                            name = "name"
                            required = false
                            description = "Updated name of the pet"
                            in << {
                                other << "header" {
                                    nativeType.string_type = true
                                }
                            } 
                        }
                        parameters[ 2 ] << {
                            name = "status"
                            required = false
                            description = "Updated status of the pet"
                            in << {
                                other << "header" {
                                    nativeType.string_type = true
                                }
                            } 
                        }
                        responses[ 0 ] << {
                            status = 400
                            description = "Invalid input"
                        }
                        responses[ 1 ] << {
                            status = 422
                            description = "Validation exception"
                        }
                    }
                    delete << {
                        tags = "pet"
                        summary = "Deletes a pet"
                        description = ""
                        operationId = "deletePet"
                        produces[ 0 ] = "application/xml"
                        produces[ 1 ] = "application/json"
                        parameters[ 0 ] << {
                            name = "api_key"
                            required = false
                            in << {
                                other << "header" {
                                    nativeType.string_type = true
                                }
                            } 
                        }
                        parameters[ 1 ] << {
                            name = "petId"
                            required = true
                            description = "Pet id to delete"
                            in << {
                                other << "path" {
                                    nativeType.long_type << true {
                                        refined_type << {
                                            ranges << {
                                                min = 10
                                                max = 100
                                            }
                                        }
                                    }
                                }
                            } 
                        }
                        responses[ 0 ] << {
                            status = 400
                            description = "Invalid input"
                        }
                        responses[ 1 ] << {
                            status = 404
                            description = "Pet not found"
                        }
                    }
                }
            }
    

            //println@Console( valueToPrettyString@StringUtils( request ) )()
            getOpenApiDefinition@OpenApi( request )( openapijson ) 
        
            validateJson@JsonUtils({
                json = openapijson
                schema = openapi_schema2
            })( validation )

            request.version = "3.0"
            getOpenApiDefinition@OpenApi( request )( openapijson ) 
            //println@Console( openapijson )()

            validateJson@JsonUtils({
                json = openapijson
                schema = openapi_schema3
            })( validation )

            if ( is_defined( validation.validationMessage ) ) {
                valueToPrettyString@StringUtils( validation )( errors )
                throw( TestFailed, "schema:" + schema_string + "\n" + errors )
            }

        }
    }

}