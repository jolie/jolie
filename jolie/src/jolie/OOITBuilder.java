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

package jolie;


import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import jolie.lang.Constants;
import jolie.lang.Constants.OperandType;
import jolie.lang.NativeType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ParsingContext;
import jolie.lang.parse.ast.AndConditionNode;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.ConstantIntegerExpression;
import jolie.lang.parse.ast.ConstantRealExpression;
import jolie.lang.parse.ast.ConstantStringExpression;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ExpressionConditionNode;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallFunctionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.IsTypeExpressionNode;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotConditionNode;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OrConditionNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.ProductExpressionNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.net.OutputPort;
import jolie.net.ext.CommProtocolFactory;
import jolie.process.AssignmentProcess;
import jolie.process.CallProcess;
import jolie.process.CompensateProcess;
import jolie.process.CorrelatedInputProcess;
import jolie.process.CorrelatedProcess;
import jolie.process.CurrentHandlerProcess;
import jolie.process.DeepCopyProcess;
import jolie.process.DefinitionProcess;
import jolie.process.ExitProcess;
import jolie.process.ForEachProcess;
import jolie.process.ForProcess;
import jolie.process.IfProcess;
import jolie.process.InputProcess;
import jolie.process.InstallProcess;
import jolie.process.LinkInProcess;
import jolie.process.LinkOutProcess;
import jolie.process.MainDefinitionProcess;
import jolie.process.MakePointerProcess;
import jolie.process.NDChoiceProcess;
import jolie.process.NotificationProcess;
import jolie.process.NullProcess;
import jolie.process.OneWayProcess;
import jolie.process.ParallelProcess;
import jolie.process.PostDecrementProcess;
import jolie.process.PostIncrementProcess;
import jolie.process.PreDecrementProcess;
import jolie.process.PreIncrementProcess;
import jolie.process.Process;
import jolie.process.RequestResponseProcess;
import jolie.process.RunProcess;
import jolie.process.ScopeProcess;
import jolie.process.SequentialProcess;
import jolie.process.SolicitResponseProcess;
import jolie.process.SpawnProcess;
import jolie.process.SynchronizedProcess;
import jolie.process.ThrowProcess;
import jolie.process.UndefProcess;
import jolie.process.WhileProcess;
import jolie.runtime.AndCondition;
import jolie.runtime.CastIntExpression;
import jolie.runtime.CastRealExpression;
import jolie.runtime.CastStringExpression;
import jolie.runtime.CompareCondition;
import jolie.runtime.Condition;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoaderCreationException;
import jolie.runtime.Expression;
import jolie.runtime.Expression.Operand;
import jolie.runtime.ExpressionCondition;
import jolie.runtime.InstallFixedVariablePath;
import jolie.runtime.InvalidIdException;
import jolie.runtime.IsDefinedExpression;
import jolie.runtime.IsIntExpression;
import jolie.runtime.IsRealExpression;
import jolie.runtime.IsStringExpression;
import jolie.runtime.NotCondition;
import jolie.runtime.OneWayOperation;
import jolie.runtime.OrCondition;
import jolie.runtime.ProductExpression;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.SumExpression;
import jolie.runtime.Value;
import jolie.runtime.ValueVectorSizeExpression;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.util.Pair;

/**
 * Builds an interpretation tree by visiting a Jolie abstract syntax tree.
 * @author Fabrizio Montesi
 * @see OLVisitor
 */
public class OOITBuilder implements OLVisitor
{
	private Program program;
	private boolean valid = true;
	final private Interpreter interpreter;

	private String currentOutputPort = null;

	/**
	 * Constructor.
	 * @param interpreter the Interpreter requesting the interpretation tree building
	 * @param program the Program to generate the interpretation tree from
	 * @see Program
	 */
	public OOITBuilder( Interpreter interpreter, Program program )
	{
		this.interpreter = interpreter;
		this.program = program;
	}
	
	private void error( ParsingContext context, String message )
	{
		valid = false;
		String s = context.sourceName() + ":" + context.line() + ": " + message;
		interpreter.logger().severe( s );
	}
	
