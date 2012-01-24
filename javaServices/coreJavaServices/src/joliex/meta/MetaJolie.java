/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

    private ArrayList<String> types = new ArrayList<String>();
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
            response.getFirstChild("any_type").setValue(1);
        } else if ( type == NativeType.STRING ) {
            response.getFirstChild("string_type").setValue(1);
        } else if ( type == NativeType.DOUBLE )  {
            response.getFirstChild("double_type").setValue(1);
        } else if ( type == NativeType.INT ) {
            response.getFirstChild("int_type").setValue(1);
        } else if ( type == NativeType.VOID ) {
            response.getFirstChild("void_type").setValue(1);
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

    private Value addTypeInLine(  ValueVector types_vector, TypeDefinition typedef ) {
        Value response = Value.create();
        response.getFirstChild("name").setValue( typedef.id() );   // not useful, inserted for respecting Type
        if ( typedef instanceof TypeDefinitionLink ) {
                    response.getFirstChild("root_type").getFirstChild("link").setValue( ((TypeDefinitionLink) typedef ).linkedTypeName());
                    insertType( types_vector, ((TypeDefinitionLink) typedef ).linkedType() );
            } else {
                    TypeInlineDefinition td = ( TypeInlineDefinition ) typedef;
                    response.getFirstChild("root_type").deepCopy( getNativeType( typedef.nativeType() ));
                     if ( td.hasSubTypes() ) {
                        for ( Entry<String,TypeDefinition> entry : td.subTypes() ) {
                            response.getChildren("sub_type").add( addSubType( types_vector, entry.getValue() ) );
                        }
                     }
            }
        return response;
    }

    private Value addSubType( ValueVector types_vector, TypeDefinition type ) {
        Value response = Value.create();
        response.getFirstChild("name").setValue( type.id() );
        response.getFirstChild("cardinality").deepCopy( addCardinality(type.cardinality()));
        if ( type instanceof TypeDefinitionLink ) {
            response.getFirstChild("type_link").setValue( ((TypeDefinitionLink) type).linkedTypeName() );
            if ( !is_generalType( ((TypeDefinitionLink) type).linkedTypeName() ) ) {
                insertType( types_vector, ((TypeDefinitionLink) type ).linkedType() );
            }
         } else {
            response.getFirstChild("type_inline").deepCopy( addTypeInLine( types_vector, type ));
        }
        return response;
    }

    private void insertType( ValueVector types_vector, TypeDefinition typedef ) {
        if ( !types.contains( typedef.id() )) {
            Value type = Value.create();
            if ( typedef instanceof TypeDefinitionLink ) {
                if ( !is_generalType( typedef.id() ) ) {
                    type.getFirstChild("name").setValue( typedef.id() );
                    type.getFirstChild("root_type").getFirstChild("link").setValue( ((TypeDefinitionLink) typedef ).linkedTypeName());
                    insertType( types_vector, ((TypeDefinitionLink) typedef ).linkedType() );
                }
            } else {
                if ( !is_generalType( typedef.id() ) ) {
                    TypeInlineDefinition td = ( TypeInlineDefinition ) typedef;
                    type.getFirstChild("name").setValue( td.id() );
                    type.getFirstChild("root_type").deepCopy( getNativeType( td.nativeType() ));
                    if ( td.hasSubTypes() ) {
                        int subtype_counter = 0;
                        for ( Entry<String,TypeDefinition> entry : td.subTypes() ) {
                            type.getChildren("sub_type").get( subtype_counter ).deepCopy( addSubType( types_vector, entry.getValue() ) );
                            subtype_counter++;
                        }
                    }
                }
            }
            types_vector.add( type );
        }
    }

    private Value getPort( PortInfo portInfo ) {
        Value response = Value.create();
        response.getFirstChild("name").setValue( portInfo.id() );
        if ( portInfo instanceof InputPortInfo ) {
           InputPortInfo port = ( InputPortInfo ) portInfo;
           response.getFirstChild("location").setValue( port.location().toString() );
           response.getFirstChild("protocol").setValue( port.protocolId() );
        } else  if ( portInfo instanceof OutputPortInfo ) {
            OutputPortInfo port = ( OutputPortInfo ) portInfo;
            response.getFirstChild("location").setValue( port.location().toString() );
           response.getFirstChild("protocol").setValue( port.protocolId() );
        }
        

        // scans first interface
        InterfaceDefinition interfaceDefinition = portInfo.getInterfaceList().get(0);
        Value input_interface = response.getFirstChild("interfaces");
        input_interface.getFirstChild("name").setValue( interfaceDefinition.name() );
        ValueVector operations = input_interface.getChildren("operations");
        ValueVector interface_types = input_interface.getChildren("types");

            // scans operations and types
           Map< String , OperationDeclaration > operationMap = interfaceDefinition.operationsMap();

           for ( Entry< String , OperationDeclaration > operationEntry:operationMap.entrySet() ) {
               Value current_operation = Value.create();;
                if ( operationEntry.getValue() instanceof OneWayOperationDeclaration ) {
                     OneWayOperationDeclaration oneWayOperation = ( OneWayOperationDeclaration)operationEntry.getValue();
                     current_operation.getFirstChild( "name").setValue( oneWayOperation.id() );
                     current_operation.getFirstChild("input").setValue( oneWayOperation.requestType().id() );
                     if ( !isNativeType( oneWayOperation.requestType().id() )) {
                        insertType( interface_types, oneWayOperation.requestType());
                     }

                } else {
                   RequestResponseOperationDeclaration requestResponseOperation = ( RequestResponseOperationDeclaration ) operationEntry.getValue();
                   current_operation.getFirstChild("name").setValue( requestResponseOperation.id() );
                   current_operation.getFirstChild("input").setValue( requestResponseOperation.requestType().id() );
                   current_operation.getFirstChild("output").setValue( requestResponseOperation.responseType().id() );
                   if ( !isNativeType( requestResponseOperation.requestType().id())) {
                        insertType( interface_types, requestResponseOperation.requestType() );
                   }
                   if ( !isNativeType( requestResponseOperation.responseType().id())) {
                        insertType( interface_types, requestResponseOperation.responseType() );
                   }
                }
                operations.add( current_operation );
        }
        return response;
    }

    @RequestResponse
    public Value  parseRoles( Value request ) {

        Value response = Value.create();
        try {
               
                types.clear();

                response.getFirstChild("name").setValue( request.getFirstChild("rolename").strValue() );
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
                    input.deepCopy( getPort ( inputPort ));
                 }
                
                // scanning first outputPort if it exists
                OutputPortInfo[] outputPortList = inspector.getOutputPorts();
                if ( outputPortList.length > 0 ) {
                    Value output = response.getFirstChild("output");
                    output.deepCopy( getPort( outputPortList[0] ));
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




