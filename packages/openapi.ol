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
from types.definition-types import Port
from types.definition-types import TypeLink
from types.definition-types import TypeInLine
from types.definition-types import TypeChoice

from console import Console
from string-utils import StringUtils
from json-utils import JsonUtils
from json-schema import JsonSchema
from runtime import Runtime

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

type GetOpenApiFromJolieMetaDataRequest {
    port: Port
    host: string
    scheme: string( enum(["http","https"]))
    level0?: bool
    version: string   // version of the document
    template {
        operations* {
            operation: string 
            path?: string
            method: string( enum(["get","post","put","delete","patch"]))
            faultsMapping* {
                jolieFault: string
                httpCode: int    
            }
        }
    }
    openApiVersion: string( enum(["2.0","3.0"]))    // version of the openapi
}

type CheckTypeConsistencyRequest: void {
    typeMap: undefined 
    typeName: string
}


type CheckBranchChoiceConsistency: void {
    branch: Type
    typeMap: undefined 
}

type GetActualCurrentTypeRequest: void {
    typeMap: undefined 
    typeName: string
}

type GetParamsListResponse: bool {
    query*: undefined
    path*: undefined
}


interface OpenApiGenerationInterface {
    RequestResponse:
        getOpenApiFromJolieMetaData( GetOpenApiFromJolieMetaDataRequest )( string ) throws DefinitionError( string ),
        checkTypeConsistency( CheckTypeConsistencyRequest )( bool ) throws DefinitionError
}

