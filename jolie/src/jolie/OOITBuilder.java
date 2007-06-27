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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import jolie.Constants.OperandType;
import jolie.deploy.InputPort;
import jolie.deploy.InputPortType;
import jolie.deploy.OutputPortType;
import jolie.deploy.PartnerLinkType;
import jolie.deploy.PortCreationException;
import jolie.deploy.PortType;
import jolie.lang.parse.DeployVisitor;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.nodes.deploy.CorrelationSetInfo;
import jolie.lang.parse.nodes.deploy.DeployInfo;
import jolie.lang.parse.nodes.deploy.DeploySyntaxNode;
import jolie.lang.parse.nodes.deploy.ExecutionInfo;
import jolie.lang.parse.nodes.deploy.InputPortTypeInfo;
import jolie.lang.parse.nodes.deploy.LocationDeployInfo;
import jolie.lang.parse.nodes.deploy.NotificationOperationDeployInfo;
import jolie.lang.parse.nodes.deploy.OneWayOperationDeployInfo;
import jolie.lang.parse.nodes.deploy.OutputPortTypeInfo;
import jolie.lang.parse.nodes.deploy.PartnerLinkTypeInfo;
import jolie.lang.parse.nodes.deploy.PortBindingInfo;
import jolie.lang.parse.nodes.deploy.RequestResponseOperationDeployInfo;
import jolie.lang.parse.nodes.deploy.ServiceInfo;
import jolie.lang.parse.nodes.deploy.SolicitResponseOperationDeployInfo;
import jolie.lang.parse.nodes.deploy.StateInfo;
import jolie.lang.parse.nodes.deploy.WSDLInfo;
import jolie.lang.parse.nodes.ol.AndConditionNode;
import jolie.lang.parse.nodes.ol.AssignStatement;
import jolie.lang.parse.nodes.ol.CompareConditionNode;
import jolie.lang.parse.nodes.ol.CompensateStatement;
import jolie.lang.parse.nodes.ol.ConstantIntegerExpression;
import jolie.lang.parse.nodes.ol.ConstantStringExpression;
import jolie.lang.parse.nodes.ol.ExitStatement;
import jolie.lang.parse.nodes.ol.ExpressionConditionNode;
import jolie.lang.parse.nodes.ol.IfStatement;
import jolie.lang.parse.nodes.ol.InStatement;
import jolie.lang.parse.nodes.ol.InstallCompensationStatement;
import jolie.lang.parse.nodes.ol.InstallFaultHandlerStatement;
import jolie.lang.parse.nodes.ol.InternalLinkDeclaration;
import jolie.lang.parse.nodes.ol.LinkInStatement;
import jolie.lang.parse.nodes.ol.LinkOutStatement;
import jolie.lang.parse.nodes.ol.LocationDeclaration;
import jolie.lang.parse.nodes.ol.NDChoiceStatement;
import jolie.lang.parse.nodes.ol.NotConditionNode;
import jolie.lang.parse.nodes.ol.NotificationOperationDeclaration;
import jolie.lang.parse.nodes.ol.NotificationOperationStatement;
import jolie.lang.parse.nodes.ol.NullProcessStatement;
import jolie.lang.parse.nodes.ol.OLSyntaxNode;
import jolie.lang.parse.nodes.ol.OneWayOperationDeclaration;
import jolie.lang.parse.nodes.ol.OneWayOperationStatement;
import jolie.lang.parse.nodes.ol.OperationDeclaration;
import jolie.lang.parse.nodes.ol.OrConditionNode;
import jolie.lang.parse.nodes.ol.OutStatement;
import jolie.lang.parse.nodes.ol.ParallelStatement;
import jolie.lang.parse.nodes.ol.Procedure;
import jolie.lang.parse.nodes.ol.ProcedureCallStatement;
import jolie.lang.parse.nodes.ol.ProductExpressionNode;
import jolie.lang.parse.nodes.ol.Program;
import jolie.lang.parse.nodes.ol.RequestResponseOperationDeclaration;
import jolie.lang.parse.nodes.ol.RequestResponseOperationStatement;
import jolie.lang.parse.nodes.ol.Scope;
import jolie.lang.parse.nodes.ol.SequenceStatement;
import jolie.lang.parse.nodes.ol.SleepStatement;
import jolie.lang.parse.nodes.ol.SolicitResponseOperationDeclaration;
import jolie.lang.parse.nodes.ol.SolicitResponseOperationStatement;
import jolie.lang.parse.nodes.ol.SumExpressionNode;
import jolie.lang.parse.nodes.ol.ThrowStatement;
import jolie.lang.parse.nodes.ol.VariableDeclaration;
import jolie.lang.parse.nodes.ol.VariableExpressionNode;
import jolie.lang.parse.nodes.ol.WhileStatement;
import jolie.net.CommCore;
import jolie.net.CommProtocol;
import jolie.net.SOAPProtocol;
import jolie.net.SODEPProtocol;
import jolie.net.UnsupportedCommMediumException;
import jolie.process.AssignmentProcess;
import jolie.process.CallProcess;
import jolie.process.CompensateProcess;
import jolie.process.CorrelatedInputProcess;
import jolie.process.CorrelatedProcess;
import jolie.process.DefinitionProcess;
import jolie.process.ExitProcess;
import jolie.process.IfProcess;
import jolie.process.InProcess;
import jolie.process.InputProcess;
import jolie.process.InstallCompensationProcess;
import jolie.process.InstallFaultHandlerProcess;
import jolie.process.LinkInProcess;
import jolie.process.LinkOutProcess;
import jolie.process.NDChoiceProcess;
import jolie.process.NotificationProcess;
import jolie.process.NullProcess;
import jolie.process.OneWayProcess;
import jolie.process.OutProcess;
import jolie.process.ParallelProcess;
import jolie.process.Process;
import jolie.process.RequestResponseProcess;
import jolie.process.ScopeProcess;
import jolie.process.SequentialProcess;
import jolie.process.SleepProcess;
import jolie.process.SolicitResponseProcess;
import jolie.process.ThrowProcess;
import jolie.process.WhileProcess;
import jolie.runtime.AndCondition;
import jolie.runtime.CompareCondition;
import jolie.runtime.Condition;
import jolie.runtime.Expression;
import jolie.runtime.ExpressionCondition;
import jolie.runtime.GlobalLocation;
import jolie.runtime.GlobalVariable;
import jolie.runtime.InputOperation;
import jolie.runtime.InternalLink;
import jolie.runtime.InvalidIdException;
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
import jolie.runtime.TempVariable;
import jolie.runtime.Variable;
import jolie.runtime.VariableLocation;
import jolie.util.Pair;

