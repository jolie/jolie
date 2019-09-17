include "metajolie.iol"
include "console.iol"
include "types/definition_types.iol"
include "file.iol"

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

service Render {
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
            response = response + "<tr><th class='resource-label-port resource-label'>port</th><th id='" + request.name + "' colspan='2' class='content-td'>" + request.name + "</th></tr>"
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
            response = response + "<tr><th id='" + request.name + "' class='resource-label-interface resource-label'>interface</th><th id='" + request.name + "' colspan='3' class='content-td'>" + request.name + "</th></tr>"
            if ( is_defined( request.documentation ) && request.documentation != "" ) {
                response = response + "<tr><td>&nbsp;</td></tr>"
                response = response + "<tr><td></td><td colspan='4' class='documentation'>" + request.documentation + "</td></tr>"
                response = response + "<tr><td>&nbsp;</td></tr>"
            } else {
                response = response + "<tr><td>&nbsp;</td></tr>"
            }
            for( o in request.operations ) {
                otype = " class='resource-label ow-type'>async"
                if ( is_defined( o.output ) ) {
                     otype = " class='resource-label rr-type'>sync"
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
                response = response + "<tr id='doc_" + o.operation_name + "' style='display:none'><td></td><td colspan='3' class='documentation'>" + o.documentation + "</td><tr>"
                response = response + "<tr><td><br></td></tr>"
            }
            response = response + "</table>"
        }]

        [ getTypeInLine( request )( response ) {
            getNativeType@Render( request.root_type )( response )
            if ( #request.sub_type > 0 ) {
                response = response + "{</td><td></td></tr>"
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
            getType@Render( request.type )( type_string )
            response = response + type_string + "</td><td class='documentation'>" + request.documentation + "</td></tr>"
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

constants {
    JOLIEDOC_FOLDER = "joliedoc"
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

    .port-definition .resource-label-port {
        background-color: blue;
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

    css_header = "<style>
        body {
            font-family: 'Courier New';
            font-size:14px;
        }

        .headertable {
            width:100%;
            heigth:50px;
            font-size:30px;
            padding:10px;
        }

        .menutd {
            width:20%;
        }

        .bodytd {
            width:80%;
        }
    </style>"
}

main {
    if ( #args != 1 ) {
        println@Console( "Usage: joliedoc <filename>")()
    } else {
        rq.filename = args[ 0 ]
        getInputPortMetaData@MetaJolie( rq )( meta_description )

        for( inpt in meta_description.input ) {
            html = "";
            getPort@Render( inpt )( port )
            
            for( itf in inpt.interfaces ) {
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
            files[ max_files ].filename = inpt.name + "Port.html"
            files[ max_files ].html = html
        }        

        /* creating index */
        getMetaData@MetaJolie( rq )( metadata )


        index = "<html><head></head><body><table class='headertable'><tr><td>Jolie Documenation:&nbsp;" + args[ 0 ] + "</td></tr></table>"
        index = index + "<table><tr><td class='menutd'></td>"
        index = index + "<td class='bodytd'></td></tr></table></body></html>"

        max_files = #files 
        files[ max_files ].filename = "index.html"
        files[ max_files ].html = index
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