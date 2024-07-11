/*
 * Copyright (C) 2006-2019 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang.parse;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import jolie.lang.CodeCheckException;
import jolie.lang.CodeCheckMessage;
import jolie.lang.Constants;
import jolie.lang.Constants.ExecutionMode;
import jolie.lang.Constants.OperandType;
import jolie.lang.Constants.OperationType;
import jolie.lang.parse.CorrelationFunctionInfo.CorrelationPairInfo;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CorrelationSetInfo.CorrelationAliasInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.DivideAssignStatement;
import jolie.lang.parse.ast.DocumentationComment;
import jolie.lang.parse.ast.EmbedServiceNode;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachArrayItemStatement;
import jolie.lang.parse.ast.ForEachSubNodeStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.ImportStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InputPortInfo.AggregationItemInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.InterfaceExtenderDefinition;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.MultiplyAssignStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ProvideUntilStatement;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SubtractAssignStatement;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.AndConditionNode;
import jolie.lang.parse.ast.expression.ConstantBoolExpression;
import jolie.lang.parse.ast.expression.ConstantDoubleExpression;
import jolie.lang.parse.ast.expression.ConstantIntegerExpression;
import jolie.lang.parse.ast.expression.ConstantLongExpression;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.FreshValueExpressionNode;
import jolie.lang.parse.ast.expression.IfExpressionNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.InstanceOfExpressionNode;
import jolie.lang.parse.ast.expression.IsTypeExpressionNode;
import jolie.lang.parse.ast.expression.NotExpressionNode;
import jolie.lang.parse.ast.expression.OrConditionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.SolicitResponseExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.lang.parse.ast.expression.VoidExpressionNode;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;
import jolie.lang.parse.module.SymbolTable;
import jolie.util.ArrayListMultiMap;
import jolie.util.MultiMap;
import jolie.util.Pair;

/**
 * Checks the well-formedness and validity of a JOLIE program.
 *
 * @see Program
 * @author Fabrizio Montesi
 */
public class SemanticVerifier implements UnitOLVisitor {
	public static class Configuration {
		private boolean checkForMain = true;
		private final String executionTarget;

		public Configuration( String executionTarget ) {
			this.executionTarget = executionTarget;
		}

		public void setCheckForMain( boolean checkForMain ) {
			this.checkForMain = checkForMain;
		}

		public boolean checkForMain() {
			return checkForMain;
		}

		public String executionTarget() {
			return executionTarget;
		}
	}

	private final Program program;
	private final List< CodeCheckMessage > errors = new ArrayList<>();
	private final Configuration configuration;

	private ExecutionInfo executionInfo = new ExecutionInfo( URIParsingContext.DEFAULT, ExecutionMode.SINGLE );
	private final Map< String, InputPortInfo > inputPorts = new HashMap<>();
	private final Map< String, OutputPortInfo > outputPorts = new HashMap<>();

	private final Set< String > subroutineNames = new HashSet<>();
	private final Map< String, OneWayOperationDeclaration > oneWayOperations =
		new HashMap<>();
	private final Map< String, RequestResponseOperationDeclaration > requestResponseOperations =
		new HashMap<>();

	private final Map< TypeDefinition, List< TypeDefinition > > typesToBeEqual = new HashMap<>();
	private final Map< OneWayOperationDeclaration, List< OneWayOperationDeclaration > > owToBeEqual =
		new HashMap<>();
	private final Map< RequestResponseOperationDeclaration, List< RequestResponseOperationDeclaration > > rrToBeEqual =
		new HashMap<>();
	private final List< CorrelationSetInfo > correlationSets = new LinkedList<>();

	private boolean insideInputPort = false;
	private boolean insideInit = false;
	private boolean mainDefined = false;
	private final CorrelationFunctionInfo correlationFunctionInfo = new CorrelationFunctionInfo();
	private final MultiMap< String, String > inputTypeNameMap =
		new ArrayListMultiMap<>(); // Maps type names to the input operations that use them

	private ExecutionMode executionMode = ExecutionMode.SINGLE;

	private static final Logger LOGGER = Logger.getLogger( "JOLIE" );

	private final Map< String, TypeDefinition > definedTypes;
	// private TypeDefinition rootType; // the type representing the whole session state
	private final Map< String, Boolean > constantFlags = new HashMap<>();

	private OperationType insideCourierOperationType = null;
	private InputPortInfo courierInputPort = null;
	private final Map< URI, SymbolTable > symbolTables;

	private final Deque< String > inScopes = new ArrayDeque<>();

	private final Map< String, ServiceNode > services;

	public SemanticVerifier( Program program, Map< URI, SymbolTable > symbolTables,
		Configuration configuration ) {
		this.program = program;
		this.definedTypes = OLParser.createTypeDeclarationMap( program.context() );
		this.configuration = configuration;
		this.symbolTables = symbolTables;
		this.services = new HashMap<>();
	}

	/**
	 * Returns the symbolTables. Used by the languageserver for vscode extension
	 *
	 * @return symbolTebles
	 */
	public Map< URI, SymbolTable > symbolTables() {
		return symbolTables;
	}

	public CorrelationFunctionInfo correlationFunctionInfo() {
		return correlationFunctionInfo;
	}

	public ExecutionMode executionMode() {
		return executionMode;
	}

	private void encounteredAssignment( String varName ) {
		constantFlags.put( varName, !constantFlags.containsKey( varName ) );
	}

	private void addTypeEqualnessCheck( TypeDefinition key, TypeDefinition type ) {
		List< TypeDefinition > toBeEqualList = typesToBeEqual.computeIfAbsent( key, k -> new LinkedList<>() );
		toBeEqualList.add( type );
	}

