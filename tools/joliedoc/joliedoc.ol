include "metajolie.iol"
include "console.iol"
include "types/definition_types.iol"

interface RenderInterface {
    RequestResponse:
        getPort( Port )( string )
}

service Render {
    Interfaces: RenderInterface

    main {
        [ getPort( request )( response ) {
            response = "<h1>" + request.name + "</h1>"
            response = response + "<b>Protocol:</b>&nbsp;" + request.protocol + "<br>"
            response = response + "<b>Location:</b>&nbsp;" + request.location + "<br>"
            response = response + "<b>Interfaces:</b><br>"
            for( i in request.interfaces ) {
                response = response + i.name + "<br>"
            }
        }]
    }
}

main {
    if ( #args != 1 ) {
        println@Console( "Usage: joliedoc <filename>")()
    } else {
        rq.filename = args[ 0 ]
        getInputPortMetaData@MetaJolie( rq )( meta_description )

        for( i in metadescription.input ) {
            getPort@Render( i )( port )
        }

        println@Console( port )()
    }
}