	private void error( ParsingContext context, Exception e )
	{
		valid = false;
		e.printStackTrace();
		error( context, e.getMessage() );
	}
	
	/**
	 * Launches the build process.
	 * 
	 * The Program passed to the constructor gets visited and the Interpreter 
	 * passed to the constructor is set with the necessary references
	 * to the interpretation tree.
	 * @return true if the build process is successfull, false otherwise
	 */
	public boolean build()
	{
		visit( program );
		
		return valid;
	}
	
	public void visit( ExecutionInfo n )
	{
		interpreter.setExecutionMode( n.mode() );
	}

	public void visit( VariablePathNode n )
	{}
	
	public void visit( CorrelationSetInfo n )
	{
		Set< List< VariablePath > > cset = new HashSet< List< VariablePath > > ();
		List< VariablePath > paths;
		for( List< VariablePathNode > list : n.cset() ) {
			paths = new Vector< VariablePath > ();
			for( VariablePathNode path : list )
				paths.add( buildVariablePath( path ) );
			cset.add( paths );
		}

		interpreter.setCorrelationSet( cset );
	}

	public void visit( OutputPortInfo n )
	{
		Process protocolConfigurationProcess = null;
		if ( n.protocolConfiguration() != null ) {
			n.protocolConfiguration().accept( this );
			protocolConfigurationProcess = currProcess;
		}

		interpreter.register( n.id(), new OutputPort(
						interpreter,
						n.id(),
						n.protocolId(),
						protocolConfigurationProcess,
						n.location()
					)
				);

		currentOutputPort = n.id();
		notificationTypes.put( currentOutputPort, new HashMap< String, Type >() );
		solicitResponseTypes.put( currentOutputPort, new HashMap< String, RequestResponseTypeDescription >() );
		for( OperationDeclaration decl : n.operations() ) {
			decl.accept( this );
		}
		currentOutputPort = null;
	}
	
	public void visit( EmbeddedServiceNode n )
	{
		VariablePath path = null;
		try {
			path = interpreter.getOutputPort( n.portId() ).locationVariablePath();
		} catch( InvalidIdException iie ) {}

		try {
			interpreter.addEmbeddedServiceLoader(
				EmbeddedServiceLoader.create(
						interpreter,
						n.type(),
						n.servicePath(),
						path
						) );
		} catch( EmbeddedServiceLoaderCreationException e ) {
			error( n.context(), e );
		}
	}

	public void visit( InputPortInfo n )
	{
		for( OperationDeclaration op : n.operations() ) {
			op.accept( this );
		}
		
		Map< String, OutputPort > redirectionMap =
			new HashMap< String, OutputPort > ();
		OutputPort oPort = null;
		for( Entry< String, String > entry : n.redirectionMap().entrySet() ) {
			try {
				oPort = interpreter.getOutputPort( entry.getValue() );
			} catch( InvalidIdException e ) {
				error( n.context(), "Unknown output port (" + entry.getValue() + ") in redirection for input port " + n.id() );
			}
			redirectionMap.put( entry.getKey(), oPort );
		}
		
		if ( n.location().toString().equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
			try {
				interpreter.commCore().addLocalInputPort( n.id(), n.operationsMap().keySet(), redirectionMap );
			} catch( IOException e ) {
				error( n.context(), e );
			}
			return;
		}
		
		String pId = n.protocolId();
		CommProtocolFactory protocolFactory = null;

		VariablePath protocolConfigurationPath =
			new VariablePathBuilder( true )
			.add( Constants.INPUT_PORTS_NODE_NAME, 0 )
			.add( n.id(), 0 )
			.add( Constants.PROTOCOL_NODE_NAME, 0 )
			.toVariablePath();
		try {
			protocolFactory = interpreter.commCore().getCommProtocolFactory( pId );
		} catch( IOException e ) {
			error( n.context(), e );
		}
		currProcess = null;
		if ( n.protocolConfiguration() != null ) {
			n.protocolConfiguration().accept( this );
		}

		if ( protocolFactory != null ) {
			try {
				interpreter.commCore().addInputPort(
					n.id(),
					n.location(),
					protocolFactory,
					protocolConfigurationPath,
					currProcess,
					n.operationsMap().keySet(),
					redirectionMap
				);
			} catch( IOException ioe ) {
				error( n.context(), ioe );
			}
		} else {
			error( n.context(), "Communication protocol extension for protocol " + pId + " not found." );
		}
	}

