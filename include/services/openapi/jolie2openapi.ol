/*
 *   Copyright (C) 2016 by Claudio Guidi <guidiclaudio@gmail.com>         
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

include "file.iol"
include "console.iol"
include "string_utils.iol"
include "metajolie.iol"

include "./public/interfaces/OpenApiDefinitionInterface.iol"
include "./public/interfaces/Jolie2OpenApiInterface.iol"
include "services/jester/JesterUtilsInterface.iol"

execution{ concurrent }


outputPort OpenApi {
  Interfaces: OpenApiDefinitionInterface
}

outputPort JesterUtils {
    Interfaces: JesterUtilsInterface
}

constants {
  LOG = false
}

type GetSchemaForFaultsRequest: void {
    .name*: string
}
type GetFaultDefinitionForOpenAPIRequest: void {
    .fault: Fault
    .name: string 
}

interface UtilsInteface {
    RequestResponse:
        getSchemaForFaults( GetSchemaForFaultsRequest )( undefined ),
        getFaultDefinitionForOpenAPI( GetFaultDefinitionForOpenAPIRequest )( FaultDefinitionForOpenAPI )
}

service Utils {
    Interfaces: UtilsInteface 

    main {

        [ getSchemaForFaults( request )( response ) {
            if ( #request.name == 1 ) {
                response.link_name = request.name
            } else {
                response.choice.left_type.link_name = request.name[ #request.name -1 ]
                undef( request.name[ #request.name - 1] )
                getSchemaForFaults@Utils( request )( response.choice.right_type )   
            }
        }]

        [ getFaultDefinitionForOpenAPI( request )( response ) {

            response.name = request.fault.name
            with( response.fault )  {
                .name = request.name;
                with( .type ) {
                    .root_type.void_type = true;
                    with( .sub_type[0] ) {
                        .name = "fault";
                        .cardinality.min = 1;
                        .cardinality.max = 1;
                        .type.root_type.string_type = true    
                    }
                    with( .sub_type[1] ) {
                        .name = "content";
                        .cardinality.min = 1;
                        .cardinality.max = 1              
                    }
                }
            }
            
            if ( request.fault.type instanceof NativeType ) {
                response.fault.type.sub_type[1].type.root_type << request.fault.type
            } else {
                response.fault.type.sub_type[1].type << request.fault.type
            }

        }]
    }
}

embedded {
  Jolie:
    "services/openapi/openapi_definition.ol" in OpenApi,
    "services/jester/jester_utils.ol" in JesterUtils
}

constants {
    JOLIE_FAULT_PREFIX = "JolieFaultType"
}


inputPort Jolie2OpenApi {
  Location: "local"
  Protocol: sodep
  Interfaces: Jolie2OpenApiInterface
}

define check_param_found {
    // __str_to_search
    // __found
    __found = "body"
    for( _pf = 0, _pf < #found_params.path, _pf++ ) {
        if ( found_params.path[ _pf ] == __str_to_search ) {
            __found = "path"
        }
    }
    for( _pf = 0, _pf < #found_params.query, _pf++ ) {
        if ( found_params.query[ _pf ] == __str_to_search ) {
            __found = "query"
        }
    }
}


define __body {
      easyInterface = false;
      if ( is_defined( request.easyInterface ) ) {
          easyInterface = request.easyInterface
      };
      router_host = request.host;
      service_filename = request.filename;
      service_input_port = request.inputPort;

      with( request_meta ) {
        .filename = service_filename
      };
      getInputPortMetaData@MetaJolie( request_meta )( metadata )
      // finding input port index
      input_port_index = 0
      for( ip = 0, ip < #metadata.input, ip++ ) {
          if ( metadata.input[ ip ].name == service_input_port ) {
              input_port_index = ip
          }
      }

      /* creating a map name-types for managing type links */
      for( itf = 0, itf < #metadata.input[ input_port_index ].interfaces, itf++ ) { 
          for( tps = 0, tps < #metadata.input[ input_port_index ].interfaces[ itf ].types, tps++ ) {
              global.type_map.( metadata.input[ input_port_index ].interfaces[ itf ].types[ tps ].name ) << metadata.input[ input_port_index ].interfaces[ itf ].types[ tps ]
          }
      } 

      if ( LOG ) {
          valueToPrettyString@StringUtils( global.type_map )( s )
          println@Console("Type map: " + s)()
      }
      /* creating openapi definition file */
      undef( openapi );
      with( openapi ) {
        with( .info ) {
            .title = service_input_port + " API";
            .description = "";
            .version = ""
        };
        .host = router_host;
        
        .basePath = "/"
       
        /* importing of all the types */
        for( itf = 0, itf < #metadata.input[ input_port_index ].interfaces, itf++ ) {
            with( .tags[ itf ] ) {
                .name = .description = metadata.input[ input_port_index ].interfaces[ itf ].name
            };
            for ( itftp = 0, itftp < #metadata.input[ input_port_index ].interfaces[ itf ].types, itftp++ ) {
                .definitions[ itftp ] << metadata.input[ input_port_index ].interfaces[ itf ].types[ itftp ]
            }
        }
      };


      jolieFaultTypeCounter = 0

      /* selecting the port and the list of the interfaces to be imported */
      for( i = 0, i < #metadata.input, i++ ) {
          // port selection from metadata
          if ( metadata.input[ i ].name == service_input_port ) {
              path_counter = -1

              // for each interface in the port
              for( int_i = 0, int_i < #metadata.input[ i ].interfaces, int_i++ ) {
                  c_interface -> metadata.input[ i ].interfaces[ int_i ]
                  c_interface_name = c_interface.name
                  
                  // for each operations in the interfaces
                  for( o = 0, o < #c_interface.operations, o++ ) {
                        oper -> c_interface.operations[ o ]
                        if ( LOG ) { println@Console("Analyzing operation:" + oper.operation_name )() }
                        error_prefix =  "ERROR on port " + service_input_port + ", operation " + oper.operation_name + ":" + "the operation has been declared to be imported as a REST ";
                        
                        undef( __template )
                        undef( __method )
                        // defining method
                        __method = request.template.( oper.operation_name ).method
                        if ( is_defined( request.template.( oper.operation_name ).template ) ) {
                            __template  = request.template.( oper.operation_name ).template
                        } 
                        if ( !easyInterface && !( __template instanceof void  ) ) {
                            if ( __method == "" || __method instanceof void ) {
                                 throw( DefinitionError, "Template " + __given_template.template + " of operation " + oper.operation_name + " does not define method, not permitted" )
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

                            // if jolie faults exist, thwy will be collected under 500
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
                            check_rq.type_map -> global.type_map
                            checkTypeConsistency@JesterUtils( check_rq )()

                            // in case the request type is a link, we must find the last linked type and pointing to that type
                            real_current_type -> c_interface.types[ tp_count ]
                            get_actual_ctype_rq = c_interface.types[ tp_count ].name
                            get_actual_ctype_rq.type_map -> global.type_map 
                            getActualCurrentType@JesterUtils( get_actual_ctype_rq )( actual_type_name );
                            current_type -> global.type_map.( actual_type_name )
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
                                                                get_actual_ctype_rq.type_map -> global.type_map 
                                                                getActualCurrentType@JesterUtils( get_actual_ctype_rq )( sbt_actual_linked_type )
                                                                if ( global.type_map.( sbt_actual_linked_type ).sub_type > 0 ) {
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
          }
      }
}

main {

    [ getOpenApi( request )( response ) {
        __body
        getOpenApiDefinition@OpenApi( openapi )( response )
    }]
}
