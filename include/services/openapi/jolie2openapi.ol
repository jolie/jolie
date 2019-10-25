/*
The MIT License (MIT)
Copyright (c) 2016 Claudio Guidi <guidiclaudio@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
                        .cardinality.max = 1;
                        .type -> request.fault.type                   
                    }
                }
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
                        
                        // proceed only if a rest template has been defined for that operation
                        if ( is_defined( request.template.( oper.operation_name ) ) ) {
                            __given_template = request.template.( oper.operation_name )
                        } else {
                            __given_template = Void
                        }
                       
                        if ( !easyInterface && !(__given_template instanceof void) ) {
                            analyzeTemplate@JesterUtils(__given_template )( analyzed_template )
                            __template = analyzed_template.template
                            __method = analyzed_template.method
                            if ( __method == "" ) {
                                 throw( DefinitionError, "Template " + __given_template + " of operation " + oper.operation_name + " does not define method, not permitted" )
                            }
                        } else {
                            __method = "post"
                        }

                        if ( LOG ) { println@Console("Operation Template:" + __template )() }

                        path_counter++;

                        with( openapi.paths[ path_counter ].( __method ) ) {
                            .tags = c_interface_name;
                            .description = "";
                            .operationId = oper.operation_name;
                            .consumes[0] = "application/json";
                            .produces = "application/json";
                            with( .responses[ 0 ] ) {
                                    .status = 200;
                                    .description = "OK";
                                    tp_resp_count = 0; tp_resp_found = false;
                                    while( !tp_resp_found && tp_resp_count < #c_interface.types ) {
                                        if ( c_interface.types[ tp_resp_count ].name == oper.output ) {
                                            tp_resp_found = true
                                        } else {
                                            tp_resp_count++
                                        }
                                    };
                                    if ( tp_resp_found ) {
                                        .schema.link_name << c_interface.types[ tp_resp_count ].name
                                    }
                            }
                            with( .responses[ 1 ] ) {
                                .status = 404;
                                .description = "resource not found"
                            }
                            undef( fnames )
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
                        openapi_params_count = -1
                        current_openapi_path -> openapi.paths[ path_counter ].( __method )

                        /* find request type description */
                        tp_count = 0; tp_found = false;
                        while( !tp_found && tp_count < #c_interface.types ) {
                            if ( c_interface.types[ tp_count ].name == oper.input ) {
                                tp_found = true
                            } else {
                                tp_count++
                            }
                        }

                        if ( tp_found ) {
                            // check the consistency of the root type of the type
                            check_rq = c_interface.types[ tp_count ].name
                            check_rq.type_map -> global.type_map
                            checkTypeConsistency@JesterUtils( check_rq )()
                        }
                        real_current_type -> c_interface.types[ tp_count ]
                        get_actual_ctype_rq = c_interface.types[ tp_count ].name
                        get_actual_ctype_rq.type_map -> global.type_map 
                        getActualCurrentType@JesterUtils( get_actual_ctype_rq )( actual_type_name );
                        current_type -> global.type_map.( actual_type_name )

                        if ( !( __template instanceof void ) ) {
                        
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
                                            for( sbt = 0, sbt < #current_type.type.sub_type, sbt++ ) {
                                                current_sbt -> current_type.type.sub_type[ sbt ];
                                                current_root_type -> current_sbt.type.root_type;  
                                                __str_to_search = current_sbt.name 
                                                __found = ""
                                                check_param_found                               
                                                
                                                openapi_params_count++;
                                                with( current_openapi_path.parameters[ openapi_params_count ] ) {
                                                    /* if a parameter name corresponds with a node of the the type, such a node must be declared as a simple native type node */
                                                    .name = current_sbt.name;
                                                    if ( current_sbt.cardinality.min > 0 ) {
                                                        .required = true
                                                    };
                                                    
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

                                                    if ( __found == "body" ) {
                                                        .in.in_body.schema_subType << current_sbt
                                                    } else {
                                                        .in.other = __found;
                                                        .in.other.type << current_sbt.type 
                                                    }
                                                }
                                            }
                                    }
                            } else {
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
                                            if ( tp_found ) {
                                                /* the fields of the request type must be transformed into parameters */
                                                for( sf = 0, sf < #current_type.type.sub_type, sf++ ) {
                                                        openapi_params_count++;
                                                        with( current_openapi_path.parameters[ openapi_params_count ] ) {
                                                            .name = real_current_type.type.sub_type[ sf ].name;
                                                            if ( current_type.type.sub_type[ sf ].cardinality.min > 0 ) {
                                                                .required = true
                                                            };
                                                            .in.in_body.schema_subType << current_type.type.sub_type[ sf ]
                                                        }
                                                }
                                            }
                                    }
                            }
                        } else {
                            /* the template is not defined */
                            __template = "/" + oper.operation_name;

                            /* if it is a GET, extract the path params from the request message */
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
                                                .in.other = "path";
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
                                    if ( tp_found ) {
                                        with( current_openapi_path.parameters ) {
                                            .in.in_body.schema_ref = real_current_type.name;
                                            .name = real_current_type.name;
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