	private void addOneWayEqualnessCheck( OneWayOperationDeclaration key, OneWayOperationDeclaration oneWay ) {
		List< OneWayOperationDeclaration > toBeEqualList = owToBeEqual.computeIfAbsent( key, k -> new LinkedList<>() );
		toBeEqualList.add( oneWay );
	}

	private void addRequestResponseEqualnessCheck( RequestResponseOperationDeclaration key,
		RequestResponseOperationDeclaration requestResponse ) {
		List< RequestResponseOperationDeclaration > toBeEqualList =
			rrToBeEqual.computeIfAbsent( key, k -> new LinkedList<>() );
		toBeEqualList.add( requestResponse );
	}

	private void encounteredAssignment( VariablePathNode path ) {
		try {
			String varName = ((ConstantStringExpression) path.path().get( 0 ).key()).value();
			if( this.inScopes.contains( varName ) ) {
				warning( path, "DEPRECATION: usage of same variable name \"" + varName + "\" inside scope \""
					+ this.inScopes.toString() + "\"" );
			}
			encounteredAssignment( varName );
		} catch( IndexOutOfBoundsException | ClassCastException e ) {
			error( path, path.toPrettyString() + " is an invalid path" );
		}
	}

	public Map< String, Boolean > constantFlags() {
		return constantFlags;
	}

	private void warning( OLSyntaxNode node, String message ) {
		if( node == null ) {
			LOGGER.warning( message );
		} else {
			LOGGER.warning( node.context().sourceName() + ":" + (node.context().startLine() + 1) + ": " + message );
		}
	}

	private void error( OLSyntaxNode node, String message ) {
		errors.add( CodeCheckMessage.buildWithoutHelp( node, message ) );
	}

	private void checkToBeEqualTypes() {
		for( Entry< TypeDefinition, List< TypeDefinition > > entry : typesToBeEqual.entrySet() ) {
			for( TypeDefinition type : entry.getValue() ) {
				if( !entry.getKey().isEquivalentTo( type ) ) {
					error( type, "type " + type.name() + " has already been defined with a different structure" );
				}
			}
		}

		for( Entry< OneWayOperationDeclaration, List< OneWayOperationDeclaration > > entry : owToBeEqual.entrySet() ) {
			for( OneWayOperationDeclaration ow : entry.getValue() ) {
				checkEqualness( entry.getKey(), ow );
			}
		}

		for( Entry< RequestResponseOperationDeclaration, List< RequestResponseOperationDeclaration > > entry : rrToBeEqual
			.entrySet() ) {
			for( RequestResponseOperationDeclaration rr : entry.getValue() ) {
				checkEqualness( entry.getKey(), rr );
			}
		}
	}

	private void checkCorrelationSets() {
		Collection< String > operations;
		Set< String > correlatingOperations = new HashSet<>();
		Set< String > currCorrelatingOperations = new HashSet<>();
		for( CorrelationSetInfo cset : correlationSets ) {
			correlationFunctionInfo.correlationSets().add( cset );
			currCorrelatingOperations.clear();
			for( CorrelationSetInfo.CorrelationVariableInfo csetVar : cset.variables() ) {
				for( CorrelationAliasInfo alias : csetVar.aliases() ) {
					checkCorrelationAlias( alias );

					operations = inputTypeNameMap.get( alias.guardName().name() );
					for( String operationName : operations ) {
						currCorrelatingOperations.add( operationName );
						correlationFunctionInfo.putCorrelationPair(
							operationName,
							new CorrelationPairInfo(
								csetVar.correlationVariablePath(),
								alias.variablePath() ) );
					}
				}
			}
			for( String operationName : currCorrelatingOperations ) {
				if( correlatingOperations.contains( operationName ) ) {
					error( cset, "Operation " + operationName +
						" is specified on more than one correlation set. Each operation can correlate using only one correlation set." );
				} else {
					correlatingOperations.add( operationName );
					correlationFunctionInfo.operationCorrelationSetMap().put( operationName, cset );
					correlationFunctionInfo.correlationSetOperations().put( cset, operationName );
				}
			}
		}

		Collection< CorrelationPairInfo > pairs;
		for( Map.Entry< String, CorrelationSetInfo > entry : correlationFunctionInfo.operationCorrelationSetMap()
			.entrySet() ) {
			pairs = correlationFunctionInfo.getOperationCorrelationPairs( entry.getKey() );
			if( pairs.size() != entry.getValue().variables().size() ) {
				error( entry.getValue(), "Operation " + entry.getKey() +
					" has not an alias specified for every variable in the correlation set." );
			}
		}
	}

	private void checkCorrelationAlias( CorrelationAliasInfo alias ) {
		TypeDefinition type = alias.guardName();
		if( type == null ) {
			error( alias.variablePath(), "type " + alias.guardName() + " is undefined" );
		} else if( type.containsPath( alias.variablePath() ) == false ) {
			error( alias.variablePath(), "type " + alias.guardName() + " does not contain the specified path" );
		}
	}

	/**
	 * Finds declared symbol by name, perform lookup at both from a program's source or from the given
	 * context. Since the context can be differ from program source if the symbol declared in the
	 * include directive case.
	 */
	private boolean hasSymbolDefined( String name, ParsingContext context ) {
		return (symbolTables.get( program.context().source() ) != null
			&& symbolTables.get( program.context().source() ).getSymbol( name ).isPresent())
			|| (symbolTables.get( context.source() ) != null
				&& symbolTables.get( context.source() ).getSymbol( name ).isPresent());
	}

