from runtime import Runtime
from string-utils import StringUtils
from console import Console 

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
from types.definition-types import RangeInt
from types.definition-types import RangeDouble
from types.definition-types import StringRefinedType
from types.definition-types import IntRefinedType
from types.definition-types import DoubleRefinedType
from types.definition-types import LongRefinedType
from types.definition-types import Operation
from types.definition-types import Service

from types.definition-types-doc-templates import __RangeInt
from types.definition-types-doc-templates import __RangeDouble
from types.definition-types-doc-templates import __RangeLong
from types.definition-types-doc-templates import __StringRefinedType
from types.definition-types-doc-templates import __IntRefinedType
from types.definition-types-doc-templates import __DoubleRefinedType
from types.definition-types-doc-templates import __LongRefinedType
from types.definition-types-doc-templates import __NativeType
from types.definition-types-doc-templates import __SubType
from types.definition-types-doc-templates import __TypeInLine
from types.definition-types-doc-templates import __TypeLink
from types.definition-types-doc-templates import __TypeChoice
from types.definition-types-doc-templates import __TypeUndefined
from types.definition-types-doc-templates import __Type
from types.definition-types-doc-templates import __TypeDefinition
from types.definition-types-doc-templates import __Fault
from types.definition-types-doc-templates import __Operation
from types.definition-types-doc-templates import __Interface
from types.definition-types-doc-templates import __Port
from types.definition-types-doc-templates import __Service


type _GetPortRequest {
    documentation_cr_replacement: string
    port: Port
}

type _GetInterface {
    documentation_cr_replacement: string
    interface: Interface
}

type _GetTypeDefinition {
    documentation_cr_replacement: string 
    type_definition: TypeDefinition
}


type _GetOperationRequest {
    documentation_cr_replacement: string 
    operation: Operation
}

type _GetSubTypeRequest {
    documentation_cr_replacement: string 
    sub_type: SubType
}

type _GetTypeInLine {
    documentation_cr_replacement: string 
    type_inline: TypeInLine
}

type _GetTypeChoice {
    documentation_cr_replacement: string 
    type_choice: TypeChoice
}

type _GetTypeRequest {
    documentation_cr_replacement: string 
    type: Type
}

interface JolieDocLibInterface {
    RequestResponse:
        _getPort( _GetPortRequest )( __Port ),
        _getInterface( _GetInterface )( __Interface ),
        _getTypeDefinition( _GetTypeDefinition )( __TypeDefinition ),
        _getNativeType( NativeType )( __NativeType ),
        _getSubType( _GetSubTypeRequest )( __SubType ),
        _getTypeInLine( _GetTypeInLine )( __TypeInLine ),
        _getType( _GetTypeRequest )( __Type ),
        _getTypeLink( TypeLink )( __TypeLink ),
        _getTypeChoice( _GetTypeChoice )( __TypeChoice ),
        _getTypeUndefined( TypeUndefined )( __TypeUndefined ),
        _getFault( Fault )( __Fault ),
        _getOperation( _GetOperationRequest )( __Operation ),
        _getStringRefinedType( StringRefinedType )( __StringRefinedType ),
        _getIntRefinedType( IntRefinedType )( __IntRefinedType ),
        _getDoubleRefinedType( DoubleRefinedType )( __DoubleRefinedType ),
        _getLongRefinedType( LongRefinedType ) ( __LongRefinedType ),
        _getService( Service )( __Service )
}

