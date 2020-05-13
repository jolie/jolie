/*
 *   Copyright (C) 2020 by Claudio Guidi <cguidi@italianasoftware.com>    
 *                                                                        
 *   This program is free software; you can redistribute it and/or modify 
 *   it under the terms of the GNU Library General Public License as      
 *   published by the Free Software Foundation; either version 2 of the   
 *   License, or (at your option) any later version.                      
 *                                                                        
 *   This program is distributed in the hope that it will be useful,      
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of       
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 *   GNU General Public License for more details.                         
 *                                                                        
 *   You should have received a copy of the GNU Library General Public    
 *   License along with this program; if not, write to the                
 *   Free Software Foundation, Inc.,                                      
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            
 *                                                                        
 *   For details about the authors of this software, see the AUTHORS file.
 */
include "public/interfaces/metajolie_javaservice.iol"
include "public/interfaces/metajolie_service.iol"
include "runtime.iol"
include "console.iol"
include "string_utils.iol"

execution{ concurrent }

outputPort MySelf {
    Interfaces: MetaJolieServiceInterface
}

type CheckOperationTypesRequest: void {
    t1: string {
        types*: TypeDefinition
    }
    t2: string {
        types*: TypeDefinition       
    }
}

type TypeLessThanRequest: void {
	t1 {
        type: Type
		types*: TypeDefinition
	}
	t2 {
        type: Type
		types*: TypeDefinition
	}
}

interface MetaJolieUtilsInterface {
    RequestResponse: 
        typeLessThan( TypeLessThanRequest )( bool ) throws TypeMissing( string ),
        checkOperationTypes( CheckOperationTypesRequest )( bool ) throws TypeMissing( string )       
}

service Utils {
    Interfaces: MetaJolieUtilsInterface

    init {
        install( TypeMissing => nullProcess )
    }

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
                        typeLessThan@Utils( rq )( response )
                    } else if ( t2 instanceof TypeChoice ) {
                        undef( rq )
                        rq.t1 -> request.t1
                        rq.t2.types -> request.t2.types
                        rq.t2.type -> t2.choice.left_type
                        typeLessThan@Utils( rq  )( response_left )
                        rq.t2.type -> t2.choice.right_type
                        typeLessThan@Utils( rq  )( response_right )
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
                                    typeLessThan@Utils( rq )( response )
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
                    typeLessThan@Utils( rq )( response )
                } else if ( t1 instanceof TypeChoice ) {
                    undef( rq )
                    rq.t2 -> request.t2
                    rq.t1.types -> request.t1.types
                    rq.t1.type -> t1.choice.left_type
                    typeLessThan@Utils( rq  )( response_left )
                    rq.t1.type -> t1.choice.right_type
                    typeLessThan@Utils( rq )( response_right )
                    response = response_left && response_right
                } else if ( t1 instanceof TypeUndefined ) {
                    response = false
                }
            }
            
        }]

        [ checkOperationTypes( request )( response ) {
            t1 -> request.t1; t2 -> request.t2

            checkNativeType@MetaJolieJavaService( { .type_name = t1 })( t1_is_native_type )
            if ( t1_is_native_type ) {
                getNativeTypeFromString@MetaJolieJavaService( { .type_name = t1 })( rq.t1.type.root_type ) 
            } else {
                if ( t1 != "undefined" ) { rq.t1.type.link_name = t1 }
                else { rq.t1.type.undefined = true }
            }
            rq.t1.types -> request.t1.types

            checkNativeType@MetaJolieJavaService( { .type_name = t2 })( t2_is_native_type )
            if ( t2_is_native_type ) {
                getNativeTypeFromString@MetaJolieJavaService( { .type_name = t2 })( rq.t2.type.root_type ) 
            } else {
                 if ( t2 != "undefined" ) { rq.t2.type.link_name = t2 }
                 else { rq.t2.type.undefined = true }
            }
            rq.t2.types -> request.t2.types
            
            typeLessThan@Utils( rq )( response )
        }]
    }
}

inputPort MetaJolie {
    Location: "local"
    Interfaces: MetaJolieServiceInterface
    Aggregates: MetaJolieJavaService
}

define check_strict {
    // __vfirst
    // __vsecond
    foreach( v : __vfirst ) {
        if ( is_defined( __vsecond.( v ) ) || ( __vfirst.( v ) instanceof void && __vsecond.( v ) instanceof void ) ) {
            for( x = 0, x <#__vfirst.( v ), x++ ) {
                with( cmp_rq ) {
                    .v1 -> __vfirst.( v )[ x ];
                    .v2 -> __vsecond.( v )[ x ]
                }
                compareValuesStrict@MySelf( cmp_rq )( response )
            }
        } else {
            throw( ComparisonFailed,  "Node " + v + " in is not present in the target value" )
        }
    }
}

