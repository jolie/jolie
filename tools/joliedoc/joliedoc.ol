from console import Console
from metajolie import MetaJolie
from file import File
from string-utils import StringUtils
from mustache import Mustache

from types.definition-types import Port
from types.definition-types import Interface
from types.definition-types import TypeDefinition
from types.definition-types import NativeType
from types.definition-types import SubType
from types.definition-types import TypeInLine
from types.definition-types import Cardinality
from types.definition-types import Type
from types.definition-types import TypeLink
from types.definition-types import TypeChoice
from types.definition-types import TypeUndefined
from types.definition-types import Fault


// redefinition of metajolie types for mustahce templates
type __RangeInt: void {
  min: int 
  max*: int
} | void {
  min: int 
  infinite: bool
}

type __RangeDouble: void {
  min: double 
  max*: double
} | void {
  min: double 
  infinite: bool
}


type __RangeLong: void {
  min: long 
  max*: long
} | void {
  min: long 
  infinite: bool
}

type __StringRefinedType: void {
  length: __RangeInt
} | void {
  enum[1,*]: string
} | void {
  regex: string
}

type __IntRefinedType: void {
  ranges[1,*]: __RangeInt
}

type __DoubleRefinedType: void {
  ranges[1,*]: __RangeDouble
}

type __LongRefinedType: void {
  ranges[1,*]: __RangeLong
}

type __NativeType: void {
  .string_type: bool {
    .refined_type*: __StringRefinedType
  }
} | void {
  .int_type: bool {
    .refined_type*: __IntRefinedType
  }
} | void {
  .double_type: bool {
    .refined_type*: __DoubleRefinedType
  }
} | void {
  .any_type: bool
} | void {
  .void_type: bool
} | void {
  .raw_type: bool
} | void {
  .bool_type: bool
} | void {
  .long_type: bool {
    .refined_type?: __LongRefinedType
  }
} 


type __Cardinality: void {
  .min: int
  .max?: int
  .infinite?: int
}

type __SubType: void {
  .name: string
  .cardinality: __Cardinality
  .type: Type
  .documentation?: string
}

type __TypeInLine: void {
  .root_type: __NativeType
  .sub_type*: __SubType
}

type __TypeLink: void {
  .link_name: string
}

type __TypeChoice: void {
  .choice: void {
      .left_type: __TypeInLine | __TypeLink 
      .right_type: __Type
  }
}

type __TypeUndefined: void {
  .undefined: bool
}

type __Type: __TypeInLine | __TypeLink | __TypeChoice | __TypeUndefined 

type __TypeDefinition: void {
  .name: string
  .type: __Type
  .documentation?: string
}


type __Fault: void {
  .name: string
  .type: __NativeType | __TypeUndefined | __TypeLink
}

type __Operation: void {
  .operation_name: string
  .documentation?: string
  .input: string
  .output?: string
  .fault*: __Fault
}

type __Interface: void {
  .name: string
  .types*: __TypeDefinition
  .operations*: __Operation
  .documentation?: string
}

type Port: void {
  .name: string
  .protocol: string
  .location: any
  .interfaces*: __Interface
}

type __Service: void {
  .name: string
  .input*: string
  .output*: string
}


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

interface RenderDocInterface {
    RequestResponse:
        getOverviewPage( GetOverviewPageRequest )( string ),
        getPortPage( GetPortPageRequest )( string ),
        getPort( Port )( __Port ),
        getInterface( Interface )( __Interface ),
        getTypeDefinition( TypeDefinition )( __TypeDefinition ),
        getNativeType( NativeType )( __NativeType ),
        getSubType( SubType )( __SubType ),
        getTypeInLine( TypeInLine )( __TypeInLine ),
        getCardinality( Cardinality )( __Cardinality ),
        getType( Type )( __Type ),
        getTypeLink( TypeLink )( __TypeLink ),
        getTypeChoice( TypeChoice )( __TypeChoice ),
        getTypeUndefined( TypeUndefined )( __TypeUndefined ),
        getFaultType( Fault )( __Fault )
}

