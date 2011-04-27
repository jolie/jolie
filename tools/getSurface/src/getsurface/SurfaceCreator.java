/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package getsurface;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;
import jolie.util.Range;

/**
 *
 * @author claudio
 */
public class SurfaceCreator {
    
    private ProgramInspector inspector;
    private URI originalFile;
    private ArrayList<RequestResponseOperationDeclaration> rr_vector;
    private ArrayList<OneWayOperationDeclaration> ow_vector;
    private ArrayList<String> types_vector;
    private ArrayList<TypeDefinition> aux_types_vector;
    private int MAX_CARD = 2147483647;
    private boolean aux_flag = false;

    public SurfaceCreator( ProgramInspector inspector, URI originalFile )
	{

                this.inspector=inspector;
		this.originalFile=originalFile;

	}

    public void ConvertDocument( String inputPortToCreate ) throws Exception {

        ArrayList<InterfaceDefinition> interface_vector = new ArrayList<InterfaceDefinition>();
        rr_vector = new ArrayList<RequestResponseOperationDeclaration>();
        ow_vector = new ArrayList<OneWayOperationDeclaration>();
        types_vector = new ArrayList<String>();
        aux_types_vector = new ArrayList<TypeDefinition>();

        // find inputPort
         InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );
         InputPortInfo inputPort = null;
         for( InputPortInfo iP : inputPortList ) {
            if ( iP.id().equals( inputPortToCreate ) ) {
                inputPort = iP;
            }
        }
        if ( inputPort == null ) {
            throw( new Exception("Error! inputPort not found!"));
        }

        // extracts the list of all the interfaces to be parsed
        // extracts interfaces declared into Interfaces
        for ( InterfaceDefinition interfaceDefinition:inputPort.getInterfaceList() ) {
            interface_vector.add( interfaceDefinition );
        }
        OutputPortInfo[] outputPortList = inspector.getOutputPorts( originalFile );
        // extracts interfaces from aggregated outputPorts
        for ( int x = 0; x < inputPort.aggregationList().length; x++ ) {
            int i = 0;
            while ( !inputPort.aggregationList()[x].equals( outputPortList[i].id()) ) {
                i++;
            }
            for ( InterfaceDefinition interfaceDefinition:outputPortList[i].getInterfaceList() ) {
                interface_vector.add( interfaceDefinition );
            }
        }

        //  for each interface extract the list of all the available operations and types
        for ( InterfaceDefinition interfaceDefinition: interface_vector ) {
            addOperation( interfaceDefinition );
        }

