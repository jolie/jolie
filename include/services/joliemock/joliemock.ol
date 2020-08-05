include "public/interfaces/JolieMockInterface.iol"
include "metarender.iol"
include "console.iol"
include "string_utils.iol"

execution{ concurrent }


type GetTypeDefinitionRenderRequest: void {
    types*: TypeDefinition
    type_name: string
    path: string 
    vector_depth: int
}

type GetTypeRenderRequest: void {
    types*: TypeDefinition
    type: Type
    path: string
    vector_depth: int
}

type GetTypeLinkRenderRequest: void {
    types*: TypeDefinition
    type: TypeLink
    path: string
    vector_depth: int
}

type GetSubTypeRenderRequest: void {
    types*: TypeDefinition
    sub_type: SubType
    path: string
    vector_depth: int
}


interface RenderResponseTypeInterface {
    RequestResponse:
        getTypeDefinitionRender( GetTypeDefinitionRenderRequest )( string ),
        getTypeRender( GetTypeRenderRequest)( string ),
        getTypeLinkRender( GetTypeLinkRenderRequest )( string ),
        getNativeTypeRender( NativeType )( string ),
        getSubTypeRender( GetSubTypeRenderRequest )( string )
}

service RenderResponseType {
    Interfaces: RenderResponseTypeInterface 

    main {
        [ getTypeDefinitionRender( request )( response ) {
            rq.vector_depth = request.vector_depth
            for ( t in request.types ) {
                if ( t.name == request.type_name ) { rq.type << t.type }
            }
            rq.types -> request.types 
            rq.path = request.path
            getTypeRender@RenderResponseType( rq )( response )
        } ]

        [ getTypeLinkRender( request )( response ) {
            rq.vector_depth = request.vector_depth
            for ( t in request.types ) {
                if ( t.name == request.type.link_name ) { rq.type << t.type }
            }
            rq.types -> request.types
            rq.path = request.path
            getTypeRender@RenderResponseType( rq )( response )
        } ]

        [ getNativeTypeRender( request )( response ) {
             if ( is_defined( request.string_type ) ) {
                 response = "STRING_CONST"
             } else if ( is_defined( request.int_type ) ) {
                 response = "INT_CONST"
             } else if ( is_defined( request.double_type ) ) {
                 response = "DOUBLE_CONST"
             } else if ( is_defined( request.any_type ) ) {
                 response = "ANY_CONST"
             } else if ( is_defined( request.void_type ) ) {
                 response = "VOID_CONST"
             } else if ( is_defined( request.raw_type ) ) {
                 response = "RAW_CONST"
             } else if ( is_defined( request.bool_type ) ) {
                 response = "BOOL_CONST"
             } else if ( is_defined( request.long_type ) ) {
                 response = "LONG_CONST"
             }
        }]

        [ getSubTypeRender( request )( response ) {
            cardinality -> request.sub_type.cardinality
            if ( cardinality.min == 0 ) {
                if ( is_defined( cardinality.max ) && ( cardinality.max < request.vector_depth ) ) {
                    max_vector = cardinality.max 
                } else {
                    max_vector = request.vector_depth
                }
            } else {
                max_vector = cardinality.min
            }

            for ( i = 0, i < max_vector, i++ ) {
                undef( rq )
                rq.vector_depth = request.vector_depth
                rq.path = request.path + "." + request.sub_type.name + "[ " + i + " ]"
                rq.types -> request.types
                rq.type -> request.sub_type.type
                getTypeRender@RenderResponseType( rq )( type_string )
                response = response + type_string
            }
        }]

        [ getTypeRender( request )( response ) {
            rq.vector_depth = request.vector_depth
            rq.types -> request.types
            if ( request.type instanceof TypeUndefined ) {
                response = "\t" + request.path + ".undefined = \"undefined type\"\n"
            } else if ( request.type instanceof TypeLink ) {
                rq.type -> request.type
                rq.path = request.path
                getTypeLinkRender@RenderResponseType( rq )( response )
            } else if ( request.type instanceof TypeChoice ) {
                rq.type -> request.choice.left
                rq.path = request.path
                getTypeRender@RenderResponseType( rq )( response )
            } else if ( request.type instanceof TypeInLine ) {
                getNativeTypeRender@RenderResponseType( request.type.root_type )( native_string )
                response = request.path + " = " + native_string + "\n"
                if ( #request.type.sub_type > 0 ) {
                    for( st in request.type.sub_type ) {
                        rq.sub_type << st
                        rq.path = request.path
                        getSubTypeRender@RenderResponseType( rq )( st_string )
                        response = response + st_string
                    }
                } 

            }

        }]
    }
}

inputPort JolieMock {
    Location: "local"
    Protocol: sodep
    Interfaces: JolieMockInterface
}


main {
    [ getMock( request )( response ) {
        if ( is_defined( request.vector_depth ) ) { rq.vector_depth = request.vector_depth }
        else { rq.vector_depth = 5 }
        getSurfaceWithoutOutputPort@MetaRender( request.input )( surface )
        getInputPort@MetaRender( request.input )( iport )
        init_string = "
        include \"converter.iol\"
        init {
            STRING_CONST = \"mock_string\"
            INT_CONST = 42
            DOUBLE_CONST = 42.42
            stringToRaw@Converter(\"hello\")( RAW_CONST )
            ANY_CONST = \"mock any\"
            BOOL_CONST = true
            LONG_CONST = 42L
            VOID_CONST = Void
        }\n\n
        "
        main_string = "\n\nmain {\n"

        // generation of main
        for( itf in request.input.interfaces ) {
            for( o in itf.operations ) {
                main_string = main_string + "[ " + o.operation_name + "( " + o.input + ")"
                if ( is_defined( o.output ) ) {
                    main_string = main_string + "( " + o.output + " ) {\n"
                    main_string = main_string + "\tvalueToPrettyString@StringUtils( request )( s ); println@Console( s )()\n"
                    rq.types -> itf.types; rq.type_name = o.output; rq.path = "\tresponse"
                    getTypeDefinitionRender@RenderResponseType( rq )( odef )
                    main_string = main_string + odef + "}"
                }
                main_string = main_string + "]\n\n"
            }
        }


        main_string = main_string + "\n}"
        response = surface + "\ninclude \"console.iol\"\ninclude \"string_utils.iol\"\nexecution{ concurrent }\n" 
        + iport + init_string +main_string 
    } ]

}