define check_light {
    // __vfirst
    // __vsecond
    foreach( v : __vfirst ) {
        if ( is_defined( __vsecond.( v ) ) || ( __vfirst.( v ) instanceof void && __vsecond.( v ) instanceof void ) ) {
            for( x = 0, x <#__vfirst.( v ), x++ ) {
                with( cmp_rq ) {
                    .v1 -> __vfirst.( v )[ x ];
                    found_item = false
                    for ( y = 0, y <#__vsecond.( v ), y++ ) {
                        .v2 -> __vsecond.( v )[ y ]
                        scope( cmp_item ) {
                            install( ComparisonFailed => nullProcess )
                            compareValuesVectorLight@MySelf( cmp_rq )( )
                            found_item = true
                        }

                    }
                    if ( !found_item ) { throw( ComparisonFailed , "Item " + x + " of node " + v + " whose value is " + __vfirst.( v )[ x ] + " does not have any correspondance")}
                }
            }
        } else {
            throw( ComparisonFailed,  "Node " + v + " in is not present in the target value" )
        }
    }
}

init {
    getLocalLocation@Runtime()( MySelf.location )
    install( ComparisonFailed => nullProcess )
}

main {
        [ compareValuesStrict( request )( response ) {

            // check root
            if ( request.v1 != request.v2 ) {
                throw( ComparisonFailed, "root nodes are different:" + request.v1 + "," + request.v2 )
            }

            // check the subnodes of v1 against subnodes of v2
            __vfirst -> request.v1
            __vsecond -> request.v2
            check_strict
           
            // check the subnodes of v2 against subnodes of v1
            __vfirst -> request.v2
            __vsecond -> request.v1
            check_strict
        }]

        [ compareValuesVectorLight( request )( response ) {
            // check root
            if ( request.v1 != request.v2 ) {
                throw( ComparisonFailed, "root nodes are different:" + request.v1 + "," + request.v2 )
            }

            // check the subnodes of v1 against subnodes of v2
            __vfirst -> request.v1
            __vsecond -> request.v2
            check_light
           
            // check the subnodes of v2 against subnodes of v1
            __vfirst -> request.v2
            __vsecond -> request.v1
            check_light
        }]

        [ typeDefinitionLessThan( request )( response ) {
            // creating hashmap of the types
            for( t in request.t1.types ) { types1.( t.name ) << t }
            for( t in request.t2.types ) { types2.( t.name ) << t }

            if ( !is_defined( types1.( request.t1 ) ) ) {
                throw( TypeMissing, "Type " + request.t1 + " is missing" )
            }
            if ( !is_defined( types2.( request.t2 ) ) ) {
                throw( TypeMissing, "Type " + request.t2 + " is missing" )
            } 

            if ( (types1.( request.t1 ).type instanceof TypeLink) 
                  || (types2.( request.t2 ).type instanceof TypeLink)  ) {
                // this is for avoinding unblanced tree navigation in case of recursion
                if ( types1.( request.t1 ).type instanceof TypeLink ) {
                    request.t1 = types1.( request.t1 ).type.link_name
                }
                if ( types2.( request.t2 ).type instanceof TypeLink ) {
                    request.t2 = types2.( request.t2 ).type.link_name
                }
                typeDefinitionLessThan@MySelf( request )( response )
            } else {
                rq.t1.type -> types1.( request.t1 ).type
                rq.t1.types -> request.t1.types
                rq.t2.type -> types2.( request.t2 ).type
                rq.t2.types -> request.t2.types
                typeLessThan@Utils( rq )( response )
            }
        
        }]

        [ interfaceDefinitionLessThan( request )( response ) {
            i1 -> request.i1; i2 -> request.i2;
            // creating hashmap of the types
            for( t in request.i1.types ) { types1.( t.name ) << t }
            for( t in request.i2.types ) { types2.( t.name ) << t }
            // create hashmap for operations of i2
            response.result = true
            for( o2 in i2.operations ) { operations2.( o2.operation_name ) << o2 }
            for( o1 in i1.operations ) {
                if ( !is_defined( operations2.( o1.operation_name ) ) ) {
                    response.result = false
                    errors[ #errors ] = "Operation " + o1.operation_name + " is missing in " + i2.name
                } else {
                    // checking request type
                    scope( tinput_check ) {
                        install( TypeMissing => 
                                response.result = false 
                                errors[#errors] = tinput_check.TypeMissing
                        )
                
                        rq_ck.t1 = o1.input
                        rq_ck.t1.types -> i1.types
                        rq_ck.t2 = operations2.( o1.operation_name ).input
                        rq_ck.t2.types -> i2.types
                        checkOperationTypes@Utils( rq_ck )( nt_ck )
                        if ( !nt_ck ) {
                            response.result = false 
                            errors[#errors] = "Type " + o1.input + " is not less than " + operations2.( o1.operation_name ).input 
                        }
                    }

                    // checking response type
                    if ( is_defined( o1.output ) ) {
                        if ( is_defined( operations2.( o1.operation_name ).output ) ) {
                            scope( toutput_check ) {
                                install( TypeMissing => 
                                    response.result = false 
                                    errors[#errors] = toutput_check.TypeMissing
                                )
                        
                                rq_ck.t1 = operations2.( o1.operation_name ).output
                                rq_ck.t1.types -> i2.types
                                rq_ck.t2 = o1.output
                                rq_ck.t2.types -> i1.types
                                checkOperationTypes@Utils( rq_ck )( nt_ck )
                                if ( !nt_ck ) {
                                    response.result = false 
                                    errors[#errors] = "Type " + operations2.( o1.operation_name ).output + " is not less than " + o1.output
                                }
                                
                            }
                        } else {
                            response.result = false
                            errors[ #errors ] = "Operation " + o1.operation_name + " is RequestResponse in " + i1.name + " but not in " + i2.name
                        }
                    } else if ( is_defined( operations2.( o1.operation_name ).output ) ) {
                        response.result = false
                        errors[ #errors ] = "Operation " + operations2.( o1.operation_name ).output + " is RequestResponse in " + i2.name + " but not in " + i1.name
                    }

                    // checking faults
                    // creating hashmap for faults of operation1
                    undef( o1faults )
                    for( f1 in o1.fault ) { o1faults.( f1.name ) << f1 }
                    for( f2 in operations2.( o1.operation_name ).fault ) {

                        if ( !is_defined( o1faults.( f2.name ) ) ) {
                            response.result = false
                            errors[ #errors ] = "Fault " + f2.name + " of operation " + o1.operation_name + " is not present in the " + i1.name                
                        } else {
                            // checking types
                            if ( !( o1faults.( f2.name ).type instanceof TypeUndefined ) ) {
                            
                                if ( o1faults.( f2.name ).type instanceof NativeType ) {

                                    // they are both native types
                                    if ( o1faults.( f2.name ).type instanceof NativeType ) {
                                        if ( !is_defined( o1faults.( f2.name ).type.any_type ) ) {
                                            foreach( e : f2.type ) {
                                                if ( !is_defined( o1faults.( f2.name ).type.( e ) ) ) {
                                                    response.result = false
                                                    errors[ #errors ] = "Fault " + f2.name + " of operation " + o1.operation_name + " has different type" 
                                                }
                                            }
                                        }
                                    } else {
                                        // f1 is a TypeLink
                                        if ( types1.(o1faults.( f2.name ).type.link_name ).type instanceof NativeType ) {
                                            if ( !is_defined( types1.(o1faults.( f2.name ).type.link_name ).type.any_type ) ) {
                                                foreach( e : f2.type ) {
                                                    if ( !is_defined( types1.(o1faults.( f2.name ).type.link_name ).type.( e ) ) ) {
                                                        response.result = false
                                                        errors[ #errors ] = "Fault " + f2.name + " of operation " + o1.operation_name + " has different type" 
                                                    }
                                                }
                                            }
                                        } else {
                                            response.result = false
                                            errors[ #errors ] = "Fault " + f2.name + " of operation " + o1.operation_name + " has different type"
                                        }
                                    }
                                } else {
                                    install( TypeMissing => 
                                        response.result = false
                                        errors[ #errors ] = toutput_check.TypeMissing 
                                    )
                                    undef( rq )
                                    if ( ( f2.type instanceof TypeLink) || ( o1faults.( f2.name ).type instanceof TypeLink)  ) {
                                        // this is for avoinding unblanced tree navigation in case of recursion
                                        if ( f2.type instanceof TypeLink ) {
                                            rq.t1.type -> types2.( f2.type.link_name ).type
                                        } else {
                                            rq.t1.type -> f2.type
                                        }
                                        if ( o1faults.( f2.name ).type instanceof TypeLink ) {
                                            rq.t2.type -> types1.( o1faults.( f2.name ).type.link_name ).type
                                        } else {
                                            rq.t2.type -> o1faults.( f2.name ).type
                                        }
                                    } 
                                    with( rq ) {
                                        .t1.types -> i2.types;
                                        .t2.types -> i1.types
                                    }
                                    typeLessThan@Utils( rq )( type_check )
                                    if ( !type_check ) {
                                        response.result = false
                                        errors[ #errors ] = "Type of fault " + f2.name + " of interface " + i2.name 
                                            + " is not less than type of fault " + f1.name + " of interface " + i1.name 
                                    }
                                }
                            } 
                        }
                    }
                }
            }
            response.errors -> errors
        }]

        [ portDefinitionLessThan( request )( response ) {
            response.result = true
            if ( request.p1.protocol != request.p2.protocol ) {
                response.result = false
                response.errors[ #response.errors ] = "Protocols are different"
            }

            // aggregating interfaces into a single interface
            for ( intf in request.p1.interfaces ) {
                for ( o in intf.operations ) {
                    i1.operations[ #i1.operations ] << o
                }
                for ( t in intf.types ) {
                    i1.types[ #i1.types ] << t
                }
            }
            i1.name = "Port1AggregatedInterface"

            for ( intf in request.p2.interfaces ) {
                for ( o in intf.operations ) {
                    i2.operations[ #i2.operations ] << o
                }
                for ( t in intf.types ) {
                    i2.types[ #i2.types ] << t
                }
            }
            i2.name = "Port2AggregatedInterface"

            interfaceDefinitionLessThan@MySelf( { .i1 -> i1, .i2 -> i2 } )( result ) 
            if ( !result.result ) {
                response.result = false
                for( e in result.errors ) {
                    response.errors[ #response.errors ] << e
                }
            }
            

        }]
}

