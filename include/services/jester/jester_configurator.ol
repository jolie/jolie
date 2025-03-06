/*

 *  Copyright (c) 2016 Claudio Guidi <guidiclaudio@gmail.com>

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

include "./JesterConfiguratorInterface.iol"
include "services/openapi/public/interfaces/OpenApiDefinitionInterface.iol"
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


embedded {
  Jolie:
    "services/openapi/openapi_definition.ol" in OpenApi,
    "services/jester/jester_utils.ol" in JesterUtils
}

inputPort JesterConfigurator {
  Location: "local"
  Interfaces: JesterConfiguratorInterface
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
        .filename = service_filename
      };
      getInputPortMetaData@MetaJolie( request_meta )( metadata )
      /* creating a map name-types for managing type links */
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


      
      /* selecting the port and the list of the interfaces to be imported */
      for( i = 0, i < #metadata.input, i++ ) {
          // port selection from metadata
          if ( metadata.input[ i ].name == service_input_port ) {
              output_port_index = #render.output_port
              with( render.output_port[ output_port_index ] ) {
                    .name = service_input_port;
                    .location = metadata.input[ i ].location;
                    .protocol = metadata.input[ i ].protocol
              };

              // for each interface in the port
              for( int_i = 0, int_i < #metadata.input[ i ].interfaces, int_i++ ) {
                  c_interface -> metadata.input[ i ].interfaces[ int_i ]
                  c_interface_name = c_interface.name
                  render.output_port[ output_port_index ].interfaces[ int_i ] = c_interface_name;
                  
                  // for each operations in the interfaces
                  for( o = 0, o < #c_interface.operations, o++ ) {
                        oper -> c_interface.operations[ o ]
                        if ( LOG ) { println@Console("Analyzing operation:" + oper.operation_name )() }
                        error_prefix =  "ERROR on port " + service_input_port + ", operation " + oper.operation_name + ":" + "the operation has been declared to be imported as a REST ";
                        
                        undef( __template )
                        undef( __method )
                        // proceed only if a rest template has been defined for that operation
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

                        
                        if ( is_defined( oper.output ) ) {
                            rr_operation_max = #render.output_port[ output_port_index ].interfaces[ int_i ].rr_operation
                            current_render_operation -> render.output_port[ output_port_index ].interfaces[ int_i ].rr_operation[ rr_operation_max ]
                        } else {
                            ow_operation_max = #render.output_port[ output_port_index ].interfaces[ int_i ].ow_operation
                            current_render_operation -> render.output_port[ output_port_index ].interfaces[ int_i ].ow_operation[ ow_operation_max ]
                        }
                        current_render_operation = oper.operation_name
                        current_render_operation.method = __method

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
                        get_actual_ctype_rq = c_interface.types[ tp_count ].name
                        get_actual_ctype_rq.type_map -> global.type_map 
                        getActualCurrentType@JesterUtils( get_actual_ctype_rq )( actual_type_name );

                        
                        current_type -> global.type_map.( actual_type_name )

                        if ( !( __template instanceof void ) ) {
                            /* check if the params are contained in the request type */
                            getParamList@JesterUtils( __template )( found_params )

                            if ( found_params ) {
                                    /* there are parameters in the template */
                                    error_prefix = error_prefix + "with template " + __template + " ";
                                    if ( !tp_found ) {
                                            error_msg = error_prefix +  "but the request type does not declare any field"
                                            throw( DefinitionError, error_msg )
                                    } else {
                                            /* if there are parameters in the template the request type must be analyzed */
                                            for( sbt = 0, sbt < #current_type.type.sub_type, sbt++ ) {

                                                /* casting */
                                                current_sbt -> current_type.type.sub_type[ sbt ];
                                                current_root_type -> current_sbt.type.root_type;
                                                __add_cast_data
                                            }

                                    }
                            } 
                        } else {
                            __template = "/" + oper.operation_name;

                            /* if it is a GET, extract the path params from the request message */
                            if ( __method == "get" ) {
                                    for( sbt = 0, sbt < #current_type.type.sub_type, sbt++ ) {
                                    /* casting */
                                    current_sbt -> current_type.type.sub_type[ sbt ];
                                    current_root_type -> current_sbt.type.type.root_type;
                                    __add_cast_data;

                                    __template = __template + "/{" + current_sbt.name + "}"
                                }
                            } 
                            if ( LOG ) { println@Console( "Template automatically generated:" + __template )() }
                        }
                        current_render_operation.template = __template
                    }
              }
          }
      }
}

define __config_operation {
    with( response.routes[ __r_counter ] ) {
        .method = __cur_op.method;
        .template = __cur_op.template;
        .operation = __cur_op;
        .outputPort = service_input_port;
        foreach( cast_par : __cast ) {
            .cast.( cast_par ) = __cast.( cast_par )
        }
    }
}



main {

    [ getJesterConfig( request )( response ) {
        __body
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
        }
    }]

}
