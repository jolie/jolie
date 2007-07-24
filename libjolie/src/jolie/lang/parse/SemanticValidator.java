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

package jolie.lang.parse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import jolie.Constants;
import jolie.lang.parse.ast.deploy.CorrelationSetInfo;
import jolie.lang.parse.ast.deploy.DeployInfo;
import jolie.lang.parse.ast.deploy.DeploySyntaxNode;
import jolie.lang.parse.ast.deploy.ExecutionInfo;
import jolie.lang.parse.ast.deploy.InputPortTypeInfo;
import jolie.lang.parse.ast.deploy.LocationDeployInfo;
import jolie.lang.parse.ast.deploy.NotificationOperationDeployInfo;
import jolie.lang.parse.ast.deploy.OneWayOperationDeployInfo;
import jolie.lang.parse.ast.deploy.OutputPortTypeInfo;
import jolie.lang.parse.ast.deploy.PartnerLinkTypeInfo;
import jolie.lang.parse.ast.deploy.PortBindingInfo;
import jolie.lang.parse.ast.deploy.RequestResponseOperationDeployInfo;
import jolie.lang.parse.ast.deploy.ServiceInfo;
import jolie.lang.parse.ast.deploy.SolicitResponseOperationDeployInfo;
import jolie.lang.parse.ast.deploy.StateInfo;
import jolie.lang.parse.ast.deploy.WSDLInfo;
import jolie.lang.parse.ast.ol.AndConditionNode;
import jolie.lang.parse.ast.ol.AssignStatement;
import jolie.lang.parse.ast.ol.CompareConditionNode;
import jolie.lang.parse.ast.ol.CompensateStatement;
import jolie.lang.parse.ast.ol.ConstantIntegerExpression;
import jolie.lang.parse.ast.ol.ConstantStringExpression;
import jolie.lang.parse.ast.ol.ExitStatement;
import jolie.lang.parse.ast.ol.ExpressionConditionNode;
import jolie.lang.parse.ast.ol.IfStatement;
import jolie.lang.parse.ast.ol.InStatement;
import jolie.lang.parse.ast.ol.InstallCompensationStatement;
import jolie.lang.parse.ast.ol.InstallFaultHandlerStatement;
import jolie.lang.parse.ast.ol.InternalLinkDeclaration;
import jolie.lang.parse.ast.ol.LinkInStatement;
import jolie.lang.parse.ast.ol.LinkOutStatement;
import jolie.lang.parse.ast.ol.LocationDeclaration;
import jolie.lang.parse.ast.ol.NDChoiceStatement;
import jolie.lang.parse.ast.ol.NotConditionNode;
import jolie.lang.parse.ast.ol.NotificationOperationDeclaration;
import jolie.lang.parse.ast.ol.NotificationOperationStatement;
import jolie.lang.parse.ast.ol.NullProcessStatement;
import jolie.lang.parse.ast.ol.OLSyntaxNode;
import jolie.lang.parse.ast.ol.OneWayOperationDeclaration;
import jolie.lang.parse.ast.ol.OneWayOperationStatement;
import jolie.lang.parse.ast.ol.OperationDeclaration;
import jolie.lang.parse.ast.ol.OrConditionNode;
import jolie.lang.parse.ast.ol.OutStatement;
import jolie.lang.parse.ast.ol.ParallelStatement;
import jolie.lang.parse.ast.ol.Procedure;
import jolie.lang.parse.ast.ol.ProcedureCallStatement;
import jolie.lang.parse.ast.ol.ProductExpressionNode;
import jolie.lang.parse.ast.ol.Program;
import jolie.lang.parse.ast.ol.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.ol.RequestResponseOperationStatement;
import jolie.lang.parse.ast.ol.Scope;
import jolie.lang.parse.ast.ol.SequenceStatement;
import jolie.lang.parse.ast.ol.SleepStatement;
import jolie.lang.parse.ast.ol.SolicitResponseOperationDeclaration;
import jolie.lang.parse.ast.ol.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.ol.SumExpressionNode;
import jolie.lang.parse.ast.ol.ThrowStatement;
import jolie.lang.parse.ast.ol.VariableDeclaration;
import jolie.lang.parse.ast.ol.VariableExpressionNode;
import jolie.lang.parse.ast.ol.WhileStatement;
import jolie.util.Pair;

