/***************************************************************************
 *   Copyright (C) by Claudio Guidi                                        *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package joliex.meta;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.NativeType;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;
import jolie.util.Range;

/**
 *
 * @author claudio guidi
 */
public class MetaJolie extends JavaService {

    private int MAX_CARD = 2147483647;

    private boolean is_generalType( String type ) {
        // standard types are not inserted. TO BE IMPROVED
        boolean response = false;
        if ( type.equals("Date") ) {
            response = true;
        } else if ( type.equals("String") ) {
              response = true;
        } else if ( type.equals("Integer") ) {
               response = true;
        } else if ( type.equals("Void") ) {
               response = true;
        } else if ( type.equals("__Role") ) {
              response = true;
        }
        return response;
    }

    private Value getNativeType( NativeType type ) {
        Value response = Value.create();
        if ( type == NativeType.ANY ) {
            response.getFirstChild("any_type").setValue( true );
        } else if ( type == NativeType.STRING ) {
            response.getFirstChild("string_type").setValue( true );
        } else if ( type == NativeType.DOUBLE )  {
            response.getFirstChild("double_type").setValue( true );
        } else if ( type == NativeType.INT ) {
            response.getFirstChild("int_type").setValue( true );
        } else if ( type == NativeType.VOID ) {
            response.getFirstChild("void_type").setValue( true );
        } else if ( type == NativeType.BOOL ) {
			response.getFirstChild("bool_type").setValue( true );
		} else if ( type == NativeType.LONG ) {
			response.getFirstChild( "long_type").setValue( true );
		}
        // type undefined?
        return response;
    }

    private boolean isNativeType ( String type ) {
        if ( type.equals("any") ||  type.equals("string") ||  type.equals("double") ||  type.equals("int") ||  type.equals("void")) {
            return true;
        } else {
            return false;
        }
    }

    private Value addCardinality( Range range ) {
        Value response = Value.create();
        response.getFirstChild("min").setValue( range.min() );
        if ( range.max() == MAX_CARD ) {
            response.getFirstChild("infinite").setValue(1);
        } else {
            response.getFirstChild("max").setValue( range.max() );
        }
        return response;
    }

    private Value addTypeInLine(  ArrayList<TypeDefinition> types, ValueVector types_vector, Value name, TypeDefinition typedef ) {
        Value response = Value.create();
        response.getFirstChild("name").getFirstChild( "name").setValue( typedef.id() );   // not useful, inserted for respecting Type
        if ( typedef instanceof TypeDefinitionLink ) {
                    response.getFirstChild("root_type").getFirstChild("link").setValue( ((TypeDefinitionLink) typedef ).linkedTypeName());
                    insertType( types, types_vector, name, ((TypeDefinitionLink) typedef ).linkedType() );
            } else {
                    TypeInlineDefinition td = ( TypeInlineDefinition ) typedef;
                    response.getFirstChild("root_type").deepCopy( getNativeType( typedef.nativeType() ));
                     if ( td.hasSubTypes() ) {
                        for ( Entry<String,TypeDefinition> entry : td.subTypes() ) {
                            response.getChildren("sub_type").add( addSubType( types, types_vector, name, entry.getValue() ) );
                        }
                     }
            }
        return response;
    }

    private Value addSubType( ArrayList<TypeDefinition> types, ValueVector types_vector, Value name, TypeDefinition type ) {
        Value response = Value.create();
        response.getFirstChild("name").setValue( type.id() );
        response.getFirstChild("cardinality").deepCopy( addCardinality(type.cardinality()));
        if ( type instanceof TypeDefinitionLink ) {
            response.getFirstChild("type_link").deepCopy( setName(name));
			response.getFirstChild( "type_link").getFirstChild( "name").setValue( ((TypeDefinitionLink) type).linkedTypeName() );
            if ( !is_generalType( ((TypeDefinitionLink) type).linkedTypeName() ) ) {
                insertType( types, types_vector, name, ((TypeDefinitionLink) type ).linkedType() );
            }
         } else {
            response.getFirstChild("type_inline").deepCopy( addTypeInLine( types, types_vector, name, type ));
        }
        return response;
    }

