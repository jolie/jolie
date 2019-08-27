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

include "./public/interfaces/ParserInterface.iol"
include "console.iol"
include "file.iol"
include "string_utils.iol"
include "runtime.iol"

execution{ concurrent }


outputPort MySelf {
  Location: "local"
  Protocol: sodep
  Interfaces: ParserInterface
}

inputPort Parser {
  Location: "local"
  Interfaces: ParserInterface
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
  [ getChoiceBranch( request )( response ) {
	  if ( is_defined( request.type_inline ) ) {
		  getTypeInLine@MySelf( request.type_inline )( response )
	  } else if ( is_defined( request.type_link ) ) {
		  response = request.type_link.name
	  }
  }]

  [ getInputPort( request )( response ) {
	response = response + "inputPort " + request.name.name + " {\n"
	  + _indentation_token + "Protocol:" + request.protocol + "\n"
	  + _indentation_token + "Location:\"" + request.location + "\"\n"
	  + _indentation_token + "Interfaces:" + request.interfaces.name.name + "\n"
	  + "}\n\n"
  }] { nullProcess }

  [ getOutputPort( request )( response ) {
	response = response + "outputPort " + request.name.name + " {\n"
	  + _indentation_token + "Protocol:" + request.protocol + "\n"
	  + _indentation_token + "Location:\"" + request.location + "\"\n"
	  + _indentation_token + "Interfaces:" + request.interfaces.name.name + "\n"
	  + "}\n\n"
  }] { nullProcess }

  [ getSurface( request )( response ) {
	// insert types
	for( i = 0, i < #request.interfaces, i++ ) {
	    intf -> request.interfaces[ i ];
	    for( t = 0, t < #intf.types, t++ ) {
	      tp -> intf.types[ t ];
	      getType@MySelf( tp )( response_type );
	      response = response + response_type
	    }
	};

	// insert interface
	response = response + "interface " + request.name.name + "Interface {\n";
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
				response = response + _indentation_string + .operation_name + "( " + .input.name + " )( " + .output.name + " )";
				for ( f = 0, f < #.fault, f++ ) {
					if ( f == 0 ) {
					      response = response + " throws "
					};
					response = response + .fault[ f ].name.name;
					if ( is_defined( .fault[ f ].type_name ) ) {
						  response = response + "(" + .fault[ f ].type_name.name + ") "
					};
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
	      response = response + _indentation_string + .operation_name + "( " + .input.name + " )";
	      if ( x < ( ow_count - 1 ) ) {
		response = response + ",\n"
	      } else {
		response = response + "\n"
	      }
	    }
	  }
	};
	response = response + "}\n\n";
	de_indentify;

	// insert outputPort
	response = response + "outputPort " + request.name.name + " {\n"
	    + _indentation_token + "Protocol:" + request.protocol + "\n"
	    + _indentation_token + "Location:\"" + request.location + "\"\n"
	    + _indentation_token + "Interfaces:" + request.name.name + "Interface\n"
	    + "}\n\n"
  }] { nullProcess }

  [ getSurfaceWithoutOutputPort( request )( response ) {
	// insert types
	for( i = 0, i < #request.interfaces, i++ ) {
	    intf -> request.interfaces[ i ];
	    for( t = 0, t < #intf.types, t++ ) {
	      tp -> intf.types[ t ];
	      getType@MySelf( tp )( response_type );
	      response = response + response_type
	    }
	};

	// insert interface
	response = response + "interface " + request.name.name + "Interface {\n";
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
				response = response + _indentation_string + .operation_name + "( " + .input.name + " )( " + .output.name + " )";
				for ( f = 0, f < #.fault, f++ ) {
					if ( f == 0 ) {
						  response = response + " throws "
					};
					response = response + .fault[ f ].name.name;
					if ( is_defined( .fault[ f ].type_name ) ) {
						  response = response + "(" + .fault[ f ].type_name.name + ") "
					};
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
	      response = response + _indentation_string + .operation_name + "( " + .input.name + " )";
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
  }] { nullProcess }

  [ getInterface( request )( response ) {
	for ( t = 0, t < #request.types, t++ ) {
	  getType@MySelf( request.types[ t ] )( resp_type );
	  response = response + resp_type
	};
	response = response + "interface " + request.name.name + " {\n";
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
	      response = response + _indentation_string + .operation_name + "( " + .input.name + " )( " + .output.name + " )";
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
	      response = response + _indentation_string + .operation_name + "( " + .input.name + " )";
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
  } ] { nullProcess }

  [ getType( request )( response ) {
	  if ( !is_defined( request.choice ) ) {
			/*getNativeType@MySelf( request.root_type )( resp_root_type );
			response = "type " + request.name.name + ": " + resp_root_type;
			if ( #request.sub_type > 0 ) {
			response = response + " {\n";
			for( s = 0, s < #request.sub_type, s++ ) {
				getSubType@MySelf( request.sub_type[ s ] )( resp_sub_type );
				response = response + _indentation_string + resp_sub_type + "\n"
			};
			response = response + _indentation_string + "}"
			}*/
			getTypeInLine@MySelf( request )( type_inline )
			response = "type " + request.name.name + ": " + type_inline
	  } else {
		  	// choice
			getChoiceBranch@MySelf( request.choice.left_type )( left )
			getChoiceBranch@MySelf( request.choice.right_type )( right )
			response = "type " + request.name.name + ": " + left + "\n|\n" + right
	  }
	  response = response + "\n"
  } ] { nullProcess }

  [ getSubType( request )( response ) {
	indentify;
	response = response + _indentation_string + "." + request.name;
	getCardinality@MySelf( request.cardinality )( cardinality );
	response = response + cardinality + ":";
	if ( is_defined( request.type_link ) ) {
	  response = response + request.type_link.name
	} else if ( is_defined( request.type_inline ) ) {
	  getTypeInLine@MySelf( request.type_inline )( resp_type_inline );
	  response = response + resp_type_inline
	}
	;
	de_indentify
  } ] { nullProcess }

  [ getTypeInLine( request )( response ) {
	    if ( !is_defined( request.choice ) ) {
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
	   } else {
		   	getChoiceBranch@MySelf( request.choice.left_type )( left )
			getChoiceBranch@MySelf( request.choice.right_type )( right )
			response = left + "\n|\n" + right
	   }
  } ] { nullProcess }

  [ getCardinality( request )( response ) {
	response = "[" + request.min + ",";
	if ( is_defined( request.max ) ) {
	  response = response + request.max
	} else {
	  response = response + "*"
	};
	response = response + "]"
  } ] { nullProcess }

  [ getNativeType( request )( response ) {
	if ( is_defined( request.string_type ) ) {
	  response = "string"
	} else if ( is_defined( request.int_type ) ) {
	  response = "int"
	} else if ( is_defined( request.double_type ) ) {
	  response = "double"
	} else if ( is_defined( request.any_type ) ) {
	  response = "any"
	} else if ( is_defined( request.raw_type ) ) {
	  response = "raw"
	} else if ( is_defined( request.void_type ) ) {
	  response = "void"
	} else if ( is_defined( request.undefined_type ) ) {
	  response = "undefined"
	} else if ( is_defined( request.bool_type ) ) {
	  response = "bool"
	} else if ( is_defined( request.long_type ) ) {
	  response = "long"
	} else if ( is_defined( request.link ) ) {
	  response = request.link.name
	}
  } ] { nullProcess }



}
