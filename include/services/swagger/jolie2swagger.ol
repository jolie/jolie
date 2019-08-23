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
include "metaparser.iol"

include "./public/interfaces/SwaggerDefinitionInterface.iol"
include "./public/interfaces/Jolie2SwaggerInterface.iol"

execution{ concurrent }

outputPort Swagger {
  Interfaces: SwaggerDefinitionInterface
}

constants {
  LOG = true
}


embedded {
  Jolie:
    "services/swagger/swagger_definition.ol" in Swagger
}

inputPort Jolie2Swagger {
  Location: "local"
  Protocol: sodep
  Interfaces: Jolie2SwaggerInterface
}

define __analize_given_template {
    /* __given_template */
    undef( __method )
    undef( __template )
    if ( !easyInterface && !(__given_template instanceof void) ) {
        r3 = __given_template
        r3.regex = ","
        split@StringUtils( r3 )( r4 )
        for( _p = 0, _p < #r4.result, _p++ ) {
            trim@StringUtils( r4.result[_p] )( r_result )
            r_result.regex = "method="
            find@StringUtils( r_result )( there_is_method )
            if ( there_is_method == 1) {
                split@StringUtils( r_result )( _params )
                trim@StringUtils( _params.result[1] )( __method )
            } else {
                r_result.regex = "template="
                find@StringUtils( r_result )( there_is_template )
                if ( there_is_template == 1) {
                    split@StringUtils( r_result )( _params )
                    trim@StringUtils( _params.result[1] )( __template )
                }
            }
        }
    } else {
        __method = "post"
    }

}