public class SemanticValidator
{
	private OLSyntaxNode olTree;
	private DeploySyntaxNode deployTree;
	
	private final Logger logger = Logger.getLogger( "JOLIE" );
	
	public SemanticValidator( OLSyntaxNode olTree, DeploySyntaxNode deployTree )
	{
		this.olTree = olTree;
		this.deployTree = deployTree;
	}
	
	public boolean validate()
	{
		boolean valid;
		
		//logger.info( "* Validating code semantics" );
		
		OLValidator olValidator = new OLValidator( logger );
		valid = olValidator.validate( olTree );
		
		if ( !valid ) {
			logger.severe( "Aborting: .ol behaviour file semantically invalid." );
			return false;
		}
		
		DeployValidator deployValidator = new DeployValidator( olValidator, logger );
		valid = deployValidator.validate( deployTree );
		
		if ( !valid )
			logger.severe( ".dol deploy file semantically invalid." );
		
		return valid;
	}
	
	private class DeployValidator implements DeployVisitor
	{
		private OLValidator olValidator;
		private Logger logger;
		private boolean valid;
		private Set< String > inputPortTypes = new HashSet< String >();
		private Set< String > outputPortTypes = new HashSet< String >();
		private HashMap< String, PortBindingInfo > bindings = new HashMap< String, PortBindingInfo >();
		private HashMap< String, Pair< String, String > > partnerLinkTypes =
					new HashMap< String, Pair< String, String > >();
		
		public DeployValidator( OLValidator olValidator, Logger logger )
		{
			this.olValidator = olValidator;
			this.valid = true;
			this.logger = logger;
		}
		
		private void error( String message )
		{
			valid = false;
			logger.severe( message );
		}
		
		public boolean validate( DeploySyntaxNode node )
		{
			node.accept( this );
			
			return valid;
		}
		
		public void visit( DeployInfo n )
		{
			for( DeploySyntaxNode node : n.children() )
				node.accept( this );
		}
		
		public void visit( CorrelationSetInfo n )
		{
			for( String varName : n.variableNames() ) {
				if ( !olValidator.variableNames().contains( varName ) )
					error( "The correlation set contains an undeclared variable" );
			}
		}
		
		public void visit( ExecutionInfo n )
		{}
		
		public void visit( StateInfo n )
		{}
		
		public void visit( LocationDeployInfo n )
		{
			if ( !olValidator.locationNames().contains( n.id() ) )
				error( "Location " + n.id() + " is not specified in the .ol behaviour file" );
			if ( Constants.stringToMediumId( n.uri().getScheme() ) == Constants.MediumId.UNSUPPORTED )
				error( "Unsupported communication medium (" + n.uri().getScheme() + ") specified for location " + n.id() );
		}
		
		public void visit( WSDLInfo n )
		{
			for( DeploySyntaxNode node : n.children() )
				node.accept( this );
		}
		
		public void visit( OneWayOperationDeployInfo n )
		{
			OperationDeclaration decl = olValidator.operations().get( n.id() );
			if ( decl == null ) { 
				error( "Operation " + n.id() + " is not defined in the .ol behaviour file" );
				return;
			}
			
			if ( !( decl instanceof OneWayOperationDeclaration ) ) {
				error( "Operation " + n.id() + " is not of the type specified in the .ol behaviour file" );
				return;
			}
			
			OneWayOperationDeclaration opDecl = (OneWayOperationDeclaration)( decl );
			if ( opDecl.inVarTypes().size() != n.inVarNames().size() )
				error( "Operation " + n.id() + " variable types and names differ in number" );
		}
		
