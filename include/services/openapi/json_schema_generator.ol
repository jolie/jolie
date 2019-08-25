/*
The MIT License (MIT)
Copyright (c) 2016 Claudio Guidi <guidiclaudio@gmail.com>

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


include "./public/interfaces/JSONSchemaGeneratorInterface.iol"
include "console.iol"
include "file.iol"
include "json_utils.iol"
include "runtime.iol"

execution{ concurrent }


outputPort MySelf {
 Location: "local"
 Protocol: sodep
 Interfaces: JSONSchemaGeneratorInterface
}

inputPort JSONSchemaGenerator {
 Location: "local"
 Interfaces: JSONSchemaGeneratorInterface
}

init
{
 getLocalLocation@Runtime( )( MySelf.location );
 request_lib.location = MySelf.location
}

main
{

 [ getSchemas( request )( response ) {
     for ( t = 0, t < #request.types, t++ ) {
       getType@MySelf( request.types[ t ] )( def );
       response.definitions << def
     }
 } ] { nullProcess }

 [ getType( request )( response ) {
     with( response.( request.name.name ) ) {
        getNativeType@MySelf( request.root_type )( resp_root_type );
        if ( is_defined( request.root_type.link ) ) {
            .("$ref") = "#/definitions/" + resp_root_type
        } else {
            .type = "object";
            if ( !(resp_root_type instanceof void) ) {
              .properties.("$") << resp_root_type
            }
        }
        ;
        /* analyzing sub types */
        if ( #request.sub_type > 0 ) {
           for( s = 0, s < #request.sub_type, s++ ) {
                  getSubType@MySelf( request.sub_type[ s ] )( resp_sub_type );
                  .properties.( request.sub_type[ s ].name ) << resp_sub_type
           }
        }
    }
 } ] { nullProcess }

 [ getSubType( request )( response ) {
       with( response ) {
          if ( is_defined( request.type_link ) ) {
              .("$ref") = "#/definitions/" + request.type_link.name;
              if ( request.cardinality.max > 1 ) {
                  .type = "array";
                  with( .items ) {
                      .minItems = request.cardinality.min;
                      if ( is_defined( request.cardinality.max ) ) {
                          .maxItems = request.cardinality.max
                      }
                  }
              }
          } else if ( is_defined( request.type_inline ) ) {
              getTypeInLine@MySelf( request.type_inline )( resp_type_inline );
              if ( request.cardinality.min  == 1 && request.cardinality.max == 1 ) {
                  response << resp_type_inline
              } else {
                  .items << resp_type_inline;
                  .type = "array";
                  with( .items ) {
                      .minItems = request.cardinality.min;
                      if ( is_defined( request.cardinality.max ) ) {
                          .maxItems = request.cardinality.max
                      }
                  }
              }
          }
      }
 } ] { nullProcess }

 [ getTypeInLine( request )( response ) {
       with( response ) {
          /* analyzing sub types */
          if ( #request.sub_type > 0 ) {
              for( st = 0, st < #request.sub_type, st++ ) {
                     getSubType@MySelf( request.sub_type[ st ] )( resp_sub_type );
                     .properties.( request.sub_type[ st ].name ) << resp_sub_type
              }
          };
          getNativeType@MySelf( request.root_type )( resp_root_type );
          if ( is_defined( request.root_type.link ) ) {
              .("$ref") = "#/definitions/" + resp_root_type
          } else {
              if ( #request.sub_type > 0 ) {
                    .type = "object";
                    pointer -> response.properties.("$")
              } else {
                    pointer -> response
              };
              if ( !(resp_root_type instanceof void) ) {
                    pointer << resp_root_type
              }
          }
      }
 } ] { nullProcess }

 [ getNativeType( request )( response ) {
       if ( is_defined( request.string_type ) ) {
         response.type = "string"
       } else if ( is_defined( request.int_type ) ) {
         response.type = "integer"
       } else if ( is_defined( request.double_type ) ) {
         response.type = "number";
         response.format = "double"
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
       } else if ( is_defined( request.long_type ) ) {
         response.type = "number";
         response.format = "int64"
       } else if ( is_defined( request.link ) ) {
         response = request.link.name
       }
 } ] { nullProcess }

}