	private Process currProcess;
	private Expression currExpression;
	private Condition currCondition;
	private boolean canSpawnSession = false;
	private Type currType;
	boolean insideType = false;
	
	private Map< String, Type > types = new HashMap< String, Type >();

	final private Map< String, Map< String, Type > > notificationTypes =
		new HashMap< String, Map< String, Type > >(); // Maps output ports to their OW operation types
	final private Map< String, Map< String, RequestResponseTypeDescription > > solicitResponseTypes =
		new HashMap< String, Map< String, RequestResponseTypeDescription > >(); // Maps output ports to their RR operation types

	public void visit( TypeInlineDefinition n )
	{
		boolean backupInsideType = insideType;
		insideType = true;

		if ( n.untypedSubTypes() ) {
			currType = new Type( n.nativeType(), n.cardinality(), true, null );
		} else {
			Map< String, Type > subTypes = new HashMap< String, Type >();
			if ( n.subTypes() != null ) {
				for( Entry< String, TypeDefinition > entry : n.subTypes() ) {
					subTypes.put( entry.getKey(), buildType( entry.getValue() ) );
				}
			}
			currType = new Type( n.nativeType(), n.cardinality(), false, subTypes );
		}

		insideType = backupInsideType;

		if ( insideType == false && insideOperationDeclaration == false ) {
			types.put( n.id(), currType );
		}
	}

	public void visit( TypeDefinitionLink n )
	{
		if ( n.untypedSubTypes() ) {
			currType = new Type( n.nativeType(), n.cardinality(), true, null );
		} else {
			Map< String, Type > subTypes = new HashMap< String, Type >();
			if ( n.subTypes() != null ) {
				for( Entry< String, TypeDefinition > entry : n.subTypes() ) {
					subTypes.put( entry.getKey(), buildType( entry.getValue() ) );
				}
			}
			currType = new Type( n.nativeType(), n.cardinality(), false, subTypes );
		}
	}

	public void visit( Program p )
	{
		for( OLSyntaxNode node : p.children() )
			node.accept( this );
	}

	private boolean insideOperationDeclaration = false;

	public void visit( OneWayOperationDeclaration decl )
	{
		boolean backup = insideOperationDeclaration;
		insideOperationDeclaration = true;
		if ( currentOutputPort == null ) {
			// Register if not already present
			try {
				interpreter.getOneWayOperation( decl.id() );
			} catch( InvalidIdException e ) {
				interpreter.register( decl.id(), new OneWayOperation( decl.id(), types.get( decl.requestType().id() ) ) );
			}
		} else {
			if ( decl.requestType() != null ) {
				notificationTypes.get( currentOutputPort ).put( decl.id(), buildType( decl.requestType() ) );
			}
		}
		insideOperationDeclaration = backup;
	}

	public void visit( RequestResponseOperationDeclaration decl )
	{
		if ( currentOutputPort == null ) {
			// Register if not already present
			try {
				interpreter.getRequestResponseOperation( decl.id() );
			} catch( InvalidIdException e ) {
				Map< String, Type > faults = new HashMap< String, Type >();
				for( Entry< String, TypeDefinition > entry : decl.faults().entrySet() ) {
					faults.put( entry.getKey(), types.get( entry.getValue().id() ) );
				}
				interpreter.register(
					decl.id(),
					new RequestResponseOperation(
						decl.id(),
						( decl.requestType() == null ) ? null : types.get( decl.requestType().id() ),
						( decl.responseType() == null ) ? null : types.get( decl.responseType().id() ),
						faults
					)
				);
			}
		} else {
			Type requestType, responseType;
			Map< String, Type > faultTypes = new HashMap< String, Type >();
			requestType = buildType( decl.requestType() );
			responseType = buildType( decl.responseType() );
			for( Entry< String, TypeDefinition > entry : decl.faults().entrySet() ) {
				faultTypes.put( entry.getKey(), types.get( entry.getValue().id() ) );
			}
			RequestResponseTypeDescription desc = new RequestResponseTypeDescription( requestType, responseType, faultTypes );
			solicitResponseTypes.get( currentOutputPort ).put( decl.id(), desc );
		}
	}

