from .public.interfaces.UtilsInterface import  MetaJolieUtilsInterface

from ...types.definition_types import TypeDefinition
from ...types.definition_types import Type
from ...types.definition_types import TypeLink
from ...types.definition_types import TypeChoice
from ...types.definition_types import TypeInLine
from ...types.definition_types import TypeUndefined

from .metaJolieJavaService import MetaJolieJavaService



service Utils {
    
    embed MetaJolieJavaService as metaJolieJavaService
    
    inputPort ip {
        location: "local"
        interfaces: MetaJolieUtilsInterface
    }


    outputPort utils {
        Location: "local"
        interfaces:  MetaJolieUtilsInterface
    }
    
    execution{ concurrent }
    
    main {
        [ typeLessThan( request )( response ) {

            for( t in request.t1.types ) { types1.( t.name ) << t }
            for( t in request.t2.types ) { types2.( t.name ) << t }

            t1 -> request.t1.type
            t2 -> request.t2.type
            
            response = true
            if ( !( t2 instanceof TypeUndefined ) ) {
                if ( t1 instanceof TypeInLine ) {
                    if ( t2 instanceof TypeLink ) {
                        undef( rq )
                        rq.t1 -> request.t1
                        // replace link with type undefined in order to avoid infinite loop of recursive types
                        if ( !is_defined( types2.( t2.link_name ) ) ) {
                            throw( TypeMissing, "Type " + t2.link_name + " is missing" )
                        }
                        for ( ty in request.t2.types ) {
                            if ( ty.name == t2.link_name ) {
                                foreach ( tf : ty ) { undef( ty.( tf ) ) }
                                ty.type.undefined = true
                                ty.name = t2.link_name
                            }
                        }
                        rq.t2.type -> types2.( t2.link_name ).type
                        rq.t2.types -> request.t2.types
                        typeLessThan@utils( rq )( response )
                    } else if ( t2 instanceof TypeChoice ) {
                        undef( rq )
                        rq.t1 -> request.t1
                        rq.t2.types -> request.t2.types
                        rq.t2.type -> t2.choice.left_type
                        typeLessThan@utils( rq  )( response_left )
                        rq.t2.type -> t2.choice.right_type
                        typeLessThan@utils( rq  )( response_right )
                        response = response_left || response_right
                    } else if ( t2 instanceof TypeInLine ) {
                        // check the root type
                        if ( !is_defined( t2.root_type.any_type ) ) {
                            foreach( f : t1.root_type ) {
                                if ( !is_defined( t2.root_type.( f ) ) ) {
                                    response = false
                                }
                            }
                        } 
                        // check the body if the root is ok
                        if ( response ) {
                            // creating hashmap of subtypes
                            for( sb in t2.sub_type ) { subtypes2.( sb.name ) << sb }
                            // performing check
                            for( sb in t1.sub_type ) {
                                // check cardinality
                                if ( !is_defined( subtypes2.( sb.name ) ) ) {
                                    response = false
                                } else {
                                    if ( sb.cardinality.min <  subtypes2.( sb.name ).cardinality.min ) {
                                        response = false
                                    }
                                    if ( !is_defined( subtypes2.( sb.name ).cardinality.infinite ) ) {
                                        if ( sb.cardinality.max >  subtypes2.( sb.name ).cardinality.max ) {
                                            response = false
                                        }
                                    }
                                }
                                // if cardianity is ok check the types
                                if ( response ) {
                                    undef( rq )
                                    rq.t1.type -> sb.type
                                    rq.t1.types -> request.t1.types
                                    rq.t2.type -> subtypes2.( sb.name ).type
                                    rq.t2.types -> request.t2.types
                                    typeLessThan@utils( rq )( response )
                                }
                            }
                        }
                    }

                } else if ( t1 instanceof TypeLink ) {
                    if ( !is_defined( types1.( t1.link_name ) ) ) {
                            throw( TypeMissing, "Type " + t1.link_name + " is missing" )
                    }
                    if ( t2 instanceof TypeLink ) {
                        undef( rq )
                        if ( !is_defined( types2.( t2.link_name ) ) ) {
                            throw( TypeMissing, "Type " + t2.link_name + " is missing" )
                        }

                        // replace link with type undefined in order to avoid infinite loop of recursive types
                        for ( ty in request.t2.types ) {
                            if ( ty.name == t2.link_name ) {
                                foreach ( tf : ty ) { undef( ty.( tf ) ) }
                                ty.type.undefined = true
                                ty.name = t2.link_name
                            }
                        }
                        rq.t2.type -> types2.( t2.link_name ).type
                        rq.t2.types -> request.t2.types
                    } else {
                        rq.t2 -> request.t2
                    }
                    
                    // replace link with type undefined in order to avoid infinite loop of recursive types
                    for ( ty in request.t1.types ) {
                        if ( ty.name == t1.link_name ) {
                            foreach ( tf : ty ) { undef( ty.( tf ) ) }
                            ty.type.undefined = true
                            ty.name = t1.link_name
                        }
                    }
                    rq.t1.type -> types1.( t1.link_name ).type
                    rq.t1.types -> request.t1.types
                    typeLessThan@utils( rq )( response )
                } else if ( t1 instanceof TypeChoice ) {
                    undef( rq )
                    rq.t2 -> request.t2
                    rq.t1.types -> request.t1.types
                    rq.t1.type -> t1.choice.left_type
                    typeLessThan@utils( rq  )( response_left )
                    rq.t1.type -> t1.choice.right_type
                    typeLessThan@utils( rq )( response_right )
                    response = response_left && response_right
                } else if ( t1 instanceof TypeUndefined ) {
                    response = false
                }
            }
            
        }]

        [ checkOperationTypes( request )( response ) {
            t1 -> request.t1; t2 -> request.t2

            checkNativeType@metaJolieJavaService( { .type_name = t1 })( t1_is_native_type )
            if ( t1_is_native_type ) {
                getNativeTypeFromString@metaJolieJavaService( { .type_name = t1 })( rq.t1.type.root_type ) 
            } else {
                if ( t1 != "undefined" ) { rq.t1.type.link_name = t1 }
                else { rq.t1.type.undefined = true }
            }
            rq.t1.types -> request.t1.types

            checkNativeType@metaJolieJavaService( { .type_name = t2 })( t2_is_native_type )
            if ( t2_is_native_type ) {
                getNativeTypeFromString@metaJolieJavaService( { .type_name = t2 })( rq.t2.type.root_type ) 
            } else {
                 if ( t2 != "undefined" ) { rq.t2.type.link_name = t2 }
                 else { rq.t2.type.undefined = true }
            }
            rq.t2.types -> request.t2.types
            
            typeLessThan@utils( rq )( response )
        }]
    }
}