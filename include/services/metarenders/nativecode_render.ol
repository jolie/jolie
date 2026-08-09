/*
 *   Copyright (C) 2010 by Claudio Guidi <cguidi@italianasoftware.com>
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

include "./public/interfaces/NativeCodeRenderInterface.iol"
include "console.iol"
include "file.iol"
include "string_utils.iol"
include "runtime.iol"

execution{ concurrent }


outputPort MySelf {
  Location: "local"
  Interfaces: NativeCodeRenderInterface
}

inputPort Render {
  Location: "local"
  Interfaces: NativeCodeRenderInterface
}



constants {
  _indentation_token = "  "
}

init
{
  global._indentation_string = "";
  global._indent = 0;
  _indent -> global._indent;
  _indentation_string -> global._indentation_string;
  getLocalLocation@Runtime( )( MySelf.location );
  request_lib.location = MySelf.location
}

define indentation_generation {
  _indentation_string = "";
  for ( _x = 0, _x < _indent, _x++ ) {
    _indentation_string = _indentation_string + _indentation_token
  }
}

define indentify {
  _indent = _indent + 1;
  indentation_generation

}

define de_indentify {
  _indent = _indent - 1;
  indentation_generation
}

main
{

  [ getInputPort( request )( response ) {
	response = response + "inputPort " + request.name + " {\n"
	  + _indentation_token + "Protocol:" + request.protocol + "\n"
	  + _indentation_token + "Location:\"" + request.location + "\"\n"
	  + _indentation_token + "Interfaces:" 
	  for ( i = 0, i < #request.interfaces, i++ ) {
		  response = response + request.interfaces[ i ].name
	  }
	  response = response + "\n"
	  + "}\n\n"
  }] 

  [ getOutputPort( request )( response ) {
	response = response + "outputPort " + request.name + " {\n"
	if ( request.protocol != "" ) {
	  response = response + _indentation_token + "Protocol:" + request.protocol + "\n"
	}
	if ( request.location != "undefined" ) {
	  response = response + _indentation_token + "Location:\"" + request.location + "\"\n"
	}
	response = response + _indentation_token + "Interfaces:" 
	  for ( i = 0, i < #request.interfaces, i++ ) {
		  response = response + request.interfaces[ i ].name
	  }
	  response = response + "\n"
	  + "}\n\n"
  }] 

  [ getSurface( request )( response ) {
	getSurfaceWithoutOutputPort@MySelf( request )( surface  )

	// insert outputPort
	response = surface + "\n\noutputPort " + request.name + " {\n"
	    + _indentation_token + "Protocol:" + request.protocol + "\n"
	    + _indentation_token + "Location:\"" + request.location + "\"\n"
	    + _indentation_token + "Interfaces:" + request.name + "Interface\n"
	    + "}\n\n"
  }] 

  [ getOperation( request )( response ) {
	  	response = request.operation_name + "( " + request.input + " )"
		if ( is_defined( request.output ) ) {
		  	response = response + "( " + request.output + " )";
			for ( f = 0, f < #request.fault, f++ ) {
				if ( f == 0 ) {
						response = response + " throws "
				};
				response = response + request.fault[ f ].name;
				if ( !(request.fault[ f ].type instanceof TypeUndefined)  ) {

					if ( request.fault[ f ].type instanceof TypeLink ) {
						response = response + "(" + request.fault[ f ].type.link_name + ") "
					}
					if ( request.fault[ f ].type instanceof NativeType ) {
						getNativeType@MySelf( request.fault[ f ].type )( fault_native_type )
						response = response + "(" + fault_native_type + ") "
					}
						
				};
				response = response + " "
			}
		}
  }]

  [ getSurfaceWithoutOutputPort( request )( response ) {
	// insert types
	for( i = 0, i < #request.interfaces, i++ ) {
	    intf -> request.interfaces[ i ];
	    for( t = 0, t < #intf.types, t++ ) {
	      tp -> intf.types[ t ];
	      getTypeDefinition@MySelf( tp )( response_type );
	      response = response + response_type
	    }
	};

	// insert interface
	response = response + "interface " + request.name + "Interface {\n";
	rr_count = ow_count = 0;
	for( i = 0, i < #request.interfaces, i++ ) {
	    intf -> request.interfaces[ i ];
	    for ( _op = 0, _op < #intf.operations, _op++ ) {
	      if ( is_defined( intf.operations[ _op ].output ) ) {
			rr[ rr_count ] << intf.operations[ _op ];
			rr_count++
	      } else {
			ow[ ow_count ] << intf.operations[ _op ];
			ow_count++
	      }
	    }
	};
	indentify;
	if ( rr_count > 0 ) {
		response = response + "RequestResponse:\n";
		for ( x = 0, x < rr_count, x++ ) {
		      with ( rr[ x ] ) {
				getOperation@MySelf( rr[ x ] )( op_rs  )
				response = response + _indentation_string + op_rs
				if ( x < ( rr_count - 1 ) ) {
					response = response + ",\n"
				} else {
					response = response + "\n"
				}
		      }
		}
	};
	if ( ow_count > 0 ) {
		response = response + "OneWay:";
		for ( x = 0, x < ow_count, x++ ) {
			getOperation@MySelf( ow[ x ] )( op_rs  )
			response = response + _indentation_string + op_rs
			if ( x < ( ow_count - 1 ) ) {
				response = response + ",\n"
			} else {
				response = response + "\n"
			}
		}
	}
	response = response + "}\n\n";
	de_indentify
  }] 



  [ getInterfaceWIthoutTypeList( request )( response ) {
	  response = response + "interface " + request.name + " {\n";
		rr_count = ow_count = 0;
		for ( _op = 0, _op < #request.operations, _op++ ) {
		if ( is_defined( request.operations[ _op ].output ) ) {
			rr[ rr_count ] << request.operations[ _op ];
			rr_count++
		} else {
			ow[ ow_count ] << request.operations[ _op ];
			ow_count++
		}
		};
		indentify;
		if ( rr_count > 0 ) {
		response = response + "RequestResponse:\n";
		for ( x = 0, x < rr_count, x++ ) {
			with ( rr[ x ] ) {
			response = response + _indentation_string + ..operation_name + "( " + ..input + " )( " + ..output + " )";
			for ( f = 0, f < #..fault, f++ ) {
					if ( f == 0 ) {
							response = response + " throws "
					};
					response = response + ..fault[ f ].name;
					if ( ..fault[ f ].type instanceof TypeLink ) {
							response = response + "( " + ..fault[ f ].type.link_name + " ) "
					} else if ( ..fault[ f ].type instanceof NativeType ) {
							getNativeType@MySelf(..fault[ f ].type  )( ntype )
							response = response + "( " + ntype + " ) "
					}
					response = response + " "
				}
				;
				if ( x < ( rr_count - 1 ) ) {
					response = response + ",\n"
				} else {
					response = response + "\n"
				}
			}
		}
		};
		if ( ow_count > 0 ) {
		response = response + "OneWay:";
		for ( x = 0, x < ow_count, x++ ) {
			with ( ow[ x ] ) {
			response = response + _indentation_string + ..operation_name + "( " + ..input + " )";
			if ( x < ( ow_count - 1 ) ) {
			response = response + ",\n"
			} else {
			response = response + "\n"
			}
			}
		}
		};
		response = response + "}\n\n";
		de_indentify
  }]

  [ getInterface( request )( response ) {
		for ( t = 0, t < #request.types, t++ ) {
		getTypeDefinition@MySelf( request.types[ t ] )( resp_type );
		response = response + resp_type
		}
		getInterfaceWIthoutTypeList@MySelf( request )( intf )
		response = response + intf
		
  } ]

  [ getTypeDefinition( request )( response ) {
		response = "type " + request.name + ":";
		getType@MySelf( request.type )( type_rs );
		response = response + type_rs + "\n\n"
  }]

  [ getTypeLink( request )( response ) {
	  	response = request.link_name
  }]

  [ getTypeChoice( request )( response ) {
	  	if ( request.choice.left_type instanceof TypeInLine ) {
			getTypeInLine@MySelf( request.choice.left_type )( left_rs )
		} else if ( request.choice.left_type instanceof TypeLink ) {
			getTypeLink@MySelf( request.choice.left_type )( left_rs )
		}
		getType@MySelf( request.choice.right_type )( right_rs )
		response = left_rs + "|" + right_rs
  }]

  [ getType( request )( response ) {
		if ( request instanceof TypeLink ) {
			getTypeLink@MySelf( request )( response )
		} else if ( request instanceof TypeChoice ) {
			getTypeChoice@MySelf( request )( response )
		} else if ( request instanceof TypeInLine ) {
			getTypeInLine@MySelf( request )( response )
		} else if ( request instanceof TypeUndefined ) {
			getTypeUndefined@MySelf( request )( response )
		}
  } ] 

  [ getTypeUndefined( request )( response ) {
	  response = "undefined"
  }]

  [ getSubType( request )( response ) {
		indentify;
		response = response + _indentation_string + "." + request.name;
		getCardinality@MySelf( request.cardinality )( cardinality );
		response = response + cardinality + ":";
		getType@MySelf( request.type )( type_rs );
		response = response + type_rs
		de_indentify
  } ]

  [ getTypeInLine( request )( response ) {
		getNativeType@MySelf( request.root_type )( resp_root_type );
		response = resp_root_type;
		if ( #request.sub_type > 0 ) {
			response = response + " {\n";
			for( s = 0, s < #request.sub_type, s++ ) {
				getSubType@MySelf( request.sub_type[ s ] )( resp_sub_type );
				response = response + resp_sub_type + "\n"
			};
			response = response + _indentation_string + "}"
		}
  } ]

  [ getCardinality( request )( response ) {
		response = "[" + request.min + ",";
		if ( is_defined( request.max ) ) {
		response = response + request.max
		} else {
		response = response + "*"
		};
		response = response + "]"
  } ] 

  [ getNativeType( request )( response ) {
		if ( is_defined( request.string_type ) ) {
			response = "string"
			if ( is_defined( request.string_type.refined_type ) ) {
				reft -> request.string_type.refined_type
				if ( is_defined( reft.length ) ) {
					if ( is_defined( reft.length.infinite ) ) { max = "*" }
					else { max = reft.length.max  }
					response = response + "( length( [ " + reft.length.min + "," + max + " ] ) )"
				} else if ( is_defined( reft.regex ) ) {
					replaceAll@StringUtils( reft.regex { .regex = "\\\\", .replacement = "\\\\\\\\" } )( reft.regex )
					response = response + "( regex( \"" + reft.regex + "\" ) )"
				} else if ( is_defined( reft.enum ) ) {
					response = response + "( enum([" 
					for ( e = 0, e < #reft.enum, e++ ) {
						response = response + "\"" + reft.enum[ e ] +"\""
						if ( e < (#reft.enum - 1 ) ) {
							response = response + ","
						}
					}
					response = response + " ] ) )"
				}
			}
		} else if ( is_defined( request.int_type ) ) {
			response = "int"
			if ( is_defined( request.int_type.refined_type ) ) {
				reft -> request.int_type.refined_type
				response = response + "( ranges( "
				for( r = 0, r < #reft.ranges, r++ ) {
					if ( is_defined( reft.ranges[ r ].infinite ) ) { max = "*" }
					else { max = reft.ranges[ r ].max   }
					response = response + "[" + reft.ranges[ r ].min + "," + max + "]"
					if ( r < ( #reft.ranges - 1) ) {
						response = response + ","
					}
				}
				response = response + ") )"
			}
		} else if ( is_defined( request.double_type ) ) {
			response = "double"
			if ( is_defined( request.double_type.refined_type ) ) {
				reft -> request.double_type.refined_type
				response = response + "( ranges( "
				for( r = 0, r < #reft.ranges, r++ ) {
					if ( is_defined( reft.ranges[ r ].infinite ) ) { max = "*" }
					else { max = reft.ranges[ r ].max   }
					response = response + "[" + reft.ranges[ r ].min + "," + max + "]"
					if ( r < ( #reft.ranges - 1) ) {
						response = response + ","
					}
				}
				response = response + ") )"
			}
		} else if ( is_defined( request.any_type ) ) {
			response = "any"
		} else if ( is_defined( request.raw_type ) ) {
			response = "raw"
		} else if ( is_defined( request.void_type ) ) {
			response = "void"
		} else if ( is_defined( request.bool_type ) ) {
			response = "bool"
		} else if ( is_defined( request.long_type ) ) {
			response = "long"
			if ( is_defined( request.long_type.refined_type ) ) {
				reft -> request.long_type.refined_type
				response = response + "( ranges( "
				for( r = 0, r < #reft.ranges, r++ ) {
					if ( is_defined( reft.ranges[ r ].infinite ) ) { max = "*" }
					else { max = reft.ranges[ r ].max   }
					response = response + "[" + reft.ranges[ r ].min + "," + max + "]"
					if ( r < ( #reft.ranges - 1) ) {
						response = response + ","
					}
				}
				response = response + ") )"
			}
		} else if ( is_defined( request.link ) ) {
			response = request.link.name
		}
  } ] 



}