		public void visit( NotificationOperationDeployInfo n )
		{
			OperationDeclaration decl = olValidator.operations().get( n.id() );
			if ( decl == null ) { 
				error( "Operation " + n.id() + " is not defined in the .ol behaviour file" );
				return;
			}
			
			if ( !( decl instanceof NotificationOperationDeclaration ) ) {
				error( "Operation " + n.id() + " is not of the type specified in the .ol behaviour file" );
				return;
			}
			
			NotificationOperationDeclaration opDecl = (NotificationOperationDeclaration)( decl );
			if ( opDecl.outVarTypes().size() != n.outVarNames().size() )
				error( "Operation " + n.id() + " variable types and names differ in number" );
		}
		
		public void visit( RequestResponseOperationDeployInfo n )
		{
			OperationDeclaration decl = olValidator.operations().get( n.id() );
			if ( decl == null ) { 
				error( "Operation " + n.id() + " is not defined in the .ol behaviour file" );
				return;
			}
			
			if ( !( decl instanceof RequestResponseOperationDeclaration ) ) {
				error( "Operation " + n.id() + " is not of the type specified in the .ol behaviour file" );
				return;
			}
			
			RequestResponseOperationDeclaration opDecl = (RequestResponseOperationDeclaration)( decl );
			if ( opDecl.inVarTypes().size() != n.inVarNames().size() )
				error( "Operation " + n.id() + " input variable types and names differ in number" );
			
			if ( opDecl.outVarTypes().size() != n.outVarNames().size() )
				error( "Operation " + n.id() + " output variable types and names differ in number" );
		}
		
		public void visit( SolicitResponseOperationDeployInfo n )
		{
			OperationDeclaration decl = olValidator.operations().get( n.id() );
			if ( decl == null ) { 
				error( "Operation " + n.id() + " is not defined in the .ol behaviour file" );
				return;
			}
			
			if ( !( decl instanceof SolicitResponseOperationDeclaration ) ) {
				error( "Operation " + n.id() + " is not of the type specified in the .ol behaviour file" );
				return;
			}
			
			SolicitResponseOperationDeclaration opDecl = (SolicitResponseOperationDeclaration)( decl );
			if ( opDecl.inVarTypes().size() != n.inVarNames().size() )
				error( "Operation " + n.id() + " input variable types and names differ in number" );
			
			if ( opDecl.outVarTypes().size() != n.outVarNames().size() )
				error( "Operation " + n.id() + " output variable types and names differ in number" );
		}

		public void visit( InputPortTypeInfo n )
		{
			if ( inputPortTypes.contains( n.id() ) )
				error( "Input port type " + n.id() + " has been already defined" );
			else
				inputPortTypes.add( n.id() );
			
			OperationDeclaration decl;
			for( String opId : n.operations() ) {
				decl = olValidator.operations().get( opId );
				if ( decl == null )
					error( "Undefined operation " + opId + " in input port type " + n.id() );
				else if (	!( decl instanceof OneWayOperationDeclaration ) &&
							!( decl instanceof RequestResponseOperationDeclaration )
							)
					error( "Input port type " + n.id() + " is trying to use a non-input operation (" + opId + ")" );
			}	
		}
		
		public void visit( OutputPortTypeInfo n )
		{
			if ( outputPortTypes.contains( n.id() ) )
				error( "Output port type " + n.id() + " has been already defined" );
			else
				outputPortTypes.add( n.id() );
			
			OperationDeclaration decl;
			for( String opId : n.operations() ) {
				decl = olValidator.operations().get( opId );
				if ( decl == null )
					error( "Undefined operation " + opId + " in output port type " + n.id() );
				else if (	!( decl instanceof NotificationOperationDeclaration ) &&
							!( decl instanceof SolicitResponseOperationDeclaration )
							)
					error( "Output port type " + n.id() + " is trying to use a non-output operation (" + opId + ")" );
			}	
		}
		
		public void visit( PartnerLinkTypeInfo n )
		{
			boolean add = true;
			if ( partnerLinkTypes.get( n.id() ) != null ) {
				error( "Partner link type " + n.id() + " has been already defined" );
				add = false;
			}
			
			String ipt = n.inputPortType();
			String opt = n.outputPortType();
			for( Pair< String, String > pair : partnerLinkTypes.values() ) {
				if ( pair.key().equals( ipt ) )
					error( "Partner link type " + n.id() +
							" is trying to use input port type " + ipt +
							", which has been already defined in another partner link type" );
				
				if ( pair.value().equals( opt ) )
					error( "Partner link type " + n.id() +
							" is trying to use output port type " + opt +
							", which has been already defined in another partner link type" );
			}
		
			if ( add )
				partnerLinkTypes.put( n.id(), new Pair< String, String >( ipt, opt ) );
		}
		
