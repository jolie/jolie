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
  template?: undefined
}

interface OpenApiGenerationInterface {
    RequestResponse:
        getOpenApiFromJolieMetaData( GetOpenApiFromJolieMetaDataRequest )( string ) throws DefinitionError( string )
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
    embed Runtime as Runtime
    embed JsonSchema as JsonSchema
    embed OpenApi2 as OpenApi2
    embed OpenApi3 as OpenApi3

    outputPort MySelf {
        interfaces: OpenApiInterface
    }
    

    inputPort OpenApi {
        location: "local"
        interfaces: OpenApiInterface, OpenApiGenerationInterface
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

            level0 = false
            if ( is_defined( request.level0 ) ) { level0 = request.level0 }
            
            /* creating a map name-types for managing type links */
            for( itf in request.port.interfaces ) { 
                for( tps in itf.types ) { type_map.( tps.name ) << tps }
            } 

            /* creating openapi definition file */
            openapi << {
                info << {
                    title = request.port.name + " API"
                    description = ""
                    version = ""
                }
                servers << {                  
                    host = request.host
                    basePath = "/"
                    schemes = request.scheme
                }
            }

            
            /* importing of tags and all the types */
            for( itf = 0, utf < #request.port.interfaces, itf++ ) {
                openapi.tags[ itf ] << {
                    name = request.port.interfaces[ itf ].name                   
                }
                if ( is_defined( request.port.interfaces[ itf ].documentation) ) {
                    openapi.description = request.port.interfaces[ itf ].documentation
                }
                for ( itftp in request.port.interfaces[ itf ].types ) {
                    openapi.types[ #openapi.types ] << itftp
                }
            }
            

            jolieFaultTypeCounter = 0

            /* selecting interfaces to be imported */
            path_counter = -1

            // for each interface in the port
            for( int_i = 0, int_i < #request.port.interfaces, int_i++ ) {
                c_interface -> request.port.interfaces[ int_i ]
                c_interface_name = c_interface.name
                
                // for each operations in the interfaces
                for( o = 0, o < #c_interface.operations, o++ ) {
                        oper -> c_interface.operations[ o ]
                        if ( LOG ) { println@Console("Analyzing operation:" + oper.operation_name )() }
                        error_prefix =  "ERROR on port " + request.port.name + ", operation " + oper.operation_name + ":" + "the operation has been declared to be imported as a REST ";
                        
                        undef( __template )
                        undef( __method )
                        // defining method
                        __method = request.template.( oper.operation_name ).method
                        if ( is_defined( request.template.( oper.operation_name ).template ) ) {
                            __template  = request.template.( oper.operation_name ).template
                        } 
                        if ( !level0 && !( __template instanceof void  ) ) {
                            if ( __method == "" || __method instanceof void ) {
                                throw( DefinitionError, "Template " + __given_template.template + " of operation " + oper.operation_name + " does not define the method, not permitted" )
                            }
                        } 
                        
                        if ( __method instanceof void ) {
                            __method = "post"
                        }

                        if ( LOG ) { println@Console("Operation Template:" + __template )() }

                        path_counter++;

                        // start definition of specific path method
                        with( openapi.paths[ path_counter ].( __method ) ) {
                            // general data
                            .tags = c_interface_name;
                            .description = "";
                            .operationId = oper.operation_name;
                            .consumes[0] = "application/json";
                            .produces = "application/json";

                            // standard responses

                            // 200
                            with( .responses[ 0 ] ) {
                                    .status = 200;
                                    .description = "OK";
                                    tp_resp_count = 0; tp_resp_found = false;

                                    // looking for response type in the list of the interface types
                                    while( !tp_resp_found && tp_resp_count < #c_interface.types ) {
                                        if ( c_interface.types[ tp_resp_count ].name == oper.output ) {
                                            tp_resp_found = true
                                        } else {
                                            tp_resp_count++
                                        }
                                    };
                                    if ( tp_resp_found ) {
                                        // if the response type has been found, the schema link to the definition is reported
                                        .schema.link_name << c_interface.types[ tp_resp_count ].name
                                    }
                            }

                            // 404
                            with( .responses[ 1 ] ) {
                                .status = 404;
                                .description = "resource not found"
                            }
                            undef( fnames )

                            // if jolie faults exist, they will be collected under 500
                            if ( #oper.fault > 0 ) {
                                for ( f = 0, f < #oper.fault, f++ ) {
                                        
                                        gdff.fault -> oper.fault[ f ];
                                        gdff.name = fnames[ #fnames ] = JOLIE_FAULT_PREFIX + jolieFaultTypeCounter
                                        getFaultDefinitionForOpenAPI@Utils( gdff )( fault_definition )
                                        openapi.definitions[ #openapi.definitions ] << fault_definition
                                        jolieFaultTypeCounter++     
                                }
                                with( .responses[ 2 ] ) {
                                    .status = 500;
                                    gsff.name -> fnames
                                    getSchemaForFaults@Utils( gsff )( .schema )
                                    .description = "JolieFault"
                                }
                            }
                        };

                        // analyzing request
                        openapi_params_count = -1
                        current_openapi_path -> openapi.paths[ path_counter ].( __method )

                        /* finding request type description */
                        tp_count = 0; tp_found = false;
                        while( !tp_found && tp_count < #c_interface.types ) {
                            if ( c_interface.types[ tp_count ].name == oper.input ) {
                                tp_found = true
                            } else {
                                tp_count++
                            }
                        }

                        if ( tp_found ) {
                            // a request type has been found
                            // check the consistency of the root type of the type: it cannot be void, etc
                            check_rq = c_interface.types[ tp_count ].name
                            check_rq.type_map -> type_map
                            checkTypeConsistency@JesterUtils( check_rq )()

                            // in case the request type is a link, we must find the last linked type and pointing to that type
                            real_current_type -> c_interface.types[ tp_count ]
                            get_actual_ctype_rq = c_interface.types[ tp_count ].name
                            get_actual_ctype_rq.type_map -> type_map 
                            getActualCurrentType@JesterUtils( get_actual_ctype_rq )( actual_type_name );
                            current_type -> type_map.( actual_type_name )
                        }

                        // request analysis depends on the presence of a template
                        if ( !( __template instanceof void ) ) {
                            
                            // the template exists, thus the parameters of the template must be separated from the body if they exist
                            getParamList@JesterUtils( __template )( found_params )

                            if ( found_params ) {
                                    /* there are parameters in the template */
                                    error_prefix = error_prefix + "with template " + __template + " ";
                                    if ( !tp_found ) {
                                            error_msg = current_type.name +  ": the request type does not declare any field";
                                            throw( DefinitionError, error_msg )
                                    } else if ( current_type.type instanceof TypeChoice ) {
                                            error_msg = current_type.name +  ": the request type cannot be a choice type when the template specifies parameters in the URL"
                                            throw( DefinitionError, error_msg )
                                    } else {
                                            /* if there are parameters in the template the request type must be analyzed */
                                            // ranging over the subtypes of the request for finding those field that corresponds to those in the template
                                            undef( body_params )
                                            for( sbt = 0, sbt < #current_type.type.sub_type, sbt++ ) {
                                                
                                                current_sbt -> current_type.type.sub_type[ sbt ];
                                                current_root_type -> current_sbt.type.root_type;  
                                                __str_to_search = current_sbt.name 
                                                __found = ""
                                                check_param_found
                                                // variable __found can be "body", "path" or "query"   

                                                /* path and query parameters msut be defined separately, whereas body parameters must be collected 
                                                under a single parameter here named "body" */                           
                                                
                                                // preparing the definition of the current parameter if it is in path or query
                                                if ( __found != "body" ) {
                                                    openapi_params_count++;
                                                    with( current_openapi_path.parameters[ openapi_params_count ] ) {
                                                        /* if a parameter name corresponds with a node of the type, such a node must be declared as a simple native type node */
                                                        .name = current_sbt.name;
                                                        if ( current_sbt.cardinality.min > 0 ) {
                                                            .required = true
                                                        };
                                                        
                                                        // a path or query parameter cannot be a structured type in jolie
                                                        if ( current_sbt.type instanceof TypeInLine ) {
                                                            if ( #current_sbt.type.sub_type > 0 ) {
                                                                error_msg = "Type " + current_type.name +  ", field " + current_sbt.name + "  has been declared as a type with subnodes which is not permitted when it is used in a template"
                                                                throw( DefinitionError, error_msg )
                                                            }
                                                        } else if ( current_sbt.type instanceof TypeChoice ) {
                                                            error_msg = "Type " + current_type.name +  ", field " + current_sbt.name + "  has been declared as a type choice. Not permitted when a template is defined"
                                                            throw( DefinitionError, error_msg )
                                                        } else if ( current_sbt.type instanceof TypeLink ) {
                                                                get_actual_ctype_rq = current_sbt.type.link_name
                                                                get_actual_ctype_rq.type_map -> type_map 
                                                                getActualCurrentType@JesterUtils( get_actual_ctype_rq )( sbt_actual_linked_type )
                                                                if ( type_map.( sbt_actual_linked_type ).sub_type > 0 ) {
                                                                    error_msg = "Type " + current_type.name +  ", field " + current_sbt.name + " cannot reference to another type because it is a path parameter"
                                                                    throw( DefinitionError, error_msg )
                                                                }
                                                        }

                                                        .in.other = __found;
                                                        .in.other.type << current_sbt.type 
                                                    }
                                                } else {
                                                    // it is a body parameter
                                                    body_params[ #body_params ] << current_sbt
                                                }
                                            }

                                            // checking if there are parameters in the body and preparing their definition
                                            if ( #body_params > 0 && __method != "get" ) {
                                                // creating a type on the fly which contains all the body parameters

                                                undef( body_type ) 
                                                body_type.root_type.void_type = true 
                                                for ( bp in body_params ) {
                                                    body_type.sub_type[ #body_type.sub_type ] << bp
                                                }

                                                
                                                openapi_params_count++
                                                current_openapi_path.parameters[ openapi_params_count ] << {
                                                    name = "body"
                                                    required = true
                                                    in.in_body.schema_type << body_type
                                                }
                                                
                                            }
                                    }

                            } else {
                                    // there are not parameters in the template
                                    /* the root type of the request type must be always void */
                                    if ( !tp_found ) {
                                        if ( oper.input != "void" ) {
                                            println@Console( "operation " + oper.operation_name + " cannot have the request message declared as void" )();
                                            throw( DefinitionError )
                                        }
                                    } 

                                    if ( __method == "get" ) {
                                            if ( tp_found ) {
                                                    if ( #current_type.type.sub_type > 0 || current_type instanceof TypeChoice ) {
                                                    println@Console( current_type.name + ": this request tyoe message is joined to a get method without any template, thus it cannot be declared as a choice type nor it cannot contain subnodes" )();
                                                    throw( DefinitionError )
                                                }
                                            }
                                    } else {
                                        // methods POST, PUT, DELETE
                                            if ( tp_found ) {
                                                /* the fields of the request type must be transformed into parameters */
                                                current_openapi_path.parameters[ openapi_params_count ] << {
                                                    name = "body"
                                                    required = true
                                                    in.in_body.schema_type << current_type.type
                                                }
                                            }
                                    }
                            }
                        } else {
                            /* the template is not defined */
                            __template = "/" + oper.operation_name;

                            /* if it is a GET, extract the query params from the request message */
                            if ( __method == "get" ) {
                                    for( sbt = 0, sbt < #current_type.type.sub_type, sbt++ ) {
                                            /* casting */
                                            current_sbt -> current_type.type.sub_type[ sbt ];
                                            current_root_type -> current_sbt.type.root_type;

                                            openapi_params_count++;
                                            with( current_openapi_path.parameters[ openapi_params_count ] ) {
                                                .name = current_sbt.name;
                                                if ( current_sbt.cardinality.min > 0 ) {
                                                    .required = true
                                                };

                                                if ( current_sbt.type instanceof TypeInLine  ) {
                                                        if ( #current_sbt.type.sub_type > 0 ) {
                                                                error_msg = "Type " + current_type.name  + ": field " + current_sbt.name + " has a type with subnodes which is not permitted"
                                                                throw( DefinitionError, error_msg )
                                                        };
                                                        .in.other = "query";
                                                        .in.other.type << current_sbt.type

                                                        if ( current_sbt.type instanceof TypeLink ) {
                                                                error_msg = "Type " + current_type.name  + ": field " + current_sbt.name + " cannot reference to another type because it is a path parameter"
                                                                throw( DefinitionError, error_msg )
                                                        }
                                                } else {
                                                        error_msg = "Type " + current_type.name  + ": field " + current_sbt.name + " of request type is not an inline type. REST template for GET method cannot be created!"
                                                        throw( DefinitionError, error_msg )
                                                }
                                            }
                                            ;
                                            __template = __template + "/{" + current_sbt.name + "}"
                                    }
                            } else {
                                // methods POST, PUT, DELETE
                                    if ( tp_found ) {
                                        with( current_openapi_path.parameters ) {
                                            .in.in_body.schema_ref = real_current_type.name;
                                            .name = "body";
                                            .required = true
                                        }

                                    }
                            };
                            if ( LOG ) { println@Console( "Template automatically generated:" + __template )() }
                        }
                        ;
                        splr = __template
                        splr.regex = "\\?"
                        split@StringUtils( splr )( splres );
                        openapi.paths[ path_counter ] = splres.result[ 0 ]
                    }
                
            }
            if ( ! is_defined( openapi.paths ) ) {
                println@Console( "WARNING: No operation to be exported (= OpenAPI path) has been found. Has the input port been specified correctly?" )()
            }
            
            getOpenApiDefinition@MySelf( openapi )( response )
        }]
    }
}

