 /****************************************************************************
   Copyright 2010 by Claudio Guidi <cguidi@italianasoftware.com>      
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
********************************************************************************/


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


  [ getParticipantInput( request )( response ) {
	response = response + "inputPort " + request.name.name + " {\n"
	  + _indentation_token + "Protocol:" + request.protocol + "\n"
	  + _indentation_token + "Location:\"" + request.location + "\"\n"
	  + _indentation_token + "Interfaces:" + request.interfaces.name.name + "\n"
	  + "}\n\n"
  }] { nullProcess }

  [ getParticipantOutput( request )( response ) {
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
	getNativeType@MySelf( request.root_type )( resp_root_type );
	response = "type " + request.name.name + ": " + resp_root_type;
	if ( #request.sub_type > 0 ) {
	  response = response + " {\n";
	  for( s = 0, s < #request.sub_type, s++ ) {
	    getSubType@MySelf( request.sub_type[ s ] )( resp_sub_type );
	    response = response + _indentation_string + resp_sub_type + "\n"
	  };
	  response = response + _indentation_string + "}"     
	};
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