		public void visit( PortBindingInfo n )
		{
			if ( inputPortTypes.contains( n.id() ) )
				error( "Port name " + n.id() +
						" has been already defined as an input port type" );
			
			if ( outputPortTypes.contains( n.id() ) )
				error( "Port name " + n.id() +
						" has been already defined as an output port type" );
			
			if ( partnerLinkTypes.get( n.id() ) != null )
				error( "Port name " + n.id() +
						" has been already defined as a partner link type" );
			
			if ( bindings.get( n.id() ) != null )
				error( "Port name " + n.id() + " has been already defined" );
			else
				bindings.put( n.id(), n );
			
			if (	!( inputPortTypes.contains( n.portType() ) ) &&
					!( outputPortTypes.contains( n.portType() ) ) )
				error( "Port " + n.id() + " tries to use an undefined port type (" + n.portType() + ")" );
		}
		
		public void visit( ServiceInfo n )
		{
			PortBindingInfo binding;
			Constants.ProtocolId protocolId = null;
			for( String p : n.inputPorts() ) {
				binding = bindings.get( p );
				if ( binding == null )
					error( "Service at URI " + n.uri().toString() + " specifies an undefined port (" + p + ")" );
				else {
					if ( protocolId == null )
						protocolId = binding.protocolId();
					else if ( protocolId != binding.protocolId() )
						error( "Service at URI " + n.uri().toString() + " specifies ports with different protocols" );
				}
			}
		}
	}
	
	private class OLValidator implements OLVisitor
	{
		private Logger logger;
		private Set< String > locationNames, linkNames, variableNames, procedureNames;
		private HashMap< String, OperationDeclaration > operations;
		private boolean mainDefined, valid;
		
		public OLValidator( Logger logger )
		{
			this.logger = logger;
			this.locationNames = new HashSet< String >();
			this.linkNames = new HashSet< String >();
			this.variableNames = new HashSet< String >();
			this.procedureNames = new HashSet< String >();
			this.operations = new HashMap< String, OperationDeclaration >();
			this.mainDefined = false;
			this.valid = true;
		}
		
		private void error( String message )
		{
			valid = false;
			logger.severe( message );
		}
		
		public boolean validate( OLSyntaxNode node )
		{
			node.accept( this );
			
			return valid;
		}
		
		public void visit( Program p )
		{
			for( LocationDeclaration decl : p.locationDeclarations() )
				decl.accept( this );
			for( OperationDeclaration decl : p.operationDeclarations() )
				decl.accept( this );
			for( InternalLinkDeclaration decl : p.linkDeclarations() )
				decl.accept( this );
			for( VariableDeclaration decl : p.variableDeclarations() )
				decl.accept( this );
			
			for( Procedure proc : p.procedures() )
				proc.accept( this );
			
			if ( mainDefined == false )
				error( "Main procedure not defined" );
		}
		
		public Set< String > locationNames()
		{
			return locationNames;
		}
		
		public Set< String > linkNames()
		{
			return linkNames;
		}
		
		public Set< String > variableNames()
		{
			return variableNames;
		}
		
		public Set< String > procedureNames()
		{
			return procedureNames;
		}
		
		public HashMap< String, OperationDeclaration > operations()
		{
			return operations;
		}
		
		private boolean isDefined( String id )
		{
			if (	locationNames.contains( id ) ||
					operations.get( id ) != null ||
					linkNames.contains( id ) ||
					variableNames.contains( id ) ||
					procedureNames.contains( id )
				)
				return true;
			
			return false;
		}
		
		public void visit( LocationDeclaration decl )
		{
			if ( isDefined( decl.id() ) )
				error( "Location " + decl.id() + " uses an already defined identifier" );
			else
				locationNames.add( decl.id() );
		}
		