        // create oputput
        createOutput(inputPort);

    }

    private void addOperation( InterfaceDefinition interfaceDefinition ) {
        for ( OperationDeclaration op: interfaceDefinition.operationsMap().values() ) {
            if ( op instanceof RequestResponseOperationDeclaration ) {
                rr_vector.add( (RequestResponseOperationDeclaration) op );
            } else {
                ow_vector.add( (OneWayOperationDeclaration) op );
            }
        }
    }

    private String getOWString( OneWayOperationDeclaration ow ) {
        String ret = ow.id() + "( " + ow.requestType().id() + " )";
        return ret;
    }

     private String getRRString( RequestResponseOperationDeclaration rr ) {
        String ret = rr.id() + "( " + rr.requestType().id() + " )( " + rr.responseType().id() + " )";
        if ( rr.faults().size() > 0 ) {
            ret = ret + " throws ";
            boolean flag= false;
            for ( Entry<String, TypeDefinition> fault: rr.faults().entrySet() ) {
                if ( flag == false ) {
                    flag = true;
                } else {
                    ret = ret + " ";
                }
                ret = ret + fault.getKey();
                if ( fault.getValue() != null ) {
                    ret = ret + "( " + fault.getValue().id() + " )";
                }
            }
        }
        return ret;
    }

    private String getMax( int max ) {
        if ( max == MAX_CARD ) {
            return "*";
        } else {
            return new Integer( max ).toString();
        }
    }
    private String getCardinality( Range card ) {
        return "[" + card.min() + "," + getMax( card.max() ) + "]";
    }

    private  String getSubType( TypeDefinition type, int indent ) {
        String ret = "";
        for ( int y=0; y < indent; y++ ) {
            ret = ret + "\t";
        }
         ret = ret + "." + type.id() + getCardinality( type.cardinality() ) + ":";
         if ( type instanceof TypeDefinitionLink ) {
                ret = ret + ((TypeDefinitionLink) type).linkedTypeName();
                if ( aux_flag == false ) { aux_types_vector.add( ((TypeDefinitionLink) type).linkedType() ); }
                
            } else {
                ret = ret + type.nativeType().id();
                if ( ((TypeInlineDefinition) type).hasSubTypes() ) {
                    ret = ret + "{ \n";
                    for ( Entry<String, TypeDefinition> entry: ((TypeInlineDefinition) type).subTypes() ) {
                        ret = ret + getSubType( entry.getValue(), indent + 1 ) +"\n";
                    }
                    for ( int y = 0; y < indent; y++ ) {
                        ret = ret + "\t";
                    }
                    ret = ret +"}";
                }
            };
          return ret;
    }

    private String getType( TypeDefinition type ) {
        String ret = "";
        if ( !types_vector.contains( type.id() ) && !NativeType.isNativeTypeKeyword( type.id() )) {
            System.out.print("type " + type.id() +":" );
            if ( type instanceof TypeDefinitionLink ) {
                System.out.println( ((TypeDefinitionLink) type).linkedTypeName() );
                if ( aux_flag == false ) { aux_types_vector.add( ((TypeDefinitionLink) type).linkedType() ); }
            } else {
                System.out.print( type.nativeType().id() );
                if ( ((TypeInlineDefinition) type).hasSubTypes() ) {
                    System.out.println("{");
                    for ( Entry<String, TypeDefinition> entry: ((TypeInlineDefinition) type).subTypes() ) {
                        System.out.println( getSubType( entry.getValue(), 1 ));
                    }
                    System.out.println("}");
                }
            }
            types_vector.add( type.id() );

        }
        return ret;
    }

    private void printType( String type ) {
        if ( !type.equals("") ) {
            System.out.println( type );
        }

    }

    private void createOutput( InputPortInfo inputPort ) {
        // types creation
        if ( ow_vector.size() > 0 ) {
            for ( int x = 0; x < ow_vector.size(); x++ ) {
                //System.out.println("// types for operation " + ow_vector.get(x).id() );
                printType( getType( ow_vector.get(x).requestType()));
            }
            System.out.println();
        }

        if ( rr_vector.size() > 0 ) {
            for ( int x = 0; x < rr_vector.size(); x++ ) {
                //System.out.println("// types for operation " + rr_vector.get(x).id() );
                printType( getType( rr_vector.get(x).requestType()));
                printType( getType( rr_vector.get(x).responseType()));
                for ( Entry<String, TypeDefinition> fault: rr_vector.get(x).faults().entrySet() ) {
                    if ( !fault.getKey().equals("undefined") ) {
                        System.out.println( getType( fault.getValue() ));
                    }
                }
            }
            System.out.println();
        }

        // add auxiliary types
        aux_flag = true;
        Iterator it = aux_types_vector.iterator();
        while ( it.hasNext() ) {
            printType( getType( (TypeDefinition) it.next() ));
        }

        System.out.println();

        // interface creation
        System.out.println("interface " + inputPort.id() + "Surface {");
        // oneway declaration
        if ( ow_vector.size() > 0 ) {
            System.out.println("OneWay:");
            for ( int x = 0; x < ow_vector.size(); x++ ) {
                if ( x != 0 ) {
                    System.out.println(",");
                }
                System.out.print( "\t" + getOWString( ow_vector.get(x)));
            }
            System.out.println();
        }
        // request response declaration
         if ( rr_vector.size() > 0 ) {
            System.out.println("RequestResponse:");
            for ( int x = 0; x < rr_vector.size(); x++ ) {
                if ( x != 0 ) {
                    System.out.println(",");
                }
                System.out.print( "\t" + getRRString( rr_vector.get(x)));
            }
            System.out.println();
        }
        System.out.println("}");
        System.out.println();

        // outputPort definition
        System.out.println("outputPort " + inputPort.id() + "{");
        System.out.println("\tLocation:\"" + inputPort.location() + "\"");
        System.out.println("\tProtocol:" + inputPort.protocolId() );
        System.out.println("\tInterfaces:" + inputPort.id() + "Surface");
        System.out.println("}");
    }
   

}