	public void validate()
		throws CodeCheckException {
		program.accept( this );
		if( services.values().isEmpty() ) {
			// this is an jolie's internal service (service with Interfaces)
			if( configuration.checkForMain && !mainDefined ) {
				error( program, "Main procedure is not defined" );
			}
		} else {
			ServiceNode executionService = null;
			if( services.values().size() == 1 ) {
				executionService = services.values().iterator().next();
			} else if( services.values().size() > 1 && configuration.checkForMain() ) {
				if( configuration.executionTarget == null ) {
					error( program, "Execution service is not defined from command line argument (--service or -s)" );
				} else if( !services.containsKey( configuration.executionTarget ) ) {
					error( program,
						"Execution service \"" + configuration.executionTarget + "\" is not defined in the module" );
				} else {
					executionService = services.get( configuration.executionTarget );
				}
			}
			if( executionService != null ) {
				executionService.program().accept( this );
				if( configuration.checkForMain && !mainDefined ) {
					// Noticed that sometimes the configuration.executionTarget is null, but the name of the service is
					// in the executionService
					if( configuration.executionTarget != null ) {
						error( executionService.node(),
							"Main procedure for service \"" + configuration.executionTarget + "\" is not defined" );
					} else if( executionService.name() != null ) {
						error( executionService.node(),
							"Main procedure for service \"" + executionService.name() + "\" is not defined" );
					} else {
						error( executionService.node(), "Main procedure for service is not defined" );
					}
				}
			}
		}
		checkToBeEqualTypes();
		checkCorrelationSets();

		if( !errors.isEmpty() ) {
			LOGGER.severe( "Aborting: input file semantically invalid." );
			/*
			 * for( SemanticException.SemanticError e : semanticException.getErrorList() ){ logger.severe(
			 * e.getMessage() ); }
			 */
			throw new CodeCheckException( errors );
		}
	}

	private boolean isTopLevelType = true;

	@Override
	public void visit( TypeInlineDefinition n ) {
		checkCardinality( n );
		boolean backupRootType = isTopLevelType;
		if( isTopLevelType ) {
			// Check if the type has already been defined with a different structure
			TypeDefinition type = definedTypes.get( n.name() );
			if( type != null ) {
				addTypeEqualnessCheck( type, n );
			}
		}

		isTopLevelType = false;

		if( n.hasSubTypes() ) {
			for( Entry< String, TypeDefinition > entry : n.subTypes() ) {
				entry.getValue().accept( this );
			}
		}

		isTopLevelType = backupRootType;

		if( isTopLevelType ) {
			definedTypes.put( n.name(), n );
		}
	}

	@Override
	public void visit( TypeDefinitionLink n ) {
		checkCardinality( n );
		if( isTopLevelType ) {
			// Check if the type has already been defined with a different structure
			TypeDefinition type = definedTypes.get( n.name() );
			if( type != null ) {
				addTypeEqualnessCheck( type, n );
			}
			definedTypes.put( n.name(), n );
		}
	}

	@Override
	public void visit( TypeChoiceDefinition n ) {
		checkCardinality( n );
		boolean backupRootType = isTopLevelType;
		if( isTopLevelType ) {
			// Check if the type has already been defined with a different structure
			TypeDefinition type = definedTypes.get( n.name() );
			if( type != null ) {
				addTypeEqualnessCheck( type, n );
			}
		}

		isTopLevelType = false;

		verify( n.left() );
		verify( n.right() );

		isTopLevelType = backupRootType;

		if( isTopLevelType ) {
			definedTypes.put( n.name(), n );
		}
	}

	private void checkCardinality( TypeDefinition type ) {
		if( type.cardinality().min() < 0 ) {
			error( type, "type " + type.name() + " specifies an invalid minimum range value (must be positive)" );
		}
		if( type.cardinality().max() < 0 ) {
			error( type, "type " + type.name() + " specifies an invalid maximum range value (must be positive)" );
		}
	}

	@Override
	public void visit( SpawnStatement n ) {
		n.body().accept( this );
	}

	@Override
	public void visit( DocumentationComment n ) {}