    private void insertType( ArrayList<TypeDefinition> types, ValueVector types_vector, Value name, TypeDefinition typedef ) {
        if ( !types.contains( typedef )) {
            Value type = Value.create();
            if ( typedef instanceof TypeDefinitionLink ) {
                if ( !is_generalType( typedef.id() ) ) {
                    type.getFirstChild("name").deepCopy( setName( name ));
					type.getFirstChild( "name").getFirstChild( "name").setValue( typedef.id() );
                    type.getFirstChild("root_type").getFirstChild("link").setValue( ((TypeDefinitionLink) typedef ).linkedTypeName());
                    insertType( types, types_vector, name, ((TypeDefinitionLink) typedef ).linkedType() );
                }
            } else {
                if ( !is_generalType( typedef.id() ) ) {
                    TypeInlineDefinition td = ( TypeInlineDefinition ) typedef;
                    type.getFirstChild("name").deepCopy( setName( name ));
					type.getFirstChild( "name").getFirstChild( "name").setValue( td.id() );
                    type.getFirstChild("root_type").deepCopy( getNativeType( td.nativeType() ));
                    if ( td.hasSubTypes() ) {
                        int subtype_counter = 0;
                        for ( Entry<String,TypeDefinition> entry : td.subTypes() ) {
                            type.getChildren("sub_type").get( subtype_counter ).deepCopy( addSubType( types, types_vector, name, entry.getValue() ) );
                            subtype_counter++;
                        }
                    }
                }
            }
            types_vector.add( type );
        }
    }


	 private Value getSubType( TypeDefinition type, Value name ) {
        Value response = Value.create();
        response.getFirstChild("name").setValue( type.id() );
        response.getFirstChild("cardinality").deepCopy( addCardinality(type.cardinality()));
        if ( type instanceof TypeDefinitionLink ) {
            response.getFirstChild("type_link").deepCopy( setName( name ));
			response.getFirstChild( "type_link").getFirstChild("name").setValue( ((TypeDefinitionLink) type).linkedTypeName() );
         } else {
            response.getFirstChild("type_inline").deepCopy( getType( type, name ));
        }
        return response;
    }
	
	private Value getType( TypeDefinition typedef, Value name ) {
		Value type = Value.create();

		type.getFirstChild("name").deepCopy( setName( name ));
		type.getFirstChild("name").getFirstChild( "name").setValue(  typedef.id() );

		if ( typedef instanceof TypeDefinitionLink ) {
			type.getFirstChild("root_type").getFirstChild("link").getFirstChild("name").setValue(((TypeDefinitionLink) typedef ).linkedTypeName());
			if ( name.getFirstChild( "domain").isDefined() ) {
				type.getFirstChild("root_type").getFirstChild("link").getFirstChild("domain").setValue( name.getFirstChild("domain").strValue() );
			}
		} else {
			TypeInlineDefinition td = ( TypeInlineDefinition ) typedef;
			type.getFirstChild("root_type").deepCopy( getNativeType( td.nativeType() ));
			if ( td.hasSubTypes() ) {
				int subtype_counter = 0;
				for ( Entry<String,TypeDefinition> entry : td.subTypes() ) {
					type.getChildren("sub_type").get( subtype_counter ).deepCopy( getSubType( entry.getValue(), name ) );
					subtype_counter++;
				}
			}
		}
		return type;
	}

	 private List<TypeDefinition> addType( List<TypeDefinition> types, TypeDefinition typedef ) {
        if ( !types.contains( typedef )) {
			types.add( typedef );
			if ( typedef instanceof TypeDefinitionLink ) {
				addType( types, ((TypeDefinitionLink) typedef ).linkedType() );
			} else {
				TypeInlineDefinition td = ( TypeInlineDefinition ) typedef;
				if ( td.hasSubTypes() ) {
						for ( Entry<String,TypeDefinition> entry : td.subTypes() ) {
							addType( types, entry.getValue() );
						}
				}
			}
        }
		return types;
    }

