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

include "json_utils.iol"
include "string_utils.iol"
include "console.iol"
include "runtime.iol"
include "./public/interfaces/OpenApiDefinitionInterface.iol"
include "services/json/jsonschema/JSONSchemaGeneratorInterface.iol"

execution{ concurrent }

constants {
  LOG = false
}

outputPort JSONSchemaGenerator {
  Interfaces: JSONSchemaGeneratorInterface
}

embedded {
  Jolie:
    "services/json/jsonschema/json_schema_generator.ol" in JSONSchemaGenerator
}

outputPort MySelf {
  Interfaces: OpenApiDefinitionInterface
}

inputPort OpenApiDefinition {
  Location: "local"
  Protocol: sodep
  Interfaces: OpenApiDefinitionInterface
}

define __clean_name {
    _rep = __name_to_clean;
    _rep.replacement = "_";
    _rep.regex = "«|»";
    replaceAll@StringUtils( _rep )( __cleaned_name )
}

define __indentation {
    for( i = 0, i < request.indentation, i++ ) {
        indentation = indentation + "\t"
    }
}


init {
  getLocalLocation@Runtime( )( MySelf.location )
}

main {
  [  getOpenApiDefinition( request )( response ) {
          with( json ) {
            .swagger = "2.0";
            .info -> request.info;
            .host = request.host;
            .basePath = request.basePath;
            if ( is_defined( request.schemes ) ) {
                .schemes -> request.schemes
            };
            if ( is_defined( request.consumes ) ) {
                .consumes -> request.consumes
            };
            if ( is_defined( request.produces ) ) {
                .produces -> request.produces
            };
            if ( is_defined( request.tags ) ) {
                  if ( #request.tags == 1 ) {
                     __tags._ << request.tags
                  } else {
                     __tags << request.tags
                  };
                  .tags << __tags
            };
            if ( is_defined( request.externalDocs ) ) {
                  .externalDocs -> request.externalDocs
            };
            for( d = 0, d < #request.definitions, d++ ) {
                if ( request.definitions[ d ] instanceof TypeDefinition ) {
                    getTypeDefinition@JSONSchemaGenerator( request.definitions[ d ] )( def )
                } else if ( request.definitions[ d ] instanceof FaultDefinitionForOpenAPI ) {
                    getTypeDefinition@JSONSchemaGenerator( request.definitions[ d ].fault )( def )
                    def.( request.definitions[ d ].fault.name ).properties.fault.pattern = request.definitions[ d ].name 
                }
                .definitions << def
            };
            if ( LOG ) { println@Console("Checking paths...")() };
            for( p = 0, p <#request.paths, p++ ) {

                  __template = request.paths[ p ];
                  if ( LOG ) { println@Console("path:" + __template )() };

                  // paths
                  foreach ( op : request.paths[ p ] ) {
                       undef( __tags );
                       if ( LOG ) { println@Console("method:" + op )() };
                       if ( #request.paths[ p ].( op ).tags == 1 ) {
                          __tags._ -> request.paths[ p ].( op ).tags
                       } else {
                          __tags -> request.paths[ p ].( op ).tags
                       };
                       .paths.(__template ).( op ).tags << __tags;
                       if ( is_defined( request.paths[ p ].( op ).summary ) ) {
                          .paths.(__template ).( op ).summary = request.paths[ p ].( op ).summary
                       };
                       if ( is_defined( request.paths[ p ].( op ).description ) ) {
                          .paths.(__template ).( op ).description = request.paths[ p ].( op ).description
                       };
                       if ( is_defined( request.paths[ p ].( op ).externalDocs ) ) {
                          .paths.(__template ).( op ).externalDocs << request.paths[ p ].( op ).externalDocs
                       };
                       if ( is_defined( request.paths[ p ].( op ).operationId ) ) {
                          .paths.(__template ).( op ).operationId = request.paths[ p ].( op ).operationId
                       };

                       // consumes
                       if ( is_defined( request.paths[ p ].( op ).consumes ) ) {
                          if ( #request.paths[ p ].( op ).consumes == 1 ) {
                            __consumes._ = request.paths[ p ].( op ).consumes
                          } else {
                            __consumes -> request.paths[ p ].( op ).consumes
                          };
                          .paths.(__template ).( op ).consumes << __consumes
                       };

                       // produces
                       if ( is_defined( request.paths[ p ].( op ).produces ) ) {
                         if ( #request.paths[ p ].( op ).produces == 1 ) {
                           __produces._ = request.paths[ p ].( op ).produces
                         } else {
                           __rpoduces -> request.paths[ p ].( op ).produces
                         };
                          .paths.(__template ).( op ).produces << __produces
                       };

                       // responses
                       if ( is_defined( request.paths[ p ].( op ).responses ) ) {
                         for( res = 0, res < #request.paths[ p ].( op ).responses, res++ ) {
                              current_response -> request.paths[ p ].( op ).responses[ res ];
                              status = string( current_response.status )
                              .paths.(__template ).( op ).responses.( status ).description = current_response.description;
                              if ( is_defined( current_response.schema ) ) {
                                   getType@JSONSchemaGenerator( current_response.schema )( .paths.(__template ).( op ).responses.( status ).schema ) 
                              }
                         }
                       };

                       // parameters
                       for( par = 0, par < #request.paths[ p ].( op ).parameters, par++ ) {
                          undef( cur_par );
                          if ( #request.paths[ p ].( op ).parameters == 1 ) {
                              cur_par.ln -> json.paths.(__template ).( op ).parameters[ 0 ]._
                          } else {
                              cur_par.ln -> json.paths.(__template ).( op ).parameters[ par ]
                          };
                          cur_par.ln.name = request.paths[ p ].( op ).parameters[ par ].name;
                          if ( LOG ) { println@Console("parameter:" + cur_par.ln.name )() };
                          if ( is_defined( request.paths[ p ].( op ).parameters[ par ].in.in_body ) ) {
                                in_body -> request.paths[ p ].( op ).parameters[ par ].in.in_body;
                                cur_par.ln.in = "body";
                                if ( is_defined( in_body.schema_subType ) ) {
                                    sb_type -> request.paths[ p ].( op ).parameters[ par ].in.in_body.schema_subType;
                                    cur_par.ln.name = sb_type.name;
                                    if ( sb_type.cardinality.min > 0 ) {
                                          cur_par.ln.required = true
                                    };

                                    getType@JSONSchemaGenerator( sb_type.type )( sbt_type_generated );
                                    cur_par.ln.schema << sbt_type_generated
                               }  else if ( is_defined( in_body.schema_type ) ) {
                                    type -> request.paths[ p ].( op ).parameters[ par ].in.in_body.schema_type;
                                    cur_par.ln.required = true;
                                    getType@JSONSchemaGenerator( type )( generated_type );
                                    cur_par.ln.schema << generated_type
                               } else if ( is_defined( in_body.schema_ref ) ) {
                                    cur_par.ln.schema.("$ref") = "#/definitions/" + in_body.schema_ref
                                    cur_par.ln.required = true
                               }
                          };
                          if ( is_defined( request.paths[ p ].( op ).parameters[ par ].in.other ) ) {
                                getNativeType@JSONSchemaGenerator( request.paths[ p ].( op ).parameters[ par ].in.other.type.root_type )( resp_root_type );
                                cur_par.ln.required = true;
                                cur_par.ln.type = resp_root_type.type;
                                if ( is_defined( resp_root_type.format ) ) {
                                    cur_par.ln.format = resp_root_type.format
                                };
                                cur_par.ln.in = request.paths[ p ].( op ).parameters[ par ].in.other;
                                if ( is_defined( request.paths[ p ].( op ).parameters[ par ].in.other.allowEmptyValue ) ) {
                                    cur_par.ln.allowEmptyValue << request.paths[ p ].( op ).parameters[ par ].in.other.allowEmptyValue
                                }
                          }
                     }
                  }
            }

         }
         ;
         if ( LOG ) { println@Console("converting value to JSON string..." )() };
         getJsonString@JsonUtils( json )( response );
         if ( LOG ) { println@Console("converted!" )() }
    } ]

    [ getJolieTypeFromOpenApiParameters( request )( response ) {
          __name_to_clean = request.name;
          __clean_name;
          response = "type " + __cleaned_name + ": void {\n";
          for( p = 0, p < #request.definition.parameters, p++ ) {
              cur_par -> request.definition.parameters[ p ];
              response = response + "." + cur_par.name;
              if ( is_defined( cur_par.type ) ) {
                  if ( cur_par.type == "array" ) {
                        if (  cur_par.required == "false" ) {
                            cur_par.schema.items.minItems = 0
                        };
                        rq_arr.definition -> cur_par;
                        rq_arr.indentation = 1;
                        getJolieDefinitionFromOpenApiArray@MySelf( rq_arr )( array );
                        response = response + array
                  } else if ( cur_par.type == "file" ) {
                        if (  cur_par.required == "false" ) {
                            response = response + "?"
                        };
                        response = response + ":raw\n"
                  } else {
                        rq_n.type = cur_par.type;
                        if ( is_defined( cur_par.format ) )  {
                            rq_n.format = cur_par.format
                        };
                        getJolieNativeTypeFromOpenApiNativeType@MySelf( rq_n )( native );
                        if (  cur_par.required == "false" ) {
                            response = response + "?"
                        };
                        response = response + ":" + native + "\n"
                  }
              } else if ( is_defined( cur_par.schema ) ) {
                  if ( cur_par.schema.type == "array" ) {
                    if (  cur_par.required == "false" ) {
                        cur_par.schema.items.minItems = 0
                    };
                    rq_arr.definition -> cur_par.schema;
                    rq_arr.indentation = 1;
                    getJolieDefinitionFromOpenApiArray@MySelf( rq_arr )( array );
                    response = response + array
                  } else {
                    if ( cur_par.required == "false" ) {
                        response = response + "?"
                    };
                    response = response + ": undefined\n"
                  }
              }
          };
          response = response + "}\n"
    }]

    [ getJolieTypeFromOpenApiDefinition( request )( response ) {
          __name_to_clean = request.name;
          __clean_name;
          scope( get_definition ) {
              install( DefinitionError => println@Console("Error for type " + request.name )() );
              if ( is_defined( request.definition.oneOf ) ) {
                      response = "type " + __cleaned_name + ":"
                      for( _of = 0, _of < #request.definition.oneOf, _of++ ) {
                          branch -> request.definition.oneOf[ _of ]
                          if ( _of == 0 ) {
                              branch_st = ""
                          } else {
                              branch_st = "\n| "
                          }
                          if ( is_defined( branch.type ) ) {
                                rq_obj.definition -> branch; rq_obj.indentation = 1;
                                getJolieDefinitionFromOpenApiObject@MySelf( rq_obj )( object )
                                response = response + branch_st + "void {\n"  + object + "}"
                          } else if ( is_defined( branch.("$ref") ) ) {
                                split@StringUtils( branch.("$ref") { . regex="#/definitions/" } )( splitted_ref )
                                response = response + branch_st + splitted_ref.result[1]
                          }
                      }
                      response = response + "\n"
              } else if ( request.definition instanceof void ) {
                      response = "type " + __cleaned_name + ": void\n"
              } else if ( is_defined( request.definition.("$ref") ) ) {
                      split@StringUtils( request.definition.("$ref") { . regex="#/definitions/" } )( splitted_ref )
                      response = "type " + __cleaned_name + ": " + splitted_ref.result[1] + "\n"
              } else if ( request.definition.type == "object" ) {
                  if ( #request.definition.properties == 0 ) {
                      response = "type " + __cleaned_name + ": undefined \n"
                  } else {
                      rq_obj.definition -> request.definition; rq_obj.indentation = 1;
                      getJolieDefinitionFromOpenApiObject@MySelf( rq_obj )( object );
                      response = "type " + __cleaned_name + ": void {\n";
                      response = response + object + "}\n"
                  }
              } else {
                if ( #request.definition.properties == 0 ) {
                      nt.type = request.definition.type;
                      getJolieNativeTypeFromOpenApiNativeType@MySelf( nt )( native_type );
                      min = "0"; max = "*";
                      if ( is_defined( request.definition.minimum ) ) {
                          min = request.definition.minimum
                      };
                      if ( is_defined( request.definition.maximum ) ) {
                          max = request.definition.maximum
                      };
                      response = "type " + __cleaned_name + "[" + min + "," + max + "]:" + native_type + "\n"
                } else {
                    rq_obj.definition -> request.definition; rq_obj.indentation = 1;
                    getJolieDefinitionFromOpenApiObject@MySelf( rq_obj )( object );
                    response = "type " + __cleaned_name + ": void {\n";
                    response = response + object + "}\n"
                }

              }
          }
    }]

    [ getJolieDefinitionFromOpenApiObject( request )( response ) {
          __indentation;
          foreach( property : request.definition.properties ) {
              response = response + indentation;
              response = response + "." + property;
              if ( is_defined( request.definition.properties.( property ).("$ref")) ) {
                  spl = request.definition.properties.( property ).("$ref");
                  spl.regex = "/";
                  split@StringUtils( spl )( reference );
                  __name_to_clean = reference.result[ #reference.result - 1 ];
                  __clean_name;
                  response = response + ":" + __cleaned_name + "\n"
              } else {
                  if ( request.definition.properties.( property ).type == "array" ) {
                      rq_arr.definition -> request.definition.properties.( property );
                      rq_arr.indentation = request.indentation + 1;
                      getJolieDefinitionFromOpenApiArray@MySelf( rq_arr )( array );
                      response = response + array
                  } else if ( request.definition.properties.( property ).type == "object" ) {
                      /* TODO */
                      response = response + ": undefined\n"

                  } else {
                      rq_n.type = request.definition.properties.( property ).type;
                      if ( is_defined( request.definition.properties.( property ).format ) )  {
                          rq_n.format = request.definition.properties.( property ).format
                      };
                      getJolieNativeTypeFromOpenApiNativeType@MySelf( rq_n )( native );
                      response = response + ":" + native + "\n"
                  }
              }
          }
    }]

    [ getJolieDefinitionFromOpenApiArray( request )( response ) {
          if ( is_defined( request.definition.items.minItems ) ) {
              min_cardinality = string( request.definition.items.minItems )
          } else {
              min_cardinality = "0"
          }
          ;
          if ( is_defined( request.definition.items.maxItems ) ) {
              max_cardinality = string( request.definition.items.maxItems )
          } else {
              max_cardinality = "*"
          }
          ;
          response = "[" + min_cardinality + "," + max_cardinality + "]:";

          if ( is_defined( request.definition.items.("$ref") ) ) {
              spl = request.definition.items.("$ref");
              spl.regex = "/";
              split@StringUtils( spl )( reference );
              __name_to_clean = reference.result[ #reference.result - 1 ];
              __clean_name;
              response = response + __cleaned_name + "\n"
          } else if ( request.definition.items.type != "object" ) {
              rq_n.type = request.definition.items.type;
              if ( is_defined( request.definition.items.format ) ) {
                  rq_n.format = request.definition.items.format
              };
              getJolieNativeTypeFromOpenApiNativeType@MySelf( rq_n )( native );
              response = response + native + "\n"
          } else {
              response = response + "undefined\n"
          }
    } ]

    [ getJolieNativeTypeFromOpenApiNativeType( request )( response ) {
          if ( request.type instanceof void ) {
              response = "void"
          } else if ( request.type == "string" ) {
              if ( request.format == "binary" ) {
                  response = "raw"
              } else {
                  response = "string"
              }
          } else if ( request.type == "file" ) {
              response = "raw"
          } else if ( request.type == "boolean" ) {
              response = "bool"
          } else if ( request.type == "number" ) {
              response = "double"
          } else if ( request.type == "integer" ) {
              if ( request.format == "int32" ) {
                  response = "int"
              };
              if ( request.format == "int64" ) {
                  response = "long"
              };
              if ( !is_defined( request.format ) ) {
                  response = "int"
              }
          }
    }]

    [ getReferenceName( request )( response ) {
          spl = request;
          spl.regex = "/";
          split@StringUtils( spl )( reference );
          __name_to_clean = reference.result[ #reference.result - 1 ];
          __clean_name;
          response = __cleaned_name
    }]
}
