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
        getFaultType( Fault )( string )
}

service Render {
    Interfaces: RenderInterface

    main {
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
            response = "<h1>" + request.name + "</h1>"
            response = response + "<span class='operations-title'>Operations:</span><br><ul class='operations-ul'>"
            for( o in request.operations ) {
                response = response + "<li>" + o.operation_name + "( " + o.input + ")"
                if ( is_defined( o.output )  ) {
                    response = response + "(" + o.output + ")"
                }
                if ( is_defined( o.fault )) {
                    response = response + " throws "
                    for ( f in o.fault ) {
                        response = response + f.name
                        getFaultType@Render( f )( fname )
                        response = response + "(" + fname + ")"
                    }
                }
                response = response + "</li><br>"
            }
            response = response + "</ul>"
        }]

        [ getTypeInLine( request )( response ) {
            getNativeType@Render( request.root_type )( response )
            if ( #request.sub_type > 0 ) {
                response = response + "{</td><td></td></tr>"
                for( s in request.sub_type ) {
                    getSubType@Render( s )( sub_type )
                    response = response + sub_type
                }
                response = response + "<tr><td>}</td><td></td></tr>"
            }
        }]

        [ getTypeDefinition( request )( response ) {
            response = "<table class='type-definition'>"
            response = response + "<tr><td><b>type</b> " + request.name + "<b>:</b>"
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
            response = response + "<tr><td>|&nbsp;" + r_type 
        }]

        [ getTypeLink( request )( response ) {
            response = request.link_name 
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
            response = response + "<tr><td>." + request.name 
            getCardinality@Render( request.cardinality )( cardinality )
            response = response + cardinality + ":"
            getType@Render( request.type )( type_string )
            response = response + type_string + "</td><td>" + request.documentation + "</td></tr>"
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
    .type-definition {
        mwrgin:10px;
        border:1px solid #ccc;
        width:100%;
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