/*
 * Copyright (C) 2011-2018 Fabrizio Montesi <famontesi@gmail.com>
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

package joliex.formatter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jolie.lang.Constants.OperandType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.Scanner;
import static jolie.lang.parse.Scanner.TokenType.EQUAL;
import static jolie.lang.parse.Scanner.TokenType.LANGLE;
import static jolie.lang.parse.Scanner.TokenType.NOT_EQUAL;
import static jolie.lang.parse.Scanner.TokenType.RANGLE;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CorrelationSetInfo.CorrelationAliasInfo;
import jolie.lang.parse.ast.CorrelationSetInfo.CorrelationVariableInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.DivideAssignStatement;
import jolie.lang.parse.ast.DocumentationComment;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachArrayItemStatement;
import jolie.lang.parse.ast.ForEachSubNodeStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InputPortInfo;
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
import jolie.lang.parse.ast.OperationCollector;
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
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.InstanceOfExpressionNode;
import jolie.lang.parse.ast.expression.IsTypeExpressionNode;
import jolie.lang.parse.ast.expression.IsTypeExpressionNode.CheckType;
import jolie.lang.parse.ast.expression.NotExpressionNode;
import jolie.lang.parse.ast.expression.OrConditionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.lang.parse.ast.expression.VoidExpressionNode;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.util.Pair;
import jolie.util.Range;

/**
 *
 * @author Fabrizio Montesi
 */
public class Formatter implements OLVisitor
{
	public static void format( Program program, PrettyPrinter printer )
		throws FormattingException
	{
		Formatter formatter = new Formatter( printer );
		formatter.prettyPrint( program );
		if ( !formatter.formattingException.getErrorList().isEmpty() ) {
			throw formatter.formattingException;
		}
	}
	
	private final PrettyPrinter printer;
	private boolean shouldPrintQuotes = true;
	private final FormattingException formattingException = new FormattingException();

	private Formatter( PrettyPrinter printer )
	{
		this.printer = printer;
	}
	
	private void prettyPrint( OLSyntaxNode node )
	{
		if ( node != null ) {
			node.accept( this );
		}
	}
	
	private void prettyPrint( Scanner.TokenType tokenType, OLSyntaxNode n )
	{
		String s = "";
		switch( tokenType ) {
		case EQUAL:
			s = "==";
			break;
		case NOT:
			s = "!";
			break;
		case NOT_EQUAL:
			s = "!=";
			break;
		case LANGLE:
			s = "<";
			break;
		case RANGLE:
			s = ">";
			break;
		case LPAREN:
			s = "(";
			break;
		case RPAREN:
			s = ")";
			break;
		default:
			error( n, "Could not print " + tokenType.toString() );
			break;
		}
		printer.write( s );
	}
	
	private void error( OLSyntaxNode node, String message )
	{
		formattingException.addError( node, message );		
	}
	
	@Override
	public void visit( Program n )
	{
		n.children().forEach( this::prettyPrint );
	}
	
	@Override
	public void visit( DefinitionNode n )
	{
		if ( !"main".equals( n.id() ) && !"init".equals( n.id() ) ) {
			printer.writeIndented( "define " );
		}
		printer.writeLineIndented( n.id(), "{" );
		printer.indented( () -> {
			prettyPrint( n.body() );
			printer.newLine();
		} );
		printer.writeLineIndented( "}" );
		printer.newLine();
	}
	
	@Override
	public void visit( ParallelStatement n )
	{
		for( int i = 0; i < n.children().size(); i++ ) {
			prettyPrint( n.children().get( i ) );
			printer.newLine();
			if ( i < n.children().size() - 1 ) {
				printer.writeLineIndented( "|" );
			}
		}
	}
	
	@Override
	public void visit( SequenceStatement n )
	{
		for( int i = 0; i < n.children().size(); i++ ) {
			final OLSyntaxNode child = n.children().get( i );
			if ( child instanceof ParallelStatement ) {
				printer.writeLineIndented( "{" );
				printer.indented( () -> {
					prettyPrint( child );
				} );
				printer.writeLineIndented( "}" );
			} else {
				prettyPrint( child );
			}
			
			if ( i < n.children().size() - 1 ) {
				printer.writeLine( ";" );
			} else {
				printer.newLine();
			}
		}
	}
	
