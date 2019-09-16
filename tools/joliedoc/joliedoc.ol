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
            response = "<h1>" + request.name + "</h1>"
            response = response + "<span class='port-element'>Protocol:</sapn>&nbsp;" + request.protocol + "<br>"
            response = response + "<span class='port-element'>Location:</span>&nbsp;" + request.location + "<br>"
            response = response + "<span class='port-element'>Interfaces:</span><br>"
            for( i in request.interfaces ) {
                response = response + i.name + "<br>"
            }
        }]

        [ getInterface( request )( response ) {
            response = "<table class='interface-definition'>"
            response = response + "<tr><th class='resource-label-interface'>interface</th><th id='" + request.name + "' colspan='2' class='content-td'>" + request.name + "</th></tr>"
            if ( is_defined( request.documentation ) && request.documentation != "" ) {
                response = response + "<tr><td>&nbsp;</td></tr>"
                response = response + "<tr><td></td><td colspan='2' class='documentation'>" + request.documentation + "</td></tr>"
                response = response + "<tr><td>&nbsp;</td></tr>"
            } else {
                response = response + "<tr><td>&nbsp;</td></tr>"
            }
            for( o in request.operations ) {
                otype = " class='ow-type'>OW"
                if ( is_defined( o.output ) ) {
                     otype = " class='rr-type'>RR"
                } 
                response = response + "<tr><td" + otype + "</td><td><b>" + o.operation_name + "</b><br></td><td></td></tr>"
                getIndentation@Render( 25 )( indent )
                response = response + "<tr><td></td><td class='content-td'>" + indent + "<span class='message-type'>Request:</span><a href='#'" + o.input + "'>" + o.input + "</a><br>"
                if ( is_defined( o.output )  ) {
                    response = response + indent + "<span class='message-type'>Response:</span><a href='#'" + o.output + "'>" + o.output + "</a><br>"
                }
                response = response + "</td><td class='content-td'>"
                if ( is_defined( o.fault )) {
                    
                    response = response + indent + "<span class='message-type'>Faults:</span> "
                    for ( f in o.fault ) {
                        response = response + "<br>" + indent + "<span class='fault-name'>" + f.name + "</span>"
                        getFaultType@Render( f )( fname )
                        response = response + "(" + fname + ")"
                    }
                    response = response + "<br>"
                }
                response = response + "</td></tr>"
                response = response + "<tr><td></td><td colspan='2' class='documentation'>" + o.documentation + "</td><tr>"
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
            response = response + "<tr><th class='resource-label-type'>type</th><th id='" + request.name + "' colspan='2' class='content-td'>" + request.name + "</th></tr>"
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

init {
    css = "<style>
                body {
        font-family: Verdana, Helvetica, sans-serif;
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
         padding: 5px;
         border-radius:5px;
         width:5%;
	     text-align:center;
    }
    .content-td {
        padding-left: 20px;
        width: 30%;
        background-color: #eee;
        padding-bottom: 3px;
        padding-top: 3px;
        border-radius: 5px;
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
         background-color: green;
         color:white;
         padding: 5px;
         border-radius:5px;
         width:5%;
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
    .rr-type {
         background-color:#000;
        text-align:center;
        color:white;
        font-weight:bold;
        border-radius:5px;
    }
    .ow-type {
        background-color:#444;
        text-align:center;
        color:white;
        font-weight:bold;
        border-radius:5px;
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
    }
    .fault-name {
        font-size:14px;
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
            getPort@Render( inpt )( port )
            
            for( itf in inpt.interfaces ) {
                getInterface@Render( itf )( itfc )
                itfcs[ #itfcs ] = itfc
                for ( tp in itf.types ) {
                    getTypeDefinition@Render( tp )( tp_string )
                    tps[ #tps ] = tp_string
                }
            }
            html = "<html><head></head>" + css + "<body>" + port + "<hr>"
            for( i in itfcs ) {
                html = html + i 
            }
            html = html + "<hr>"
            for( t in tps ) {
                html = html + t
            }
            html = html + "</body></html>"
        }        
    }

    f.filename = "test.html"
    f.content -> html
    writeFile@File( f )(  )
}