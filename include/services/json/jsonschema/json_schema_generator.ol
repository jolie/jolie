/*

 * Copyright (c) 2016 Claudio Guidi <guidiclaudio@gmail.com>
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


include "./JSONSchemaGeneratorInterface.iol"
include "console.iol"
include "file.iol"
include "json_utils.iol"
include "string_utils.iol"
include "runtime.iol"

execution{ concurrent }


outputPort MySelf {
 Location: "local"
 Interfaces: JSONSchemaGeneratorInterface
}

inputPort JSONSchemaGenerator {
 Location: "local"
 Interfaces: JSONSchemaGeneratorInterface
}

define __number_refinements
{
  with ( __type ) {
    if ( is_defined( .refined_type ) ) {
      if ( is_defined( .refined_type.ranges ) ) {
        if ( is_defined( .refined_type.ranges.min ) ) {
          response.minimum = .refined_type.ranges.min
        }
        if ( is_defined( .refined_type.ranges.max ) ) {
          response.maximum = .refined_type.ranges.max
        }
      }
    }
  }
}

init
{
 getLocalLocation@Runtime( )( MySelf.location );
 request_lib.location = MySelf.location
}

main
{

 [ getTypeDefinition( request )( response ) {
      getType@MySelf( request.type )( response.( request.name ) )
 }] 

 [ getType( request )( response ) {
      if ( request instanceof TypeInLine ) {
          getTypeInLine@MySelf( request )( response )
      } else if ( request instanceof TypeLink ) {
          getTypeLink@MySelf( request )( response )
      } else if ( request instanceof TypeChoice ) {
          getTypeChoice@MySelf( request )( response )
      }
 }]

 [ getTypeInLine( request )( response ) {
       with( response ) {
          getNativeType@MySelf( request.root_type )( resp_root_type );
          if ( #request.sub_type > 0 ) {
                ..type = "object"
          } else {
                response << resp_root_type
          }
          
          /* analyzing sub types */
          if ( #request.sub_type > 0 ) {
              for( st = 0, st < #request.sub_type, st++ ) {
                     getSubType@MySelf( request.sub_type[ st ] )( resp_sub_type );
                     ..properties.( request.sub_type[ st ].name ) << resp_sub_type
              }
          }
      }
 } ]

 [ getTypeLink( request )( response ) {
      response.("$ref") = "#/definitions/" + request.link_name
 }]

 [ getTypeChoice( request )( response ) {
      getType@MySelf( request.choice.left_type )( left )
      getType@MySelf( request.choice.right_type )( right )
      response.oneOf[ 0 ] << left
      if ( is_defined( right.oneOf ) ) {
          for( o = 0, o < #right.oneOf, o++ ) {
            response.oneOf[ o + 1 ] << right.oneOf[ o ]
          }
      } else {
          response.oneOf[ 1 ] << right 
      }
 }]

 [ getSubType( request )( response ) {
      getType@MySelf( request.type )( typedef )
      with( response ) {
        if ( request.cardinality.min  == 1 && request.cardinality.max == 1 ) {
            response << typedef
        } else {
            ..items << typedef;
            ..type = "array";
            ..minItems = request.cardinality.min;
            if ( is_defined( request.cardinality.max ) ) {
                ..maxItems = request.cardinality.max
            }
        }
      }
 } ]



 [ getNativeType( request )( response ) {
       if ( is_defined( request.string_type ) ) {
         response.type = "string"
         with ( request.string_type ) {
          if ( is_defined( .refined_type ) ) {
            if ( is_defined( .refined_type.length ) ) {
              response.minLength = .refined_type.length.min
              response.maxLength = .refined_type.length.max
            }
            if ( is_defined( .refined_type.regex ) ) {
              // OpenAPI needs REs embedded in ^...$ (choices need to put under parentheses)
              if ( contains@StringUtils( .refined_type.regex { substring = "|" } ) ) {
                response.pattern = "^(" + .refined_type.regex + ")$"
              } else {
                response.pattern = "^" + .refined_type.regex + "$"
              }
            }
            if ( is_defined( .refined_type.enum ) ) {
              response.enum << .refined_type.enum
            }
          }
         }
       } else if ( is_defined( request.int_type ) ) {
         response.type = "integer"
         __type -> request.int_type
         __number_refinements
       } else if ( is_defined( request.long_type ) ) {
         response.type = "number";
         response.format = "int64"
         __type -> request.long_type
         __number_refinements
       } else if ( is_defined( request.double_type ) ) {
         response.type = "number";
         response.format = "double"
         __type -> request.double_type
         __number_refinements
       } else if ( is_defined( request.any_type ) ) {
         response.type = "string"
       } else if ( is_defined( request.raw_type ) ) {
         response.type = "string";
         response.format = "binary"
       } else if ( is_defined( request.void_type ) ) {
         nullProcess
       } else if ( is_defined( request.undefined_type ) ) {
         nullProcess
       } else if ( is_defined( request.bool_type ) ) {
         response.type = "boolean"
       }
 } ]

}
