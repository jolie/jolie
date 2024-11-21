from console import Console
from metajolie import MetaJolie
from file import File
from string-utils import StringUtils
from mustache import Mustache
from joliedoc-lib import JolieDocLib


from types.definition-types import Port

constants {
    JOLIEDOC_FOLDER = "joliedoc"
}

/*ce JolieDocRender {

    Interfaces: RenderInterface

    inputPort JolieDocRender {
        location: "local"
        interfaces: RenderInterface
    }


    main {
        [ getIndentation( request )( response ) {
            response = ""
            for( i = 0, i < request, i++ ) {
                response = response + "&nbsp;"
            }
        }]

        [ getFaultType( request )( response ) {
            response = "undefined"
            if ( request.type instanceof NativeType ) {
                getNativeType@Render( request.type )( response )
            } else if ( request.type instanceof TypeLink ) {
                getTypeLink@Render( request.type )( response )
            }
        }]

        [ getPort( request )( response ) {
            response = "<table class='port-definition'>"
            response = response + "<tr><th class='resource-label-PORTTYPE resource-label'>PORTTYPE</th><th id='" + request.name + "' colspan='2' class='content-td'>" + request.name + "</th></tr>"
            response = response + "<tr><td></td><td class='content-td port-element'>Location:</td><td class='content-td'>" + request.location + "</td></tr>"
            response = response + "<tr><td></td><td class='content-td port-element'>Protocol:</td><td class='content-td'>" + request.protocol + "</td></tr>"
            response = response + "<tr><td></td><td class='content-td port-element'>Interfaces:</td><td class='content-td'>" 
            for( i in request.interfaces ) {
                response = response + "<a href='#" + "'>" + i.name + "<a>&nbsp;&nbsp;"
            }
            response = response + "</td></tr></table>"
        }]

        [ getInterface( request )( response ) {
            response = "<table class='interface-definition'>"
            response = response + "<tr><th id='" + request.name + "' class='resource-label-interface resource-label'>intf</th><th id='" + request.name + "' colspan='3' class='content-td'>" + request.name + "</th></tr>"
            if ( is_defined( request.documentation ) && request.documentation != "" ) {
                response = response + "<tr><td>&nbsp;</td></tr>"
                response = response + "<tr><td></td><td colspan='4' class='documentation'>" + request.documentation + "</td></tr>"
                response = response + "<tr><td>&nbsp;</td></tr>"
            } else {
                response = response + "<tr><td>&nbsp;</td></tr>"
            }


            for( o in request.operations ) {
                otype = " class='resource-label ow-type'>ow"
                if ( is_defined( o.output ) ) {
                     otype = " class='resource-label rr-type'>rr"
                } 
                response = response + "<tr><td" + otype + "</td><td class='content-td operation-name'><span>" + o.operation_name + "</span><button onclick='openDetails(\"" + o.operation_name + "\")' class='operation-button'>Details</button></td>"
                response = response + "<td class='content-td'><span class='message-type'>Request:</span><a href='#" + o.input + "'>" + o.input + "</a></td>"
                response = response + "<td class='content-td'>"
                if ( is_defined( o.output ) )  {
                    response = response + "<span class='message-type'>Response:</span><a href='#" + o.output + "'>" + o.output + "</a>"
                } 
                response = response + "</td></tr>"
                if ( is_defined( o.fault )) {
                    
                    response = response + "<tr id='faults_" + o.operation_name + "' style='display:none'><td></td><td></td><td></td><td class='content-td'><span class='message-type'>Faults:</span> "
                    for ( f in o.fault ) {
                        response = response + "<br>" + indent + "<span class='fault-name'>" + f.name + "</span>"
                        getFaultType@Render( f )( fname )
                        response = response + "(" + fname + ")"
                    }
                    response = response + "<br>"
                }
                response = response + "</td></tr>"
                replaceAll@StringUtils( o.documentation { .regex = "\n", .replacement = "<br>" } )( o_documentation )
                response = response + "<tr id='doc_" + o.operation_name + "' style='display:none'><td></td><td colspan='3' class='documentation'>" 
                    + o_documentation + "</td><tr>"
                response = response + "<tr><td><br></td></tr>"
            }
            response = response + "</table>"
        }]

        [ getTypeInLine( request )( response ) {
            getNativeType@Render( request.root_type )( response )
            if ( #request.sub_type > 0 ) {
                response = response + " {</td><td class='documentation'>" + global.documentation + "</td></tr>"
                global.documentation = "";
                for( s in request.sub_type ) {
                    getSubType@Render( s )( sub_type )
                    response = response + sub_type
                }
                getIndentation@Render( global.indentation )( indent )
                response = response + "<tr><td></td><td class='content-td'>" + indent + "}"
            }
        }]

        [ getTypeDefinition( request )( response ) {
            global.indentation = 0;
            response = "<table class='type-definition'>"
            response = response + "<tr><th class='resource-label-type resource-label'>type</th><th id='" + request.name + "' colspan='2' class='content-td'>" + request.name + "</th></tr>"
            if ( is_defined( request.documentation ) && request.documentation != "" ) {
                response = response + "<tr><td>&nbsp;</td></tr>"
                response = response + "<tr><td></td><td colspan='2' class='documentation'>" + request.documentation + "</td></tr>"
                response = response + "<tr><td>&nbsp;</td></tr>"
            } else {
                response = response + "<tr><td>&nbsp;</td></tr>"
            }
            response = response + "<tr><td></td><td class='content-td'><b>type</b> " + request.name + "<b>:</b>&nbsp;"
            getType@Render( request.type )( type_string )
            response = response + type_string + "</td><td></td></tr></table>"
        }]

        [ getType( request )( response ) {
            if ( request instanceof TypeInLine ) {
                getTypeInLine@Render( request )( response )
            }
            if ( request instanceof TypeChoice ) {
                getTypeChoice@Render( request )( response )
            }
            if ( request instanceof TypeLink ) {
                getTypeLink@Render( request )( response )
            }
            if ( request instanceof TypeUndefined ) {
                getTypeUndefined@Render( request )( response )
            } 
        }]

        [ getTypeChoice( request )( response ) {
            if ( request.choice.left_type instanceof TypeInLine ) {
                getTypeInLine@Render( request.choice.left_type )( response )
            } else if ( request.choice.left_type instanceof TypeLink ) {
                getTypeLink@Render( request.choice.left_type )( response ) 
            }
            getType@Render( request.choice.right_type )( r_type )
            getIndentation@Render( global.indentation )( indent )
            response = response + "<tr><td></td><td class='content-td'>" + indent + "<b>|</b>&nbsp;" + r_type 
        }]

        [ getTypeLink( request )( response ) {
            response = "<a href='#" + request.link_name + "'>" + request.link_name + "</a>" 
        }]

        [ getTypeUndefined( request )( response ) {
            response = "undefined"
        }]

        [ getNativeType( request )( response ) {
            if ( is_defined( request.string_type ) ) {
                response = "string"
            }
            if ( is_defined( request.int_type ) ) {
                response = "int"  
            }
            if ( is_defined( request.double_type ) ) {
                response = "double"
            }
            if ( is_defined( request.any_type ) ) {
                response = "any"
            }
            if ( is_defined( request.void_type ) ) {
                response = "void"
            }
            if ( is_defined( request.raw_type ) ) {
                response = "raw"
            }
            if ( is_defined( request.bool_type ) ) {
                response = "bool"
            }
            if ( is_defined( request.long_type ) ) {
                response = "long"
            }
            response = "<b>" + response + "</b>"
        }]

        [ getSubType( request )( response ) {
            global.indentation = global.indentation + 5
            getIndentation@Render( global.indentation )( indent )
            response = response + "<tr><td></td><td class='content-td'>" + indent + "." + request.name 
            getCardinality@Render( request.cardinality )( cardinality )
            response = response + cardinality + ":&nbsp;"
            global.documentation = request.documentation;  // documentation must be passed because it could be used in TypeInLine
            getType@Render( request.type )( type_string )
            response = response + type_string + "</td><td class='documentation'>" + global.documentation + "</td></tr>"
            global.documentation = ""
            global.indentation = global.indentation - 5
        }]

        [ getCardinality( request )( response ) {
            response = "[" + request.min + ","
            if ( request.infinite > 0 ) {
                response = response + "*"
            } else {
                response = response + request.max
            }
            response = response + "]"
        }]

    }
}*/