service RenderDocPages {

    embed Mustache as Mustache
    embed StringUtils as StringUtils

    inputPort RenderDocPages {
        location: "local"
        interfaces: RenderDocInterface
    }

    execution: concurrent 


    main {

        [ getFaultType( request )( response ) {
            
        }]

        [ getPort( request )( response ) {

        }]

        [ getInterface( request )( response ) {

        }]

        [ getTypeInLine( request )( response ) {

        }]

        [ getTypeDefinition( request )( response ) {

        }]

        [ getType( request )( response ) {

        }]

        [ getTypeChoice( request )( response ) {

        }]

        [ getTypeLink( request )( response ) {
            
        }]

        [ getTypeUndefined( request )( response ) {
            
        }]

        [ getNativeType( request )( response ) {

        }]

        [ getSubType( request )( response ) {

        }]

        [ getCardinality( request )( response ) {

        }]
        
        [ getOverviewPage( request )( response ) {
            render@Mustache( {
                template = request.template
                data << request 
            })( response )
        }]

        [ getPortPage( request )( response ) {
            for ( itf in request.port.interfaces ) {
                for( o in itf.operations ) {
                    replaceAll@StringUtils( o.documentation { .regex = "\n", .replacement = "<br>" } )( o.documentation )
                }
            }
            
            render_req << {
                template = request.template
                data << request 
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

                    portPage = getPortPage@RenderDocPages( { 
                        template = port_page_template
                        port_type = __port_type
                        port << ptt
                    })
                    
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
                        endname = "IPort.html"
                    } else {
                        endname = "OPort.html"
                    }
                    files[ max_files ].filename = ptt.name + endname
                    files[ max_files ].html = portPage
                }
            }      
    }

    init {
        

        css_index = "<style>
            body {
                font-family: 'Courier New';
                font-size: 14px;
            }

            .headertable {
                width: 100%;
                font-size: 30px;
                padding: 10px;
                border-bottom: 1px dotted #444;
            }

            .menutd {
                width: 12%;
                border-right: 1px dotted #444;
                height: 1000px;
            }

            .bodytd {
                width: 80%;
            }

            .bodytd object {
                width:100%;
                height:100vh;
            }
            .maintable {
                width: 100%;
            }

            .menutitle {
                font-weight: bold;
                font-size: 20px;
            }

            .menutd a {
                cursor: pointer;
            }

            .itemmenu {
                text-align: center;
                width:100%;
            }

            .itemmenu a:hover {
                color:#600;
            }
            </style>"
        js_index = "<script>
            function loadPage( page ) {
                document.getElementById(\"bodytd\").innerHTML='<object type=\"text/html\" data=\"' + page + '\"></object>';
            }
        </script>"

        

        index_template = "<html><head>" + css_index + js_index + "</head><body><table class='headertable'><tr><td>Jolie Documentation:&nbsp;<b>{{filename}}</b></td></tr></table>"
            + "<table class='maintable'><tr><td  valign='top' class='menutd'><br><br>"
            + "<table width='100%'><tr><td class='itemmenu'><a onclick='loadPage(\"Overview.html\")'>OVERVIEW</a></td></tr></table><br><br>"
            + "<table width='100%'><tr><td class='itemmenu menutitle'>input ports</td></tr></table><br>"
            + "<table width='100%'>"
            + "  {{#ipitems}}
                    <tr><td class='itemmenu'><a onclick='loadPage(\"{{ipname}}IPort.html\")'>{{ipname}}</a></td></tr>"
                + "{{/ipitems}}"
            + "</table><br><br>"
            + "<table width='100%'><tr><td class='itemmenu menutitle'>output ports</td></tr></table><br>"
            + "<table width='100%'>"
            + "{{#opitems}}"
            + "<tr><td class='itemmenu'><a onclick='loadPage(\"{{opname}}OPort.html\")'>{{opname}}</a></td></tr>"
            + "{{/opitems}}"
            + "</table><br><br>"
            + "</td><td id='bodytd' class='bodytd'></td></tr></table></body><script>loadPage('Overview.html')</script></html>"



        install( Abort => nullProcess )
    }

    init {
        getServiceDirectory@File()( joliedoc_directory )
        readFile@File( { filename = joliedoc_directory + "/overview-page-template.html" } )( ovh_template )
        readFile@File( { filename = joliedoc_directory + "/port-page-template.html" } )( port_page_template )
    }

    main {
        
        if ( #args == 0 || #args > 2 ) {
            println@Console( "Usage: joliedoc <filename> [--internals ]")()
        } else {
            internals = false
            if ( #args == 2 && args[ 1 ] == "--internals" ) {
                internals = true
            } else if ( #args == 2 ) {
                println@Console("Argument " + args[ 1 ] + " not recognized" )()
                throw( Abort )
            }
            rq.filename = args[ 0 ]
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
            /*index = "<html><head>" + css_index + js_index + "</head><body><table class='headertable'><tr><td>Jolie Documentation:&nbsp;<b>" + args[ 0 ] + "</b></td></tr></table>"
            index = index + "<table class='maintable'><tr><td  valign='top' class='menutd'><br><br>"
            index = index + "<table width='100%'><tr><td class='itemmenu'><a onclick='loadPage(\"Overview.html\")'>OVERVIEW</a></td></tr></table><br><br>"
            index = index + "<table width='100%'><tr><td class='itemmenu menutitle'>input ports</td></tr></table><br>"
            index = index + "<table width='100%'>"*/
            for( i in metadata.input ) {
                startsWith@StringUtils( i.location { .prefix = "local://" } )( starts_with_local )
                if ( internals || ( i.location != "undefined" && i.location != "local" && !starts_with_local ) ) {
                    //index = index + "<tr><td class='itemmenu'><a onclick='loadPage(\"" + i.name + "IPort.html\")'>" + i.name + "</a></td></tr>"
                    inputs[ #inputs ] << i
                    index_data.ipitems[ #index_data.ipitems ].ipname = i.name
                }
            }
            /*index = index + "</table><br><br>"
            index = index + "<table width='100%'><tr><td class='itemmenu menutitle'>output ports</td></tr></table><br>"
            index = index + "<table width='100%'>"*/
            for( o in metadata.output ) {
                startsWith@StringUtils( o.location { .prefix = "local://" } )( starts_with_local )
                if ( internals || ( o.location != "undefined" && o.location != "local" && !starts_with_local ) ) {
                    //ndex = index + "<tr><td class='itemmenu'><a onclick='loadPage(\"" + o.name + "OPort.html\")'>" + o.name + "</a></td></tr>"
                    outputs[ #outputs ] << o
                    index_data.opitems[ #index_data.opitems ].opname = o.name
                }
            }
            /*index = index + "</table><br><br>"
            index = index + "</td><td id='bodytd' class='bodytd'></td></tr></table></body><script>loadPage('Overview.html')</script></html>"*
            */

            max_files = #files 
            //files[ max_files ].filename = "index.html"
            //files[ max_files ].html = index

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
            //ovw = "<html><head>" + ovw_css + "</head><body><table width='100%'><tr><td valign='top' class='picture' width='50%'>" + svg + "</td><td valign='top' class='dependency-map'>"
            //ovw = ovw + "<h1>Dependency Map</h1>"
            //ovw = ovw + "<span class='legenda'>Legenda:</span><br><table class='dependency-table'><tr><th class='input-operation'>input operation</th></tr><tr><td class='dependency-list'><i>communication dependencies</i></td></tr></tr></table><br><br>"
            for( i = 0, i < #metadata.communication_dependencies, i++ ) {
                com -> metadata.communication_dependencies[ i ]
                ovh_template_req.communication_dependencies[ i ].operation_name = com.input_operation.name
                
                //ovw = ovw + "<table class='dependency-table'><tr><td colspan='2' class='input-operation'>" + com.input_operation.name + "</td></tr>"
                //ovw = ovw + "<tr><td class='dependency-list'><table width='100%'>"
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
                           // port_string = "@<b><a href='" + dep.port + "OPort.html'>" + dep.port + "</a></b>"
                        }
                        //ovw = ovw + "<tr><td>" + dep.name + port_string + "</td></tr>"
                    } 
                }
                //ovw = ovw + "</table></td></tr></table>"
            }
            //ovw = ovw + "<td></table></body></html>"
            max_files = #files 

            getOverviewPage@RenderDocPages( ovh_template_req )( ovw )
            files[ max_files ].filename = "Overview.html"
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

    }
}