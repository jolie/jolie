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
        typeLessThan( TypeLessThanRequest )( bool )
        
}
service Utils {
    Interfaces: MetaJolieUtilsInterface

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
                        rq.t1 -> request.t1
                        // replace link with type undefined in order to avoid infinite loop of recursive types
                        for ( ty in request.t2.types ) {
                            if ( ty.name == t2.link_name ) {
                                foreach ( tf : ty ) { undef( ty.( tf ) ) }
                                ty.type.undefined = true
                                ty.name = t1.link_name
                            }
                        }
                        rq.t2.types -> request.t2.types
                        typeLessThan@Utils( rq )( response )
                    } else if ( t2 instanceof TypeChoice ) {
                        rq.t1 -> request.t1
                        rq.t2.types -> request.t2.types
                        rq.t2.type -> t2.choice.left_type
                        typeLessThan@Utils( rq  )( response_left )
                        rq.t2.type -> t2.choice.right_type
                        typeLessThan@Utils( rq  )( response_right )
                        reponse = response_left && response_right
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
                    if ( t2 instanceof TypeLink ) {
                        rq.t2.type << types2.( t2.link_name ).type
                        // replace link with type undefined in order to avoid infinite loop of recursive types
                        for ( ty in request.t2.types ) {
                            if ( ty.name == t2.link_name ) {
                                foreach ( tf : ty ) { undef( ty.( tf ) ) }
                                ty.type.undefined = true
                                ty.name = t2.link_name
                            }
                        }
                        rq.t2.types -> request.t2.types
                    } else {
                        rq.t2 -> request.t2
                    }
                    rq.t1.type << types1.( t1.link_name ).type
                    // replace link with type undefined in order to avoid infinite loop of recursive types
                    for ( ty in request.t1.types ) {
                        if ( ty.name == t1.link_name ) {
                            foreach ( tf : ty ) { undef( ty.( tf ) ) }
                            ty.type.undefined = true
                            ty.name = t1.link_name
                        }
                    }
                    rq.t1.types -> request.t1.types
                    typeLessThan@Utils( rq )( response )
                } else if ( t1 instanceof TypeChoice ) {
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

            rq.t1.type -> types1.( request.t1 ).type
            rq.t1.types -> request.t1.types
            rq.t2.type -> types2.( request.t2 ).type
            rq.t2.types -> request.t2.types
            typeLessThan@Utils( rq )( response )
        
        }]
}