	@Override
	public void visit( AssignStatement n )
	{
//		printer.writeIndented( "" );
		prettyPrint( n.variablePath() );
		printer.write( " = " );
		prettyPrint( n.expression() );
	}
	
	@Override
	public void visit( NotExpressionNode n )
	{
		prettyPrint( Scanner.TokenType.NOT, n );
		prettyPrint( Scanner.TokenType.LPAREN, n );
		prettyPrint( n.expression() );
		prettyPrint( Scanner.TokenType.RPAREN, n );
	}
	
	public void visit( TypeChoiceDefinition n )	{}
	
	public void visit( ProvideUntilStatement n ) {}
	
	public void visit( VoidExpressionNode n ) {}
	
	public void visit( InlineTreeExpressionNode n ) {}
	
	public void visit( InstanceOfExpressionNode n ) {}
	
	public void visit( ForEachArrayItemStatement n ) {}
	
	public void visit( ForEachSubNodeStatement n ) {}
	
	public void visit( OneWayOperationDeclaration n )
	{
		printer.writeLineIndented( n.id() + "(undefined)" );
	}

	
	public void visit( RequestResponseOperationDeclaration n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( NDChoiceStatement n )
	{
		int level;
		Pair< OLSyntaxNode, OLSyntaxNode > pair;
		for( int i = 0; i < n.children().size(); i++ ) {
			pair = n.children().get( i );
			printer.writeIndented( "[ " );
			level = printer.indentationLevel();
			printer.setIndentationLevel( 0 );
			prettyPrint( pair.key() );
			printer.setIndentationLevel( level );
			printer.writeLine( " ] {" );
			printer.indent();
			prettyPrint( pair.value() );
			printer.unindent();
			printer.newLine();
			printer.writeIndented( "}" );
			if ( i < n.children().size() - 1 ) {
				printer.newLine();
			}
		}
	}

	
	public void visit( OneWayOperationStatement n )
	{
		printer.writeIndented( n.id() + "(" );
		prettyPrint( n.inputVarPath() );
		printer.write( ")" );
	}

	
	public void visit( RequestResponseOperationStatement n )
	{
		printer.writeIndented( n.id() + "(" );
		prettyPrint( n.inputVarPath() );
		printer.write( ")(" );
		prettyPrint( n.outputExpression() );
		printer.writeLineIndented( ") {" );
		printer.indent();
		prettyPrint( n.process() );
		printer.unindent();
		printer.writeLineIndented( "" );
		printer.writeIndented( "}" );
	}

	
	public void visit( NotificationOperationStatement n )
	{
		printer.writeIndented( n.id() + "@" + n.outputPortId() + "(" );
		prettyPrint( n.outputExpression() );
		printer.write( ")" );
	}

	
	public void visit( SolicitResponseOperationStatement n )
	{
		printer.writeIndented( n.id() + "@" + n.outputPortId() + "(" );
		prettyPrint( n.outputExpression() );
		printer.write( ")(" );
		prettyPrint( n.inputVarPath() );
		printer.write( ")" );
	}

	
	public void visit( LinkInStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( LinkOutStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( IfStatement n )
	{
		Pair< OLSyntaxNode, OLSyntaxNode > choice;
		for( int i = 0; i < n.children().size(); i++ ) {
			if ( i == 0 ) {
				printer.writeIndented( "if (" );
			} else {
				printer.write( " else if (" );
			}
			choice = n.children().get( i );
			prettyPrint( choice.key() );
			printer.writeLine( ") {");
			printer.indent();
			prettyPrint( choice.value() );
			printer.unindent();
			printer.newLine();
			printer.writeIndented( "}" );
		}
		if ( n.elseProcess() != null ) {
			printer.writeLine( " else {" );
			printer.indent();
			prettyPrint( n.elseProcess() );
			printer.unindent();
			printer.newLine();
			printer.writeIndented( "}" );
		}
	}

	
	public void visit( DefinitionCallStatement n )
	{
		printer.writeLineIndented( n.id() );
	}

	
	public void visit( WhileStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( OrConditionNode n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( AndConditionNode n )
	{
		int i = 0;
		prettyPrint( n.children().get( 0 ) );
		i++;
		for( ; i < n.children().size(); i++ ) {
			printer.write( " && " );
			prettyPrint( n.children().get( i ) );
		}
	}
	
	public void visit( CompareConditionNode n )
	{
		prettyPrint( n.leftExpression() );
		printer.write( " " );
		prettyPrint( n.opType(), n );
		printer.write( " " );
		prettyPrint( n.rightExpression() );
	}

	
	public void visit( ConstantIntegerExpression n )
	{
		printer.write( new Integer( n.value() ).toString() );
	}

	
	public void visit( ConstantStringExpression n )
	{
		if ( shouldPrintQuotes ) {
			printer.write( "\"" );
		}
		printer.write( n.value() );
		if ( shouldPrintQuotes ) {
			printer.write( "\"" );
		}
	}

	
	public void visit( ProductExpressionNode n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( SumExpressionNode n )
	{
		Pair< OperandType, OLSyntaxNode > pair;
		Iterator< Pair< OperandType, OLSyntaxNode > > it = n.operands().iterator();
		for( int i = 0; i < n.operands().size(); i++ ) {
			pair = it.next();
			if ( i > 0 ) {
				if ( pair.key() == OperandType.ADD ) {
					printer.write( " + " );
				} else {
					printer.write( " - " );
				}
			}
			prettyPrint( pair.value() );
		}
	}

	
	public void visit( VariableExpressionNode n )
	{
		prettyPrint( n.variablePath() );
	}

	
	public void visit( NullProcessStatement n )
	{
		printer.writeIndented( "nullProcess" );
	}

	
	public void visit( Scope n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( InstallStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( CompensateStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( ThrowStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( ExitStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( ExecutionInfo n )
	{
		printer.writeIndented( "execution { " );
		printer.write( n.mode().name().toLowerCase() );
		printer.writeLineIndented( " }" );
		printer.newLine();
	}

	
	public void visit( CorrelationSetInfo n )
	{
		printer.writeLineIndented( "cset { " );
		for( CorrelationVariableInfo var : n.variables() ) {
			prettyPrint( var.correlationVariablePath() );
			printer.writeLine( ":" );
			printer.indent();
			printer.writeIndented( "" );
			int i = 0;
			for( CorrelationAliasInfo alias : var.aliases() ) {
				printer.write( alias.guardName() + "." );
				prettyPrint( alias.variablePath() );
				if ( i++ < var.aliases().size() - 1 ) {
					printer.write( ", " );
				} else {
					printer.newLine();
				}
			}
			printer.unindent();
		}
		printer.writeLineIndented( "}" );
		printer.newLine();
	}

	
	public void visit( InputPortInfo n )
	{
		printer.writeLineIndented( "inputPort " + n.id() + " {" );
		printer.writeIndented( "Location: " );
		printer.writeLine( "\"" + n.location().toString() + "\"" );
		if ( n.protocolId() != null ) {
			printer.writeIndented( "Protocol: " );
			printer.writeLine( n.protocolId() );
		}
		if ( !n.getInterfaceList().isEmpty() ) {
			printer.writeIndented( "Interfaces: " );
			int i = 0;
			for( InterfaceDefinition iface : n.getInterfaceList() ) {
				printer.write( iface.name() );
				if ( i++ < n.getInterfaceList().size() - 1 ) {
					printer.write( ", " );
				}
			}
			printer.newLine();
		}
		printer.writeLineIndented( "}" );
		printer.newLine();
	}

	
	public void visit( OutputPortInfo n )
	{
		printer.writeLineIndented( "outputPort " + n.id() + " {" );
		if ( n.location() != null ) {
			printer.writeIndented( "Location: " );
			printer.writeLine( "\"" + n.location().toString() + "\"" );
		}
		if ( n.protocolId() != null ) {
			printer.writeIndented( "Protocol: " );
			printer.writeLine( n.protocolId() );
		}
		if ( !n.getInterfaceList().isEmpty() ) {
			printer.writeIndented( "Interfaces: " );
			int i = 0;
			for( InterfaceDefinition iface : n.getInterfaceList() ) {
				printer.write( iface.name() );
				if ( i++ < n.getInterfaceList().size() - 1 ) {
					printer.write( ", " );
				}
			}
			printer.newLine();
		}
		printOperationDeclarations( n );
		printer.writeLineIndented( "}" );
		printer.newLine();
	}

	
	public void visit( PointerStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( DeepCopyStatement n )
	{
		printer.writeIndented( "" );
		prettyPrint( n.leftPath() );
		printer.write( " << " );
		prettyPrint( n.rightExpression() );
	}

	
	public void visit( RunStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( UndefStatement n )
	{
		printer.writeIndented( "undef(" );
		prettyPrint( n.variablePath() );
		printer.write( ")" );
	}

	
	public void visit( ValueVectorSizeExpressionNode n )
	{
		printer.write( "#" );
		prettyPrint( n.variablePath() );
	}

	
	public void visit( PreIncrementStatement n )
	{
		printer.write( "++" );
		prettyPrint( n.variablePath() );
	}

	
	public void visit( PostIncrementStatement n )
	{
		prettyPrint( n.variablePath() );
		printer.write( "++" );
	}

	
	public void visit( PreDecrementStatement n )
	{
		printer.write( "--" );
		prettyPrint( n.variablePath() );
	}

	
	public void visit( PostDecrementStatement n )
	{
		prettyPrint( n.variablePath() );
		printer.write( "--" );
	}

	
	public void visit( ForStatement n )
	{
		// TODO Auto-generated method stub

	}
	
	public void visit( SpawnStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( IsTypeExpressionNode n )
	{
		if ( n.type() == CheckType.DEFINED ) {
			printer.write( "is_defined(" );
			prettyPrint( n.variablePath() );
			printer.write( ")" );
		}
	}

	
	public void visit( TypeCastExpressionNode n )
	{
		printer.write( n.type().id() );
		printer.write( "(" );
		prettyPrint( n.expression() );
		printer.write( ")" );
	}

	
	public void visit( SynchronizedStatement n )
	{
		printer.writeLineIndented( "synchronized(" + n.id() + ") {" );
		printer.indent();
		prettyPrint( n.body() );
		printer.unindent();
		printer.writeIndented( "}" );
	}

	
	public void visit( CurrentHandlerStatement n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( EmbeddedServiceNode n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( InstallFixedVariableExpressionNode n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( VariablePathNode n )
	{
		boolean backupShouldPrintQuotes = shouldPrintQuotes;
		shouldPrintQuotes = false;
		Pair< OLSyntaxNode, OLSyntaxNode > node;
		if ( n.isGlobal() ) {
			printer.write( "global." );
		}
		for( int i = 0; i < n.path().size(); i++ ) {
			node = n.path().get( i );
			prettyPrint( node.key() );
			if ( !(node.value() instanceof ConstantIntegerExpression &&
				((ConstantIntegerExpression)node.value()).value() == 0)
			) {
				printer.write( "[" );
				prettyPrint( node.value() );
				printer.write( "]" );
			}
			if ( i < n.path().size() - 1 ) {
				printer.write( "." );
			}
		}
		shouldPrintQuotes = backupShouldPrintQuotes;
	}

	boolean insideType = false;
	
	public void prettyPrint( Range r )
	{
		if ( r.min() == r.max() && r.min() == 1 ) {
			return;
		}
		
		if ( r.min() == 0 && r.max() == 1 ) {
			printer.write( "?" );
		} else if ( r.min() == 0 && r.max() == Integer.MAX_VALUE ) {
			printer.write( "*" );
		} else {
			printer.write( "[" + r.min() + "," + r.max() + "]" );
		}
	}

	
	public void visit( TypeInlineDefinition n )
	{
		if ( insideType == false ) {
			printer.writeIndented( "type " );
		}
		printer.write( n.id() ); 
		prettyPrint( n.cardinality() );
		printer.write( ":" + n.nativeType().id() );
		if ( n.untypedSubTypes() ) {
			printer.write( " { ? }" );
		} else if ( n.hasSubTypes() ) {
			boolean backup = insideType;
			insideType = true;
			printer.writeLine( " {" );
			printer.indent();
			for( Entry< String, TypeDefinition > entry : n.subTypes() ) {
				printer.writeIndented( "." );
				prettyPrint( entry.getValue() );
			}
			printer.unindent();
			printer.writeIndented( "}" );
			insideType = backup;
		}
		printer.newLine();
		if ( insideType == false ) {
			printer.newLine();
		}
	}

	
	public void visit( TypeDefinitionLink n )
	{
		// TODO Auto-generated method stub

	}
	
	private void printOperationDeclarations( OperationCollector c )
	{
		Set< Entry< String, OperationDeclaration > > entries = c.operationsMap().entrySet();
		Map< String, OneWayOperationDeclaration > ow = new HashMap< String, OneWayOperationDeclaration >();
		Map< String, RequestResponseOperationDeclaration > rr = new HashMap< String, RequestResponseOperationDeclaration >();
		for( Entry< String, OperationDeclaration > entry : entries ) {
			if ( entry.getValue() instanceof OneWayOperationDeclaration ) {
				ow.put( entry.getValue().id(), (OneWayOperationDeclaration) entry.getValue() );
			} else {
				rr.put( entry.getValue().id(), (RequestResponseOperationDeclaration) entry.getValue() );
			}
		}
		if ( ow.isEmpty() == false ) {
			printer.writeLineIndented( "OneWay:" );
			printer.indent();
			printer.writeIndented( "" );
			int i = 0;
			for( OneWayOperationDeclaration decl : ow.values() ) {
				printer.write( decl.id() );
				if ( decl.requestType() != null ) {
					printer.write( "(" + decl.requestType().id() + ")" );
				}
				if ( i++ < ow.size() - 1 ) {
					printer.write( ", " );
				}
			}
			printer.unindent();
			printer.newLine();
		}
		if ( rr.isEmpty() == false ) {
			printer.writeLineIndented( "RequestResponse:" );
			printer.indent();
			printer.writeIndented( "" );
			int i = 0;
			for( RequestResponseOperationDeclaration decl : rr.values() ) {
				printer.write( decl.id() );
				if ( decl.requestType() != null ) {
					printer.write( "(" + decl.requestType().id() + ")" );
				}
				if ( decl.responseType() != null ) {
					printer.write( "(" + decl.responseType().id() + ")" );
				}
				if ( i++ < rr.size() - 1 ) {
					printer.write( ", " );
				}
			}
			printer.unindent();
			printer.newLine();
		}
	}

	
	public void visit( InterfaceDefinition n )
	{
		printer.writeLineIndented( "interface " + n.name() + " {" );
		printOperationDeclarations( n );
		printer.writeLineIndented( "}" );
		printer.newLine();
	}

	
	public void visit( DocumentationComment n )
	{
		// TODO Auto-generated method stub

	}

	
	public void visit( AddAssignStatement arg0 )
	{
		// TODO Auto-generated method stub
		
	}

	
	public void visit( SubtractAssignStatement arg0 )
	{
		// TODO Auto-generated method stub
		
	}

	
	public void visit( MultiplyAssignStatement arg0 )
	{
		// TODO Auto-generated method stub
		
	}

	
	public void visit( DivideAssignStatement arg0 )
	{
		// TODO Auto-generated method stub
		
	}

	
	public void visit(ConstantDoubleExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	
	public void visit(ConstantBoolExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	
	public void visit(ConstantLongExpression arg0) {
		// TODO Auto-generated method stub
		
	}

	
	public void visit( FreshValueExpressionNode arg0 )
	{
		printer.write( "new" );
	}

	
	public void visit( CourierDefinitionNode arg0 ) {
		// TODO Auto-generated method stub
		
	}

	
	public void visit( CourierChoiceStatement arg0 ) {
		// TODO Auto-generated method stub
		
	}

	
	public void visit( NotificationForwardStatement arg0 ) {
		// TODO Auto-generated method stub
		
	}

	
	public void visit( SolicitResponseForwardStatement arg0 ) {
		// TODO Auto-generated method stub
		
	}

	
	public void visit(InterfaceExtenderDefinition arg0) {
		// TODO Auto-generated method stub
		
	}

}
