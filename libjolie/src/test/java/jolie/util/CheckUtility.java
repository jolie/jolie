package jolie.util;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.module.SymbolInfo;
import jolie.lang.parse.module.SymbolTable;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

/**
 * This class holds utility functions for checking ASTnodes and Symbols for Jolie's module system.
 */
public class CheckUtility {

	/**
	 * checks if the symbol table has all given symbols name in expectedSymbols, throws an Exception if
	 * a symbol is not found
	 * 
	 * @param st Checking SymbolTable
	 * @param expectedSymbols Set of symbols name expected to be declared in SymbolTable st
	 * @throws Exception
	 */
	public static void checkSymbols( SymbolTable st, Set< String > expectedSymbols )
		throws Exception {
		for( SymbolInfo symbolInfo : st.symbols() ) {
			expectedSymbols.remove( symbolInfo.name() );
		}
		if( !expectedSymbols.isEmpty() ) {
			throw new Exception( "Symbols " + Arrays.toString( expectedSymbols.toArray() )
				+ " not found in table " + st.source().toString() );
		}
	}

	/**
	 * checking if the given type has pointer type assigned
	 * 
	 * @param td Checking type
	 * @throws Exception
	 */
	public static void checkTypeIsResolved( TypeDefinition td ) throws Exception {
		if( td instanceof TypeInlineDefinition ) {
			checkTypeIsResolved( (TypeInlineDefinition) td );
		} else if( td instanceof TypeDefinitionLink ) {
			checkTypeIsResolved( (TypeDefinitionLink) td );
		} else if( td instanceof TypeChoiceDefinition ) {
			checkTypeIsResolved( (TypeChoiceDefinition) td );
		}
	}

	/**
	 * checking if the TypeDefinitionLink has pointer assign
	 */
	private static void checkTypeIsResolved( TypeDefinitionLink td ) throws Exception {
		if( td.linkedType() == null ) {
			throw new Exception( "type " + td.id() + " not resolved" );
		}
	}

	/**
	 * checking if the all of it's subtype of type TypeDefinitionLink has pointer assigned
	 */
	private static void checkTypeIsResolved( TypeInlineDefinition td ) throws Exception {
		if( td.hasSubTypes() ) {
			for( Map.Entry< String, TypeDefinition > subtype : td.subTypes() ) {
				checkTypeIsResolved( subtype.getValue() );
			}
		}
	}

	/**
	 * checking if the all of it's choice of type TypeDefinitionLink has pointer assigned
	 */
	private static void checkTypeIsResolved( TypeChoiceDefinition td ) throws Exception {
		checkTypeIsResolved( td.left() );
		if( td.right() != null ) {
			checkTypeIsResolved( td.right() );
		}
	}

	/**
	 * checks if the ASTNode has all given types name in expectedTypes, throws an Exception is a type is
	 * not found
	 * 
	 * @param p Checking Program
	 * @param expectedTypes Set of types name expected to be declared in Program p
	 * @throws Exception
	 */
	public static void checkTypes( Program p, Set< String > expectedTypes ) throws Exception {
		ProgramInspector pi = ParsingUtils.createInspector( p );

		for( TypeDefinition td : pi.getTypes() ) {
			if( expectedTypes.contains( td.id() ) ) {
				expectedTypes.remove( td.id() );
			}
		}
		if( !expectedTypes.isEmpty() ) {
			throw new Exception(
				"type " + Arrays.toString( expectedTypes.toArray() ) + " not found" );
		}
	}

	/**
	 * checks if the given InterfaceDefinition id has all operation declared in expectedOps. And all of
	 * those operation's types are already resolved throws an Exception if some operation is missing in
	 * definition
	 * 
	 * @param id InterfaceDefiniton to check
	 * @param expectedOps Expected operation names
	 * @throws Exception
	 */
	private static void checkInterfaceOperation( InterfaceDefinition id, Set< String > expectedOps )
		throws Exception {
		for( OperationDeclaration od : id.operationsMap().values() ) {
			if( expectedOps.contains( od.id() ) ) {
				expectedOps.remove( od.id() );
			}

			// check type in ops is resolved
			if( od instanceof RequestResponseOperationDeclaration ) {
				RequestResponseOperationDeclaration rrd = (RequestResponseOperationDeclaration) od;
				checkTypeIsResolved( rrd.requestType() );
				checkTypeIsResolved( rrd.responseType() );
				if( rrd.faults() != null ) {
					for( TypeDefinition fault : rrd.faults().values() ) {
						checkTypeIsResolved( fault );
					}
				}
			} else {
				OneWayOperationDeclaration owd = (OneWayOperationDeclaration) od;
				checkTypeIsResolved( owd.requestType() );
			}
		}
		if( !expectedOps.isEmpty() ) {
			throw new Exception(
				"operation " + Arrays.toString( expectedOps.toArray() ) + " not found" );
		}
	}