		public void visit( OneWayOperationDeclaration decl )
		{
			if ( isDefined( decl.id() ) )
				error( "Operation " + decl.id() + " uses an already defined identifier" );
			else
				operations.put( decl.id(), decl );
		}
		
		public void visit( RequestResponseOperationDeclaration decl )
		{
			if ( isDefined( decl.id() ) )
				error( "Operation " + decl.id() + " uses an already defined identifier" );
			else
				operations.put( decl.id(), decl );
		}
		
		public void visit( NotificationOperationDeclaration decl )
		{
			if ( isDefined( decl.id() ) )
				error( "Operation " + decl.id() + " uses an already defined identifier" );
			else
				operations.put( decl.id(), decl );
		}
		
		public void visit( SolicitResponseOperationDeclaration decl )
		{
			if ( isDefined( decl.id() ) )
				error( "Operation " + decl.id() + " uses an already defined identifier" );
			else
				operations.put( decl.id(), decl );
		}
		
		public void visit( InternalLinkDeclaration decl )
		{
			if ( isDefined( decl.id() ) )
				error( "Operation " + decl.id() + " uses an already defined identifier" );
			else
				linkNames.add( decl.id() );
		}
		
		public void visit( VariableDeclaration decl )
		{
			if ( isDefined( decl.id() ) )
				error( "Operation " + decl.id() + " uses an already defined identifier" );
			else
				variableNames.add( decl.id() );
		}
		
		public void visit( Procedure procedure )
		{
			if ( isDefined( procedure.id() ) )
				error( "Operation " + procedure.id() + " uses an already defined identifier" );
			else
				procedureNames.add( procedure.id() );
			
			if ( "main".equals( procedure.id() ) )
				mainDefined = true;
			
			procedure.body().accept( this );
		}
		
		public void visit( ParallelStatement stm )
		{
			for( OLSyntaxNode node : stm.children() )
				node.accept( this );
		}
		
		public void visit( SequenceStatement stm )
		{
			for( OLSyntaxNode node : stm.children() )
				node.accept( this );
		}
		
		public void visit( NDChoiceStatement stm )
		{
			for( Pair< OLSyntaxNode, OLSyntaxNode > pair : stm.children() ) {
				pair.key().accept( this );
				pair.value().accept( this );
			}
		}
		
		public void visit( ThrowStatement n ) {}
		public void visit( CompensateStatement n ) {}
		public void visit( InstallCompensationStatement n ) {}
		public void visit( InstallFaultHandlerStatement n ) {}
		public void visit( Scope n ) {}
		
		/*
		 * @todo Must check operation names in opNames and links in linkNames
		 */
		public void visit( OneWayOperationStatement n ) {}
		public void visit( RequestResponseOperationStatement n ) {}
		public void visit( NotificationOperationStatement n ) {}
		public void visit( SolicitResponseOperationStatement n ) {}
		public void visit( LinkInStatement n ) {}
		public void visit( LinkOutStatement n ) {}
		
		/**
		 * @todo Must assign to a variable in varNames
		 */
		public void visit( AssignStatement n ) {}
		public void visit( IfStatement n ) {}
		public void visit( InStatement n ) {}
		public void visit( OutStatement n ) {}
		public void visit( ProcedureCallStatement n ) {}
		public void visit( SleepStatement n ) {}
		public void visit( WhileStatement n ) {}
		public void visit( OrConditionNode n ) {}
		public void visit( AndConditionNode n ) {}
		public void visit( NotConditionNode n ) {}
		public void visit( CompareConditionNode n ) {}
		public void visit( ExpressionConditionNode n ) {}
		public void visit( ConstantIntegerExpression n ) {}
		public void visit( ConstantStringExpression n ) {}
		public void visit( ProductExpressionNode n ) {}
		public void visit( SumExpressionNode n ) {}
		public void visit( VariableExpressionNode n ) {}
		public void visit( NullProcessStatement n ) {}
		public void visit( ExitStatement n ) {}
	}
}