type GetOverviewPageRequest {
    svgIcon: string 
    template: string
    communication_dependencies*: void {
        operation_name: string 
        dependencies*: void {
            name: string
            port?: string
        }
    }
}

type GetPortPageRequest {
    port_type: string( enum(["input","output"] ))
    port: Port
    template: string 
    partials* {
        name: string 
        template: string
    }
}

type GetIndexPageRequest {
    template: string 
    partials* {
        name: string 
        template: string
    }
    data {
        filename: string 
        ipitems* {
            ipname: string 
        }
        opitems* {
            opname: string
        }
    }
}


interface RenderDocInterface {
    RequestResponse:
        getOverviewPage( GetOverviewPageRequest )( string ),
        getPortPage( GetPortPageRequest )( string ),
        getIndexPage( GetIndexPageRequest )( string )
}

service RenderDocPages {

    embed Mustache as Mustache
    embed StringUtils as StringUtils
    embed JolieDocLib as JolieDocLib
    embed Console as Console



    inputPort RenderDocPages {
        location: "local"
        interfaces: RenderDocInterface
    }

    execution: concurrent 


    main {

        [ getOverviewPage( request )( response ) {
            render@Mustache( {
                template = request.template
                data << request 
            })( response )
        }]

        [ getPortPage( request )( response ) {
            
            _getPort@JolieDocLib( {
                indentation_cr_replacement = "&nbsp;&nbsp;"
                documentation_cr_replacement = "<br>"
                port << request.port
            })( port )
            
            render_req << {
                template = request.template
                data.port << port 
                recursionLimit = 20
                partialsRecursionLimit = 10
            }
            if ( is_defined( request.partials ) ) { render_req.partials << request.partials }
            render@Mustache( render_req )( response )
        }]

        [ getIndexPage( request )( response ) {
            render_req << {
                template = request.template
                data << request.data 
                recursionLimit = 20
                partialsRecursionLimit = 10
            }
            if ( is_defined( request.partials ) ) { render_req.partials << request.partials }
            render@Mustache( render_req )( response )
        }]
    }
    
}