	private Value getInterface( InterfaceDefinition intf, Value name, List<TypeDefinition> types ) {
		Value response = Value.create();

		// setting the name
		response.getFirstChild("name").deepCopy( setName( name ) );
		response.getFirstChild( "name" ).getFirstChild( "name" ).setValue( intf.name() );

		
		ValueVector operations = response.getChildren("operations");

		// scans operations and types
		Map< String , OperationDeclaration > operationMap = intf.operationsMap();

		for ( Entry< String , OperationDeclaration > operationEntry:operationMap.entrySet() ) {
		   Value current_operation = Value.create();
			if ( operationEntry.getValue() instanceof OneWayOperationDeclaration ) {
				 OneWayOperationDeclaration oneWayOperation = ( OneWayOperationDeclaration)operationEntry.getValue();
				 current_operation.getFirstChild( "operation_name").setValue( oneWayOperation.id() );
				 current_operation.getFirstChild( "input" ).deepCopy( setName( name ));
				 current_operation.getFirstChild( "input").getFirstChild( "name" ).setValue( oneWayOperation.requestType().id() );
				 if ( !isNativeType( oneWayOperation.requestType().id() )) {
					addType( types, oneWayOperation.requestType());
				 }

			} else {
			   RequestResponseOperationDeclaration requestResponseOperation = ( RequestResponseOperationDeclaration ) operationEntry.getValue();
			   current_operation.getFirstChild("operation_name").setValue( requestResponseOperation.id() );
			   current_operation.getFirstChild( "input" ).deepCopy( setName( name ));
			   current_operation.getFirstChild("input").getFirstChild( "name" ).setValue( requestResponseOperation.requestType().id() );
			   current_operation.getFirstChild("output").deepCopy( setName( name ));
			   current_operation.getFirstChild("output").getFirstChild( "name" ).setValue( requestResponseOperation.responseType().id() );
			   if ( !isNativeType( requestResponseOperation.requestType().id())) {
					addType( types, requestResponseOperation.requestType() );
			   }
			   if ( !isNativeType( requestResponseOperation.responseType().id())) {
					addType( types, requestResponseOperation.responseType() );
			   }
			}
			operations.add( current_operation );

		}
		return response;
	}

	private List<InterfaceDefinition> addInterfaceToList ( List<InterfaceDefinition> list, InterfaceDefinition intf ) {
		if ( !list.contains( intf ) ) {
			list.add(  intf );
		}
		return list;
	}

	private Value setName( String name ) {
		Value v = Value.create();
		v.getFirstChild( "name").setValue( name );
		return v;
	}

	private Value setName( String name, String domain ) {
		Value v = setName( name );
		v.getFirstChild( "domain" ).setValue( domain );
		return v;

	}

	private Value setName( String name, String domain, String registry ) {
		Value v = setName( name, domain );
		v.getFirstChild( "registry" ).setValue( registry );
		return v;
	}

	private Value setName( Value name ) {
		Value v;
		if ( name.getFirstChild( "domain" ).isDefined() && name.getFirstChild( "registry" ).isDefined() ) {
			v = setName( name.getFirstChild( "name").strValue(), name.getFirstChild( "domain" ).strValue(), name.getFirstChild( "registry").strValue() );
		} else if ( name.getFirstChild( "domain" ).isDefined() && !name.getFirstChild( "registry" ).isDefined() ) {
			v = setName( name.getFirstChild( "name").strValue(), name.getFirstChild( "domain" ).strValue() );
		} else {
			v = setName( name.getFirstChild( "name").strValue() );
		}
		return v;
	}

