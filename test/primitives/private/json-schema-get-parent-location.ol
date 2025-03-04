/*
The MIT License (MIT)
Copyright (c) 2025 Claudio Guidi <guidiclaudio@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/


from types.definition-types import TypeDefinition
from types.definition-types import Type
from types.definition-types import TypeInLine
from types.definition-types import TypeChoice
from types.definition-types import TypeLink 
from types.definition-types import SubType
from types.definition-types import NativeType
from types.definition-types import IntRefinedType
from types.definition-types import DoubleRefinedType
from types.definition-types import LongRefinedType
from types.definition-types import TypeUndefined

from string-utils import StringUtils



type GetSchemasResponse: void {
  definitions*: undefined
}

type GetNumberRefinedTypeVersionedRequest: IntRefinedType | DoubleRefinedType | LongRefinedType

type MinMax {
    minimum?: int | long | double 
    maximum?: int | long | double 
}

type GetNumberRefinedTypeResponse {
    anyOf*: MinMax
} | {
    MinMax
}

type SchemaVersion: string( enum(["2.0","3.0"]) )

type GetTypeDefinitionRequest: void {
    schemaVersion: SchemaVersion
    typeDefinition: TypeDefinition
}

type GetTypeRequest: void {
    schemaVersion: SchemaVersion
    type: Type
}

type GetTypeInLineRequest: void {
    schemaVersion: SchemaVersion
    typeInLine: TypeInLine
}

type GetTypeChoiceRequest: void {
    schemaVersion: SchemaVersion
    typeChoice: TypeChoice
}

type GetTypeLinkRequest: void {
    schemaVersion: SchemaVersion
    typeLink: TypeLink
}

type GetSubType: void {
    schemaVersion: SchemaVersion
    subType: SubType
}

type GetNativeTypeRequest: void {
    schemaVersion: SchemaVersion
    nativeType: NativeType  
}

type GetNumberRefinedTypeRequest: void {
    schemaVersion: SchemaVersion
    numberRefined: GetNumberRefinedTypeVersionedRequest
}

interface JSONSchemaGeneratorInterface {
 RequestResponse:
   getTypeDefinition( GetTypeDefinitionRequest )( undefined ) throws GenerationError( string ),
   getType( GetTypeRequest )( undefined ) throws GenerationError( string ),
   getTypeInLine( GetTypeInLineRequest )( undefined ) throws GenerationError( string ),
   getTypeChoice( GetTypeChoiceRequest )( undefined ) throws GenerationError( string ),
   getTypeLink( GetTypeLinkRequest )( undefined ) throws GenerationError( string ),
   getSubType( GetSubType )( undefined ) throws GenerationError( string ),
   getNativeType( GetNativeTypeRequest )( undefined ) throws GenerationError( string ),
   getNumberRefinedType( GetNumberRefinedTypeRequest )( GetNumberRefinedTypeResponse ) throws GenerationError( string )
}



private service JsonSchema2 {

    embed StringUtils as StringUtils

    execution: concurrent

    outputPort Parent {
        location: "local://internalJsonSchema"
        interfaces: JSONSchemaGeneratorInterface
    }


    inputPort JSONSchemaGenerator2 {
        location: "local"
        interfaces: JSONSchemaGeneratorInterface
    }

    init {
        install( GenerationError => nullProcess )
    }

    main {
        [ getNumberRefinedType( request )( response ) {
                if ( #request.numberRefined.ranges > 1 ) {
                    throw( GenerationError, "Multiple ranges cannot be converted in openApi 2.0")
                } else {
                    if ( is_defined( request.numberRefined.ranges.min ) ) { response.minimum = request.ranges.min }
                    if ( is_defined( request.numberRefined.ranges.max ) ) { response.maximum = request.ranges.max }
                }
        }]

        [ getTypeChoice( request )( response ) {
                throw( GenerationError, "TypeChoice cannot be converted in openApi 2.0")
        }]

        [ getTypeLink( request )( response ) {
                response.("$ref") = "#/definitions/" + request.typeLink.link_name
        }]
    }
}

private service JsonSchema3 {

    embed StringUtils as StringUtils

    execution: concurrent

    outputPort Parent {
        location: "local://internalJsonSchema"
        interfaces: JSONSchemaGeneratorInterface
    }


    inputPort JSONSchemaGenerator3 {
        location: "local"
        interfaces: JSONSchemaGeneratorInterface
    }

    init {
        install( GenerationError => nullProcess )
    }

    main {
        [ getNumberRefinedType( request )( response ) {
                if ( #request.numberRefined.ranges > 1 ) {
                    for ( r in request.numberRefined.ranges ) {
                        index = #response.anyOf
                        if ( is_defined( r.min ) ) { response.anyOf[ index ].minimum = r.min }
                        if ( is_defined( r.max ) ) { response.anyOf[ index ].maximum = r.max }
                    }
                } else {
                    if ( is_defined( request.numberRefined.ranges.min ) ) { response.minimum = request.ranges.min }
                    if ( is_defined( request.numberRefined.ranges.max ) ) { response.maximum = request.ranges.max }
                }
        }]

        [ getTypeChoice( request )( response ) {
                getType@Parent( {
                    schemaVersion = request.schemaVersion
                    type << request.typeChoice.choice.left_type
                } )( left )
                getType@Parent( {
                    schemaVersion = request.schemaVersion
                    type << request.typeChoice.choice.right_type 
                })( right )
                response.oneOf[ 0 ] << left
                if ( is_defined( right.oneOf ) ) {
                    for( o = 0, o < #right.oneOf, o++ ) {
                        response.oneOf[ o + 1 ] << right.oneOf[ o ]
                    }
                } else {
                    response.oneOf[ 1 ] << right 
                }
        }]

        [ getTypeLink( request )( response ) {
                response.("$ref") = "#/components/schemas/" + request.typeLink.link_name
        }]
    }
}


/* this service converts jolie types into json schema definitions */
service JsonSchema {

    execution: concurrent

    embed StringUtils as StringUtils
    embed JsonSchema2 as JsonSchema2
    embed JsonSchema3 as JsonSchema3

    outputPort MySelf {
        location: "local://internalJsonSchema"
        interfaces: JSONSchemaGeneratorInterface
    }

    inputPort JSONSchemaGeneratorInternal {
        location: "local://internalJsonSchema"
        interfaces: JSONSchemaGeneratorInterface
    }

    inputPort JSONSchemaGenerator {
        location: "local"
        interfaces: JSONSchemaGeneratorInterface
    }

    init {
        install( GenerationError => nullProcess )
    }

    main
    {

        [ getTypeDefinition( request )( response ) {
            scope( generation ) {
                install( GenerationError => throw( GenerationError,request.typeDefinition.name + ":" + generation.GenerationError ) )
                getType@MySelf( {
                    schemaVersion = request.schemaVersion
                    type << request.typeDefinition.type         
                })( response.( request.typeDefinition.name ) )
            }
        }] 

        [ getNumberRefinedType( request )( response ) {
                if ( request.schemaVersion == "2.0") { getNumberRefinedType@JsonSchema2( request )( response ) }
                if ( request.schemaVersion == "3.0") { getNumberRefinedType@JsonSchema3( request )( response ) }
        }]

        [ getType( request )( response ) {
                if ( request.type instanceof TypeInLine ) { getTypeInLine@MySelf( {
                    schemaVersion = request.schemaVersion 
                    typeInLine << request.type
                })( response ) } 
                else if ( request.type instanceof TypeLink ) { 
                    getTypeLink@MySelf( {
                        schemaVersion = request.schemaVersion 
                        typeLink << request.type
                    } )( response ) } 
                else if ( request.type instanceof TypeChoice ) { getTypeChoice@MySelf( {
                    schemaVersion = request.schemaVersion 
                    typeChoice << request.type
                } )( response ) }
                else if ( request.type instanceof TypeUndefined ) {
                    response.type = "object"
                }
        }]

        [ getTypeInLine( request )( response ) {   
                getNativeType@MySelf( {
                    schemaVersion = request.schemaVersion 
                    nativeType << request.typeInLine.root_type 
                })( resp_root_type )
                if ( #request.typeInLine.sub_type > 0 ) { response.type = "object" } 
                else { response << resp_root_type }
                
                /* analyzing sub types */
                if ( #request.typeInLine.sub_type > 0 ) {
                    for( st in request.typeInLine.sub_type ) {
                        getSubType@MySelf( {
                            schemaVersion = request.schemaVersion 
                            subType << st 
                        })( resp_sub_type )
                        response.properties.( st.name ) << resp_sub_type
                    }
                }    
        } ]

        [ getTypeLink( request )( response ) {
                if ( request.schemaVersion == "2.0") { getTypeLink@JsonSchema2( request )( response ) }
                if ( request.schemaVersion == "3.0") { getTypeLink@JsonSchema3( request )( response ) }
        }]

        [ getTypeChoice( request )( response ) {
                if ( request.schemaVersion == "2.0") { getTypeChoice@JsonSchema2( request )( response ) }
                if ( request.schemaVersion == "3.0") { getTypeChoice@JsonSchema3( request )( response ) }
        }]

        [ getSubType( request )( response ) {

                getType@MySelf( {
                    schemaVersion = request.schemaVersion
                    type << request.subType.type 
                })( typedef )
                
                if ( request.subType.cardinality.min  == 1 && request.subType.cardinality.max == 1 ) {
                    response << typedef
                } else {
                    response << {
                        items << typedef
                        type = "array"
                        minItems = request.subType.cardinality.min
                    }
                    if ( is_defined( request.subType.cardinality.max ) ) {
                        response.maxItems = request.subType.cardinality.max
                    }
                }
        } ]



        [ getNativeType( request )( response ) {
            if ( is_defined( request.nativeType.string_type ) ) {
                response.type = "string"
                st_type -> request.nativeType.string_type 
                
                if ( is_defined( st_type.refined_type ) ) {
                    ref_t -> st_type.refined_type
                    if ( is_defined( ref_t.length ) ) {
                        response << {
                            minLength = ref_t.length.min
                            maxLength = ref_t.length.max
                        }
                    }
                    if ( is_defined( st_type.refined_type.regex ) ) {
                        // OpenAPI needs REs embedded in ^...$ (choices need to put under parentheses)
                        if ( contains@StringUtils( ref_t.regex { substring = "|" } ) ) {
                            response.pattern = "^(" + ref_t.regex + ")$"
                        } else {
                            response.pattern = "^" + ref_t.regex + "$"
                        }
                    }
                    if ( is_defined( ref_t.enum ) ) { response.enum << ref_t.enum }
                }
                
            } else if ( is_defined( request.nativeType.int_type ) ) {
                if ( is_defined( request.nativeType.int_type.refined_type ) ) {
                    getNumberRefinedType@MySelf( {
                        schemaVersion = request.schemaVersion
                        numberRefined << request.nativeType.int_type.refined_type 
                    })( response )
                }
                response.type = "integer"
            } else if ( is_defined( request.nativeType.long_type ) ) {
                if ( is_defined( request.nativeType.long_type.refined_type ) ) {
                    getNumberRefinedType@MySelf( {
                        schemaVersion = request.schemaVersion
                        numberRefined << request.nativeType.long_type.refined_type 
                    })( response )
                }
                response.type = "number"
                response.format = "int64"
            } else if ( is_defined( request.nativeType.double_type ) ) {
                if ( is_defined( request.nativeType.double_type.refined_type ) ) {
                    getNumberRefinedType@MySelf( {
                        schemaVersion = request.schemaVersion
                        numberRefined << request.nativeType.double_type.refined_type 
                    })( response )
                }
                response.type = "number"
                response.format = "double"
            } else if ( is_defined( request.nativeType.any_type ) ) {
                response.anyOf[0].type = "string"
                response.anyOf[1].type = "number"
                response.anyOf[2].type = "boolean"
            } else if ( is_defined( request.nativeType.raw_type ) ) {
                response.type = "string"
                response.format = "binary"
            } else if ( is_defined( request.nativeType.void_type ) ) {
                response << {
                    type = "object"                
                    nullable = true 
                }
            } else if ( is_defined( request.nativeType.bool_type ) ) {
                response.type = "boolean"
            }
        } ]

    }
}