public class OOITBuilder
{
	private Program program;
	private DeployInfo deployInfo;
	
	public OOITBuilder( Program program, DeployInfo deployInfo )
	{
		 this.program = program;
		 this.deployInfo = deployInfo;
	}
	
	public boolean build()
	{
		boolean valid;
		
		valid = (new ProcessBuilder( Interpreter.logger() )).build( program );
		if ( !valid )
			return false;
		
		valid = (new DeployBuilder( Interpreter.logger() )).build( deployInfo );
		
		return valid;
	}
	
	private class DeployBuilder implements DeployVisitor
	{
		private Logger logger;
		private boolean valid = true;
		
		public DeployBuilder( Logger logger )
		{
			this.logger = logger;
		}
		
		private void error( String message )
		{
			valid = false;
			logger.severe( message );
		}
		
		private void error( Exception e )
		{
			error( e.getMessage() );
		}
		
		public boolean build( DeployInfo deployInfo )
		{
			deployInfo.accept( this );
			return valid;
		}

		public void visit( DeployInfo n )
		{
			for( DeploySyntaxNode node : n.children() )
				node.accept( this );
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
			Set< GlobalVariable > cVars = new HashSet< GlobalVariable > ();
			for( String varName : n.variableNames() ) {
				try {
					cVars.add( GlobalVariable.getById( varName ) );
				} catch( InvalidIdException e ) {
					error( e );
				}
			}
			
			Interpreter.setCorrelationSet( cVars );
		}
		
