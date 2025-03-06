/*
 *   Copyright (C) 2025 by Claudio Guidi <guidiclaudio@gmail.com>         
 *                                                                        
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

from types.definition-types import TypeDefinition
from types.definition-types import Type
from types.definition-types import NativeType

from console import Console
from string-utils import StringUtils
from json-utils import JsonUtils
from json-schema import JsonSchema

type ExternalDocs {
    url: string
    description?: string
}

type SchemaType {
    schema_type: Type             // used when there are more parameters in the body
}

type SchemaRef {
    schema_ref?: string           // add a reference to a schema
}
type InBody  {
    body: SchemaType | SchemaRef
}

type InOther {
    other: string(enum(["query","header","path"])) {
        nativeType: NativeType
        allowEmptyValue?: bool
    }
}


type Responses {
    status: int 
    schema?: Type
    description: string
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

 
type GetOpenApiDefinitionRequest {
    info {
        title: string
        description?: string
        termsOfService?: string
        contact? {
            name?: string
            url?: string
            email?: string
        }
        license? {
            name: string
            url: string
        }
        version: string
    }
    servers[1,*] {                  // in case of openapi 2.0 only the first will be considered
        host: string
        basePath: string
        schemes[1,*]: string        // in openapi 3.0 only the first will be considered
        description?: string        // not used in openapi 2.0
    }
    tags*: void {
        name: string
        description?: string
        externalDocs?: ExternalDocs
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

interface OpenApiInterface {
    RequestResponse:
        getOpenApiDefinition( GetOpenApiDefinitionRequest )( string )
}


private service OpenApi2 {

    execution: concurrent 

    embed Console as Console
    embed StringUtils as StringUtils
    embed JsonUtils as JsonUtils
    embed JsonSchema as JsonSchema

    inputPort OpenApi2 {
        location: "local"
        interfaces: OpenApiInterface
    }


    main {
        [  getOpenApiDefinition( request )( response ) {

            // 2.0
            openapi << {
                swagger = "2.0"
                info << request.info
                host = request.servers[ 0 ].host
                basePath = request.servers[ 0 ].basePath
            }

            
            // schemes
            if ( #request.servers[ 0 ].schemes == 1 ) {
                openapi.schemes._ = request.servers[ 0 ].schemes // because it must be converted as an array
            } else {
                openapi.schemes << request.servers[ 0 ].schemes
            }
            // tags
            if ( is_defined( request.tags ) ) {
                if ( #request.tags == 1 ) {
                    openapi.tags._ << request.tags   // because it must be converted as an array
                } else { openapi.tags << request.tags }
            }

            // servers
            openapi.host = request.servers[ 0 ].host
            openapi.basePath = request.servers[ 0 ].basePath


            // paths
            for( path in request.paths ) {
                foreach ( method : path ) {

                    // paths.tags
                    if ( #path.( method ).tags == 1 ) {
                        openapi.paths.( path ).( method ).tags._ << path.( method ).tags // because it must be converted as an array
                    } else {
                        openapi.paths.( path ).( method ).tags << path.( method ).tags
                    }

                    // paths.summary
                    if ( is_defined( path.( method ).summary ) ) {
                            openapi.paths.( path ).( method ).summary = path.( method ).summary
                    }

                    // paths.description
                    if ( is_defined( path.( method ).description ) ) {
                        openapi.paths.( path ).( method ).description = path.( method ).description
                    }
                      
                    // paths.externalDocs
                    if ( is_defined( path.( method ).externalDocs ) ) {
                        openapi.paths.( path ).( method ).externalDocs << path.( method ).externalDocs
                    }

                    // paths.operationId
                    if ( is_defined( path.( method ).operationId ) ) {
                        openapi.paths.( path ).( method ).operationId = path.( method ).operationId
                    }

                    // consumes
                    if ( is_defined( path.( method ).consumes ) ) {
                        if ( #path.( method ).consumes == 1 ) {
                            openapi.paths.( path ).( method ).consumes._ = path.( method ).consumes
                        } else {
                            openapi.paths.( path ).( method ).consumes << path.( method ).consumes
                        }
                    }

                    // produces
                    if ( is_defined( path.( method ).produces ) ) {
                        if ( #path.( method ).produces == 1 ) {
                            openapi.paths.( path ).( method ).produces._ = path.( method ).produces
                        } else {
                            openapi.paths.( path ).( method ).produces << path.( method ).produces
                        }
                    }

                    // responses
                    if ( is_defined(  path.( method ).responses ) ) {
                        for( res  in path.( method ).responses ) {
                            openapi.paths.( path ).( method ).responses.( res.status ).description = res.description
                            if ( is_defined( res.schema ) ) {
                                if ( res.schema instanceof SchemaType ) {
                                    openapi.paths.( path ).( method ).responses.( res.status ).schema << getType@JsonSchema( {
                                        type << res.schema 
                                        schemaVersion = request.version 
                                    })
                                }
                            }
                        }
                    }

                    // parameters
                    for( par = 0, par < #path.( method ).parameters, par++ ) {
                        cur_par -> openapi.paths.( path ).( method ).parameters[ 0 ]._[ par ]
                        cur_par.name = path.( method ).parameters[ par ].name
                        if ( is_defined( ath.( method ).parameters[ par ].required ) ) {
                            cur_par.required = path.( method ).parameters[ par ].required
                        }
                        
                        if ( path.( method ).parameters[ par ].in instanceof InBody ) {
                            
                                cur_par.in = "body"
                               
                        
                                if ( path.( method ).parameters[ par ].in.body instanceof SchemaType ) {
                                    type -> request.paths[ p ].( op ).parameters[ par ].in.in_body.schema_type
                                    cur_par.schema << getType@JsonSchema( {
                                        type << par.( method ).parameters[ par ].in.body.schema_type
                                        schemaVersion = request.version
                                    } )
                                } else if ( path.( method ).parameters[ par ].in.body instanceof SchemaRef ) {
                                    
                                    cur_par.schema.("$ref") = "#/definitions/" + path.( method ).parameters[ par ].in.body.schema_ref
                                }
                        } else if ( path.( method ).parameters[ par ].in instanceof InOther ) {
                                cur_par << getNativeType@JsonSchema( {
                                    nativeType << path.( method ).parameters[ par ].in.other.nativeType
                                    schemaVersion = request.version
                                } )
                                cur_par.in = path.( method ).parameters[ par ].in.other
                                cur_par.name = path.( method ).parameters[ par ].name
                                if ( is_defined( path.( method ).parameters[ par ].required ) ) {
                                    cur_par.required = path.( method ).parameters[ par ].required
                                }
                                if ( is_defined( path.( method ).parameters[ par ].in.other.allowEmptyValue ) ) {
                                    cur_par.allowEmptyValue = path.( method ).parameters[ par ].in.other.allowEmptyValue
                                }
                        }
                    }
                }
            }


            // definitions
            for( d in request.types ) {
                getTypeDefinition@JsonSchema( {
                    typeDefinition << d
                    schemaVersion = request.version
                })( typedef ) 
                foreach( name : typedef ) {
                    openapi.definitions.( name ) << typedef.( name )
                }

            }
        
            getJsonString@JsonUtils( openapi )( response )

    
        } ]

    }
}


private service OpenApi3 {

    execution: concurrent 

    embed Console as Console
    embed StringUtils as StringUtils
    embed JsonUtils as JsonUtils
    embed JsonSchema as JsonSchema

    inputPort OpenApi3 {
        location: "local"
        interfaces: OpenApiInterface
    }


    main {
        [  getOpenApiDefinition( request )( response ) {

            // 2.0
            openapi << {
                openapi = "3.0.0"
                info << request.info
            }

            
            // servers
            for( server = 0, server < #request.servers, server++ ) {
                for( scheme = 0, scheme < #request.servers[ server ].schemes, scheme++ ) {
                    openapi.servers[ server ]._.url = request.servers[ server ].schemes[ scheme ] + "://" + request.servers[ s ].host + request.servers[ s ].basePath
                }
            }

            // tags
            if ( is_defined( request.tags ) ) {
                if ( #request.tags == 1 ) {
                    openapi.tags._ << request.tags   // because it must be converted as an array
                } else { openapi.tags << request.tags }
            }


            // paths
            for( path in request.paths ) {
                foreach ( method : path ) {

                    // paths.tags
                    if ( #path.( method ).tags == 1 ) {
                        openapi.paths.( path ).( method ).tags._ << path.( method ).tags // because it must be converted as an array
                    } else {
                        openapi.paths.( path ).( method ).tags << path.( method ).tags
                    }

                    // paths.summary
                    if ( is_defined( path.( method ).summary ) ) {
                            openapi.paths.( path ).( method ).summary = path.( method ).summary
                    }

                    // paths.description
                    if ( is_defined( path.( method ).description ) ) {
                        openapi.paths.( path ).( method ).description = path.( method ).description
                    }
                      
                    // paths.externalDocs
                    if ( is_defined( path.( method ).externalDocs ) ) {
                        openapi.paths.( path ).( method ).externalDocs << path.( method ).externalDocs
                    }

                    // paths.operationId
                    if ( is_defined( path.( method ).operationId ) ) {
                        openapi.paths.( path ).( method ).operationId = path.( method ).operationId
                    }

                    // parameters or requestBody
                    for( par = 0, par < #path.( method ).parameters, par++ ) {
                        
                        // requestBody
                        if ( path.( method ).parameters[ par ].in instanceof InBody ) {

                                cur_body -> openapi.paths.( path ).( method ).requestBody
                                if ( is_defined( ath.( method ).parameters[ par ].required ) ) {
                                    cur_body.required = path.( method ).parameters[ par ].required
                                }
                                                              
                                if ( path.( method ).parameters[ par ].in.body instanceof SchemaType ) {
                                    for( c in path.( method ).consumes ) {
                                        type -> request.paths[ p ].( op ).parameters[ par ].in.in_body.schema_type
                                        cur_body.content.( c ).schema << getType@JsonSchema( {
                                            type << par.( method ).parameters[ par ].in.body.schema_type
                                            schemaVersion = request.version
                                        } )
                                    }
                                } else if ( path.( method ).parameters[ par ].in.body instanceof SchemaRef ) {
                                    for( c in path.( method ).consumes ) {
                                        cur_body.content.( c ).schema.("$ref") = "#/components/schemas/" + path.( method ).parameters[ par ].in.body.schema_ref
                                    }
                                }
                        // parameters
                        } else if ( path.( method ).parameters[ par ].in instanceof InOther ) {
                                cur_par -> openapi.paths.( path ).( method ).parameters._[ par ]
                                cur_par.schema << getNativeType@JsonSchema( {
                                    nativeType << path.( method ).parameters[ par ].in.other.nativeType
                                    schemaVersion = request.version
                                } )
                                cur_par.in = path.( method ).parameters[ par ].in.other
                                cur_par.explode = true
                                cur_par.name = path.( method ).parameters[ par ].name
                                if ( is_defined( path.( method ).parameters[ par ].required ) ) {
                                    cur_par.required = path.( method ).parameters[ par ].required
                                }
                                if ( is_defined( path.( method ).parameters[ par ].in.other.allowEmptyValue ) ) {
                                    cur_par.allowEmptyValue = path.( method ).parameters[ par ].in.other.allowEmptyValue
                                }
                        }
                    }



                    // responses
                    if ( is_defined(  path.( method ).responses ) ) {
                        for( res  in path.( method ).responses ) {
                            openapi.paths.( path ).( method ).responses.( res.status ).description = res.description
                            if ( is_defined( res.schema ) ) {
                                for( p in path.( method ).produces ) {
                                    if ( res.schema instanceof SchemaType ) {
                                        openapi.paths.( path ).( method ).responses.( res.status ).content.( p ).schema << getType@JsonSchema( {
                                            type << res.schema 
                                            schemaVersion = request.version 
                                        })
                                    }
                                }
                            }
                        }
                    }

                    
                }
            }


            // definitions
            for( d in request.types ) {
                getTypeDefinition@JsonSchema( {
                    typeDefinition << d
                    schemaVersion = request.version
                })( typedef ) 
                foreach( name : typedef ) {
                    openapi.components.schemas.( name ) << typedef.( name )
                }

            }
        
            getJsonString@JsonUtils( openapi )( response )

    
        } ]

    }
}

service OpenApi {

    execution: concurrent 

    embed Console as Console
    embed StringUtils as StringUtils
    embed JsonUtils as JsonUtils
    embed JsonSchema as JsonSchema
    embed OpenApi2 as OpenApi2
    embed OpenApi3 as OpenApi3
    

    inputPort OpenApi {
        location: "local"
        interfaces: OpenApiInterface
    }


    main {
        [  getOpenApiDefinition( request )( response ) {
            if ( request.version == "2.0" ) { getOpenApiDefinition@OpenApi2( request )( openapi ) }
            else if ( request.version == "3.0" ) { getOpenApiDefinition@OpenApi3( request )( openapi ) }

            
            // necessary for converting null into {}
            replaceAll@StringUtils( openapi { regex = ":null", replacement = ":{}"})( response )
        }]
    }
}