	private Value getPort( PortInfo portInfo, Value name ) {

        Value response = Value.create();
        response.getFirstChild("name").deepCopy( setName( name ) );
		// setting the name of the port
		response.getFirstChild("name").getFirstChild("name").setValue( portInfo.id() );

        if ( portInfo instanceof InputPortInfo ) {

           InputPortInfo port = ( InputPortInfo ) portInfo;
           response.getFirstChild("location").setValue( port.location().toString() );
		   if ( port.protocolId() != null ) {
				response.getFirstChild("protocol").setValue( port.protocolId() );
		   } else {
			   response.getFirstChild("protocol").setValue( "" );
		   }

        } else  if ( portInfo instanceof OutputPortInfo ) {

           OutputPortInfo port = ( OutputPortInfo ) portInfo;
           response.getFirstChild("location").setValue( port.location().toString() );

		   if ( port.protocolId() != null )  {
				response.getFirstChild("protocol").setValue( port.protocolId() );
		   } else {
			   response.getFirstChild("protocol").setValue( "" );
		   }
        }

		ArrayList<TypeDefinition> types = new ArrayList<TypeDefinition>();

        // scan all the interfaces first interface
		for( int intf_index = 0; intf_index < portInfo.getInterfaceList().size(); intf_index++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf_index );
			Value input_interface = response.getFirstChild("interfaces");
			input_interface.getFirstChild("name").deepCopy( setName( name ));
			input_interface.getFirstChild( "name").getFirstChild( "name" ).setValue( interfaceDefinition.name() );

			ValueVector operations = input_interface.getChildren("operations");
			ValueVector interface_types = input_interface.getChildren("types");

            // scans operations and types
			Map< String , OperationDeclaration > operationMap = interfaceDefinition.operationsMap();

			for ( Entry< String , OperationDeclaration > operationEntry:operationMap.entrySet() ) {
               Value current_operation = Value.create();;
                if ( operationEntry.getValue() instanceof OneWayOperationDeclaration ) {
                     OneWayOperationDeclaration oneWayOperation = ( OneWayOperationDeclaration)operationEntry.getValue();
                     current_operation.getFirstChild( "operation_name").setValue( oneWayOperation.id() );
                     current_operation.getFirstChild("input").deepCopy( setName( name ));
					 current_operation.getFirstChild( "input").getFirstChild( "name").setValue( oneWayOperation.requestType().id() );
                     if ( !isNativeType( oneWayOperation.requestType().id() )) {
                        insertType( types, interface_types, name, oneWayOperation.requestType());
                     }

                } else {
                   RequestResponseOperationDeclaration requestResponseOperation = ( RequestResponseOperationDeclaration ) operationEntry.getValue();
                   current_operation.getFirstChild("operation_name").setValue( requestResponseOperation.id() );
                   current_operation.getFirstChild("input").deepCopy( setName( name ));
				   current_operation.getFirstChild( "input").getFirstChild( "name").setValue( requestResponseOperation.requestType().id() );
                   current_operation.getFirstChild("output").deepCopy( setName( name ));
				   current_operation.getFirstChild("output").getFirstChild("name").setValue( requestResponseOperation.responseType().id() );
                   if ( !isNativeType( requestResponseOperation.requestType().id())) {
                        insertType( types, interface_types, name, requestResponseOperation.requestType() );
                   }
                   if ( !isNativeType( requestResponseOperation.responseType().id())) {
                        insertType( types, interface_types, name, requestResponseOperation.responseType() );
                   }
                }
                operations.add( current_operation );
			}
		}
        return response;

    }

	private Value getPort( PortInfo portInfo, Value name, List<InterfaceDefinition> interfaces ) {
        Value response = Value.create();

		// setting domain and registry from request
        response.getFirstChild("name").deepCopy( setName( name ) );
		// setting the name of the port
		response.getFirstChild("name").getFirstChild("name").setValue( portInfo.id() );

        if ( portInfo instanceof InputPortInfo ) {
			InputPortInfo port = ( InputPortInfo ) portInfo;
			if ( port.location() != null ) {
				response.getFirstChild("location").setValue( port.location().toString() );
			} else {	
				response.getFirstChild("location").setValue( "local" );
			}
			if ( port.protocolId() != null ) {
				response.getFirstChild("protocol").setValue( port.protocolId() );
			} else {
				response.getFirstChild("protocol").setValue( "" );
			}
        } else  if ( portInfo instanceof OutputPortInfo ) {
           OutputPortInfo port = ( OutputPortInfo ) portInfo;
		   if ( port.location() != null ) {
				response.getFirstChild("location").setValue( port.location().toString() );
			} else {
				response.getFirstChild("location").setValue( "local" );
			}
			if ( port.protocolId() != null ) {
				response.getFirstChild("protocol").setValue( port.protocolId() );
			} else {
				response.getFirstChild("protocol").setValue( "" );
			}
        }


        // scans interfaces
		List<InterfaceDefinition> interfaceList = portInfo.getInterfaceList();
		for ( int intf = 0; intf < interfaceList.size(); intf++ ) {
			InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get( intf );

			// setting the name of the interface within the port response
			response.getChildren( "interfaces" ).get( intf ).getFirstChild( "name" ).deepCopy( setName( name ));
			response.getChildren( "interfaces" ).get( intf ).getFirstChild( "name" ).getFirstChild( "name").setValue( interfaceDefinition.name() );
			interfaces = addInterfaceToList( interfaces, interfaceDefinition );

        }
        return response;
    }

    @RequestResponse
    public Value  parseRoles( Value request ) {

        Value response = Value.create();
        try {

                response.getFirstChild("name").deepCopy( setName( request.getFirstChild( "rolename") ));
                String[] args = new String[] { request.getFirstChild("filename").strValue(), "-i", "/opt/jolie/include"};
                CommandLineParser cmdParser = new CommandLineParser( args, MetaJolie.class.getClassLoader() );
                args = cmdParser.arguments();
                Program program = ParsingUtils.parseProgram(
                        cmdParser.programStream(),
                        URI.create( "file:" + cmdParser.programFilepath() ),
                        cmdParser.includePaths(), MetaJolie.class.getClassLoader(), cmdParser.definedConstants() );
                ProgramInspector inspector=ParsingUtils.createInspector( program );

                URI originalFile = program.context().source();
                // scanning first inputport
                InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );
                Value input = response.getFirstChild("input");
                if ( inputPortList.length > 0 ) {
                    InputPortInfo inputPort = inputPortList[0];
                    input.deepCopy( getPort ( inputPort, request.getFirstChild( "name") ));
                 }
                
                // scanning first outputPort if it exists
                OutputPortInfo[] outputPortList = inspector.getOutputPorts();
                if ( outputPortList.length > 0 ) {
                    Value output = response.getFirstChild("output");
                    output.deepCopy( getPort( outputPortList[0], request.getFirstChild( "name") ));
                }
                
        } catch ( CommandLineException e ) {
            // TO DO
               e.printStackTrace();
        } catch( IOException e ) {
            // TO DO
            e.printStackTrace();
        } catch( ParserException e ) {
            // TO DO
            e.printStackTrace();
        }
        return response;
    }

	@RequestResponse
    public Value  getMetaData( Value request ) {

		String domain = "";
		List<TypeDefinition> types = new ArrayList<TypeDefinition>();
		List<InterfaceDefinition> interfaces = new ArrayList<InterfaceDefinition>();
        Value response = Value.create();
        try {

                String[] args = new String[] { request.getFirstChild("filename").strValue(), "-i", "/opt/jolie/include"};
				if ( request.getFirstChild("name").getFirstChild( "domain" ).isDefined() ) {
					domain = request.getFirstChild("name").getFirstChild( "domain" ).strValue();
				}
                CommandLineParser cmdParser = new CommandLineParser( args, MetaJolie.class.getClassLoader() );
                args = cmdParser.arguments();
                Program program = ParsingUtils.parseProgram(
                        cmdParser.programStream(),
                        URI.create( "file:" + cmdParser.programFilepath() ),
                        cmdParser.includePaths(), MetaJolie.class.getClassLoader(), cmdParser.definedConstants() );
                ProgramInspector inspector=ParsingUtils.createInspector( program );

                URI originalFile = program.context().source();

				response.getFirstChild( "service" ).getFirstChild( "name").deepCopy( setName( request.getFirstChild( "name" ) ));

                InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );
                ValueVector input = response.getChildren("input");
                if ( inputPortList.length > 0 ) {
					for( int ip = 0; ip < inputPortList.length; ip++ ) {
						InputPortInfo inputPort = inputPortList[ ip ];
						input.get( ip ).deepCopy( getPort ( inputPort, request.getFirstChild( "name"), interfaces ));
						response.getFirstChild( "service" ).getChildren( "input" ).get( ip ).getFirstChild( "name" ).setValue(  inputPort.id() );
						response.getFirstChild( "service" ).getChildren( "input" ).get( ip ).getFirstChild( "domain" ).setValue(  domain );
					}
                 }

                OutputPortInfo[] outputPortList = inspector.getOutputPorts();
                if ( outputPortList.length > 0 ) {
                    ValueVector output = response.getChildren("output");
					for ( int op = 0; op < outputPortList.length; op++ ) {
						OutputPortInfo outputPort = outputPortList[ op ];
						output.get(  op ).deepCopy( getPort( outputPort, request.getFirstChild( "name"), interfaces ));
						response.getFirstChild( "service" ).getChildren( "output" ).get( op ).getFirstChild( "name" ).setValue(  outputPort.id() );
						response.getFirstChild( "service" ).getChildren( "output" ).get( op ).getFirstChild( "domain" ).setValue(  domain );
					}
                }
				// adding interfaces
				for( int intf = 0; intf < interfaces.size(); intf++ ) {
					InterfaceDefinition interfaceDefinition = interfaces.get( intf );
					response.getChildren("interfaces").get(  intf ).deepCopy( getInterface( interfaceDefinition, request.getFirstChild( "name"), types ));
				}

				// adding types
				for ( int tp = 0; tp < types.size(); tp++ ) {
					TypeDefinition typeDefinition = types.get( tp );
					response.getChildren( "types" ).get(  tp ).deepCopy( getType( typeDefinition, request.getFirstChild( "name" ) ));
				}

        } catch ( CommandLineException e ) {
            // TO DO
               e.printStackTrace();
        } catch( IOException e ) {
            // TO DO
            e.printStackTrace();
        } catch( ParserException e ) {
            // TO DO
            e.printStackTrace();
        }
        return response;
    }

	@RequestResponse
    public Value  getInputPortMetaData( Value request ) {

		String domain = "";
		List<TypeDefinition> types = new ArrayList<TypeDefinition>();
		List<InterfaceDefinition> interfaces = new ArrayList<InterfaceDefinition>();
        Value response = Value.create();
        try {

                String[] args = new String[] { request.getFirstChild("filename").strValue(), "-i", "/opt/jolie/include"};
				if ( request.getFirstChild( "domain" ).isDefined() ) {
					domain = request.getFirstChild( "domain" ).strValue();
				}
                CommandLineParser cmdParser = new CommandLineParser( args, MetaJolie.class.getClassLoader() );
                args = cmdParser.arguments();
                Program program = ParsingUtils.parseProgram(
                        cmdParser.programStream(),
                        URI.create( "file:" + cmdParser.programFilepath() ),
                        cmdParser.includePaths(), MetaJolie.class.getClassLoader(), cmdParser.definedConstants() );
                ProgramInspector inspector=ParsingUtils.createInspector( program );

                URI originalFile = program.context().source();


                InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );
                ValueVector input = response.getChildren("input");
                if ( inputPortList.length > 0 ) {
					for( int ip = 0; ip < inputPortList.length; ip++ ) {
						InputPortInfo inputPort = inputPortList[ ip ];
						input.get( ip ).deepCopy( getPort ( inputPort, request.getFirstChild( "name") ));
					}
                 }


        } catch ( CommandLineException e ) {
            // TO DO
               e.printStackTrace();
        } catch( IOException e ) {
            // TO DO
            e.printStackTrace();
        } catch( ParserException e ) {
            // TO DO
            e.printStackTrace();
        }
        return response;
    }

}