	@Override
	public void visit( Program n ) {
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	@Override
	public void visit( VariablePathNode n ) {
		if( insideInit && n.isCSet() ) {
			error( n, "Correlation variable access is forbidden in init procedures" );
		}

		if( n.isCSet() && !n.isStatic() ) {
			error( n, "Correlation paths must be statically defined" );
		}

		try {
			if( !(n.path().get( 0 ).key() instanceof ConstantStringExpression) ) {
				if( n.isGlobal() ) {
					error( n, "the global keyword in paths must be followed by an identifier" );
				} else if( n.isCSet() ) {
					error( n, "the csets keyword in paths must be followed by an identifier" );
				} else {
					error( n, "paths must start with an identifier" );
				}
			}
		} catch( IndexOutOfBoundsException e ) {
			error( n, "invalid path: " + n.toPrettyString() );
		}
	}

	@Override
	public void visit( final InputPortInfo n ) {
		if( inputPorts.get( n.id() ) != null ) {
			error( n, "input port " + n.id() + " has been already defined" );
		}

		if( n.protocol() != null && !(n.protocol() instanceof ConstantStringExpression
			|| n.protocol() instanceof InlineTreeExpressionNode
			|| n.protocol() instanceof VariableExpressionNode) ) {
			error( n, "input port " + n.id() + "'s protocol is not a valid expression" );
		}

		if( n.location() != null
			&& !(n.location() instanceof ConstantStringExpression || n.location() instanceof VariableExpressionNode) ) {
			error( n, "input port " + n.id() + "'s location is not a valid expression" );
		}

		if( n.location() instanceof ConstantStringExpression ) {
			try {
				URI.create( n.location().toString() );
			} catch( IllegalArgumentException e ) {
				error( n, "input port " + n.id() + "'s location is not a valid URI" );
			}
		}

		inputPorts.put( n.id(), n );

		insideInputPort = true;

		Set< String > opSet = new HashSet<>();

		for( OperationDeclaration op : n.operations() ) {
			if( opSet.contains( op.id() ) ) {
				error( n, "input port " + n.id() + " declares operation " + op.id() + " multiple times" );
			} else {
				opSet.add( op.id() );
				op.accept( this );
			}
		}

		for( InputPortInfo.AggregationItemInfo item : n.aggregationList() ) {
			for( String portName : item.outputPortList() ) {
				final OutputPortInfo outputPort = outputPorts.get( portName );
				if( outputPort == null ) {
					error( n, "input port " + n.id() + " aggregates an undefined output port (" + portName + ")" );
				} else {
					if( item.interfaceExtender() != null ) {
						outputPort.operations().forEach( opDecl -> {
							final TypeDefinition requestType =
								opDecl instanceof OneWayOperationDeclaration
									? ((OneWayOperationDeclaration) opDecl).requestType()
									: ((RequestResponseOperationDeclaration) opDecl).requestType();
							/*
							 * if ( requestType instanceof TypeInlineDefinition == false ) { error( n, "input port " +
							 * n.id() + " is trying to extend the type of operation " + opDecl.id() + " in output port "
							 * + outputPort.id() +
							 * " but such operation has an unsupported type structure (type reference or type choice)"
							 * ); }
							 */if( requestType instanceof TypeInlineDefinition
								&& ((TypeInlineDefinition) requestType).untypedSubTypes() ) {
								error( n,
									"input port " + n.id()
										+ " is trying to extend the type of operation " + opDecl.id()
										+ " in output port " + outputPort.id()
										+ " but such operation has undefined subnode types ({ ? } or undefined)" );
							}
						} );
					}
				}

				/*
				 * else { for( OperationDeclaration op : outputPort.operations() ) { if ( opSet.contains( op.id() )
				 * ) { error( n, "input port " + n.id() + " declares duplicate operation " + op.id() +
				 * " from aggregated output port " + outputPort.id() ); } else { opSet.add( op.id() ); } } }
				 */
			}
		}

		insideInputPort = false;
	}

	@Override
	public void visit( OutputPortInfo n ) {
		if( outputPorts.get( n.id() ) != null )
			error( n, "output port " + n.id() + " has been already defined" );

		if( n.protocol() != null && !(n.protocol() instanceof ConstantStringExpression
			|| n.protocol() instanceof InlineTreeExpressionNode || n.protocol() instanceof VariableExpressionNode
			|| n.location() instanceof VariablePathNode) ) {
			error( n, "output port " + n.id() + "'s protocol is not a valid expression" );
		}

		if( n.location() != null
			&& !(n.location() instanceof ConstantStringExpression || n.location() instanceof VariableExpressionNode) ) {
			error( n, "output port " + n.id() + "'s location is not a valid expression" );
		}

		if( n.location() instanceof ConstantStringExpression ) {
			try {
				URI.create( n.location().toString() );
			} catch( IllegalArgumentException e ) {
				error( n, "input port " + n.id() + "'s location is not a valid URI" );
			}
		}

		outputPorts.put( n.id(), n );

		encounteredAssignment( n.id() );

		for( OperationDeclaration op : n.operations() ) {
			op.accept( this );
		}
	}

	@Override
	public void visit( OneWayOperationDeclaration n ) {
		if( definedTypes.get( n.requestType().name() ) == null ) {
			if( !hasSymbolDefined( n.requestType().name(), n.context() ) ) {
				error( n, "unknown type: " + n.requestType().name() + " for operation " + n.id() );
			}
		}

		if( insideInputPort ) { // Input operation
			if( oneWayOperations.containsKey( n.id() ) ) {
				OneWayOperationDeclaration other = oneWayOperations.get( n.id() );
				addOneWayEqualnessCheck( n, other );
			} else {
				oneWayOperations.put( n.id(), n );
				inputTypeNameMap.put( n.requestType().name(), n.id() );
			}
		}
	}

	@Override
	public void visit( RequestResponseOperationDeclaration n ) {
		if( definedTypes.get( n.requestType().name() ) == null ) {
			if( !hasSymbolDefined( n.requestType().name(), n.context() ) ) {
				error( n, "unknown type: " + n.requestType().name() + " for operation " + n.id() );
			}
		}
		if( definedTypes.get( n.responseType().name() ) == null ) {
			if( !hasSymbolDefined( n.responseType().name(), n.context() ) ) {
				error( n, "unknown type: " + n.responseType().name() + " for operation " + n.id() );
			}
		}
		for( Entry< String, TypeDefinition > fault : n.faults().entrySet() ) {
			if( definedTypes.get( fault.getValue().name() ) == null ) {
				if( !hasSymbolDefined( fault.getValue().name(), n.context() ) ) {
					error( n, "unknown type for fault " + fault.getKey() );
				}
			}
		}

		if( insideInputPort ) { // Input operation
			if( requestResponseOperations.containsKey( n.id() ) ) {
				RequestResponseOperationDeclaration other = requestResponseOperations.get( n.id() );
				addRequestResponseEqualnessCheck( n, other );
			} else {
				requestResponseOperations.put( n.id(), n );
				inputTypeNameMap.put( n.requestType().name(), n.id() );
			}
		}
	}

	private void checkEqualness( OneWayOperationDeclaration n, OneWayOperationDeclaration other ) {
		if( n.requestType().isEquivalentTo( other.requestType() ) == false ) {
			error( n,
				"input operations sharing the same name cannot declare different request types (One-Way operation "
					+ n.id() + ")" );
		}
	}

	private void checkEqualness( RequestResponseOperationDeclaration n, RequestResponseOperationDeclaration other ) {
		if( n.requestType().isEquivalentTo( other.requestType() ) == false ) {
			error( n,
				"input operations sharing the same name cannot declare different request types (Request-Response operation "
					+ n.id() + ")" );
		}

		if( n.responseType().isEquivalentTo( other.responseType() ) == false ) {
			error( n,
				"input operations sharing the same name cannot declare different response types (Request-Response operation "
					+ n.id() + ")" );
		}

		if( n.faults().size() != other.faults().size() ) {
			error( n,
				"input operations sharing the same name cannot declared different fault types (Request-Response operation "
					+ n.id() );
		}

		for( Entry< String, TypeDefinition > fault : n.faults().entrySet() ) {
			if( fault.getValue() != null ) {
				if( !other.faults().containsKey( fault.getKey() )
					|| !other.faults().get( fault.getKey() ).isEquivalentTo( fault.getValue() ) ) {
					error( n,
						"input operations sharing the same name cannot declared different fault types (Request-Response operation "
							+ n.id() );
				}
			}
		}
	}

	@Override
	public void visit( DefinitionNode n ) {
		if( subroutineNames.contains( n.id() ) ) {
			error( n, "Procedure " + n.id() + " uses an already defined identifier" );
		} else {
			subroutineNames.add( n.id() );
		}

		if( "main".equals( n.id() ) ) {
			mainDefined = true;
			if( executionInfo.mode() != ExecutionMode.SINGLE ) {
				if( (n.body() instanceof NDChoiceStatement
					|| n.body() instanceof RequestResponseOperationStatement
					|| n.body() instanceof OneWayOperationStatement) == false ) {
					// The main body is not an input
					if( n.body() instanceof SequenceStatement ) {
						OLSyntaxNode first = ((SequenceStatement) n.body()).children().get( 0 );
						if( (first instanceof RequestResponseOperationStatement
							|| first instanceof OneWayOperationStatement) == false ) {
							// The main body is not even a sequence starting with an input
							error( n.body(),
								"If execution is not single, the body of main must be either an input choice or a sequence that starts with an input statement (request-response or one-way)" );
						}
					} else {
						// The main body is not even a sequence
						error( n.body(),
							"If execution is not single, the body of main must be either an input choice or a sequence that starts with an input statement (request-response or one-way)" );
					}
				}
			}
		}
		if( n.id().equals( "init" ) ) {
			insideInit = true;
		}
		n.body().accept( this );
		insideInit = false;
	}

	@Override
	public void visit( ParallelStatement stm ) {
		for( OLSyntaxNode node : stm.children() ) {
			node.accept( this );
		}
	}

	@Override
	public void visit( SequenceStatement stm ) {
		for( OLSyntaxNode node : stm.children() ) {
			node.accept( this );
		}
	}

	@Override
	public void visit( NDChoiceStatement stm ) {
		Set< String > operations = new HashSet<>();
		String name = null;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : stm.children() ) {
			if( pair.key() instanceof OneWayOperationStatement ) {
				name = ((OneWayOperationStatement) pair.key()).id();
			} else if( pair.key() instanceof RequestResponseOperationStatement ) {
				name = ((RequestResponseOperationStatement) pair.key()).id();
			} else {
				error( pair.key(), "Input choices can contain only One-Way or Request-Response guards" );
			}
			if( operations.contains( name ) ) {
				error( pair.key(),
					"Input choices can not have duplicate input guards (input statement for operation " + name + ")" );
			} else {
				operations.add( name );
			}
			pair.key().accept( this );
			pair.value().accept( this );
		}
	}

