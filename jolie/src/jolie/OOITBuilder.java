package jolie;


import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import jolie.Constants.OperandType;
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
import jolie.lang.parse.ast.NotificationOperationDeclaration;
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
import jolie.lang.parse.ast.ServiceInfo;
import jolie.lang.parse.ast.SolicitResponseOperationDeclaration;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.net.CommProtocol;
import jolie.net.HTTPProtocol;
import jolie.net.InputPort;
import jolie.net.OutputPort;
import jolie.net.SOAPProtocol;
import jolie.net.SODEPProtocol;
import jolie.net.UnsupportedCommMediumException;
import jolie.process.AssignmentProcess;
import jolie.process.CallProcess;
import jolie.process.CompensateProcess;
import jolie.process.CorrelatedInputProcess;
import jolie.process.CorrelatedProcess;
import jolie.process.CurrentHandlerProcess;
import jolie.process.DeepCopyProcess;
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
import jolie.process.DefinitionProcess;
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
import jolie.runtime.EmbeddedServiceLoader;
import jolie.runtime.EmbeddedServiceLoaderCreationException;
import jolie.runtime.Expression;
import jolie.runtime.ExpressionCondition;
import jolie.runtime.InputOperation;
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
import jolie.util.Pair;

public class OOITBuilder implements OLVisitor
{
	private Program program;
	private boolean valid = true;
	private Interpreter interpreter;

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
		error( context, e.getMessage() );
	}
	
	public boolean build()
	{
		visit( program );
		
		return valid;
	}
	
	public void visit( ExecutionInfo n )
	{
		interpreter.setExecutionMode( n.mode() );
	}
	
	public void visit( CorrelationSetInfo n )
	{
		Set< List< VariablePath > > cset = new HashSet< List< VariablePath > > ();
		List< VariablePath > paths;
		for( List< VariablePathNode > list : n.cset() ) {
			paths = new Vector< VariablePath > ();
			for( VariablePathNode path : list )
				paths.add( getGlobalVariablePath( path ) );
			cset.add( paths );
		}

		interpreter.setCorrelationSet( cset );
	}
		
	public void visit( InputPortInfo n )
	{
		Vector< InputOperation > operations = new Vector< InputOperation > ();
		for( OperationDeclaration op : n.operations() ) {
			op.accept( this );
			try {
				operations.add( interpreter.getInputOperation( op.id() ) );
			} catch( InvalidIdException e ) {
				error( n.context(), e );
			}
		}
		interpreter.register( n.id(), new InputPort( n.id(), operations ) );
	}
	
	public void visit( NotificationOperationDeclaration n ) {}
	public void visit( SolicitResponseOperationDeclaration n ) {}
		
	public void visit( OutputPortInfo n )
	{
		Vector< String > operations = new Vector< String > ();
		for( OperationDeclaration op : n.operations() )
			operations.add( op.id() );

		Process protocolConfigurationProcess = null;
		if ( n.protocolConfiguration() != null ) {
			n.protocolConfiguration().accept( this );
			protocolConfigurationProcess = currProcess;
		}
		
		//if ( n.protocolId() == null )
			//error( n.context(), "you must specify a protocol for output port " + n.id() );

		interpreter.register( n.id(), new OutputPort(
						n.id(),
						operations,
						n.protocolId(),
						protocolConfigurationProcess,
						n.location()
					)
				);
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
						n.type(),
						n.servicePath(),
						path
						) );
		} catch( EmbeddedServiceLoaderCreationException e ) {
			error( n.context(), e );
		}
		
	}
	
	public void visit( ServiceInfo n )
	{
		InputPort port = null;
		Vector< InputPort > inputPorts = new Vector< InputPort > ();
		for( String portId : n.inputPorts() ) {
			try {
				port = interpreter.getInputPort( portId );
				inputPorts.add( port );
			} catch( InvalidIdException e ) {
				error( n.context(), e );
			}
		}
		
		Constants.ProtocolId pId = n.protocolId();
		CommProtocol protocol = null;
		
		Vector< Pair< String, Expression > > path =
					new Vector< Pair< String, Expression > >();
		path.add( new Pair< String, Expression >( n.id(), null ) );
		path.add( new Pair< String, Expression >( "protocol", null ) );
		VariablePath configurationPath = new VariablePath( path, null, true );
		
		if ( pId.equals( Constants.ProtocolId.SOAP ) )
			protocol = new SOAPProtocol( configurationPath, n.location(), interpreter );
		else if ( pId.equals( Constants.ProtocolId.SODEP ) )
			protocol = new SODEPProtocol( configurationPath );
		else if ( pId.equals( Constants.ProtocolId.HTTP ) )
			protocol = new HTTPProtocol( configurationPath, n.location() );
		else
			error( n.context(), "Unsupported protocol specified for service " + n.id() );
		
		currProcess = null;
		if ( n.protocolConfiguration() != null )
			n.protocolConfiguration().accept( this );
		
		if ( protocol != null ) {
			try {
				interpreter.commCore().addService( n.location(), inputPorts, protocol, currProcess );
			} catch( UnsupportedCommMediumException e ) {
				error( n.context(), e );
			} catch( IOException ioe ) {
				error( n.context(), ioe );
			}
		}
	}
	
	private Process currProcess;
	private Expression currExpression;
	private Condition currCondition;
	private boolean canSpawnSession = false;
		
	public void visit( Program p )
	{
		for( OLSyntaxNode node : p.children() )
			node.accept( this );
	}

	public void visit( OneWayOperationDeclaration decl )
	{
		interpreter.register( decl.id(), new OneWayOperation( decl.id() ) );
	}
		
	public void visit( RequestResponseOperationDeclaration decl )
	{
		interpreter.register(
				decl.id(),
				new RequestResponseOperation( decl.id(), decl.faultNames() ) 
						);
	}
	
	public void visit( DefinitionNode n )
	{
		DefinitionProcess def;
		
		if ( "main".equals( n.id() ) ) {
			canSpawnSession = true;
			n.body().accept( this );
			canSpawnSession = false;
			if ( currProcess instanceof CorrelatedInputProcess ) {
				CorrelatedProcess corrProc = new CorrelatedProcess( currProcess );
				((CorrelatedInputProcess)currProcess).setCorrelatedProcess( corrProc );
				currProcess = corrProc;
			}
			currProcess = new ScopeProcess( "main", currProcess );
			def = new MainDefinitionProcess();
		} else {
			def = new DefinitionProcess( n.id() );
			n.body().accept( this );
		}

		def.setProcess( currProcess );
		interpreter.register( n.id(), def );
		// currProcess = def;
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
		while( it.hasNext() ) {
			node = it.next();
			node.accept( this );
			if ( currProcess instanceof CorrelatedInputProcess && canSpawnSession ) {
				/**
				 * Do not use multiple CorrelatedInputProcess
				 * in the same sequence!
				 */
				canSpawnSession = false;
				SequentialProcess sequence = new SequentialProcess();
				CorrelatedProcess corrProc = new CorrelatedProcess( sequence );
				((CorrelatedInputProcess)currProcess).setCorrelatedProcess( corrProc );
				sequence.addChild( currProcess );
				while( it.hasNext() ) {
					node = it.next();
					node.accept( this );
					sequence.addChild( currProcess );
				}
				currProcess = corrProc;
				canSpawnSession = true;
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
			if ( guard instanceof CorrelatedInputProcess )
				((CorrelatedInputProcess) guard).setCorrelatedProcess( corrProc );
			pair.value().accept( this );
			branches.add( new Pair< InputProcess, Process >( (InputProcess)guard, currProcess ) );
		}
		
		NDChoiceProcess proc = new NDChoiceProcess( branches );
		
		canSpawnSession = origSpawnSession;
		if( canSpawnSession ) {
			corrProc = new CorrelatedProcess( proc );
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
						getGlobalVariablePath( n.inputVarPath() )
						);
		} catch( InvalidIdException e ) {
			error( n.context(), e ); 
		}
	}

	public void visit( RequestResponseOperationStatement n )
	{
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
						getGlobalVariablePath( n.inputVarPath() ),
						outputExpression,
						currProcess
						);
		} catch( InvalidIdException e ) {
			error( n.context(), e ); 
		}
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
						outputExpression
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
						getGlobalVariablePath( n.inputVarPath() ),
						installProcess
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
		currProcess = new ThrowProcess( n.id() );
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
				getGlobalVariablePath( n.variablePath() ),
				currExpression
				);
		currProcess = p;
		currExpression = p;
	}
	
	private VariablePath getGlobalVariablePath( VariablePathNode path )
	{
		if ( path == null )
			return null;
		
		Expression backupExpr = currExpression;
		
		LinkedList< Pair< String, Expression > > list =
							new LinkedList< Pair< String, Expression > >();
		Expression attribute = null;
		for( Pair< String, OLSyntaxNode > pair : path.path() ) {
			currExpression = null;
			if ( pair.value() != null )
				pair.value().accept( this );
			list.add( new Pair< String, Expression >( pair.key(), currExpression ) );
		}
		if ( path.attribute() != null ) {
			path.attribute().accept( this );
			attribute = currExpression;
		}
		
		currExpression = backupExpr;
		
		return new VariablePath( list, attribute, path.isGlobal() );
	}
	
	public void visit( PointerStatement n )
	{
		currProcess =
			new MakePointerProcess(
				getGlobalVariablePath( n.leftPath() ),
				getGlobalVariablePath( n.rightPath() )
				);
	}
	
	public void visit( DeepCopyStatement n )
	{
		currProcess =
			new DeepCopyProcess(
				getGlobalVariablePath( n.leftPath() ),
				getGlobalVariablePath( n.rightPath() )
				);
	}
	
	public void visit( IfStatement n )
	{
		IfProcess ifProc = new IfProcess();
		Condition condition;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
			pair.key().accept( this );
			condition = currCondition;
			pair.value().accept( this );
			ifProc.addPair( condition, currProcess );
		}
		
		if ( n.elseProcess() != null ) {
			n.elseProcess().accept( this );
			ifProc.setElseProcess( currProcess );
		}
		
		currProcess = ifProc;
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
		ProductExpression expr = new ProductExpression();
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
			if( pair.key() == OperandType.MULTIPLY )
				expr.multiply( currExpression );
			else
				expr.divide( currExpression );
		}
		currExpression = expr;
	}
	
	public void visit( SumExpressionNode n )
	{
		SumExpression expr = new SumExpression();
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
			if( pair.key() == OperandType.ADD )
				expr.add( currExpression );
			else
				expr.subtract( currExpression );
		}
		currExpression = expr;
	}

	public void visit( VariableExpressionNode n )
	{
		currExpression = getGlobalVariablePath( n.variablePath() );
	}
	
	public void visit( InstallFixedVariableExpressionNode n )
	{
		currExpression =
				new InstallFixedVariablePath(
					getGlobalVariablePath( n.variablePath() )
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
		currExpression = new ValueVectorSizeExpression( getGlobalVariablePath( n.variablePath() ) );
	}
	
	public void visit( PreDecrementStatement n )
	{
		PreDecrementProcess p =
			new PreDecrementProcess( getGlobalVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p; 
	}
	
	public void visit( PostDecrementStatement n )
	{
		PostDecrementProcess p =
			new PostDecrementProcess( getGlobalVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p;
	}
	
	public void visit( IsTypeExpressionNode n )
	{
		IsTypeExpressionNode.CheckType type = n.type();
		if ( type == IsTypeExpressionNode.CheckType.DEFINED ) {
			currExpression =
				new IsDefinedExpression( getGlobalVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.INT ) {
			currExpression =
				new IsIntExpression( getGlobalVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.REAL ) {
			currExpression =
				new IsRealExpression( getGlobalVariablePath( n.variablePath() ) );	
		} else if ( type == IsTypeExpressionNode.CheckType.STRING ) {
			currExpression =
				new IsStringExpression( getGlobalVariablePath( n.variablePath() ) );
		}
	}
	
	public void visit( TypeCastExpressionNode n )
	{
		if ( n.type() == Constants.VariableType.INT ) {
			currExpression = new CastIntExpression( getGlobalVariablePath( n.variablePath() ) );
		} else if ( n.type() == Constants.VariableType.REAL ) {
			currExpression = new CastRealExpression( getGlobalVariablePath( n.variablePath() ) );
		} else if ( n.type() == Constants.VariableType.STRING ) {
			currExpression = new CastStringExpression( getGlobalVariablePath( n.variablePath() ) );
		}
	}
	
	public void visit( PreIncrementStatement n )
	{
		PreIncrementProcess p =
			new PreIncrementProcess( getGlobalVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p; 
	}
	
	public void visit( PostIncrementStatement n )
	{
		PostIncrementProcess p =
			new PostIncrementProcess( getGlobalVariablePath( n.variablePath() ) );
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
				getGlobalVariablePath( n.keyPath() ),
				getGlobalVariablePath( n.valuePath() ),
				getGlobalVariablePath( n.targetPath() ),
				currProcess
				);
	}
	
	public void visit( UndefStatement n )
	{
		currProcess = new UndefProcess( getGlobalVariablePath( n.variablePath() ) );
	}
}

