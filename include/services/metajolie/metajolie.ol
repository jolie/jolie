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

execution{ concurrent }

outputPort MySelf {
    Interfaces: MetaJolieServiceInterface
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
}