service JolieDocLib {
    
    embed Runtime as Runtime
    embed StringUtils as StringUtils
    embed Console as Console

    execution: concurrent

    outputPort MySelf {
        location: "local"
        interfaces: JolieDocLibInterface
    }

    inputPort JolieDocLib {
        location: "local"
        interfaces: JolieDocLibInterface
    }

    init {
        getLocalLocation@Runtime()( MySelf.location )
    }

    main {
        [ _getFault( request )( response ) {
            response.name = request.name 
            if ( is_defined( request.type.undefined ) ) {
                response.type << _getTypeUndefined@MySelf( request.type ) 
            } else if ( is_defined( request.type.link_name ) ) {
                response.type << _getTypeLink@MySelf( request.type ) 
            } else {
                response.type << _getNativeType@MySelf( request.type )
            }
        }]

        [ _getPort( request )( response ) {
            for ( i = 0, i < #request.port.interfaces, i++ ) {
                _getInterface@MySelf( {
                    documentation_cr_replacement = request.documentation_cr_replacement
                    interface << request.port.interfaces[ i ] 
                } )( request.port.interfaces[ i ] )
            }
            response -> request.port

        }]

        [ _getInterface( request )( response ) {
            response.name = request.interface.name
            for( o = 0, o < #request.interface.operations, o++ ) {
                _getOperation@MySelf( {
                    documentation_cr_replacement = request.documentation_cr_replacement
                    operation << request.interface.operations[ o ]
                })( response.operations[ o ] )   
            }
            for( t = 0, t < #request.interface.types, t++ ) {
                _getTypeDefinition@MySelf( {
                    type_definition << request.interface.types[ t ]
                    documentation_cr_replacement = request.documentation_cr_replacement
                } )( response.types[ t ] )
            }
        }]

        [ _getOperation( request )( response ) {
            replaceAll@StringUtils( request.operation.documentation { 
                regex = "\n"
                replacement = request.documentation_cr_replacement 
            } )( request.operation.documentation )
            for( f in request.fault ) {
                _getFault@MySelf( f )( f )
            }
            response -> request.operation
        }]

        [ _getTypeInLine( request )( response ) {
            if ( is_defined( request.type_inline.documentation ) ) {
                response.documentation = replaceAll@StringUtils( request.type_inline.documentation { 
                        regex = "\n"
                        replacement = request.documentation_cr_replacement 
                } )
            }
            
            _getNativeType@MySelf( request.type_inline.root_type )( response.root_type )
            
            for ( s = 0, s < #request.type_inline.sub_type, s++ ) {
                _getSubType@MySelf( {
                    documentation_cr_replacement = request.documentation_cr_replacement
                    sub_type << request.type_inline.sub_type[ s ] 
                })( response.sub_type[ s ].sb )
                
                if ( s == 0 ) { response.sub_type[ s ].isFirst = true } else { response.sub_type[ s ].isFirst = false }
                if ( s == (#response.sub_type - 1) ) { response.sub_type[ s ].isLast = true } else { response.sub_type[ s ].isLast = false }
            }
        }]

        [ _getTypeDefinition( request )( response ) {
            replaceAll@StringUtils( request.type_definition.documentation { 
                regex = "\n"
                replacement = request.documentation_cr_replacement 
            } )( request.documentation )
            
            _getType@MySelf( {
                documentation_cr_replacement = request.documentation_cr_replacement
                type << request.type_definition.type 
            })( request.type_definition.type )

            response -> request.type_definition
        }]

        [ _getType( request )( response ) {
            if ( request.type instanceof TypeInLine ) {
                _getTypeInLine@MySelf( {
                    documentation_cr_replacement = request.documentation_cr_replacement
                    type_inline << request.type  
                })( response )
            }
            if ( request.type instanceof TypeChoice ) {
                _getTypeChoice@MySelf( {
                    documentation_cr_replacement = request.documentation_cr_replacement
                    type_choice << request.type  
                } )( response )
            }
            if ( request.type instanceof TypeLink ) {
                undef( request.documentation_cr_replacement )
                _getTypeLink@MySelf( request.type )( response )
            }
            if ( request.type instanceof TypeUndefined ) {
                undef( request.documentation_cr_replacement )
                _getTypeUndefined@MySelf( request.type )( response )
            } 
        }]

        [ _getTypeChoice( request )( response ) {
            request.type_choice.documentation = replaceAll@StringUtils( response.documentation { 
                    regex = "\n"
                    replacement = request.documentation_cr_replacement 
            } )
            if ( request.type_choice.choice.left_type instanceof TypeInLine ) {
                _getTypeInLine@MySelf( request.type_choice.choice.left_type )( response.choice.left_type )
            } else if ( request.type_choice.choice.left_type instanceof TypeLink ) {
                _getTypeLink@MySelf( request.type_choice.choice.left_type )( response.choice.left_type ) 
            }
            _getType@MySelf( { 
                documentation_cr_replacement = request.documentation_cr_replacement 
                type << request.type_choice.choice.right_type 
            } )( response.choice.right_type )
            
        }]

        [ _getTypeLink( request )( response ) {
            response -> request
        }]

        [ _getTypeUndefined( request )( response ) {
            response -> request
        }]

        [ _getNativeType( request )( response ) {
            if ( is_defined( request.string_type.refined_type ) ) {
                _getStringRefinedType@MySelf( request.string_type.refined_type ) ( request.string_type.refined_type )
            } else if ( is_defined( request.int_type.refined_type ) ) {
                _getIntRefinedType@MySelf( request.int_type.refined_type ) ( request.int_type.refined_type )
            } else if ( is_defined( request.double_type.refined_type ) ) {
                _getDoubleRefinedType@MySelf( request.double_type.refined_type ) ( request.double_type.refined_type )
            }  else if ( is_defined( request.long_type.refined_type ) ) {
                _getLongRefinedType@MySelf( request.long_type.refined_type ) ( request.long_type.refined_type )
            }
            response -> request 
        }]

        [ _getStringRefinedType( request )( response ) {
            if ( is_defined( request.enum ) ) { 
                for ( e = 0, e < #request.enum, e++ ) {
                    if ( e == (#request.enum - 1) ) { request.enum[ e ].isLast = true }
                    else { request.enum[ e ].isLast = false }
                }
            }
            response -> request
        }]

        [ _getIntRefinedType( request )( response ) {
            for( r = 0, r < #request.ranges, r++ ) {
                response.ranges[ r ].rangeInt << request.ranges[ r ]
                if ( r == ( #request.ranges -1 ) ) { response.ranges[ r ].isLast = true }    
                else { response.ranges[ r ].isLast = false }
            }
        }]

        [ _getDoubleRefinedType( request )( response ) {
            for( r = 0, r < #request.ranges, r++ ) {
                response.ranges[ r ].rangeDouble << request.ranges[ r ]
                if ( r == ( #request.ranges -1 ) ) { response.ranges[ r ].isLast = true }    
                else { response.ranges[ r ].isLast = false }
            }
        }]

        [ _getLongRefinedType( request )( response ) {
            for( r = 0, r < #request.ranges, r++ ) {
                response.ranges[ r ].rangeLong << request.ranges[ r ]
                if ( r == ( #request.ranges -1 ) ) { response.ranges[ r ].isLast = true }    
                else { response.ranges[ r ].isLast = false }
            }
        }]


        [ _getSubType( request )( response ) {
            response << {
                name = request.sub_type.name 
                cardinality << request.sub_type.cardinality
                
            }
            if ( is_defined( request.documentation ) ) {
                response.documentation = replaceAll@StringUtils( request.sub_type.documentation { 
                    regex = "\n"
                    replacement = request.documentation_cr_replacement 
                } )
            }
            _getType@MySelf( {
                documentation_cr_replacement = request.documentation_cr_replacement 
                type <<  request.sub_type.type
            } )( response.type )
        }]

        [ _getService( request )( response ) {
            response -> request
        }]
    }


}