	@Override
	public void visit( NotificationOperationStatement n ) {
		OutputPortInfo p = outputPorts.get( n.outputPortId() );
		if( p == null ) {
			error( n, n.outputPortId() + " is not a valid output port" );
		} else {
			OperationDeclaration decl = p.operationsMap().get( n.id() );
			if( decl == null )
				error( n, "Operation " + n.id() + " has not been declared in output port type " + p.id() );
			else if( !(decl instanceof OneWayOperationDeclaration) )
				error( n, "Operation " + n.id() + " is not a valid one-way operation in output port " + p.id() );
		}
	}

	@Override
	public void visit( SolicitResponseOperationStatement n ) {
		if( n.inputVarPath() != null ) {
			encounteredAssignment( n.inputVarPath() );
		}

		checkSolicitResponseInOutputPort( n, n.id(), n.outputPortId() );

		/*
		 * if ( n.inputVarPath() != null && n.inputVarPath().isCSet() ) { error( n,
		 * "Receiving a message in a correlation variable is forbidden" ); }
		 */
	}

	@Override
	public void visit( ThrowStatement n ) {
		verify( n.expression() );
	}

	@Override
	public void visit( CompensateStatement n ) {}

	@Override
	public void visit( InstallStatement n ) {
		for( Pair< String, OLSyntaxNode > pair : n.handlersFunction().pairs() ) {
			pair.value().accept( this );
		}
	}

	@Override
	public void visit( Scope n ) {
		this.inScopes.push( n.id() );
		n.body().accept( this );
		this.inScopes.pop();
	}

	@Override
	public void visit( OneWayOperationStatement n ) {
		if( insideCourierOperationType != null ) {
			error( n, "input statements are forbidden inside courier definitions" );
		}
		verify( n.inputVarPath() );
		if( n.inputVarPath() != null ) {
			if( n.inputVarPath().isCSet() ) {
				error( n, "Receiving a message in a correlation variable is forbidden" );
			}
			if( n.inputVarPath().isGlobal() ) {
				error( n, "Receiving a message in a global variable is forbidden" );
			}
			encounteredAssignment( n.inputVarPath() );
		}
	}

