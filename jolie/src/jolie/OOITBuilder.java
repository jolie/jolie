/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi  								   *
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

package jolie;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import jolie.Constants.OperandType;
import jolie.deploy.InputPort;
import jolie.deploy.InputPortType;
import jolie.deploy.OutputPortType;
import jolie.deploy.PortCreationException;
import jolie.deploy.PortType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.AndConditionNode;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.ConstantIntegerExpression;
import jolie.lang.parse.ast.ConstantRealExpression;
import jolie.lang.parse.ast.ConstantStringExpression;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ExpressionConditionNode;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InStatement;
import jolie.lang.parse.ast.InputPortTypeInfo;
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
import jolie.lang.parse.ast.OutStatement;
import jolie.lang.parse.ast.OutputPortTypeInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.Procedure;
import jolie.lang.parse.ast.ProcedureCallStatement;
import jolie.lang.parse.ast.ProductExpressionNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.ServiceInfo;
import jolie.lang.parse.ast.SleepStatement;
import jolie.lang.parse.ast.SolicitResponseOperationDeclaration;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.StateInfo;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePath;
import jolie.lang.parse.ast.WhileStatement;
import jolie.net.CommCore;
import jolie.net.CommProtocol;
import jolie.net.HTTPProtocol;
import jolie.net.SOAPProtocol;
import jolie.net.SODEPProtocol;
import jolie.net.UnsupportedCommMediumException;
import jolie.process.AssignmentProcess;
import jolie.process.CallProcess;
import jolie.process.CompensateProcess;
import jolie.process.CorrelatedInputProcess;
import jolie.process.CorrelatedProcess;
import jolie.process.DeepCopyProcess;
import jolie.process.DefinitionProcess;
import jolie.process.ExitProcess;
import jolie.process.ForEachProcess;
import jolie.process.ForProcess;
import jolie.process.IfProcess;
import jolie.process.InProcess;
import jolie.process.InputProcess;
import jolie.process.InstallProcess;
import jolie.process.LinkInProcess;
import jolie.process.LinkOutProcess;
import jolie.process.MakePointerProcess;
import jolie.process.NDChoiceProcess;
import jolie.process.NotificationProcess;
import jolie.process.NullProcess;
import jolie.process.OneWayProcess;
import jolie.process.OutProcess;
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
import jolie.process.SleepProcess;
import jolie.process.SolicitResponseProcess;
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
import jolie.runtime.Expression;
import jolie.runtime.ExpressionCondition;
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;
import jolie.runtime.IsDefinedExpression;
import jolie.runtime.IsIntExpression;
import jolie.runtime.IsRealExpression;
import jolie.runtime.IsStringExpression;
import jolie.runtime.Location;
import jolie.runtime.NotCondition;
import jolie.runtime.NotificationOperation;
import jolie.runtime.OneWayOperation;
import jolie.runtime.OrCondition;
import jolie.runtime.OutputOperation;
import jolie.runtime.ProductExpression;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.SolicitResponseOperation;
import jolie.runtime.SumExpression;
import jolie.runtime.Value;
import jolie.runtime.ValueVectorSizeExpression;
import jolie.util.Pair;

public class OOITBuilder implements OLVisitor
{
	private Program program;
	private boolean valid = true;

	public OOITBuilder( Program program )
	{
		 this.program = program;
	}
	
	private void error( String message )
	{
		valid = false;
		Interpreter.logger().severe( message );
	}
	
	private void error( Exception e )
	{
		valid = false;
		Interpreter.logger().severe( e.getMessage() );
	}
	
	public boolean build()
	{
		visit( program );
		
		return valid;
	}
	
	public void visit( StateInfo n )
	{
		Interpreter.setStateMode( n.mode() );
	}
		
	public void visit( ExecutionInfo n )
	{
		Interpreter.setExecutionMode( n.mode() );
	}
	
	public void visit( CorrelationSetInfo n )
	{
		Set< GlobalVariablePath > cPaths = new HashSet< GlobalVariablePath > ();
		for( VariablePath path : n.variablePaths() )
			cPaths.add( getGlobalVariablePath( path ) );

		Interpreter.setCorrelationSet( cPaths );
	}
		
