/*
 *   Copyright (C) 2019 by Claudio Guidi <guidiclaudio@gmail.com>         
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
 include "services/openapi/public/interfaces/OpenApi2JolieInterface.iol"
include "services/openapi/public/interfaces/OpenApiDefinitionInterface.iol"
include "console.iol"
include "string_utils.iol"

execution{ concurrent }


outputPort OpenApiDefinition {
    Interfaces: OpenApiDefinitionInterface
}

embedded {
    Jolie:
        "services/openapi/openapi_definition.ol" in OpenApiDefinition
}

inputPort OpenApi2Jolie {
    Location: "local"
    Interfaces: OpenApi2JolieInterface
}

define _create_outputport_info {
        outputPort.name = port_name + "Port";
        outputPort.location = "socket://" + openapi.host + openapi.basePath;
        outputPort.protocol = protocol;
        outputPort.interface = port_name + "Interface";

        foreach( path : openapi.paths ) {
            foreach( method : openapi.paths.( path ) ) {
            
                if ( !is_defined( openapi.paths.( path ).( method ).operationId ) ) {
                    println@Console( "Path " + method + ":" + path + " does not define an operationId, please insert an operationId for this path.")();
                    print@Console(">")();
                    in( operationId )
                } else {
                    operationId = openapi.paths.( path ).( method ).operationId
                }
                ;
                op_max = #outputPort.interface.operation;
                spl = path;
                spl.replacement = "%!\\{_p";  // _p has been added in order to be compatible with path parameters on the interface which are prefixed with letter p
                spl.regex = "\\{";
                replaceAll@StringUtils( spl )( corrected_path );

                outputPort.interface.operation[ op_max ] = operationId;
                outputPort.interface.operation[ op_max ].path = corrected_path;
                outputPort.interface.operation[ op_max ].method = method;
                outputPort.interface.operation[ op_max ].parameters << openapi.paths.( path ).( method ).parameters;
                found500 = false
                foreach( res : openapi.paths.( path ).( method ).responses ) {
                    if ( res == "500" ) { found500 = true }
                    if ( res == "200" ) {
                        outputPort.interface.operation[ op_max ].response << openapi.paths.( path ).( method ).responses.( res )
                    } else {
                        outputPort.interface.operation[ op_max ].faults.( res ) << openapi.paths.( path ).( method ).responses.( res )
                    }
                }
                if ( !found500 ) {
                    outputPort.interface.operation[ op_max ].faults.( "500" ).description = "Internal Server Error"
                }
                
            }
        }
}

define __getFaultDefinition {
    splref = __link_name
    split@StringUtils( splref { regex = global.DEFINITIONS_PATH } )( splrefres )
    __jolietypename = splrefres.result[1]
    foreach( definition : openapi.definitions ) {
        if ( definition == __jolietypename ) {
            __fault_name = openapi.definitions.( definition ).properties.fault.pattern
            if ( is_defined( openapi.definitions.( definition ).properties.content.("$ref") ) ) {
                splref = openapi.definitions.( definition ).properties.content.("$ref")
                split@StringUtils( splref { regex = global.DEFINITIONS_PATH } )( splrefres )
                __fault_type = splrefres.result[1]
            } else {
                // jolie generated type not found
                __fault_type = ""
            }
     
        }
    }
}

main {

    [ getJolieInterface( request )( response ) {
        /* creating outputPort */
        port_name -> request.port_name
        openapi -> request.openapi
        _create_outputport_info

        global.DEFINITIONS_PATH = "#/definitions/"
        if ( is_defined( openapi.openapi ) && match@StringUtils( openapi.openapi { regex = "^3\\.(.*)" } ) ) {
            println@Console("We are parsing in OpenAPI 3.0 mode")()
            global.DEFINITIONS_PATH = "#/components/schemas/"
            setDefinitionsPath@OpenApiDefinition( global.DEFINITIONS_PATH )( )
            openapi.definitions -> openapi.components.schemas
        }

        // create definition array
        foreach( definition : openapi.definitions ) {
            get_def.name = definition;
            get_def.definition -> openapi.definitions.( definition )
            definitionIsArray@OpenApiDefinition( get_def )( is_array )
            if ( is_array ) {
                array_def_list.( definition ) << openapi.definitions.( definition )
            }
        }

        foreach( definition : openapi.definitions ) {
            
                get_def.name = definition;
                get_def.definition -> openapi.definitions.( definition );
                get_def.array_def_list -> array_def_list
                getJolieTypeFromOpenApiDefinition@OpenApiDefinition( get_def )( j_definition );
                interface_file = interface_file + j_definition
                
        }

        for( o = 0, o < #outputPort.interface.operation, o++ ) {
            rq_p.definition.parameters -> outputPort.interface.operation[ o ].parameters;
            rq_p.name = outputPort.interface.operation[ o ] + "Request";
            rq_p.array_def_list -> array_def_list
            getJolieTypeFromOpenApiParameters@OpenApiDefinition( rq_p )( parameters );

            type_string = parameters;

            type_string = type_string + "type " + outputPort.interface.operation[ o ] + "Response:";
            if ( is_defined( outputPort.interface.operation[ o ].response.schema ) ) {
                if ( is_defined( outputPort.interface.operation[ o ].response.schema.("$ref") ) ) {
                    getReferenceName@OpenApiDefinition( outputPort.interface.operation[ o ].response.schema.("$ref") )( ref );
                    type_string = type_string + ref + "\n"
                } else if ( outputPort.interface.operation[ o ].response.schema.type == "array" ) {
                    rq_arr.definition -> outputPort.interface.operation[ o ].response.schema;
                    rq_arr.indentation = 1;
                    getJolieDefinitionFromOpenApiArray@OpenApiDefinition( rq_arr )( array );
                    type_string = type_string + " void {\n\t._" + array. cardinality + ":" + array + "}\n"
                } else if ( outputPort.interface.operation[ o ].response.schema.type == "object" ) {
                    type_string = type_string + "undefined\n"
                } else {
                    rq_n.type = outputPort.interface.operation[ o ].response.schema.type;
                    if ( is_defined( outputPort.interface.operation[ o ].response.schema.format ) ) {
                        rq_n.format = outputPort.interface.operation[ o ].response.schema.format
                    };
                    getJolieNativeTypeFromOpenApiNativeType@OpenApiDefinition( rq_n )( native );
                    type_string = type_string + native + "\n"
                }
            } else {
                type_string = type_string + "undefined \n"
            }
            ;
            interface_file = interface_file + type_string
        };


        /* create interface file */
        interface_file = interface_file + "\ninterface " + outputPort.interface + " {\n";
        interface_file = interface_file + "RequestResponse:\n";
        for( o = 0, o < #outputPort.interface.operation, o++ ) {
            interface_file = interface_file + "\t" + outputPort.interface.operation[ o ]
                + "( " + outputPort.interface.operation[ o ] + "Request )"
                + "( " + outputPort.interface.operation[ o ] + "Response )";
            if ( is_defined( outputPort.interface.operation[ o ].faults ) ) {
                interface_file = interface_file + " throws";
                foreach( f : outputPort.interface.operation[ o ].faults ) {
                    faultType = "";
                    if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).schema.("$ref") ) ) {
                        __link_name = outputPort.interface.operation[ o ].faults.( f ).schema.("$ref")
                        __getFaultDefinition
                        if ( __fault_name != ""  && __fault_type != "" ) {
                            faultType = __fault_name + "( " + __fault_type + " )"
                        } else {
                            getReferenceName@OpenApiDefinition( outputPort.interface.operation[ o ].faults.( f ).schema.("$ref") )( ref );
                            faultType = "Fault" + f + "( " + ref + " ) "
                        }
                    } else if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).schema.oneOf ) ) {
                        for( of = 0, of < #outputPort.interface.operation[ o ].faults.( f ).schema.oneOf, of++ ) {
                            __link_name = outputPort.interface.operation[ o ].faults.( f ).schema.oneOf[ of ].("$ref")
                            __getFaultDefinition
                            if ( __fault_name != "" && __fault_type != "" ) {
                                faultType = faultType + " " + __fault_name + "( " + __fault_type + " )"
                            } else {
                                getReferenceName@OpenApiDefinition( outputPort.interface.operation[ o ].faults.( f ).schema.oneOf[ of ].("$ref") )( ref );
                                faultType = faultType + " Fault" + f + "_" + of + "( " + ref + " ) "
                            }
                        }
                    } else {
                        if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).description ) ) {
                            faultType = "Fault" + f + "( string ) "
                        }
                    }
                    interface_file = interface_file + " " + faultType
                }
            };

            if ( o < (#outputPort.interface.operation - 1) ) {
                interface_file = interface_file + ",\n"
            }
        };
        interface_file =  interface_file + "\n}\n\n";

        response -> interface_file
    }]

    [ getJolieClient( request )( response ) {
        port_name -> request.port_name
        openapi -> request.openapi
        protocol -> request.protocol
        _create_outputport_info

        //writing client service file
        client_file = "include \"" + port_name + "Interface.iol\"\n";
        client_file = client_file + "include \"string_utils.iol\"\n\n";

        client_file = client_file + "interface " + outputPort.interface + "HTTP {\n";
        client_file = client_file + "RequestResponse:\n";
        for( o = 0, o < #outputPort.interface.operation, o++ ) {
            client_file = client_file + "\t" + outputPort.interface.operation[ o ]
            if ( o < (#outputPort.interface.operation - 1) ) {
                client_file = client_file + ",\n"
            }
        };
        client_file =  client_file + "\n}\n\n";


        client_file = client_file + "execution { concurrent }\n\n";

        endsWith@StringUtils( outputPort.location { .suffix = "/" } )( ends_with_slash )
        split@StringUtils( outputPort.location { .regex = "/" } )( spl_loc )
        // if location is in the form socket://ip:port it must not finish with /, in this case the interpreter will put the /
        // if it is in the form socket://ip:port/something it must be finish with / because the interpreter won't put the /
        if ( #spl_loc.result == 3 ) {
            // it is in the form socket://ip:port
            if ( ends_with_slash ) {
                sbst = outputPort.location
                length@StringUtils( outputPort.location )( location_length )
                with ( sbst ) {
                    .begin = 0;
                    .end = location_length - 1
                }
                substring@StringUtils( sbst )( outputPort.location )
            }
        } else {
            // it is in the form socket://ip:port/something
            if ( !ends_with_slash ) {
                outputPort.location = outputPort.location + "/"
            }
        }

        client_file = client_file + "outputPort " + outputPort.name + " {\n";
        client_file = client_file + "Location: \"" + outputPort.location + "\"\n";
        client_file = client_file + "Protocol: " + outputPort.protocol + " {\n";
        if ( protocol == "https" ) { 
            client_file = client_file + ".ssl.protocol = \"TLSv1.1\";"
        };
        client_file = client_file + "\t.format = \"json\";\n"
        client_file = client_file + "\t.responseHeaders=\"@header\";\n"
        for( o = 0, o < #outputPort.interface.operation, o++ ) {
            
            client_file = client_file + "\t.osc." + outputPort.interface.operation[ o ]
                            + ".alias-> alias;\n";
            client_file = client_file + "\t.osc." + outputPort.interface.operation[ o ]
                            + ".method=\"" + outputPort.interface.operation[ o ].method + "\"";
            if ( o < (#outputPort.interface.operation - 1) ) {
                client_file = client_file + ";\n"
            }
        };
        client_file = client_file + "\n}\n";

        client_file = client_file + "Interfaces: " + outputPort.interface + "HTTP\n}\n\n";

        client_file = client_file + "inputPort " + port_name + " {\n";
        client_file = client_file + "Location:\"local\"\n";
        client_file = client_file + "Protocol: sodep\n";
        client_file = client_file + "Interfaces: " + outputPort.interface + "\n}\n\n";

        client_file = client_file + "init { install( default => nullProcess ) }\n"

        client_file = client_file + "main {\n";
        for( o = 0, o < #outputPort.interface.operation, o++ ) {
            undef( parameters )
            for( p in outputPort.interface.operation[ o ].parameters ) {
                parameters.( p.name ) << p
            }

            startsWith@StringUtils(  outputPort.interface.operation[ o ].path  { .prefix = "/" } )( starts_with_slash )
            if ( starts_with_slash ) {
                sbst = outputPort.interface.operation[ o ].path
                length@StringUtils( outputPort.interface.operation[ o ].path )( path_length )
                with ( sbst ) {
                    .begin = 1;
                    .end = path_length
                }
                substring@StringUtils( sbst )( alias_path )
            }  else {
                alias_path = outputPort.interface.operation[ o ].path
            }
            client_file = client_file + "\t[ " + outputPort.interface.operation[ o ] + "( request )( response ) {\n";
            
            client_file = client_file + "
            query_parameter_is_present = false
            alias = \"" +  alias_path + "\"
            foreach( f : request ) {
                // depending on the type of the field (query, path or body ) there are different behaviour
                startsWith@StringUtils( f { .prefix = \"_p\" } )( is_path )
                startsWith@StringUtils( f { .prefix = \"_q\" } )( is_query )
                if ( is_path || is_query ) {
                    requestToAPI.( f ) = request.( f )
                } else {
                    // it is a body parameter the name must be skipped
                    foreach( sf : request.( f ) ) {
                        requestToAPI.( sf ) << request.( f ).( sf )
                    }
		}
                if ( is_query ) {
                    if ( !query_parameter_is_present ) { alias = alias + \"?\" }
                    else { 
                        query_parameter_is_present = true
                        alias = alias + \"&\" 
                    }
                    length@StringUtils( f )( field_length )
                    substring@StringUtils( f { begin = 2, end = field_length } )( real_query_parameter_name )
                    alias = alias + real_query_parameter_name + \"=%!{\" + f + \"}\"
                }
            }\n\n"

            client_file = client_file + "\t\t" + outputPort.interface.operation[ o ] + "@" + outputPort.name + "( requestToAPI )( response )\n";
            if ( is_defined( outputPort.interface.operation[ o ].faults ) ) {
                if ( outputPort.interface.operation[ o ].faults.( "500" ).description == "JolieFault" ) {
                    // openapi generated by jolie2openapi
                    faultValue = ", response.content"
                    client_file = client_file + "\t\tif ( response.(\"@header\").statusCode == 500 ) {\n ";
                    if ( is_defined(  outputPort.interface.operation[ o ].faults.( "500" ).schema.oneOf ) ) {
                        //more than one fault
                        for( f = 0, f < #outputPort.interface.operation[ o ].faults.( "500" ).schema.oneOf, f++ ) {
                            __link_name = outputPort.interface.operation[ o ].faults.( "500" ).schema.oneOf[ f ].("$ref")
                            __getFaultDefinition
                            client_file = client_file + "\t\t\tif ( response.fault == \"" + __fault_name + "\" ) {\n"
                            client_file = client_file + "\t\t\t\tthrow( " + __fault_name + faultValue + " )\n"
                            client_file = client_file + "\t\t\t}\n"
                        }
                    } else {
                        __link_name = outputPort.interface.operation[ o ].faults.( "500" ).schema.("$ref")
                        __getFaultDefinition
                        client_file = client_file + "\t\t\tthrow( " + __fault_name + faultValue + " )\n"
                    }
                    client_file = client_file + "\t\t}\n"
                    
                } else {
                    found500 = false
                    foreach( f : outputPort.interface.operation[ o ].faults ) {
                        if ( f == "500" ) {
                            found500 = true
                        }
                        faultValue = "";
                        faultName = "Fault" + f
                        if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).schema ) ) {
                            faultValue = ", response"
                        } else {
                            if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).description ) ) {
                                faultValue = ", \"" + outputPort.interface.operation[ o ].faults.( f ).description + "\""
                            }
                        }
                        if ( f == "default" ) {
                            f = "400"
                        }
                        client_file = client_file + "\t\tif ( response.(\"@header\").statusCode == " + f + " ) {\n";
                        client_file = client_file + "\t\t\tthrow( " + faultName + faultValue + " )\n"
                        client_file = client_file + "\t\t}\n"
                    }
                    if ( !found500 ) {
                        client_file = client_file + "\t\tif ( response.(\"@header\").statusCode == 500 ) {\n ";
                        client_file = client_file + "\t\t\tthrow( fault500 )\n"
                        client_file = client_file + "\t\t}\n"
                    }
                }
                
            };
            client_file = client_file + "\t\tundef( response.(\"@header\"))\n\t}]\n\n"
        };
        client_file = client_file + "}"

        response -> client_file
    }]
}
