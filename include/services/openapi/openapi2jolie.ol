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
    Protocol: sodep
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
                spl.replacement = "%!\\{";
                spl.regex = "\\{";
                replaceAll@StringUtils( spl )( corrected_path );

                outputPort.interface.operation[ op_max ] = operationId;
                outputPort.interface.operation[ op_max ].path = corrected_path;
                outputPort.interface.operation[ op_max ].method = method;
                outputPort.interface.operation[ op_max ].parameters << openapi.paths.( path ).( method ).parameters;
                foreach( res : openapi.paths.( path ).( method ).responses ) {
                    if ( res == "200" ) {
                        outputPort.interface.operation[ op_max ].response << openapi.paths.( path ).( method ).responses.( res )
                    } else {
                        outputPort.interface.operation[ op_max ].faults.( res ) << openapi.paths.( path ).( method ).responses.( res )
                    }
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

        foreach( definition : openapi.definitions ) {
            get_def.name = definition;
            get_def.definition -> openapi.definitions.( definition );
            getJolieTypeFromOpenApiDefinition@OpenApiDefinition( get_def )( j_definition );
            interface_file = interface_file + j_definition
        };

        for( o = 0, o < #outputPort.interface.operation, o++ ) {
            rq_p.definition.parameters -> outputPort.interface.operation[ o ].parameters;
            rq_p.name = outputPort.interface.operation[ o ] + "Request";
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
                    type_string = type_string + " void {\n\t._" + array + "}\n"
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
        interface_file = interface_file + "interface " + outputPort.interface + "{\n";
        interface_file = interface_file + "RequestResponse:\n";
        for( o = 0, o < #outputPort.interface.operation, o++ ) {
            interface_file = interface_file + "\t" + outputPort.interface.operation[ o ]
                + "( " + outputPort.interface.operation[ o ] + "Request )"
                + "( " + outputPort.interface.operation[ o ] + "Response )";
            if ( is_defined( outputPort.interface.operation[ o ].faults ) ) {
                interface_file = interface_file + " throws";
                foreach( f : outputPort.interface.operation[ o ].faults ) {
                    faultType = "";
                    if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).schema ) ) {
                        getReferenceName@OpenApiDefinition( outputPort.interface.operation[ o ].faults.( f ).schema.("$ref") )( ref );
                        faultType = "(" + ref + ")"
                    } else {
                        if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).description ) ) {
                            faultType = "( string )"
                        }
                    }
                    interface_file = interface_file + " Fault" + f + faultType
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
        client_file = "include \"" + port_name + "Interface.iol\"\n\n";
        client_file = client_file + "execution{ concurrent }\n\n";


        client_file = client_file + "outputPort " + outputPort.name + "{\n";
        client_file = client_file + "Location: \"" + outputPort.location + "\"\n";
        client_file = client_file + "Protocol: " + outputPort.protocol + "{\n";
        if ( protocol == "https" ) { 
            client_file = client_file + ".ssl.protocol = \"TLSv1.1\";"
        };
        for( o = 0, o < #outputPort.interface.operation, o++ ) {
            client_file = client_file + ".osc." + outputPort.interface.operation[ o ]
                            + ".alias=\"" + outputPort.interface.operation[ o ].path + "\";\n";
            client_file = client_file + ".osc." + outputPort.interface.operation[ o ]
                            + ".method=\"" + outputPort.interface.operation[ o ].method + "\"";
            if ( o < (#outputPort.interface.operation - 1) ) {
                client_file = client_file + ";\n"
            }
        };
        client_file = client_file + "}\n";

        client_file = client_file + "Interfaces: " + outputPort.interface + "\n}\n\n";

        client_file = client_file + "inputPort " + port_name + "{\n";
        client_file = client_file + "Location:\"local\"\n";
        client_file = client_file + "Protocol: sodep\n";
        client_file = client_file + "Interfaces: " + outputPort.interface + "\n}\n\n";

        client_file = client_file + "main {\n";
        for( o = 0, o < #outputPort.interface.operation, o++ ) {
            client_file = client_file + "\t[ " + outputPort.interface.operation[ o ] + "( request )( response ) {\n";
            client_file = client_file + "\t\t" + outputPort.interface.operation[ o ] + "@" + outputPort.name + "( request )( response )\n";
            if ( is_defined( outputPort.interface.operation[ o ].faults ) ) {
                foreach( f : outputPort.interface.operation[ o ].faults ) {
                    faultValue = "";
                    if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).schema ) ) {
                        faultValue = ", response"
                    } else {
                        if ( is_defined( outputPort.interface.operation[ o ].faults.( f ).description ) ) {
                            faultValue = ", response.description"
                        }
                    }
                    client_file = client_file + "\t\tif ( response.(\"@header\").statusCode == " + f + ") {\n ";
                    client_file = client_file + "\t\t\tthrow( Fault" + f + faultValue + ")\n"
                    client_file = client_file + "\t\t}\n"
                }
            };
            client_file = client_file + "\t}]\n\n"
        };
        client_file = client_file + "}"

        response -> client_file
    }]
}