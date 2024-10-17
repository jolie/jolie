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

constants {
    JOLIEDOC_FOLDER = "joliedoc"
}

interface RenderInterface {
    RequestResponse:
        getPort( Port )( string ),
        getInterface( Interface )( string ),
        getTypeDefinition( TypeDefinition )( string ),
        getNativeType( NativeType )( string ),
        getSubType( SubType )( string ),
        getTypeInLine( TypeInLine )( string ),
        getCardinality( Cardinality )( string ),
        getType( Type )( string ),
        getTypeLink( TypeLink )( string ),
        getTypeChoice( TypeChoice )( string ),
        getTypeUndefined( TypeUndefined )( string ),
        getFaultType( Fault )( string ),
        getIndentation( int )( string )
}

private service Render {
    Interfaces: RenderInterface

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
}

type GetOverviewPageRequest {
    svgIcon: string 
    communication_dependencies*: void {
        operation_name: string 
        dependencies*: void {
            name: string
            port?: string
        }
    }
}

interface TemplatesInterface {
    RequestResponse:
        getOverviewPage( GetOverviewPageRequest )( string )
}

private service Templates {

    inputPort Templates {
        location: "local"
        interfaces: TemplatesInterface
    }

    execution: concurrent 

    init {
        ovw_css = "<style>
            body {
                font-family:'Courier New';
            }
            .picture {
                padding-top:50px;
                text-align:center;
            }

            .link:hover {
                fill: #660000;
            }
            .dependency-map {
                padding:20px;
            }
            .input-operation {
                width:50%;
            }
        .dependency-table {
                width: 100%;
                border: 1px solid #444;
                padding: 10px;
            }

            .dependency-table a {
                color: black;
                text-decoration: none;
            }


            .dependency-table a:hover {
                color: #600;
            }

            .dependency-table th {
                text-align: left;
            }

            .dependency-list {
                background-color: #ddd;
                padding-left: 20%;
            }
            .legenda {
                font-size: 12px;
                font-style: italic;
                color: #888;
            }
        </style>"

        overview_template = "<html><head>" + ovw_css 
            + "</head><body><table width='100%'><tr><td valign='top' class='picture' width='50%'>{{{svgIcon}}}</td><td valign='top' class='dependency-map'>"
            + "<h1>Dependency Map</h1>"
            + "<span class='legenda'>Legenda:</span><br><table class='dependency-table'>"
            + "<tr><th class='input-operation'>input operation</th></tr><tr><td class='dependency-list'><i>communication dependencies</i></td></tr></table><br><br>"
            + "{{#communication_dependencies}}"
                + "<table class='dependency-table'><tr><td colspan='2' class='input-operation'>{{operation_name}}</td></tr>"
                + "<tr><td class='dependency-list'><table width='100%'>"
                    + "{{#dependencies}}"
                    + "<tr><td>{{name}}"
                        + "{{#port}}"
                        + "@<b><a href='{{port}}OPort.html'>{{port}}</a></b>"
                        + "{{/port}}"
                    + "</td></tr>"
                    + "{{/dependencies}}"
            + "{{/communication_dependencies}}"
            + "<td></table></body></html>"


    }

    main {
        [ getOverviewPage( request )( response ) {
            render@Mustache( {
                template = overview_template
                data << request 
            })( response )
        }]
    }
    
}