interface OpenApiGenerationInterfacePrivate {
    RequestResponse:
        getParamListFromPathTemplate( string )( GetParamsListResponse ),
        checkBranchChoiceConsistency( CheckBranchChoiceConsistency )( bool ) throws DefinitionError,
        getActualCurrentType( GetActualCurrentTypeRequest )( string )
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

constants {
    ERROR_TYPE_NAME = "ErrorType",
    LOG = true
}

service OpenApi {

    execution: concurrent 

    embed Console as Console
    embed StringUtils as StringUtils
    embed JsonUtils as JsonUtils
    embed Runtime as Runtime
    embed JsonSchema as JsonSchema
    embed OpenApi2 as OpenApi2
    embed OpenApi3 as OpenApi3

    outputPort MySelf {
        interfaces: OpenApiInterface, OpenApiGenerationInterface, OpenApiGenerationInterfacePrivate
    }
    

    inputPort OpenApi {
        location: "local"
        interfaces: OpenApiInterface, OpenApiGenerationInterface, OpenApiGenerationInterfacePrivate
    }

    init {
        getLocalLocation@Runtime()( MySelf.location )
    }

    main {
        [  getOpenApiDefinition( request )( response ) {
            if ( request.version == "2.0" ) { getOpenApiDefinition@OpenApi2( request )( openapi ) }
            else if ( request.version == "3.0" ) { getOpenApiDefinition@OpenApi3( request )( openapi ) }

            
            // necessary for converting null into {}
            replaceAll@StringUtils( openapi { regex = ":null", replacement = ":{}"})( response )
        }]

        [ getOpenApiFromJolieMetaData( request )( response ) {

            // prepare template hashmap 
            if ( is_defined( request.template ) ) {
                for( op in request.template.operations ) {
                    templateHashmap.( op.operation ) << op 
                }
            }
            

            level0 = false
            if ( is_defined( request.level0 ) ) { level0 = request.level0 }
            
            /* creating a map name-types and a map of faults for managing type links */
            for( itf in request.port.interfaces ) { 
                for( tps in itf.types ) { typeMap.( tps.name ) << tps }
                for( op in itf.operations ) {
                    for( f in op.faults ) {
                        faultsMap.( op.operation_name ).( f.name ) << f
                    }
                }
            } 

            /* creating hashmap for faults */

            
            /* creating openapi definition file */
            openapi << {
                info << {
                    title = request.port.name + " API"
                    description = ""
                    version = request.version
                }
                servers << {                  
                    host = request.host
                    basePath = "/"
                    schemes = request.scheme
                }
                version = request.openApiVersion
            }
            
            /* importing of tags and all the types */
            for( itf = 0, itf < #request.port.interfaces, itf++ ) {
                openapi.tags[ itf ] << {
                    name = request.port.interfaces[ itf ].name                   
                }
                if ( is_defined( request.port.interfaces[ itf ].documentation) ) {
                    openapi.description = request.port.interfaces[ itf ].documentation
                }
                for ( itftp in request.port.interfaces[ itf ].types ) {
                    openapi.types[ #openapi.types ] << itftp
                    // cration of types hashmap
                    typeMap.( itftp.name ) << itftp
                }

                // adding a general type InternalServerError for managing 500 codes
                errorType << {
                    name = ERROR_TYPE_NAME
                    type << {
                        root_type.void_type = true
                        sub_type[ 0 ] << {
                            name = "faultName"
                            cardinality << {
                                min = 1
                                max = 1
                            }
                            type.root_type.string_type = true
                        }
                        sub_type[ 1 ] << {
                            name = "message"
                            cardinality << {
                                min = 1
                                max = 1
                            }
                            type.root_type.string_type = true
                        }
                    }
                }
                openapi.types[ #openapi.types ] << errorType
                typeMap.( ERROR_TYPE_NAME ) << itftp
            }
            

            jolieFaultTypeCounter = 0

            /* selecting interfaces to be imported */
            path_counter = -1

            // for each interface in the port
            for( int_i = 0, int_i < #request.port.interfaces, int_i++ ) {
                c_interface -> request.port.interfaces[ int_i ]
                
                // for each operations in the interfaces
                for( o = 0, o < #c_interface.operations, o++ ) {
                    c_op -> c_interface.operations[ o ]
                    if ( LOG ) { println@Console("Analyzing operation:" + c_op.operation_name )() }
                    error_prefix =  "ERROR on port " + request.port.name + ", operation " + c_op.operation_name + ":" + "the operation has been declared to be imported as a REST ";
                    
                    undef( c_template )
                
                    // defining method
                    if ( is_defined( templateHashmap.( c_op.operation_name ) ) ) {
                        c_template  << templateHashmap.( c_op.operation_name )
                    } 

                    if ( LOG ) { println@Console("Operation Template:" + valueToPrettyString@StringUtils( c_template ) )() }

                    path_counter++;

                    // start definition of specific path method
                    // general data
                    if ( is_defined( c_template.path ) ) {
                        split@StringUtils( c_template.path {
                            regex = "\\?"
                        } )( splres )
                        openapi.paths[ path_counter ] = splres.result[ 0 ]
                    } else {
                        // if there is not template, the path is just the name of the operation
                        c_template.path = openapi.paths[ path_counter ] = "/" + c_op.operation_name
                    }

                    if ( is_defined( c_template.method ) ) {
                        method = c_template.method
                    } else {
                        // if there is not template, the default method is post
                        method = "post"
                    }


                    openapi.paths[ path_counter ].( method ) << {
                        tags = c_interface.name
                        description = ""
                        operationId = c_op.operation_name
                        consumes = "application/json"
                        produces = "application/json"
                    }

                    // standard responses

                    // 200
                    openapi.paths[ path_counter ].( method ).responses[ 0 ] << {
                        status = 200
                        description = "Success"
                    }
                    // schema_link is added only if the response has a type
                    if ( is_defined( typeMap.( c_op.output ) ) ) {
                        openapi.paths[ path_counter ].( method ).responses[ 0 ].schema.link_name << typeMap.( c_op.output )
                    }


                    // 500 added by default, it covers all the not mapped faults
                    openapi.paths[ path_counter ].( method ).responses[ 1 ] << {
                        status = 500
                        description = "resource not found"
                        schema.link_name = ERROR_TYPE_NAME
                    }

                    // mapped faults
                    for( fm in c_template.faultsMapping ) {
                        // http code 500 is added by default, and covers all the not mapped faults
                        if ( fm.httpCode == 500 ) {
                            throw( DefinitionError, "HttpCode 500 in fault mapping, not permitted")
                        }
                        responses_index = #openapi.paths[ path_counter ].( method ).responses
                        openapi.paths[ path_counter ].( method ).responses[ responses_index ]<< {
                            status = fm.httpCode
                            description = fm.jolieFault
                            schema << faultsMap.( c_op.operation_name ).( fm.jolieFault )
                        }
                    } 
                
                    undef( fnames )

                    // analyzing request


                    /* finding request type description */
                    api_request_type << typeMap.( c_op.input )

                    // checking request type consistency and get the actual one
                    if ( !(api_request_type instanceof void ) ) {
                        // a request type has been found
                        // check the consistency of the root type of the type: it cannot be void, etc
                        checkTypeConsistency@MySelf( {
                            typeName = api_request_type.name
                            typeMap -> typeMap
                        } )( response )

                        // in case the request type is a link, we must find the last linked type and pointing to that type
                        getActualCurrentType@MySelf( {
                            typeName = api_request_type.name
                            typeMap -> typeMap
                        } )( actual_type_name )
                        api_request_type << typeMap.( actual_type_name )
                    } 

                           
                    // the template exists, thus the parameters of the template must be separated from the body if they exist
                    getParamListFromPathTemplate@MySelf( c_template.path )( found_params )

                    if ( found_params ) {
                        /* there are parameters in the template */
                        if ( api_request_type instanceof void ) {
                            // void request is not allowed in case of params
                            throw( DefinitionError, api_request_type.name +  ": the request type does not declare any field" )
                        } else if ( api_request_type.type instanceof TypeChoice ) {
                            // type choice are not allowed in case of params
                            throw( DefinitionError, api_request_type.name +  ": the request type cannot be a choice type when the template specifies parameters in the URL" )
                        } 

                        /* if there are parameters in the template path the request type must be analyzed */
                        // ranging over the subtypes of the request for finding those field that corresponds to those in the template
                        undef( body_params )
                        for( sbt in api_request_type.type.sub_type ) {
                            if ( sbt.cardinality.min > 0 ) { required = true } else { required = false }
                    
                            if ( is_defined( found_params.query.( sbt.name ) ) || is_defined( found_params.path.( sbt.name ) ) ) {
                                /* path and query parameters msut be defined separately, whereas body parameters must be collected 
                                under a single parameter here named "body" */    
                                // a path or query parameter cannot be a structured type in jolie
                                if ( sbt.type instanceof TypeInLine ) {
                                    if ( #sbt.type.sub_type > 0 ) {
                                        throw( DefinitionError, "Type " + api_request_type.name +  ", field " + sbt.name + "  has been declared as a type with subnodes which is not permitted when it is used in a template" )
                                    }
                                } else if ( sbt.type instanceof TypeChoice ) {
                                    throw( DefinitionError, "Type " + api_request_type.name +  ", field " + sbt.name + "  has been declared as a type choice. Not permitted when a template is defined" )
                                } else if ( sbt.type instanceof TypeLink ) {
                                    sbt_actual_linked_type = getActualCurrentType@MySelf( sbt.type.link_name {
                                        typeMap -> typeMap
                                    } )
                                    if ( #typeMap.( sbt_actual_linked_type ).sub_type > 0 ) {
                                        throw( DefinitionError, "Type " + api_request_type.name +  ", field " + sbt.name + " cannot reference to another type because it is a path parameter" )
                                    }
                                }                

                                paramType = "path" 
                                if ( is_defined( found_params.query.( sbt.name ) )  ) { paramType = "query" }
                                                   
                                openapi.paths[ path_counter ].( method ).parameters[ #openapi.paths[ path_counter ].( method ).parameters ] << {
                                    /* if a parameter name corresponds with a node of the type, such a node must be declared as a simple native type node */
                                    name = sbt.name
                                    required = required
                                    in.other << paramType {
                                        type << sbt.type 
                                    }
                                }

                            } else {
                                // it is a body parameter
                                body_params[ #body_params ] << sbt
                            }

                            if ( method == "get" && #body_params > 0 ) throw( DefinitionError, "Type " + api_request_type.name +  ", field " + sbt.name + ": with method get, body params are not allowed")

                            // checking if there are parameters in the body and preparing their definition
                            if ( #body_params > 0 ) {
                                // creating a type on the fly which contains all the body parameters
                                undef( body_type ) 
                                body_type.root_type.void_type = true 
                                for ( bp in body_params ) {
                                    body_type.sub_type[ #body_type.sub_type ] << bp
                                }

                                
                                openapi.paths[ path_counter ].( method ).parameters[ #openapi.paths[ path_counter ].( method ).parameters ] << {
                                    name = "body"
                                    required = required
                                    in.in_body.schema_type << body_type
                                }
                                
                            }
                        }          

                    } else {
                        // there are not parameters in the template
                        /* the root type of the request type must be always void */
                        if ( method == "get" ) {
                            // in case there is not template path and method is get, api_request_type must be void
                            if ( !( api_request_type instanceof void ) ) {
                                throw( DefinitionError, "Operation  " + c_op.operation_name + " has been declared as a GET method without any path template, but the request type is not void" )
                            }
                        } else {
                            // methods POST, PUT, DELETE, PATCH
                            if ( !(api_request_type instanceof void ) ) {
                                /* the fields of the request type must be transformed into parameters */
                                openapi.paths[ path_counter ].( method ).parameters[ #openapi.paths[ path_counter ].( method ).parameters ] << {
                                    name = "body"
                                    required = true
                                    in.in_body.schema_type << api_request_type.type
                                }
                            }
                        }
                    }
                            
                }                 
            }
            
            println@Console( valueToPrettyString@StringUtils( openapi ) )()
            getOpenApiDefinition@MySelf( openapi )( response )
        }]

        [ checkTypeConsistency( request )( response ) {

            current_type -> request.typeMap.( request.typeName )
            scope( analysis ) {
                install( DefinitionError => {
                    throw( DefinitionError, "Type " + current_type.name + ": root native type must be void" )
                })

                // link
                if ( current_type.type instanceof TypeLink ) {
                    checkTypeConsistency@MySelf( {  
                        typeName = current_type.type.link_name
                        typeMap -> request.typeMap
                    } )( response )
                } 
                
                // choice
                else if ( current_type.type instanceof TypeChoice ) 
                {
                    checkBranchChoiceConsistency@MySelf( {
                        branch -> current_type.type.choice.left_type
                        typeMap -> request.typeMap
                    } )( response )
                    checkBranchChoiceConsistency@MySelf( {
                        branch -> current_type.type.choice.right_type
                        typeMap -> request.typeMap
                    } )( response )
                }
                // usual typeinline
                else if ( !is_defined( current_type.type.root_type.void_type ) ) {
                    throw( DefinitionError, "" )
                }
            }
            response = true
        }]

         /* private operations */
        [ checkBranchChoiceConsistency( request )( response ) {
            response = true
            if ( request.branch instanceof TypeLink ) {
                checkTypeConsistency@MySelf( {
                    typeName = check_rq.link_name
                    typeMap -> request.typeMap
                } )( response )
            } else if ( request.branch instanceof TypeChoice ) {
                checkBranchChoiceConsistency@MySelf(  {
                    branch -> request.branch.choice.left_type
                    typeMap -> request.typeMap
                } )( response )
                checkBranchChoiceConsistency@MySelf( {
                    branch -> request.branch.choice.right_type
                    typeMap -> request.typeMap
                } )( response )
            } else if ( request.branch instanceof TypeInLine ) {
                if ( !is_defined( request.branch.root_type.void_type ) ) {
                    throw( DefinitionError, "" )
                } 
            }
        }]

        [ getActualCurrentType( request )( response ) {
            current_type -> request.typeMap.( request.typeName )
            if ( current_type.type instanceof TypeLink ) {
                getActualCurrentType@MySelf( {
                    typeName = current_type.type.link_name
                    typeMap -> request.typeMap
                } )( response )
            } else {
                response = current_type.name
            } 
        }]

        [ getParamListFromPathTemplate( request )( response ) {
            response = false
            // separate path from query params
            split@StringUtils( request {
                regex = "\\?"
            } )( splres )
            pathpart = splres.result[ 0 ]
            querypart = splres.result[ 1 ]

            // path part
            split@StringUtils( pathpart {
                regex =  "/"
            } )( splres )
            for( pr in splres.result ) {
                // extract tokens within {}, it means there is a path param
                params = find@StringUtils( pr { regex = "\\{(.*)\\}" } )
                if ( params == 1 ) {
                    response = true
                    response.path.( params.group[ 1 ] ) = params.group[ 1 ]
                }
            }

            // query part
            if ( !( querypart instanceof void ) )  {
                split@StringUtils( querypart { regex =  "&|=" } )( splres )
                for( pr in splres.result ) {
                    // extract tokens within {}, it means there is a query param
                    find@StringUtils( pr { regex = "\\{(.*)\\}" } )( params )
                    if ( params == 1 ) {
                        response = true
                        response.query.( params.group[ 1 ] ) = params.group[ 1 ]
                    }
                }
            }
        }]
    }
}