	/**
	 * checks if given port has coresponding interfaces and operations accrording to PortStub instance
	 * throws an Exception if it is declared in diffent way
	 * 
	 * @param port Checking AST PortInfo
	 * @param ps PortStub instance
	 * @throws Exception
	 */
	public static void checkPort( PortInfo port, PortStub ps ) throws Exception {
		for( InterfaceDefinition id : port.getInterfaceList() ) {
			checkInterfaceOperation( id, ps.ifaces.get( id.name() ) );
			for( OperationDeclaration od : port.operationsMap().values() ) {
				if( ps.ops.contains( od.id() ) ) {
					ps.ops.remove( od.id() );
				}

				// check type in ops is set
				if( od instanceof RequestResponseOperationDeclaration ) {
					RequestResponseOperationDeclaration rrd =
						(RequestResponseOperationDeclaration) od;
					checkTypeIsResolved( rrd.requestType() );
					checkTypeIsResolved( rrd.responseType() );
				} else {
					OneWayOperationDeclaration owd = (OneWayOperationDeclaration) od;
					checkTypeIsResolved( owd.requestType() );
				}
			}
			if( !ps.ops.isEmpty() ) {
				throw new Exception(
					"operation " + Arrays.toString( ps.ops.toArray() ) + " not found" );
			}
		}
	}

	/**
	 * checks if the ASTNode has all given InputPort name in expectedPorts, throws an Exception is a
	 * port is not found or has different declaration
	 * 
	 * @param p Checking Program
	 * @param expectedPorts Map of Port name and PortStub expected to be declared in Program p
	 * @throws Exception
	 */
	public static void checkInputPorts( Program p, Map< String, PortStub > expectedPorts )
		throws Exception {
		ProgramInspector pi = ParsingUtils.createInspector( p );
		for( InputPortInfo port : pi.getInputPorts() ) {
			if( expectedPorts.containsKey( port.id() ) ) {
				PortStub ps = expectedPorts.get( port.id() );
				checkPort( port, ps );
				expectedPorts.remove( port.id(), ps );
			}
		}

		if( !expectedPorts.isEmpty() ) {
			throw new Exception(
				"port " + Arrays.toString( expectedPorts.keySet().toArray() ) + " not found" );
		}
	}


	/**
	 * checks if the ASTNode has all given OutputPort in expectedPorts, throws an Exception is a port is
	 * not found or has different declaration
	 * 
	 * @param p Checking Program
	 * @param expectedPorts Map of Port name and PortStub expected to be declared in Program p
	 * @throws Exception
	 */
	public static void checkOutputPorts( Program p, Map< String, PortStub > expectedPorts )
		throws Exception {

		ProgramInspector pi = ParsingUtils.createInspector( p );
		for( PortInfo port : pi.getOutputPorts() ) {
			if( expectedPorts.containsKey( port.id() ) ) {
				PortStub ps = expectedPorts.get( port.id() );
				checkPort( port, ps );
				expectedPorts.remove( port.id(), ps );
			}
		}

		if( !expectedPorts.isEmpty() ) {
			throw new Exception(
				"port " + Arrays.toString( expectedPorts.keySet().toArray() ) + " not found" );
		}
	}

	/**
	 * check if all SymbolInfos has a pointer to ASTnode set, throws an Exception if there is a node
	 * that has not pointer set
	 */
	public static void checkSymbolNodeLinked( SymbolTable st ) throws Exception {
		for( SymbolInfo si : st.symbols() ) {
			if( si.node() == null ) {
				throw new Exception(
					"external symbolinfo " + si.name() + " has no node reference" );
			}
		}
	}

	public static void checkSemantic( Program program, Map< URI, SymbolTable > symbolTables, boolean checkForMain )
		throws SemanticException {
		SemanticVerifier.Configuration conf = new SemanticVerifier.Configuration();
		conf.setCheckForMain( checkForMain );
		SemanticVerifier semanticVerifier = new SemanticVerifier( program, symbolTables, conf );
		semanticVerifier.validate();
	}
}