service JolieDoc {

    embed Console as Console
    embed MetaJolie as MetaJolie 
    embed StringUtils as StringUtils
    embed File as File
    embed Templates as Templates



    define _get_ports {
        // __port_type
        for( ptt in meta_description.( __port_type ) ) {
                startsWith@StringUtils( ptt.location { .prefix = "local://" } )( starts_with_local )
                if ( internals || ( ptt.location != "undefined" && ptt.location != "local" && !starts_with_local ) ) {
                    html = "";
                    getPort@Render( ptt )( port )
                    if ( __port_type == "input" ) {
                        replaceAll@StringUtils( port { .regex = "PORTTYPE", .replacement = "ip" } )( port )
                    } else {
                        replaceAll@StringUtils( port { .regex = "PORTTYPE", .replacement = "op" } )( port )
                    }
                    
                    undef( itfcs )
                    undef( tps )
                    for( itf in ptt.interfaces ) {
                        getInterface@Render( itf )( itfc )
                        itfcs[ #itfcs ] = itfc
                        for ( tp in itf.types ) {
                            getTypeDefinition@Render( tp )( tp_string )
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
                    files[ max_files ].html = html
                }
            }      
    }

    init {
        js = "<script>
            function openDetails( operation ) {
                if ( document.getElementById( \"doc_\" + operation ).style.display == \"none\" ) {
                    document.getElementById( \"doc_\" + operation ).style.display = \"table-row\";
                    document.getElementById( \"faults_\" + operation ).style.display = \"table-row\"
                } else {
                    document.getElementById( \"doc_\" + operation ).style.display = \"none\";
                    document.getElementById( \"faults_\" + operation ).style.display = \"none\"
                }
            }
        </script>"
        css = "<style>
                    body {
            font-family: Verdana, Helvetica, sans-serif;
        }

        .resource-label {
            height:40px;
            font-size: 14px;
            text-align:center;
            color:white;
            font-weight:bold;
            border-radius:5px;
            width:6%;
        }

        a {
            font-weight:bold;
            text-decoration:none;
            color: #600;
        }

        tr:hover td:not(:first-child) {
            background-color: #ffffcc;
        }


        .type-definition a:hover {
            color: #aa0000;
        }

        .type-definition {
            margin-left:30px;
            margin-top:50px;
            width:90%;
        }
        .type-definition th {
            font-size:20px;
            text-align: left;
            background-color: #fff;
            color:black;
            border-radius:5px;
        }
        .type-definition th.content-td{
            border-bottom:1px solid #555;
            border-radius:0px;
        }
        .type-definition tr td {

            border-bottom:1px dotted #ddd;
        }
        .type-definition tr td:first-child {
            border-bottom:0px;
        }

        .type-definition .resource-label-type {
            background-color: red;
            color:white;
            text-align:center;
            font-size:14px;
        }

        .content-td {
            padding-left: 20px;
            width: 30%;
            background-color: #eee;
            padding-bottom: 5px;
            padding-top: 5px;
            border-radius: 5px;
            font-size: 15px;
            font-family: 'Courier New';
        }
        .documentation {
            font-style: italic;
        }

        .interface-definition {
            margin-left:30px;
            margin-top:50px;
            width:90%;
        }

        .interface-definition .resource-label-interface {
            background-color: #107710;
            color:white;
            text-align:center;
            font-size:14px;
        }

        .interface-definition th {
            font-size:20px;
            text-align: left;
            background-color: #fff;
            color:black;
            border-radius:5px;
        }

        .interface-definition th.content-td{
            border-bottom:1px solid #555;
            border-radius:0px;
        }

        .operation-name {
            font-weight: bold;
            font-size:18px;
            padding-left:20px;
        }

        .operation-button {
            font-size: 10px;
            border: 1px solid #666;
            padding: 4px;
            margin-right: 10px;
            float: right;
            cursor: pointer;
        }

        .operation-button:hover {
            background-color:#666;
            color:white;
        }

        .rr-type {
            background-color:#929900;
            color:white;
        }

        .ow-type {
            background-color:#cca133;
            color:white;
        }

        .message-type {
            font-style:italic;
            font-size:12px;
            color:#444;
        }

        .interface-definition a {
            font-size: 14px;
        }

        .interface-definition .content-td {
            padding-top:10px;
            padding-bottom:10px;
            width:32%;
        }

        .fault-name {
            font-size:14px;
        }

            .port-definition a {
            font-size: 14px;
            color: #050;
        }

        .port-definition th.content-td{
            border-bottom:1px solid #555;
            border-radius:0px;
        }

        .port-definition .resource-label-ip {
            background-color: blue;
            color:white;
            text-align:center;
            font-size:14px;
        }

        .port-definition .resource-label-op {
            background-color: black;
            color:white;
            text-align:center;
            font-size:14px;
        }

        .port-definition th {
            font-size: 20px;
            text-align: left;
            background-color: #fff;
            color: black;
        }

        .port-definition {
            margin-left:30px;
            margin-top:50px;
            width:90%;
        }

        .port-element {
            font-weight: bold;
        }

        .port-definition .content-td {
            width:44%;
        }
        </style>"

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

            getOverviewPage@Templates( ovh_template_req )( ovw )
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