	@Override
	public void visit( RequestResponseOperationStatement n ) {
		if( insideCourierOperationType != null ) {
			error( n, "input statements are forbidden inside courier definitions" );
		}
		verify( n.inputVarPath() );
		verify( n.process() );
		if( n.inputVarPath() != null ) {
			if( n.inputVarPath().isCSet() ) {
				error( n, "Receiving a message in a correlation variable is forbidden" );
			}
			if( n.inputVarPath().isGlobal() ) {
				error( n, "Receiving a message in a global variable is forbidden" );
			}
			encounteredAssignment( n.inputVarPath() );
		}
	}

	@Override
	public void visit( LinkInStatement n ) {}

	@Override
	public void visit( LinkOutStatement n ) {}

	@Override
	public void visit( SynchronizedStatement n ) {
		n.body().accept( this );
	}

	@Override
	public void visit( AssignStatement n ) {
		n.variablePath().accept( this );
		encounteredAssignment( n.variablePath() );
		n.expression().accept( this );
	}

	@Override
	public void visit( InstanceOfExpressionNode n ) {
		n.expression().accept( this );
	}

	@Override
	public void visit( AddAssignStatement n ) {
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		n.expression().accept( this );
	}

	@Override
	public void visit( SubtractAssignStatement n ) {
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		n.expression().accept( this );
	}

	@Override
	public void visit( MultiplyAssignStatement n ) {
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		n.expression().accept( this );
	}

	@Override
	public void visit( DivideAssignStatement n ) {
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		n.expression().accept( this );
	}

	private void verify( OLSyntaxNode n ) {
		if( n != null ) {
			n.accept( this );
		}
	}

	private void verify( OLSyntaxNode... nodes ) {
		for( OLSyntaxNode node : nodes ) {
			verify( node );
		}
	}

	@Override
	public void visit( IfExpressionNode n ) {
		verify( n.guard(), n.thenExpression(), n.elseExpression() );
	}

	@Override
	public void visit( PointerStatement n ) {
		encounteredAssignment( n.leftPath() );
		encounteredAssignment( n.rightPath() );
		n.leftPath().accept( this );
		n.rightPath().accept( this );

		if( n.rightPath().isCSet() ) {
			error( n, "Making an alias to a correlation variable is forbidden" );
		}
	}

	@Override
	public void visit( DeepCopyStatement n ) {
		encounteredAssignment( n.leftPath() );
		n.leftPath().accept( this );
		n.rightExpression().accept( this );
		if( n.leftPath().isCSet() ) {
			error( n, "Deep copy on a correlation variable is forbidden" );
		}
	}

	@Override
	public void visit( IfStatement n ) {
		for( Pair< OLSyntaxNode, OLSyntaxNode > choice : n.children() ) {
			verify( choice.key() );
			verify( choice.value() );
		}
		verify( n.elseProcess() );
	}

	@Override
	public void visit( DefinitionCallStatement n ) {
		if( !subroutineNames.contains( n.id() ) ) {
			error( n, "Call to undefined definition: " + n.id() );
		}
	}

	@Override
	public void visit( WhileStatement n ) {
		n.condition().accept( this );
		n.body().accept( this );
	}

	@Override
	public void visit( OrConditionNode n ) {
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	@Override
	public void visit( AndConditionNode n ) {
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	@Override
	public void visit( NotExpressionNode n ) {
		n.expression().accept( this );
	}

	@Override
	public void visit( CompareConditionNode n ) {
		n.leftExpression().accept( this );
		n.rightExpression().accept( this );
	}

	@Override
	public void visit( ConstantIntegerExpression n ) {}

	@Override
	public void visit( ConstantDoubleExpression n ) {}

	@Override
	public void visit( ConstantStringExpression n ) {}

	@Override
	public void visit( ConstantLongExpression n ) {}

	@Override
	public void visit( ConstantBoolExpression n ) {}

	@Override
	public void visit( ProductExpressionNode n ) {
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
		}
	}

	@Override
	public void visit( SumExpressionNode n ) {
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
		}
	}

	@Override
	public void visit( VariableExpressionNode n ) {
		n.variablePath().accept( this );
	}

	@Override
	public void visit( InstallFixedVariableExpressionNode n ) {
		n.variablePath().accept( this );
	}

	@Override
	public void visit( NullProcessStatement n ) {}

	@Override
	public void visit( ExitStatement n ) {}

	@Override
	public void visit( ExecutionInfo n ) {
		executionMode = n.mode();
		executionInfo = n;
	}

	@Override
	public void visit( CorrelationSetInfo n ) {
		VariablePathSet< VariablePathNode > pathSet = new VariablePathSet<>();

		VariablePathNode path;
		for( CorrelationSetInfo.CorrelationVariableInfo csetVar : n.variables() ) {
			path = csetVar.correlationVariablePath();
			if( path.isGlobal() ) {
				error( path, "Correlation variables can not be global" );
			} else if( path.isCSet() ) {
				error( path, "Correlation variables can not be in the csets structure" );
			} else {
				if( path.isStatic() == false ) {
					error( path, "correlation variable paths can not make use of dynamic evaluation" );
				}
			}

			if( pathSet.contains( path ) ) {
				error( path, "Duplicate correlation variable" );
			} else {
				pathSet.add( path );
			}

			for( CorrelationAliasInfo alias : csetVar.aliases() ) {
				if( alias.variablePath().isGlobal() ) {
					error( alias.variablePath(), "Correlation variables can not be global" );
				} else if( path.isCSet() ) {
					error( alias.variablePath(), "Correlation variables can not be in the csets structure" );
				} else {
					if( alias.variablePath().isStatic() == false ) {
						error( alias.variablePath(),
							"correlation variable path aliases can not make use of dynamic evaluation" );
					}
				}
			}
		}
		correlationSets.add( n );
		/*
		 * VariablePathNode varPath; List< Pair< OLSyntaxNode, OLSyntaxNode > > path; for( List<
		 * VariablePathNode > list : n.variables() ) { varPath = list.get( 0 ); if ( varPath.isGlobal() ) {
		 * error( list.get( 0 ), "Correlation variables can not be global" ); } path = varPath.path(); if (
		 * path.size() > 1 ) { error( varPath, "Correlation variables can not be nested paths" ); } else if
		 * ( path.get( 0 ).value() != null ) { error( varPath, "Correlation variables can not use arrays" );
		 * } else { correlationSet.add( ((ConstantStringExpression)path.get( 0 ).key()).value() ); } }
		 */
	}