	public void visit( InputPortTypeInfo n )
	{
		InputPortType pt = new InputPortType( n.id() );
		for( OperationDeclaration op : n.operations() ) {
			op.accept( this );
			try {
				pt.addOperation( InputOperation.getById( op.id() ) );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		pt.register();
	}
		
	public void visit( OutputPortTypeInfo n )
	{
		OutputPortType pt = new OutputPortType( n.id(), n.namespace() );
		for( OperationDeclaration op : n.operations() ) {
			op.accept( this );
			try {
				pt.addOperation( OutputOperation.getById( op.id() ) );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		pt.register();
	}
		
	public void visit( PortInfo n )
	{
		try {
			PortType pt = PortType.getById( n.portType() );
			(pt.createPort( n.id(), n.protocolId() )).register();
		} catch( InvalidIdException e ) {
			error( e );
		} catch( PortCreationException pce ) {
			error( pce );
		}
	}
	
	/**
	 * @todo implement jolie soap namespace
	 */
	public void visit( ServiceInfo n )
	{
		InputPort port = null;
		Vector< InputPort > inputPorts = new Vector< InputPort > ();
		for( String portId : n.inputPorts() ) {
			try {
				port = InputPort.getById( portId );
				inputPorts.add( port );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		
		Constants.ProtocolId pId = port.protocolId();
		CommProtocol protocol = null;
		
		if ( pId == Constants.ProtocolId.SOAP )
			protocol = new SOAPProtocol( n.uri(), "" );
		else if ( pId == Constants.ProtocolId.SODEP )
			protocol = new SODEPProtocol();
		else if ( pId == Constants.ProtocolId.HTTP )
			protocol = new HTTPProtocol( n.uri() );
		else
			error( "Unsupported protocol specified for port " + port.id() );
		
		if ( protocol != null ) {
			try {
				CommCore.addService( n.uri(), protocol, inputPorts );
			} catch( UnsupportedCommMediumException e ) {
				error( e );
			} catch( IOException ioe ) {
				error( ioe );
			}
		}
	}
	
	private Process currProcess;
	private Expression currExpression;
	private Condition currCondition;
	private boolean alreadyCorrelated = false;
		
	public void visit( Program p )
	{
		for( OLSyntaxNode node : p.children() )
			node.accept( this );
	}

	public void visit( OneWayOperationDeclaration decl )
	{
		(new OneWayOperation( decl.id() )).register();
	}
		
	public void visit( RequestResponseOperationDeclaration decl )
	{
		(new RequestResponseOperation(
			decl.id(),
			decl.faultNames() )).register();
	}
		
	public void visit( NotificationOperationDeclaration decl )
	{
		(new NotificationOperation( decl.id() )).register();
	}
		
	public void visit( SolicitResponseOperationDeclaration decl )
	{
		(new SolicitResponseOperation( decl.id() )).register();
	}
	
	public void visit( Procedure n )
	{
		DefinitionProcess def = new DefinitionProcess( n.id() );
		n.body().accept( this );
		if ( "main".equals( n.id() ) )
			currProcess = new ScopeProcess( "main", currProcess );

		def.setProcess( currProcess );
		def.register();
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
		
	/**
	 * @todo we should not create multiple CorrelatedInputProcess through procedures. Perhaps limit them to main{} ?
	 * @todo allow ndchoice at the beginning of main to be CorrelatedInputProcess
	 * @todo allow input operations at the beginning of main to be CorrelatedInputProcess
	 */
	public void visit( SequenceStatement n )
	{
		SequentialProcess proc = new SequentialProcess();
		Iterator< OLSyntaxNode > it = n.children().iterator();
		OLSyntaxNode node;
		while( it.hasNext() ) {
			node = it.next();
			node.accept( this );
			if ( currProcess instanceof CorrelatedInputProcess && !alreadyCorrelated ) {
				/**
				 * Do not use multiple CorrelatedInputProcess
				 * in the same sequence!
				 */
				alreadyCorrelated = true;
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
				alreadyCorrelated = false;
			}
			proc.addChild( currProcess );
			if ( !it.hasNext() ) // Dirty trick, remove this
				break;
		}
		currProcess = proc;
	}
	
	public void visit( NDChoiceStatement n )
	{
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
		
		if( !alreadyCorrelated ) {
			alreadyCorrelated = true;
			corrProc = new CorrelatedProcess( proc );
			proc.setCorrelatedProcess( corrProc );
			alreadyCorrelated = false;
		}

		currProcess = ( corrProc == null ) ? proc : corrProc;
	}
		
	public void visit( OneWayOperationStatement n )
	{
		try {
			currProcess =
				new OneWayProcess(
						OneWayOperation.getById( n.id() ),
						getGlobalVariablePath( n.inputVarPath() )
						);
		} catch( InvalidIdException e ) {
			error( e ); 
		}
	}
		
	public void visit( RequestResponseOperationStatement n )
	{
		try {
			n.process().accept( this );
			currProcess =
				new RequestResponseProcess(
						RequestResponseOperation.getById( n.id() ),
						getGlobalVariablePath( n.inputVarPath() ),
						getGlobalVariablePath( n.outputVarPath() ),
						currProcess
						);
		} catch( InvalidIdException e ) {
			error( e ); 
		}
	}
		
	public void visit( NotificationOperationStatement n )
	{
		n.locationExpression().accept( this );
		try {
			currProcess =
				new NotificationProcess(
						NotificationOperation.getById( n.id() ),
						new Location( currExpression ),
						getGlobalVariablePath( n.outputVarPath() )
						);
		} catch( InvalidIdException e ) {
			error( e );
		}
	}
		
	public void visit( SolicitResponseOperationStatement n )
	{
		n.locationExpression().accept( this );
		Expression location = currExpression;
		try {
			Process installProcess = NullProcess.getInstance();
			if ( n.handlersFunction() != null )
				installProcess = new InstallProcess( getHandlersFunction( n.handlersFunction() ) );
			currProcess =
				new SolicitResponseProcess(
						SolicitResponseOperation.getById( n.id() ),
						new Location( location ),
						getGlobalVariablePath( n.outputVarPath() ),
						getGlobalVariablePath( n.inputVarPath() ),
						installProcess
						);
		} catch( InvalidIdException e ) {
			error( e );
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
	
	private GlobalVariablePath getGlobalVariablePath( VariablePath path )
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
		
		return new GlobalVariablePath( list, attribute );
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
	
	public void visit( InStatement n )
	{
		currProcess = new InProcess(
				getGlobalVariablePath( n.variablePath() )
				);
	}
	
	public void visit( OutStatement n )
	{
		n.expression().accept( this );
		currProcess = new OutProcess( currExpression );
	}
	
	public void visit( ProcedureCallStatement n )
	{
		try {
			currProcess = new CallProcess( DefinitionProcess.getById( n.id() ) );
		} catch( InvalidIdException e ) {
			error( e );
		}
	}
	
	public void visit( SleepStatement n )
	{
		n.expression().accept( this );
		currProcess = new SleepProcess( currExpression );
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