	// TODO use this in every session spawner creation
	private CorrelatedProcess makeSessionSpawner( CorrelatedInputProcess process )
	{
		CorrelatedProcess ret = new CorrelatedProcess( interpreter, process );
		process.setCorrelatedProcess( ret );
		return ret;
	}
	
	public void visit( DefinitionNode n )
	{
		DefinitionProcess def;
		
		if ( "main".equals( n.id() ) ) {
			canSpawnSession = true;
			n.body().accept( this );
			canSpawnSession = false;
			if ( currProcess instanceof CorrelatedInputProcess ) {
				currProcess = makeSessionSpawner( (CorrelatedInputProcess)currProcess );
			}
			
			SequentialProcess mainProcessBody = new SequentialProcess();
			try {
				mainProcessBody.addChild( interpreter.getDefinition( "init" ) );
			} catch( InvalidIdException e ) {}
			mainProcessBody.addChild( currProcess );
			currProcess = mainProcessBody;
			
			currProcess = new ScopeProcess(
				"main",
				currProcess
			);
			def = new MainDefinitionProcess( currProcess );
		} else {
			n.body().accept( this );
			def = new DefinitionProcess( currProcess );
		}

		interpreter.register( n.id(), def );
	}

	public void visit( ParallelStatement n )
	{
		ParallelProcess proc = new ParallelProcess();
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
			proc.addChild( currProcess );
		}
		currProcess = proc;
	}
	
	public void visit( SynchronizedStatement n )
	{
		n.body().accept( this );
		currProcess = new SynchronizedProcess( n.id(), currProcess );
	}
		
	public void visit( SequenceStatement n )
	{
		SequentialProcess proc = new SequentialProcess();
		Iterator< OLSyntaxNode > it = n.children().iterator();
		OLSyntaxNode node;
		boolean origSpawnSession;
		while( it.hasNext() ) {
			node = it.next();
			node.accept( this );
			if ( currProcess instanceof CorrelatedInputProcess && canSpawnSession ) {
				/**
				 * Do not use multiple CorrelatedInputProcess
				 * in the same sequence!
				 */
				origSpawnSession = canSpawnSession;
				canSpawnSession = false;
				SequentialProcess sequence = new SequentialProcess();
				CorrelatedProcess corrProc = new CorrelatedProcess( interpreter, sequence );
				((CorrelatedInputProcess)currProcess).setCorrelatedProcess( corrProc );
				sequence.addChild( currProcess );
				while( it.hasNext() ) {
					node = it.next();
					node.accept( this );
					sequence.addChild( currProcess );
				}
				currProcess = corrProc;
				canSpawnSession = origSpawnSession;
			}
			proc.addChild( currProcess );
			if ( !it.hasNext() ) // Dirty trick, remove this
				break;
		}
		currProcess = proc;
	}

	public void visit( NDChoiceStatement n )
	{
		boolean origSpawnSession = canSpawnSession;
		canSpawnSession = false;
		
		CorrelatedProcess corrProc = null;
		Vector< Pair< InputProcess, Process > > branches = 
					new Vector< Pair< InputProcess, Process > >();
		
		Process guard;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
			pair.key().accept( this );
			guard = currProcess;
			if ( guard instanceof CorrelatedInputProcess ) {
				((CorrelatedInputProcess) guard).setCorrelatedProcess( corrProc );
			}
			pair.value().accept( this );
			try {
				branches.add(
					new Pair< InputProcess, Process >( (InputProcess)guard, currProcess )
				);
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
		
		NDChoiceProcess proc = new NDChoiceProcess( branches );
		
		canSpawnSession = origSpawnSession;
		if( canSpawnSession ) {
			corrProc = new CorrelatedProcess( interpreter, proc );
			proc.setCorrelatedProcess( corrProc );
		}

		currProcess = ( corrProc == null ) ? proc : corrProc;
	}
		
	public void visit( OneWayOperationStatement n )
	{
		try {
			currProcess =
				new OneWayProcess(
						interpreter.getOneWayOperation( n.id() ),
						buildVariablePath( n.inputVarPath() )
						);
		} catch( InvalidIdException e ) {
			error( n.context(), e ); 
		}
	}

	public void visit( RequestResponseOperationStatement n )
	{
		boolean origSpawnSession = canSpawnSession;
		canSpawnSession = false;
		
		Expression outputExpression = null;
		if ( n.outputExpression() != null ) {
			n.outputExpression().accept( this );
			outputExpression = currExpression;
		}
		try {
			n.process().accept( this );
			currProcess =
				new RequestResponseProcess(
						interpreter.getRequestResponseOperation( n.id() ),
						buildVariablePath( n.inputVarPath() ),
						outputExpression,
						currProcess
						);
		} catch( InvalidIdException e ) {
			error( n.context(), e ); 
		}

		canSpawnSession = origSpawnSession;
	}
		
	public void visit( NotificationOperationStatement n )
	{
		Expression outputExpression = null;
		if ( n.outputExpression() != null ) {
			n.outputExpression().accept( this );
			outputExpression = currExpression;
		}
		try {
			currProcess =
				new NotificationProcess(
						n.id(),
						interpreter.getOutputPort( n.outputPortId() ),
						outputExpression,
						notificationTypes.get( n.outputPortId() ).get( n.id() )
					);
		} catch( InvalidIdException e ) {
			error( n.context(), e );
		}
	}
		
	public void visit( SolicitResponseOperationStatement n )
	{
		Expression outputExpression = null;
		if ( n.outputExpression() != null ) {
			n.outputExpression().accept( this );
			outputExpression = currExpression;
		}
		try {
			Process installProcess = NullProcess.getInstance();
			if ( n.handlersFunction() != null )
				installProcess = new InstallProcess( getHandlersFunction( n.handlersFunction() ) );
			currProcess =
				new SolicitResponseProcess(
						n.id(),
						interpreter.getOutputPort( n.outputPortId() ),
						outputExpression,
						buildVariablePath( n.inputVarPath() ),
						installProcess,
						solicitResponseTypes.get( n.outputPortId() ).get( n.id() )
					);
		} catch( InvalidIdException e ) {
			error( n.context(), e );
		}
	}
		
	public void visit( LinkInStatement n )
	{
		currProcess = new LinkInProcess( n.id() );
	}
	
	public void visit( LinkOutStatement n )
	{
		currProcess = new LinkOutProcess( n.id() );
	}
	
	public void visit( ThrowStatement n )
	{
		Expression expression = null;
		if ( n.expression() != null ) {
			n.expression().accept( this );
			expression = currExpression;
		}
		currProcess = new ThrowProcess( n.id(), expression );
	}
	
	public void visit( CompensateStatement n )
	{
		currProcess = new CompensateProcess( n.id() );
	}
		
	public void visit( Scope n )
	{
		n.body().accept( this );
		currProcess = new ScopeProcess( n.id(), currProcess );
	}
		
	public void visit( InstallStatement n )
	{
		currProcess = new InstallProcess( getHandlersFunction( n.handlersFunction() ) );
	}
	
	private Vector< Pair< String, Process > > getHandlersFunction( InstallFunctionNode n )
	{
		Vector< Pair< String, Process > > pairs = new Vector< Pair< String, Process > >();
		for( Pair< String, OLSyntaxNode > pair : n.pairs() ) {
			pair.value().accept( this );
			pairs.add( new Pair< String, Process >( pair.key(), currProcess ) );
		}
		return pairs;
	}
			
	public void visit( AssignStatement n )
	{
		n.expression().accept( this );
			
		AssignmentProcess p = 
			new AssignmentProcess(
				buildVariablePath( n.variablePath() ),
				currExpression
				);
		currProcess = p;
		currExpression = p;
	}
	
	private VariablePath buildVariablePath( VariablePathNode path )
	{
		if ( path == null )
			return null;
		
		Expression backupExpr = currExpression;
		
		LinkedList< Pair< Expression, Expression > > list =
							new LinkedList< Pair< Expression, Expression > >();
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : path.path() ) {
			pair.key().accept( this );
			Expression keyExpr = currExpression;
			currExpression = null;
			if ( pair.value() != null )
				pair.value().accept( this );
			list.add( new Pair< Expression, Expression >( keyExpr, currExpression ) );
		}
		
		currExpression = backupExpr;
		
		return new VariablePath( list, path.isGlobal() );
	}
	
	public void visit( PointerStatement n )
	{
		currProcess =
			new MakePointerProcess(
				buildVariablePath( n.leftPath() ),
				buildVariablePath( n.rightPath() )
				);
	}
	
	public void visit( DeepCopyStatement n )
	{
		currProcess =
			new DeepCopyProcess(
				buildVariablePath( n.leftPath() ),
				buildVariablePath( n.rightPath() )
				);
	}
	
	public void visit( IfStatement n )
	{
		IfProcess.CPPair[] pairs = new IfProcess.CPPair[ n.children().size() ];
		Process elseProcess = null;

		Condition condition;
		int i = 0;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
			pair.key().accept( this );
			condition = currCondition;
			pair.value().accept( this );
			pairs[ i++ ] = new IfProcess.CPPair( condition, currProcess );
		}
		
		if ( n.elseProcess() != null ) {
			n.elseProcess().accept( this );
			elseProcess = currProcess;
		}
		
		currProcess = new IfProcess( pairs, elseProcess );
	}
	
	public void visit( CurrentHandlerStatement n )
	{
		currProcess = CurrentHandlerProcess.getInstance();
	}

	public void visit( DefinitionCallStatement n )
	{
		try {
			currProcess = new CallProcess( interpreter.getDefinition( n.id() ) );
		} catch( InvalidIdException e ) {
			error( n.context(), e );
		}
	}
	
	public void visit( RunStatement n )
	{
		n.expression().accept( this );
		currProcess = new RunProcess( currExpression );
	}
	
	public void visit( WhileStatement n )
	{
		n.condition().accept( this );
		Condition condition = currCondition;
		n.body().accept( this );
		currProcess = new WhileProcess( condition, currProcess );
	}
	
	public void visit( OrConditionNode n )
	{
		OrCondition cond = new OrCondition();
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
			cond.addChild( currCondition );
		}
		currCondition = cond;
	}
	
	public void visit( AndConditionNode n )
	{
		AndCondition cond = new AndCondition();
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
			cond.addChild( currCondition );
		}
		currCondition = cond;
	}
	
	public void visit( NotConditionNode n )
	{
		n.condition().accept( this );
		currCondition = new NotCondition( currCondition );
	}
	
	public void visit( CompareConditionNode n )
	{
		n.leftExpression().accept( this );
		Expression left = currExpression;
		n.rightExpression().accept( this );
		currCondition = new CompareCondition( left, currExpression, n.opType() );
	}
	
	public void visit( ExpressionConditionNode n )
	{
		n.expression().accept( this );
		currCondition = new ExpressionCondition( currExpression );
	}
	
	public void visit( ConstantIntegerExpression n )
	{
		currExpression = Value.create( n.value() );
	}
	
	public void visit( ConstantRealExpression n )
	{
		currExpression = Value.create( n.value() );
	}
	
	public void visit( ConstantStringExpression n )
	{
		currExpression = Value.create( n.value() );
	}
	
	public void visit( ProductExpressionNode n )
	{
		Operand[] operands = new Operand[ n.operands().size() ];
		int i = 0;
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
			operands[i++] = new Operand( pair.key(), currExpression );
		}

		currExpression = new ProductExpression( operands );
	}
	
	public void visit( SumExpressionNode n )
	{
		Operand[] operands = new Operand[ n.operands().size() ];
		int i = 0;
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
			operands[i++] = new Operand( pair.key(), currExpression );
		}

		currExpression = new SumExpression( operands );
	}

	public void visit( VariableExpressionNode n )
	{
		currExpression = buildVariablePath( n.variablePath() );
	}
	
	public void visit( InstallFixedVariableExpressionNode n )
	{
		currExpression =
				new InstallFixedVariablePath(
					buildVariablePath( n.variablePath() )
				);
	}
	
	public void visit( NullProcessStatement n )
	{
		currProcess = NullProcess.getInstance();
	}
	
	public void visit( ExitStatement n )
	{
		currProcess = ExitProcess.getInstance();
	}
	
	public void visit( ValueVectorSizeExpressionNode n )
	{
		currExpression = new ValueVectorSizeExpression( buildVariablePath( n.variablePath() ) );
	}
	
	public void visit( PreDecrementStatement n )
	{
		PreDecrementProcess p =
			new PreDecrementProcess( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p; 
	}
	
	public void visit( PostDecrementStatement n )
	{
		PostDecrementProcess p =
			new PostDecrementProcess( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p;
	}
	
	public void visit( IsTypeExpressionNode n )
	{
		IsTypeExpressionNode.CheckType type = n.type();
		if ( type == IsTypeExpressionNode.CheckType.DEFINED ) {
			currExpression =
				new IsDefinedExpression( buildVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.INT ) {
			currExpression =
				new IsIntExpression( buildVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.REAL ) {
			currExpression =
				new IsRealExpression( buildVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.STRING ) {
			currExpression =
				new IsStringExpression( buildVariablePath( n.variablePath() ) );
		}
	}
	
	public void visit( TypeCastExpressionNode n )
	{
		n.expression().accept( this );
		if ( n.type() == NativeType.INT ) {
			currExpression = new CastIntExpression( currExpression );
		} else if ( n.type() == NativeType.DOUBLE ) {
			currExpression = new CastRealExpression( currExpression );
		} else if ( n.type() == NativeType.STRING ) {
			currExpression = new CastStringExpression( currExpression );
		}
	}
	
	public void visit( PreIncrementStatement n )
	{
		PreIncrementProcess p =
			new PreIncrementProcess( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p; 
	}
	
	public void visit( PostIncrementStatement n )
	{
		PostIncrementProcess p =
			new PostIncrementProcess( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p;
	}
	
	public void visit( ForStatement n )
	{
		n.init().accept( this );
		Process init = currProcess;
		n.post().accept( this );
		Process post = currProcess;
		n.condition().accept( this );
		Condition condition = currCondition;
		n.body().accept( this );
		currProcess = new ForProcess( init, condition, post, currProcess );
	}
	
	public void visit( ForEachStatement n )
	{
		n.body().accept( this );
		currProcess =
			new ForEachProcess(
				buildVariablePath( n.keyPath() ),
				buildVariablePath( n.targetPath() ),
				currProcess
				);
	}

	private Expression buildExpression( OLSyntaxNode n )
	{
		if ( n == null ) {
			return null;
		}
		n.accept( this );
		return currExpression;
	}

	private Process buildProcess( OLSyntaxNode n )
	{
		if ( n == null ) {
			return null;
		}
		n.accept( this );
		return currProcess;
	}

	private Type buildType( OLSyntaxNode n )
	{
		if ( n == null ) {
			return null;
		}
		n.accept( this );
		return currType;
	}

	public void visit( SpawnStatement n )
	{
		currProcess = new SpawnProcess(
			buildVariablePath( n.indexVariablePath() ),
			buildExpression( n.upperBoundExpression() ),
			buildVariablePath( n.inVariablePath() ),
			buildProcess( n.body() )
		);
	}
	
	public void visit( UndefStatement n )
	{
		currProcess = new UndefProcess( buildVariablePath( n.variablePath() ) );
	}
}