define __add_cast_data {
  if ( is_defined( current_root_type.int_type ) ) {
      current_render_operation.cast.( current_sbt.name ) = "int"
  } else if ( is_defined( current_root_type.long_type ) ){
      current_render_operation.cast.( current_sbt.name ) = "long"
  } else if ( is_defined( current_root_type.double_type ) ){
      current_render_operation.cast.( current_sbt.name ) = "double"
  } else if ( is_defined( current_root_type.bool_type ) ){
      current_render_operation.cast.( current_sbt.name ) = "bool"
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
        .filename = service_filename;
        .name.name  = "";
        .name.domain = ""
      };
      getInputPortMetaData@MetaJolie( request_meta )( metadata )
      ;
      /* creating swagger definition file */
      undef( swagger );
      with( swagger ) {
        with( .info ) {
            .title = service_input_port + " API";
            .description = "";
            .version = ""
        };
        .host = router_host;
        if ( easyInterface ) {
          .basePath = "/"
        } else {
          .basePath = "/" + service_input_port
        };

        /* importing of all the types */
        for( itf = 0, itf < #metadata.input.interfaces, itf++ ) {
            with( .tags[ itf ] ) {
                .name = .description = metadata.input.interfaces[ itf ].name.name
            };
            .definitions[ itf ] << metadata.input.interfaces[ itf ]
        }
      };



      /* selecting the port and the list of the interfaces to be imported */
      for( i = 0, i < #metadata.input, i++ ) {
          // port selection from metadata
          if ( metadata.input[ i ].name.name == service_input_port ) {
              output_port_index = #render.output_port
              getSurface@Parser( metadata.input )( render.output_port[ output_port_index ].surface );
              with( render.output_port[ output_port_index ] ) {
                    .name = service_input_port;
                    .location = metadata.input[ i ].location;
                    .protocol = metadata.input[ i ].protocol
              };
              path_counter = -1

              // for each interface in the port
              for( int_i = 0, int_i < #metadata.input[ i ].interfaces, int_i++ ) {
                  c_interface -> metadata.input[ i ].interfaces[ int_i ]
                  c_interface_name = c_interface.name.name
                  render.output_port[ output_port_index ].interfaces[ int_i ] = c_interface_name;
                  
                  // for each operations in the interfaces
                  for( o = 0, o < #c_interface.operations, o++ ) {
                        oper -> c_interface.operations[ o ]
                        if ( LOG ) { println@Console("Analyzing operation:" + oper.operation_name )() }
                        error_prefix =  "ERROR on port " + service_input_port + ", operation " + oper.operation_name + ":" + "the operation has been declared to be imported as a REST ";
                        
                        // proceed only if a rest template has been defined for that operation
                        if ( is_defined( request.template.( oper.operation_name ) ) ) {
                            __given_template = request.template.( oper.operation_name )
                        } else {
                            __given_template = ""
                        }
                       
                        __analize_given_template
                        if ( LOG ) { println@Console("Operation Template:" + __template )() }

                        path_counter++;
                        if ( is_defined( oper.output ) ) {
                            rr_operation_max = #render.output_port[ output_port_index ].interfaces[ int_i ].rr_operation
                            current_render_operation -> render.output_port[ output_port_index ].interfaces[ int_i ].rr_operation[ rr_operation_max ]
                        } else {
                            ow_operation_max = #render.output_port[ output_port_index ].interfaces[ int_i ].ow_operation
                            current_render_operation -> render.output_port[ output_port_index ].interfaces[ int_i ].ow_operation[ ow_operation_max ]
                        }
                        current_render_operation = oper.operation_name
                        current_render_operation.method = __method

                        with( swagger.paths[ path_counter ].( __method ) ) {
                            .tags = c_interface_name;
                            .description = __given_template;
                            .operationId = oper.operation_name;
                            .consumes[0] = "application/json";
                            .produces = "application/json";
                            with( .responses.( "200" ) ) {
                                    .description = "OK";
                                    tp_resp_count = 0; tp_resp_found = false;
                                    while( !tp_resp_found && tp_resp_count < #c_interface.types ) {
                                        if ( c_interface.types[ tp_resp_count ].name.name == oper.output.name ) {
                                            tp_resp_found = true
                                        } else {
                                            tp_resp_count++
                                        }
                                    };
                                    if ( tp_resp_found ) {
                                        .schema << c_interface.types[ tp_resp_count ]
                                    }
                            }
                        };
                        swagger_params_count = -1
                        current_swagger_path -> swagger.paths[ path_counter ].( __method )

                        /* find request type description */
                        tp_count = 0; tp_found = false;
                        while( !tp_found && tp_count < #c_interface.types ) {
                            if ( c_interface.types[ tp_count ].name.name == oper.input.name ) {
                                tp_found = true
                            } else {
                                tp_count++
                            }
                        }

                        if ( tp_found ) {
                            current_type -> c_interface.types[ tp_count ];
                            if ( !is_defined( current_type.root_type.void_type ) ) {
                                println@Console( error_prefix + "but the root type of the request type is not void" )();
                                throw( DefinitionError )
                            }
                        }
                        

                        if ( !( __template instanceof void ) ) {
                            /* check if the params are contained in the request type */
                            splr =__template;
                            splr.regex = "/|\\?|=|&";
                            split@StringUtils( splr )( splres );
                            undef( par );
                            found_params = false;
                            for( pr = 0, pr < #splres.result, pr++ ) {
                                w = splres.result[ pr ];
                                w.regex = "\\{(.*)\\}";
                                find@StringUtils( w )( params );
                                if ( params == 1 ) {
                                    found_params = true;
                                    par = par + params.group[1] + "," /* string where looking for */
                                }
                            }

                            ;

                            if ( found_params ) {

                                    /* there are parameters in the template */
                                    error_prefix = error_prefix + "with template " + __template + " ";
                                    if ( !tp_found ) {
                                        println@Console( error_prefix +  "but the request type does not declare any field")();
                                        throw( DefinitionError )
                                    } else {
                                            /* if there are parameters in the template the request type must be analyzed */
                                            for( sbt = 0, sbt < #current_type.sub_type, sbt++ ) {

                                                /* casting */
                                                current_root_type -> current_sbt.type_inline.root_type;
                                                __add_cast_data;

                                                current_sbt -> current_type.sub_type[ sbt ];
                                                find_str = par;
                                                find_str.regex = current_sbt.name;

                                                find@StringUtils( find_str )( find_str_res );

                                                swagger_params_count++;
                                                with( current_swagger_path.parameters[ swagger_params_count ] ) {
                                                    .name = current_sbt.name;
                                                    if ( current_sbt.cardinality.min > 0 ) {
                                                        .required = true
                                                    };
                                                    if ( find_str_res == 1 ) {
                                                        if ( is_defined( current_sbt.type_inline ) ) {
                                                            if ( #current_sbt.type_inline.sub_type > 0 ) {
                                                                println@Console( error_prefix +  "but the field " + .name + " has a type with subnodes which is not permitted")();
                                                                throw( DefinitionError )
                                                            };
                                                            .in.other = "path";
                                                            .in.other.type << current_sbt.type_inline;

                                                            if ( is_defined( current_sbt.type_inline.root_type.type_link ) ) {
                                                                println@Console( error_prefix +  "but the field " + .name + " cannot reference to another type because it is a path parameter")();
                                                                throw( DefinitionError )
                                                            }
                                                        }

                                                    } else {
                                                        .in.in_body.schema << current_sbt
                                                    }

                                                }
                                            }

                                    }
                            } else {
                                /* no parameters in the template
                                    if the method is GET, the request type must be void
                                    if the method is different from GET, the request stype must be declared as a reference
                                */

                                /* the root type of the request type must be always void */
                                if ( !tp_found ) {
                                    if ( oper.input.name != "void" ) {

                                        println@Console( error_prefix + "0but its request message must be declared as void" )();
                                        throw( DefinitionError )
                                    }
                                } else {
                                    /* check if the request type is void */
                                    if ( !current_type.root_type.void_type ) {
                                        println@Console( error_prefix + "1but its request message must be declared as void" )();
                                        throw( DefinitionError )
                                    }
                                };

                                if ( __method == "get" ) {
                                        if ( tp_found && #current_type.sub_type > 0 ) {
                                            println@Console( error_prefix + "2but its request message must be declared as void" )();
                                            throw( DefinitionError )
                                        }
                                } else {
                                        if ( tp_found ) {
                                            /* the fields of the request type must be transformed into parameters */
                                            for( sf = 0, sf < #current_type.sub_type, sf++ ) {
                                                    swagger_params_count++;
                                                    with( current_swagger_path.parameters[ swagger_params_count ] ) {
                                                        .name = current_type.sub_type[ sf ].name;
                                                        if ( current_type.sub_type[ sf ].cardinality.min > 0 ) {
                                                            .required = true
                                                        };
                                                        .in.in_body.schema_subType << current_type.sub_type[ sf ]
                                                    }
                                            }
                                        }
                                }

                            }
                        } else {
                            __template = "/" + oper.operation_name;

                            /* if it is a GET, extract the path params from the request message */
                            if ( __method == "get" ) {
                                    for( sbt = 0, sbt < #current_type.sub_type, sbt++ ) {
                                    /* casting */
                                    current_root_type -> current_sbt.type_inline.root_type;
                                    __add_cast_data;

                                    current_sbt -> current_type.sub_type[ sbt ];
                                    swagger_params_count++;
                                    with( current_swagger_path.parameters[ swagger_params_count ] ) {
                                        .name = current_sbt.name;
                                        if ( current_sbt.cardinality.min > 0 ) {
                                            .required = true
                                        };

                                        if ( is_defined( current_sbt.type_inline ) ) {
                                                if ( #current_sbt.type_inline.sub_type > 0 ) {
                                                    println@Console( error_prefix +  "but the field " + .name + " has a type with subnodes which is not permitted")();
                                                    throw( DefinitionError )
                                                };
                                                .in.other = "path";
                                                .in.other.type << current_sbt.type_inline;
                                                if ( is_defined( current_sbt.type_inline.root_type.type_link ) ) {
                                                println@Console( error_prefix +  "but the field " + .name + " cannot reference to another type because it is a path parameter")();
                                                throw( DefinitionError )
                                                }
                                        } else {
                                                println@Console( error_prefix +  "but the field " + current_sbt.name + " of request type is not an inline type. REST template for GET method cannot be created!")();
                                                throw( DefinitionError )
                                        }
                                    }
                                    ;
                                    __template = __template + "/{" + current_sbt.name + "}"
                                }
                            } else {

                                    if ( tp_found ) {
                                        with( current_swagger_path.parameters ) {
                                            .in.in_body.schema_type << current_type;
                                            .name = current_type.name.name;
                                            .required = true
                                        }

                                    }
                            };
                            if ( LOG ) { println@Console( "Template automatically generated:" + __template )() }
                        }
                        ;
                        swagger.paths[ path_counter ] = __template
                        current_render_operation.template = "/" + service_input_port + __template
                    }
              }
          }
      }
}