		public void visit( LocationDeployInfo n )
		{
			try {
				GlobalLocation loc = GlobalLocation.getById( n.id() );
				loc.setValue( n.uri().toString() );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		
		public void visit( WSDLInfo n )
		{
			for( DeploySyntaxNode node : n.children() )
				node.accept( this );
		}
		
		public void visit( OneWayOperationDeployInfo n )
		{
			try {
				OneWayOperation op = OneWayOperation.getById( n.id() );
				op.wsdlInfo().setBoundName( n.boundName() );
				op.wsdlInfo().setInVarNames( new Vector< String > (n.inVarNames()) );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		
		public void visit( NotificationOperationDeployInfo n )
		{
			try {
				NotificationOperation op = NotificationOperation.getById( n.id() );
				op.wsdlInfo().setBoundName( n.boundName() );
				op.wsdlInfo().setOutVarNames( new Vector< String > (n.outVarNames()) );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		
		public void visit( RequestResponseOperationDeployInfo n )
		{
			try {
				RequestResponseOperation op = RequestResponseOperation.getById( n.id() );
				op.wsdlInfo().setBoundName( n.boundName() );
				op.wsdlInfo().setInVarNames( new Vector< String > (n.inVarNames()) );
				op.wsdlInfo().setOutVarNames( new Vector< String > (n.outVarNames()) );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		
		public void visit( SolicitResponseOperationDeployInfo n )
		{
			try {
				SolicitResponseOperation op = SolicitResponseOperation.getById( n.id() );
				op.wsdlInfo().setBoundName( n.boundName() );
				op.wsdlInfo().setInVarNames( new Vector< String > (n.inVarNames()) );
				op.wsdlInfo().setOutVarNames( new Vector< String > (n.outVarNames()) );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		
		public void visit( InputPortTypeInfo n )
		{
			InputPortType pt = new InputPortType( n.id() );
			for( String id : n.operations() ) {
				try {
					pt.addOperation( InputOperation.getById( id ) );
				} catch( InvalidIdException e ) {
					error( e );
				}
			}
			pt.register();
		}
		
		public void visit( OutputPortTypeInfo n )
		{
			OutputPortType pt = new OutputPortType( n.id() );
			for( String id : n.operations() ) {
				try {
					pt.addOperation( OutputOperation.getById( id ) );
				} catch( InvalidIdException e ) {
					error( e );
				}
			}
			pt.register();
		}
		
		public void visit( PartnerLinkTypeInfo n )
		{
			try {
				( new PartnerLinkType(
						n.id(),
						InputPortType.getById( n.outputPortType() ),
						OutputPortType.getById( n.outputPortType() )
						) ).register();
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		
		public void visit( PortBindingInfo n )
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
				protocol = new SOAPProtocol( n.uri() );
			else if ( pId == Constants.ProtocolId.SODEP )
				protocol = new SODEPProtocol();
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
	}
	
	private class ProcessBuilder implements OLVisitor
	{
		private Process currProcess;
		private Expression currExpression;
		private Condition currCondition;
		private Logger logger;
		private boolean valid = true, correlatedSequence = false;
		
		public ProcessBuilder( Logger logger )
		{
			this.logger = logger;
		}
		
		private void error( String message )
		{
			valid = false;
			logger.severe( message );
		}
		
		private void error( Exception e )
		{
			error( e.getMessage() );
		}
		
		public boolean build( Program p )
		{
			p.accept( this );
			return valid;
		}
		
		public void visit( Program p )
		{
			for( LocationDeclaration decl : p.locationDeclarations() )
				decl.accept( this );
			
			for( OperationDeclaration decl : p.operationDeclarations() )
				decl.accept( this );
			
			for( VariableDeclaration decl : p.variableDeclarations() )
				decl.accept( this );
			
			for( InternalLinkDeclaration decl : p.linkDeclarations() )
				decl.accept( this );
			
			for( Procedure proc : p.procedures() )
				proc.accept( this );
		}
		
		public void visit( LocationDeclaration decl )
		{
			(new GlobalLocation( decl.id() )).register();
		}
		
		public void visit( OneWayOperationDeclaration decl )
		{
			(new OneWayOperation(
				decl.id(),
				new Vector< Constants.VariableType >( decl.inVarTypes() ) )).register();
		}
		
		public void visit( RequestResponseOperationDeclaration decl )
		{
			(new RequestResponseOperation(
				decl.id(),
				new Vector< Constants.VariableType >( decl.inVarTypes() ),
				new Vector< Constants.VariableType >( decl.outVarTypes() ),
				decl.faultNames() )).register();
		}
		
		public void visit( NotificationOperationDeclaration decl )
		{
			(new NotificationOperation(
				decl.id(),
				new Vector< Constants.VariableType >( decl.outVarTypes() ),
				decl.boundOperationId() )).register();
		}
		
		public void visit( SolicitResponseOperationDeclaration decl )
		{
			(new SolicitResponseOperation(
				decl.id(),
				new Vector< Constants.VariableType >( decl.outVarTypes() ),
				new Vector< Constants.VariableType >( decl.inVarTypes() ),
				decl.boundOperationId() )).register();
		}
		
		public void visit( InternalLinkDeclaration n )
		{
			(new InternalLink( n.id() )).register();
		}
		
		public void visit( VariableDeclaration n )
		{
			(new GlobalVariable( n.id() )).register();
		}
		
		public void visit( Procedure n )
		{
			DefinitionProcess def = new DefinitionProcess( n.id() );
			n.body().accept( this );
			if ( n.id().equals( "main" ) )
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
		
		/**
		 * @todo we should not create multiple CorrelatedInputProcess through procedures. Perhaps limit them to main{} ?
		 * @todo allow ndchoice at the beginning of main to be CorrelatedInputProcess
		 */
		public void visit( SequenceStatement n )
		{
			SequentialProcess proc = new SequentialProcess();
			Iterator< OLSyntaxNode > it = n.children().iterator();
			OLSyntaxNode node;
			while( it.hasNext() ) {
				node = it.next();
				node.accept( this );
				if ( currProcess instanceof CorrelatedInputProcess && !correlatedSequence ) {
					
					/**
					 * Do not use multiple CorrelatedInputProcess
					 * in the same sequence!
					 */
					correlatedSequence = true;
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
					correlatedSequence = false;
				}
				proc.addChild( currProcess );
				if ( !it.hasNext() ) // Dirty trick, remove this
					break;
			}
			currProcess = proc;
		}
		
		public void visit( NDChoiceStatement n )
		{
			NDChoiceProcess proc = new NDChoiceProcess();
			Process guard;
			for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
				pair.key().accept( this );
				guard = currProcess;
				pair.value().accept( this );
				proc.addChoice( (InputProcess)guard, currProcess );
			}
			currProcess = proc;
		}
		
		private Vector< GlobalVariable > getVariables( Collection< String > varNames )
		{
			Vector< GlobalVariable > vars = new Vector< GlobalVariable >();
			for( String id : varNames ) {
				try {
					vars.add( GlobalVariable.getById( id ) );
				} catch( InvalidIdException e ) {
					error( e ); 
				}
			}
			
			return vars;
		}
		
		public void visit( OneWayOperationStatement n )
		{
			try {
				currProcess =
					new OneWayProcess( OneWayOperation.getById( n.id() ), getVariables( n.inVars() ) );
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
							getVariables( n.inVars() ),
							getVariables( n.outVars() ),
							currProcess
							);
			} catch( InvalidIdException e ) {
				error( e ); 
			}
		}
		
		public void visit( NotificationOperationStatement n )
		{
			Location location;
			try {
				location = GlobalLocation.getById( n.locationId() );
			} catch( InvalidIdException e ) {
				Variable var = null;
				try {
					var = GlobalVariable.getById( n.locationId() );
				} catch( InvalidIdException iie ) {
					error( iie );
				}
				location = new VariableLocation( var );
			}
			try {
				currProcess =
					new NotificationProcess(
							NotificationOperation.getById( n.id() ),
							location,
							getVariables( n.outVars() )
							);
			} catch( InvalidIdException e ) {
				error( e ); 
			}
		}
		
		public void visit( SolicitResponseOperationStatement n )
		{
			Location location;
			try {
				location = GlobalLocation.getById( n.locationId() );
			} catch( InvalidIdException e ) {
				Variable var = null;
				try {
					var = GlobalVariable.getById( n.locationId() );
				} catch( InvalidIdException iie ) {
					error( iie );
				}
				location = new VariableLocation( var );
			}
			try {
				currProcess =
					new SolicitResponseProcess(
							SolicitResponseOperation.getById( n.id() ),
							location,
							getVariables( n.outVars() ),
							getVariables( n.inVars() )
							);
			} catch( InvalidIdException e ) {
				error( e ); 
			}
		}
		
		public void visit( LinkInStatement n )
		{
			try {
				currProcess = new LinkInProcess( InternalLink.getById( n.id() ) );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		
		public void visit( LinkOutStatement n )
		{
			try {
				currProcess = new LinkOutProcess( InternalLink.getById( n.id() ) );
			} catch( InvalidIdException e ) {
				error( e );
			}
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
		
		public void visit( InstallCompensationStatement n )
		{
			n.body().accept( this );
			currProcess = new InstallCompensationProcess( currProcess );
		}
		
		public void visit( InstallFaultHandlerStatement n )
		{
			n.body().accept( this );
			currProcess = new InstallFaultHandlerProcess( n.id(), currProcess );
		}
		
		public void visit( AssignStatement n )
		{
			try {
				n.expression().accept( this );
				currProcess =
					new AssignmentProcess(
						GlobalVariable.getById( n.id() ),
						currExpression
						);
			} catch( InvalidIdException e ) {
				error( e );
			}
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
			try {
				currProcess = new InProcess( GlobalVariable.getById( n.id() ) );
			} catch( InvalidIdException e ) {
				error( e );
			}
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
			currExpression = new TempVariable( n.value() );
		}
		
		public void visit( ConstantStringExpression n )
		{
			currExpression = new TempVariable( n.value() );
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
			try {
				currExpression = GlobalVariable.getById( n.id() );
			} catch( InvalidIdException e ) {
				error( e );
			}
		}
		
		public void visit( NullProcessStatement n )
		{
			currProcess = NullProcess.getInstance();
		}
		
		public void visit( ExitStatement n )
		{
			currProcess = ExitProcess.getInstance();
		}
	}
}