	@Override
	public void visit( RunStatement n ) {
		warning( n, "Run statement is not a stable feature yet." );
	}

	@Override
	public void visit( ValueVectorSizeExpressionNode n ) {
		n.variablePath().accept( this );
	}

	@Override
	public void visit( InlineTreeExpressionNode n ) {
		n.rootExpression().accept( this );
		for( InlineTreeExpressionNode.Operation operation : n.operations() ) {
			if( operation instanceof InlineTreeExpressionNode.AssignmentOperation ) {
				InlineTreeExpressionNode.AssignmentOperation op =
					(InlineTreeExpressionNode.AssignmentOperation) operation;
				go( op.path() );
				go( op.expression() );
			} else if( operation instanceof InlineTreeExpressionNode.DeepCopyOperation ) {
				InlineTreeExpressionNode.DeepCopyOperation op = (InlineTreeExpressionNode.DeepCopyOperation) operation;
				go( op.path() );
				go( op.expression() );
			} else if( operation instanceof InlineTreeExpressionNode.PointsToOperation ) {
				InlineTreeExpressionNode.PointsToOperation op = (InlineTreeExpressionNode.PointsToOperation) operation;
				go( op.path() );
				go( op.target() );
			} else {
				error( n, "incomplete case analysis for InlineTreeExpressionNode.Operation (" + n.getClass() + ")" );
			}
		}
	}

	@Override
	public void visit( PreIncrementStatement n ) {
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
	}

	@Override
	public void visit( PostIncrementStatement n ) {
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
	}

	@Override
	public void visit( PreDecrementStatement n ) {
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
	}

	@Override
	public void visit( PostDecrementStatement n ) {
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
	}

	@Override
	public void visit( UndefStatement n ) {
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		if( n.variablePath().isCSet() ) {
			error( n, "Undefining a correlation variable is forbidden" );
		}
	}


	@Override
	public void visit( ForStatement n ) {
		n.init().accept( this );
		n.condition().accept( this );
		n.post().accept( this );
		n.body().accept( this );
	}

	@Override
	public void visit( ForEachSubNodeStatement n ) {
		n.keyPath().accept( this );
		n.targetPath().accept( this );
		n.body().accept( this );
	}

	@Override
	public void visit( ForEachArrayItemStatement n ) {
		n.keyPath().accept( this );
		n.targetPath().accept( this );
		n.body().accept( this );
	}

	@Override
	public void visit( IsTypeExpressionNode n ) {
		n.variablePath().accept( this );
	}

	@Override
	public void visit( TypeCastExpressionNode n ) {
		n.expression().accept( this );
	}

	@Override
	public void visit( EmbeddedServiceNode n ) {}

	@Override
	public void visit( InterfaceExtenderDefinition n ) {}

	@Override
	public void visit( CourierDefinitionNode n ) {
		courierInputPort = inputPorts.get( n.inputPortName() );

		if( courierInputPort == null ) {
			error( n, "undefined input port: " + n.inputPortName() );
			return;
		}

		verify( n.body() );
		courierInputPort = null;
	}