service JolieDoc {

    embed Console as Console
    embed MetaJolie as MetaJolie 
    embed StringUtils as StringUtils
    embed File as File
    embed RenderDocPages as RenderDocPages
    


   define _get_ports {
        // __port_type
        for( ptt in meta_description.( __port_type ) ) {
                startsWith@StringUtils( ptt.location { .prefix = "local://" } )( starts_with_local )
                if ( internals || ( ptt.location != "undefined" && ptt.location != "local" && !starts_with_local ) ) {

                    print@Console("generating port " + ptt.name + "...")()
                    undef( rq_portPage )
                    rq_portPage <<  { 
                        template = port_page_template
                        partials << partials
                        port_type = __port_type
                        port << ptt
                    }
                
                    portPage = getPortPage@RenderDocPages( rq_portPage )
                    
                    undef( itfcs )
                    undef( tps )
                    for( itf in ptt.interfaces ) {
                        //getInterface@Render( itf )( itfc )
                        itfcs[ #itfcs ] = itfc
                        for ( tp in itf.types ) {
                            //getTypeDefinition@Render( tp )( tp_string )
                            tps[ #tps ] = tp_string
                        }
                    }
                    html = "<html><head>" + css + js + "</head><body>" + port + "<hr>"
                    for( i in itfcs ) {
                        html = html + i 
                    }
                    html = html + "<hr>"
                    for( t in tps ) {
                        html = html + t
                    }
                    html = html + "</body></html>"

                    max_files = #files 
                    if ( __port_type == "input" ) {
                        endname = "IPort." + type
                    } else {
                        endname = "OPort." + type
                    }
                    files[ max_files ].filename = ptt.name + endname
                    files[ max_files ].html = portPage
                    println@Console("done")()
                }
            }      
    }

    init {
        install( Abort => nullProcess )
    }

    main {
        
        if ( #args == 0 ) {
            println@Console( "Usage: joliedoc <filename> [ --internals ] [ --out-type 'html|md' ]")()
        } else {
            println@Console("Generating...")()
            internals = false
            type = "html"
            for ( i=0, i< #args, i++ ) {
                if ( i == 0 ){
                    rq.filename = args[ i ]
                } else {
                    if ( args[ i ] == "--internals" ){
                        internals = true
                    } else if ( args[ i ] == "--out-type" ){
                        type = args[ ++i ]
                    }

                }
            }
            
            getServiceDirectory@File()( joliedoc_directory )
            getFileSeparator@File()(sep)
            // loading templates
            readFile@File( { filename = joliedoc_directory + sep + "templates" + sep + type + sep + "overview-page-template.mustache" } )( ovh_template )
            readFile@File( { filename = joliedoc_directory + sep + "templates" + sep + type + sep + "port-page-template.mustache" } )( port_page_template )
            readFile@File( { filename = joliedoc_directory + sep + "templates" + sep + type + sep + "index-template.mustache" } )( index_template )
            // loading partials
            partials[0].name = "cardinality-template"
            readFile@File( { filename = joliedoc_directory + sep + "templates" + sep + type + sep + "cardinality-template.mustache" } )( partials[0].template )
            partials[1].name = "native-type-template"
            readFile@File( { filename = joliedoc_directory + sep + "templates" + sep + type + sep + "native-type-template.mustache" } )( partials[1].template )
            partials[2].name = "sub-type-template"
            readFile@File( { filename = joliedoc_directory + sep + "templates" + sep + type + sep + "sub-type-template.mustache" } )( partials[2].template )
            partials[3].name = "type-template"
            readFile@File( { filename = joliedoc_directory + sep + "templates" + sep + type + sep + "type-template.mustache" } )( partials[3].template )
            partials[4].name = "typeinline-template"
            readFile@File( { filename = joliedoc_directory + sep + "templates" + sep + type + sep + "typeinline-template.mustache" } )( partials[4].template )
            partials[5].name = "typelink-template"
            readFile@File( { filename = joliedoc_directory + sep + "templates" + sep + type + sep + "typelink-template.mustache" } )( partials[5].template )

            getInputPortMetaData@MetaJolie( rq )( meta_description )
            __port_type = "input"
            _get_ports
            input_ports_number = #files
            
            getOutputPortMetaData@MetaJolie( rq )( meta_description )
            __port_type = "output"
            _get_ports
            output_ports_number = #files - input_ports_number

            /* creating index */
            getMetaData@MetaJolie( rq )( metadata )
            for( i in metadata.input ) {
                startsWith@StringUtils( i.location { .prefix = "local://" } )( starts_with_local )
                if ( internals || ( i.location != "undefined" && i.location != "local" && !starts_with_local ) ) {
                    //index = index + "<tr><td class='itemmenu'><a onclick='loadPage(\"" + i.name + "IPort.html\")'>" + i.name + "</a></td></tr>"
                    inputs[ #inputs ] << i
                    index_rq.data.ipitems[ #index_rq.data.ipitems ].ipname = i.name
                }
            }
            for( o in metadata.output ) {
                startsWith@StringUtils( o.location { .prefix = "local://" } )( starts_with_local )
                if ( internals || ( o.location != "undefined" && o.location != "local" && !starts_with_local ) ) {
                    //ndex = index + "<tr><td class='itemmenu'><a onclick='loadPage(\"" + o.name + "OPort.html\")'>" + o.name + "</a></td></tr>"
                    outputs[ #outputs ] << o
                    index_rq.data.opitems[ #index_rq.data.opitems ].opname = o.name
                }
            }
            
            index_rq.data.filename = rq.filename
            index_rq.template = index_template
            getIndexPage@RenderDocPages( index_rq )( index )

            max_files = #files 
            files[ max_files ].filename = "index." + type
            files[ max_files ].html = index

            /* creating overview */
            minimum_ports = 3
            slot_height_for_port = 100
            trapezoid_delta = 80
            svg_width = 800
            svg_padding = 300
            iport_diagonal = 50
            text_displacement = 10

            max_port_number = minimum_ports

            if ( input_ports_number > minimum_ports || output_ports_number > minimum_ports ) {
                max_port_number = input_ports_number
                if ( output_ports_number > max_port_number ) {
                    max_port_number = output_ports_number
                }
            }

            body_height = slot_height_for_port * max_port_number
            svg_height = body_height + trapezoid_delta*2
            svg = "<svg height='" + svg_height + "' width='" + svg_width + "'>"
            // microservice body
            left_corner_top_y = svg_height - trapezoid_delta
            right_corners_x = svg_width -svg_padding

            left_corner_bottom = svg_padding + "," + trapezoid_delta
            left_corner_top = svg_padding + "," + left_corner_top_y
            right_corner_top = right_corners_x + "," + svg_height
            right_corner_bottom = right_corners_x + ",0"
            svg = svg + "<polygon points='" + left_corner_bottom + " " + left_corner_top + " " + right_corner_top + " " + right_corner_bottom + "' style='fill:#ffcc00;stroke:#cc6600;stroke-width:2' />"
            
    
            // microservice input ports
            if ( input_ports_number > 0 ) {
                ip_slot = int( body_height / input_ports_number )
                for( i = 0, i < input_ports_number, i++ ) {
                    iport_diagonal_half = int( iport_diagonal / 2 )
                    iport_height = trapezoid_delta + ip_slot * i + int( ip_slot / 2 )
                    left_corner_x = svg_padding - iport_diagonal_half
                    up_corner_y = iport_height + iport_diagonal_half
                    right_corner_x = svg_padding + iport_diagonal_half
                    bottom_corner_y = iport_height - iport_diagonal_half

                    left_corner = left_corner_x + "," + iport_height
                    up_corner = svg_padding + "," + up_corner_y
                    right_corner = right_corner_x + "," + iport_height
                    bottom_corner = svg_padding + "," + bottom_corner_y

                    svg = svg + "<polygon points='" + left_corner + " " + up_corner + " " + right_corner + " " + bottom_corner + " ' style='fill:#ffff66;stroke:#cc9900;stroke-width:2' />"
                    text_x = left_corner_x - text_displacement
                    text_y = iport_height - text_displacement
                    text_y2 = iport_height + text_displacement*2
                    protocol_text_x = left_corner_x + text_displacement
                    protocol_text_y = iport_height + int( text_displacement / 2 )
                    svg = svg + "<a xlink:href='" + inputs[ i ].name + "IPort.html'><text class='link' x='" + text_x + "' y='" + text_y + "' text-anchor='end' fill='black' font-family='Courier New' font-weight='bold'>" + inputs[ i ].name + "</text></a>"
                    svg = svg + "<text x='" + protocol_text_x + "' y='" + protocol_text_y + "'  font-size='10px' fill='black' font-family='Courier New'>" + inputs[ i ].protocol + "</text>"
                    svg = svg + "<text x='" + text_x + "' y='" + text_y2 + "' text-anchor='end'  font-size='10px' fill='black' font-family='Courier New'>" + inputs[ i ].location + "</text>"
                    svg = svg + "<line x1='0' y1='" + iport_height + "' x2='" + left_corner_x + "' y2='" + iport_height + "' style='stroke:#ddd;stroke-width:1'/>"
                }
            }

            // microservices output ports
            if ( output_ports_number > 0 ) {
                op_slot = int( body_height / output_ports_number )
                for( i = 0, i < output_ports_number, i++ ) {
                    iport_diagonal_half = int( iport_diagonal / 2 )
                    iport_height = trapezoid_delta + op_slot * i + int( op_slot / 2 )
                    up_corner_y = iport_height + iport_diagonal_half
                    basic_x = svg_width - svg_padding
                    right_corner_x = basic_x + iport_diagonal_half
                    bottom_corner_y = iport_height - iport_diagonal_half

                    up_corner = basic_x + "," + up_corner_y
                    right_corner = right_corner_x + "," + iport_height
                    bottom_corner = basic_x + "," + bottom_corner_y
                    text_x = right_corner_x + text_displacement
                    text_y = iport_height - text_displacement
                    text_y2 = iport_height + text_displacement*2

                    svg = svg + "<polygon points='" + up_corner + " " + right_corner + " " + bottom_corner + " '  style='fill:#ff0000;stroke:#000000;stroke-width:2' />"
                    svg = svg + "<a xlink:href='" + outputs[ i ].name + "OPort.html'><text class='link' x='" + text_x + "' y='" + text_y + "' fill='black' font-family='Courier New' font-weight='bold'>" + outputs[ i ].name + "</text></a>"
                    svg = svg + "<text x='" + text_x + "' y='" + text_y2 + "' fill='black' font-size='10px' font-family='Courier New'>" + outputs[ i ].location + "," + outputs[ i ].protocol + "</text>"
                    svg = svg + "<line x1='" + right_corner_x + "' y1='" + iport_height + "' x2='" + svg_width + "' y2='" + iport_height + "' style='stroke:#ddd;stroke-width:1'/>"
                }
            }
        
            svg = svg + "</svg>"

            ovh_template_req.template = ovh_template
            ovh_template_req.svgIcon = svg
            
            for( i = 0, i < #metadata.communication_dependencies, i++ ) {
                com -> metadata.communication_dependencies[ i ]
                ovh_template_req.communication_dependencies[ i ].operation_name = com.input_operation.name
                
                for( d = 0, d < #com.dependencies, d++ ) {
                    dep -> com.dependencies[ d ]
                    ovh_template_req.communication_dependencies[ i ].dependencies[ d ].name = dep.name
                    found_p = false
                    if ( is_defined( dep.port ) ) {
                        for ( tmp_o in outputs ) {
                            if ( tmp_o.name == dep.port ) { found_p = true }
                        }
                    } else { found_p = true }
                    if ( internals || found_p ) {
                        port_string = ""
                        if ( is_defined( dep.port ) ) {
                            ovh_template_req.communication_dependencies[ i ].dependencies[ d ].port = dep.port
                        }
                    } 
                }
            }
            
            max_files = #files 

            getOverviewPage@RenderDocPages( ovh_template_req )( ovw )
            files[ max_files ].filename = "Overview." + type
            files[ max_files ].html = ovw
        }

        exists@File( JOLIEDOC_FOLDER )( joliedoc_exists )
        if ( joliedoc_exists ) {
            deleteDir@File( JOLIEDOC_FOLDER )()
        }
        mkdir@File( JOLIEDOC_FOLDER )()

        for( file in files ) {
            f.filename = JOLIEDOC_FOLDER + "/" + file.filename;
            f.content -> file.html
            writeFile@File( f )(  )
        }
        println@Console("DONE")()

    }
}