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
                paths[ 0 ] << {
                    get << {
                        tags[ 0 ] = "pet"
                        summary = "Add a new pet to the store"
                        description = ""
                        operationId = "addPet"
                        consumes[ 0 ] = "application/json"
                        consumes[ 1 ] = "application/xml"
                        produces[ 0 ] = "application/xml"
                        produces[ 1 ] "application/json"
                        parameters << {
                            name = "body"
                            description = "Pet object that needs to be added to the store"
                            required = true
                            in << {
                                body.schema_ref = "Pet"
                            }
                        }
                        responses << {

                        }

}

      type Parameter {
    name: string
    in: InBody | InOther
    required?: bool
    description?: string


    }

    type OperationObject {
    tags*: string
    summary?: string
    description?: string
    externalDocs?: ExternalDocs
    operationId: string
    consumes*: string
    produces*: string
    parameters*: Parameter
    responses*: Responses
}

    paths*: string {
        get?: OperationObject
        post?: OperationObject
        delete?: OperationObject
        put?: OperationObject
        /* TODO
        .options?: OperationObject
        .head?: OperationObject
        .patch?: OperationObject
        .parameters?:
        */
    }
    types*: TypeDefinition
    /* TODO
      .security?
      .securityDefinitions?
      .defintions?

    */
    version: string( enum(["2.0","3.0"]))
            }


            getInputPortMetaData@MetaJolie( { filename = "private/sample_service_joliedoclib.ol" } )( meta_description )
            for ( i in meta_description.input.interfaces ) {
                for( t in i.types ) {}
            }


            validateJson@JsonUtils({
                json = "{\n" 
                    + "  \"openapi\": \"3.0.0\",\n" 
                    + "  \"info\": { \"title\": \"Test API\", \"version\": \"1.0\" },\n" 
                    + "  \"paths\": {},\n" 
                    + "  \"components\": {\n"
                    + "     \"schemas\":" 
                    + schema_string 
                    + "  }\n"
                    + "}"
                schema = openapi_schema3 
            })( validation )

            

            if ( is_defined( validation.validationMessage ) ) {
                valueToPrettyString@StringUtils( validation )( errors )
                throw( TestFailed, "schema:" + schema_string + "\n" + errors )
            }

        }
    }

}