	private boolean isAggregated( String operation, InputPortInfo inputPort ) {
		for( AggregationItemInfo item : inputPort.aggregationList() ) {
			for( String outputPortName : item.outputPortList() ) {
				final OutputPortInfo outputPort = outputPorts.get( outputPortName );
				if( outputPort != null ) {
					if( outputPort.operationsMap().containsKey( operation ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void assertAggregated( OLSyntaxNode node, String operationName, InputPortInfo inputPort ) {
		if( !isAggregated( operationName, inputPort ) ) {
			error( node, operationName + " is not an aggregated operation at input port " + inputPort.id() );
		}
	}

	@Override
	public void visit( CourierChoiceStatement n ) {
		for( CourierChoiceStatement.InterfaceOneWayBranch branch : n.interfaceOneWayBranches() ) {
			insideCourierOperationType = OperationType.ONE_WAY;
			branch.interfaceDefinition.operationsMap().forEach(
				( opName, opDecl ) -> {
					if( opDecl instanceof OneWayOperationDeclaration ) {
						assertAggregated( n, opName, courierInputPort );
					}
				} );
			verify( branch.body );
		}

		for( CourierChoiceStatement.InterfaceRequestResponseBranch branch : n.interfaceRequestResponseBranches() ) {
			insideCourierOperationType = OperationType.REQUEST_RESPONSE;
			branch.interfaceDefinition.operationsMap().forEach(
				( opName, opDecl ) -> {
					if( opDecl instanceof RequestResponseOperationDeclaration ) {
						assertAggregated( n, opName, courierInputPort );
					}
				} );
			verify( branch.body );
		}

		for( CourierChoiceStatement.OperationOneWayBranch branch : n.operationOneWayBranches() ) {
			insideCourierOperationType = OperationType.ONE_WAY;
			assertAggregated( n, branch.operation, courierInputPort );
			verify( branch.body );
		}

		for( CourierChoiceStatement.OperationRequestResponseBranch branch : n.operationRequestResponseBranches() ) {
			insideCourierOperationType = OperationType.REQUEST_RESPONSE;
			assertAggregated( n, branch.operation, courierInputPort );
			verify( branch.body );
		}

		insideCourierOperationType = null;
	}

	/*
	 * todo: Check that the output port of the forward statement is right wrt the input port aggregation
	 * definition.
	 */
	@Override
	public void visit( NotificationForwardStatement n ) {
		if( insideCourierOperationType == null ) {
			error( n, "the forward statement may be used only inside a courier definition" );
		} else if( insideCourierOperationType != OperationType.ONE_WAY ) {
			error( n,
				"forward statement is a notification, but is inside a request-response courier definition. Maybe you wanted to specify a solicit-response forward?" );
		}
	}

	/**
	 * todo: Check that the output port of the forward statement is right wrt the input port aggregation
	 * definition.
	 */
	@Override
	public void visit( SolicitResponseForwardStatement n ) {
		if( insideCourierOperationType == null ) {
			error( n, "the forward statement may be used only inside a courier definition" );
		} else if( insideCourierOperationType != OperationType.REQUEST_RESPONSE ) {
			error( n,
				"forward statement is a solicit-response, but is inside a one-way courier definition. Maybe you wanted to specify a notification forward?" );
		}
	}

	/**
	 * todo: Must check if it's inside an install function
	 */
	@Override
	public void visit( CurrentHandlerStatement n ) {}

	@Override
	public void visit( InterfaceDefinition n ) {}

	@Override
	public void visit( FreshValueExpressionNode n ) {}

	@Override
	public void visit( VoidExpressionNode n ) {}

	@Override
	public void visit( ProvideUntilStatement n ) {
		if( !(n.provide() instanceof NDChoiceStatement) ) {
			error( n, "provide branch is not an input choice" );
		} else if( !(n.until() instanceof NDChoiceStatement) ) {
			error( n, "until branch is not an input choice" );
		}

		NDChoiceStatement provide = (NDChoiceStatement) n.provide();
		NDChoiceStatement until = (NDChoiceStatement) n.until();

		NDChoiceStatement total = new NDChoiceStatement( n.context() );
		total.children().addAll( provide.children() );
		total.children().addAll( until.children() );
		total.accept( this );
	}

	@Override
	public void visit( ImportStatement n ) {}

	@Override
	public void visit( ServiceNode n ) {
		if( n.type() == Constants.EmbeddedServiceType.SERVICENODE ) {
			this.services.put( n.name(), n );
		} else if( n.type() == Constants.EmbeddedServiceType.SERVICENODE_JAVA ) {
			boolean inputPortDefined = false;
			for( OLSyntaxNode node : n.program().children() ) {
				if( !(node instanceof InputPortInfo || node instanceof OutputPortInfo) ) {
					error( node, "foreign service " + n.name() + " only accepts communication ports declaration" );
				} else if( node instanceof InputPortInfo ) {
					if( inputPortDefined ) {
						error( node, "foreign service " + n.name()
							+ " should only have one inputPort defined" );
					}
					inputPortDefined = true;

					InputPortInfo ip = (InputPortInfo) node;
					if( ip.protocol() != null ) {
						error( ip, "port" + ip.id() + " in foreign service " + n.name()
							+ " should only have location and interfaces defined" );
					}
					if( ip.location() instanceof ConstantStringExpression ) {
						ConstantStringExpression location = (ConstantStringExpression) ip.location();
						if( !location.value().equals( "local" ) ) {
							error( ip, "port" + ip.id() + " in foreign service " + n.name()
								+ " should only have location with 'local' scheme" );
						}
					}
				} else if( node instanceof OutputPortInfo ) {
					OutputPortInfo op = (OutputPortInfo) node;
					if( op.protocol() != null ) {
						error( op, "port" + op.id() + " in foreign service " + n.name()
							+ " should only have location and interfaces defined" );
					}
					if( op.location() instanceof ConstantStringExpression ) {
						ConstantStringExpression location = (ConstantStringExpression) op.location();
						if( !location.value().startsWith( "local" ) ) {
							error( op, "port" + op.id() + " in foreign service " + n.name()
								+ " should only have location with 'local' scheme" );
						}
					}

				}
			}
		}
	}

	@Override
	public void visit( EmbedServiceNode n ) {
		if( !(n.service() instanceof ServiceNode) ) {
			error( n, "service " + n.serviceName() + " is not defined" );
		}
		if( n.bindingPort() != null && !outputPorts.containsKey( n.bindingPort().id() ) ) {
			error( n, "binding port is not defined" );
		}
	}

	@Override
	public void visit( SolicitResponseExpressionNode n ) {
		checkSolicitResponseInOutputPort( n, n.id(), n.outputPortId() );
	}

	private void checkSolicitResponseInOutputPort( OLSyntaxNode n, String id, String outputPortId ) {
		OutputPortInfo p = outputPorts.get( outputPortId );
		if( p == null ) {
			error( n, outputPortId + " is not a valid output port" );
		} else {
			OperationDeclaration decl = p.operationsMap().get( id );
			if( decl == null ) {
				error( n, "Operation " + id + " has not been declared in output port " + p.id() );
			} else if( !(decl instanceof RequestResponseOperationDeclaration) ) {
				error( n,
					"Operation " + id + " is not a valid request-response operation in output port " + p.id() );
			}
		}
	}
}