define __config_operation {
    /* __cur_op, __op_name, __r_counter, _cast */
    if ( __r_counter > 0 ) {
        file_content = file_content + "\t;\n"
    };
    file_content = file_content + "\troutes[__route_counter++] << {\n";
    file_content = file_content + "\t\t.outputPort=\"" + __op_name + "\"\n";
    file_content = file_content + "\t\t,.method=\"" + __cur_op.method + "\"\n";
    file_content = file_content + "\t\t,.template=\"" + __cur_op.template + "\"\n";
    file_content = file_content + "\t\t,.operation=\"" + __cur_op + "\"\n";
    foreach( cast_par : __cast ) {
        file_content = file_content + "\t\t,.cast." + cast_par + "=\"" + __cast.( cast_par ) + "\"\n"
    };
    file_content = file_content + "\t}\n"
}

define __get_jester_config {

    /* creation of file router_import.ol */
    file_content = "";
    for( op = 0, op < #render.output_port, op++ ) {
         c_op -> render.output_port[ op ];
         file_content = file_content + render.output_port[ op ].surface
    };



    /* creation of the definition */
    file_content = file_content + "init {\n";
    route_counter = 0;
    for( op = 0, op < #render.output_port, op++ ) {
         c_op -> render.output_port[ op ];

         for( int_i = 0, int_i < #c_op.interfaces, int_i++ ) {
              for( opr = 0, opr < #c_op.interfaces[ int_i ].ow_operation, opr++ ) {
                    __op_name= c_op.name;
                    __r_counter = route_counter;
                    __cur_op -> c_op.interfaces[ int_i ].ow_operation[ opr ];
                    __cast -> c_op.interfaces[ int_i ].ow_operation[ opr ].cast;
                    __config_operation;
                    route_counter++
              }
              ;
              for( opr = 0, opr < #c_op.interfaces[ int_i ].rr_operation, opr++ ) {
                    __op_name= c_op.name;
                    __r_counter = route_counter;
                    __cur_op -> c_op.interfaces[ int_i ].rr_operation[ opr ];
                    __cast -> c_op.interfaces[ int_i ].rr_operation[ opr ].cast;
                    __config_operation;
                    route_counter++
              }
          }
    };

    file_content = file_content + "}";
    response -> file_content
}


main {

    [ getSwagger( request )( response ) {
        __body
        createSwaggerFile@Swagger( swagger )( response )
    }]

    [ getJesterConfig( request )( response ) {
        __body
        __get_jester_config
    }]

}
