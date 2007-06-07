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

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import jolie.Constants;
import jolie.Constants.VariableType;
import jolie.lang.parse.nodes.ol.AndConditionNode;
import jolie.lang.parse.nodes.ol.AssignStatement;
import jolie.lang.parse.nodes.ol.CompareConditionNode;
import jolie.lang.parse.nodes.ol.CompensateStatement;
import jolie.lang.parse.nodes.ol.ConstantIntegerExpression;
import jolie.lang.parse.nodes.ol.ConstantStringExpression;
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
import jolie.util.Pair;

/** Parser for a .ol file. 
 * @author Fabrizio Montesi
 *
 */
public class OLParser extends AbstractParser
{
	public OLParser( Scanner scanner )
	{
		super( scanner );
	}

	public Program parse()
		throws IOException, ParserException
	{
		getToken();
		Program program = new Program(
				parseLocations(),
				parseOperations(),
				parseLinks(),
				parseVariables(),
				parseCode()
			);

		return program;
	}
	
	private Collection< LocationDeclaration > parseLocations()
		throws IOException, ParserException
	{
		Vector< LocationDeclaration > locationDecls = new Vector< LocationDeclaration >();
		
		// The locations block is optional
		if ( token.is( Scanner.TokenType.LOCATIONS ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			
			boolean keepRun = true;
			while( token.isNot( Scanner.TokenType.RCURLY ) && keepRun ) {
				tokenAssert( Scanner.TokenType.ID, "expected location identifier" );
				locationDecls.add( new LocationDeclaration( token.content() ) );
				getToken();
				if ( token.isNot( Scanner.TokenType.COMMA ) )
					keepRun = false;
				else
					getToken();
			}
		
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
		
		return locationDecls;
	}
	
	private Collection< OperationDeclaration > parseOperations()
		throws IOException, ParserException
	{
		Vector< OperationDeclaration > operationDecls = new Vector< OperationDeclaration >();
		
		// The operations block is optional
		if ( token.is( Scanner.TokenType.OPERATIONS ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			
			boolean keepRun = true;
			while( keepRun ) {
				if ( token.is( Scanner.TokenType.OP_OW ) )
					parseOneWayOperations( operationDecls );
				else if ( token.is( Scanner.TokenType.OP_RR ) )
					parseRequestResponseOperations( operationDecls );
				else if ( token.is( Scanner.TokenType.OP_N ) )
					parseNotificationOperations( operationDecls );
				else if ( token.is( Scanner.TokenType.OP_SR ) )
					parseSolicitResponseOperations( operationDecls );
				else
					keepRun = false;
			}
			
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
		
		return operationDecls;
	}
	
	private void parseOneWayOperations( Collection< OperationDeclaration > operationDecls )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );
		
		boolean keepRun = true;
		String opId;
		Collection< Constants.VariableType > inVarTypes; 
		
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.ID ) ) {
				opId = token.content();
				getToken();
				inVarTypes = parseVarTypes();
				operationDecls.add( new OneWayOperationDeclaration( opId, inVarTypes ) );
				//getToken();
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
	}
	
	private void parseRequestResponseOperations( Collection< OperationDeclaration > operationDecls )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );
		
		boolean keepRun = true;
		String opId;
		Collection< Constants.VariableType > inVarTypes, outVarTypes; 
		
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.ID ) ) {
				opId = token.content();
				getToken();
				inVarTypes = parseVarTypes();
				outVarTypes = parseVarTypes();
				operationDecls.add(
					new RequestResponseOperationDeclaration( opId, inVarTypes, outVarTypes )
				);
				//getToken();
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
	}
	
	private void parseNotificationOperations( Collection< OperationDeclaration > operationDecls )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );
		
		boolean keepRun = true;
		String opId;
		Collection< VariableType > outVarTypes; 
		
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.ID ) ) {
				opId = token.content();
				getToken();
				outVarTypes = parseVarTypes();
				eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.ID, "bound input operation name expected" );
				operationDecls.add(
					new NotificationOperationDeclaration( opId, outVarTypes, token.content() )
				);
				getToken();
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
	}
	
	private void parseSolicitResponseOperations( Collection< OperationDeclaration > operationDecls )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );
		
		boolean keepRun = true;
		String opId;
		Collection< VariableType > outVarTypes, inVarTypes; 
		
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.ID ) ) {
				opId = token.content();
				getToken();
				outVarTypes = parseVarTypes();
				inVarTypes = parseVarTypes();
				eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.ID, "bound input operation name expected" );
				operationDecls.add(
					new SolicitResponseOperationDeclaration( opId, outVarTypes, inVarTypes, token.content() )
				);
				getToken();
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
	}
	
	private Collection< Constants.VariableType > parseVarTypes()
		throws IOException, ParserException
	{
		eat( Scanner.TokenType.LANGLE, "expected <" );
		
		Vector< VariableType > varTypes = new Vector< VariableType > ();
		boolean keepRun = true;
		
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.RANGLE ) )
				keepRun = false;
			else {
				if ( token.is( Scanner.TokenType.VAR_TYPE_INT ) )
					varTypes.add( Constants.VariableType.INT );
				else if ( token.is( Scanner.TokenType.VAR_TYPE_STRING ) )
					varTypes.add( Constants.VariableType.STRING );
				else if ( token.is( Scanner.TokenType.VAR_TYPE_VARIANT ) )
					varTypes.add( Constants.VariableType.VARIANT );
				else
					throwException( "expected variable type" );
				
				getToken();
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;
			}
		}

		eat( Scanner.TokenType.RANGLE, "expected >" );
		return varTypes;	
	}
	
	public Collection< InternalLinkDeclaration > parseLinks()
		throws IOException, ParserException
	{
		Vector< InternalLinkDeclaration > linkDecls = new Vector< InternalLinkDeclaration >();
		
		// The links block is optional
		if ( token.is( Scanner.TokenType.LINKS ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
		
			boolean keepRun = true;
		
			while( token.isNot( Scanner.TokenType.RCURLY ) && keepRun ) {
				tokenAssert( Scanner.TokenType.ID, "expected link identifier" );
				linkDecls.add( new InternalLinkDeclaration( token.content() ) );
				getToken();
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;
			}
			
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
		
		return linkDecls;
	}
	
	public Collection< VariableDeclaration > parseVariables()
		throws IOException, ParserException
	{
		Vector< VariableDeclaration > varDecls = new Vector< VariableDeclaration >();
		
		// The links block is optional
		if ( token.is( Scanner.TokenType.VARIABLES ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			
			boolean keepRun = true;
			
			while( token.isNot( Scanner.TokenType.RCURLY ) && keepRun ) {
				tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
				varDecls.add( new VariableDeclaration( token.content() ) );
				getToken();
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;
			}
			
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
		
		return varDecls;
	}
	
	private Collection< Procedure > parseCode()
		throws IOException, ParserException
	{
		Vector< Procedure > procedures = new Vector< Procedure >();
		
		boolean mainDefined = false;
		while ( token.isNot( Scanner.TokenType.EOF ) ) {
			if ( token.is( Scanner.TokenType.DEFINE ) )
				procedures.add( parseProcedure() );
			else if ( token.is( Scanner.TokenType.MAIN ) ) {
				if ( mainDefined )
					throwException( "you must specify only one main definition" );
				procedures.add( parseMain() );
				mainDefined = true;
			} else
				throwException( "expected definition" );
		}
		if ( !mainDefined )
			throwException( "main definition not found" );
		
		return procedures;
	}
	
	private Procedure parseMain()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.LCURLY, "expected { after procedure identifier" );
		Procedure retVal = new Procedure( "main", parseProcess() );
		eat( Scanner.TokenType.RCURLY, "expected } after procedure definition" );
		return retVal;
	}
	
	private Procedure parseProcedure()
		throws IOException, ParserException
	{
		getToken();
		tokenAssert( Scanner.TokenType.ID, "expected procedure identifier" );
		String procedureId = token.content();
		getToken();
		eat( Scanner.TokenType.LCURLY, "expected { after procedure identifier" );
		Procedure retVal = new Procedure( procedureId, parseProcess() );
		eat( Scanner.TokenType.RCURLY, "expected } after procedure definition" );
		
		return retVal;
	}
	
	private OLSyntaxNode parseProcess()
		throws IOException, ParserException
	{
		if( token.is( Scanner.TokenType.LSQUARE ) )
			return parseNDChoiceStatement();
		
		return parseParallelStatement();
	}
	
	private ParallelStatement parseParallelStatement()
		throws IOException, ParserException
	{
		ParallelStatement stm = new ParallelStatement();
		stm.addChild( parseSequenceStatement() );
		while( token.is( Scanner.TokenType.PARALLEL ) ) {
			getToken();
			stm.addChild( parseSequenceStatement() );
		}
		
		return stm;
	}
	
	private SequenceStatement parseSequenceStatement()
		throws IOException, ParserException
	{
		SequenceStatement stm = new SequenceStatement();
		
		stm.addChild( parseBasicStatement() );
		while( token.is( Scanner.TokenType.SEQUENCE ) ) {
			getToken();
			stm.addChild( parseBasicStatement() );
		}
		
		return stm;
	}
	
	private OLSyntaxNode parseBasicStatement()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;
		
		if ( token.is( Scanner.TokenType.ID ) ) {
			String id = token.content();
			getToken();
			if ( token.is( Scanner.TokenType.ASSIGN ) ) {
				getToken();
				retVal = new AssignStatement( id, parseExpression() );
			} else if ( token.is( Scanner.TokenType.LANGLE ) ) {
				retVal = parseInputOperationStatement( id );
			} else if ( token.is( Scanner.TokenType.AT ) ) {
				getToken();
				retVal = parseOutputOperationStatement( id );
			} else
				retVal = new ProcedureCallStatement( id );
		} else if ( token.is( Scanner.TokenType.OUT ) ) { // out( <Expression> )
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			retVal = new OutStatement( parseExpression() );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IN ) ) // in( <id> )
			retVal = parseInStatement();
		else if ( token.is( Scanner.TokenType.LINKIN ) )
			retVal = parseLinkInStatement();
		else if ( token.is( Scanner.TokenType.NULL_PROCESS ) ) {
			getToken();
			retVal = NullProcessStatement.getInstance();
		} else if ( token.is( Scanner.TokenType.WHILE ) )
			retVal = parseWhileStatement();
		else if ( token.is( Scanner.TokenType.SLEEP ) )
			retVal = parseSleepStatement();
		else if ( token.is( Scanner.TokenType.LINKOUT ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected link identifier" );
			retVal = new LinkOutStatement( token.content() );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal = parseProcess();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.SCOPE ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected scope identifier" );
			String id = token.content();
			getToken();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			eat( Scanner.TokenType.LCURLY, "expected {" );
			retVal = new Scope( id, parseProcess() );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		} else if ( token.is( Scanner.TokenType.COMPENSATE ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected scope identifier" );
			retVal = new CompensateStatement( token.content() );
			getToken();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.THROW ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected fault identifier" );
			retVal = new ThrowStatement( token.content() );
			getToken();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.INSTALL_COMPENSATION ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			retVal = new InstallCompensationStatement( parseProcess() );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.INSTALL_FAULT_HANDLER ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected fault identifier" );
			String id = token.content();
			getToken();
			eat( Scanner.TokenType.COMMA, "expected ," );
			retVal = new InstallFaultHandlerStatement( id, parseProcess() );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IF ) ) {
			IfStatement stm = new IfStatement();
			OLSyntaxNode cond;
			OLSyntaxNode node;
			
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			cond = parseCondition();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			eat( Scanner.TokenType.LCURLY, "expected {" );
			node = parseProcess();
			eat( Scanner.TokenType.RCURLY, "expected }" );
			stm.addChild(
				new Pair< OLSyntaxNode, OLSyntaxNode >( cond, node )
			);
			
			boolean keepRun = true;
			while( token.is( Scanner.TokenType.ELSE ) && keepRun ) {
				getToken();
				if ( token.is( Scanner.TokenType.IF ) ) { // else if branch
					getToken();
					eat( Scanner.TokenType.LPAREN, "expected (" );
					cond = parseCondition();
					eat( Scanner.TokenType.RPAREN, "expected )" );
					eat( Scanner.TokenType.LCURLY, "expected {" );
					node = parseProcess();
					eat( Scanner.TokenType.RCURLY, "expected }" );
					stm.addChild(
						new Pair< OLSyntaxNode, OLSyntaxNode >( cond, node )
					);
				} else if ( token.is( Scanner.TokenType.LCURLY ) ) { // else branch
					keepRun = false;
					eat( Scanner.TokenType.LCURLY, "expected {" );
					stm.setElseProcess( parseProcess() );
					eat( Scanner.TokenType.RCURLY, "expected }" );
				} else
					throwException( "expected else or else if branch" );
			}
			
			retVal = stm;
		}
		
		if ( retVal == null )
			throwException( "expected basic statement" );
		
		return retVal;
	}
	
	private InStatement parseInStatement()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.LPAREN, "expected (" );
		tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
		InStatement stm = new InStatement( token.content() );
		getToken();
		eat( Scanner.TokenType.RPAREN, "expected )" );
		return stm;
	}
	
	private NDChoiceStatement parseNDChoiceStatement()
		throws IOException, ParserException
	{
		NDChoiceStatement stm = new NDChoiceStatement();
		OLSyntaxNode inputGuard = null;
		OLSyntaxNode process;
		boolean keepRun = true;
		while( keepRun ) {
			getToken(); // Eat [
			if ( token.is( Scanner.TokenType.LINKIN ) )
				inputGuard = parseLinkInStatement();
			else if ( token.is( Scanner.TokenType.SLEEP ) )
				inputGuard = parseSleepStatement();
			else if ( token.is( Scanner.TokenType.LINKIN ) )
				inputGuard = parseInStatement();
			else if ( token.is( Scanner.TokenType.ID ) ) {
				String id = token.content();
				getToken();
				inputGuard = parseInputOperationStatement( id );
			} else
				throwException( "expected input guard" );
			
			eat( Scanner.TokenType.RSQUARE, "] expected" );
			process = parseProcess();
			stm.addChild( new Pair< OLSyntaxNode, OLSyntaxNode >( inputGuard, process ) );
			if ( token.is( Scanner.TokenType.CHOICE ) ) {
				getToken();
				tokenAssert( Scanner.TokenType.LSQUARE, "expected [" );
			} else
				keepRun = false;
		}
		
		return stm;
	}
	
	private SleepStatement parseSleepStatement()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.LPAREN, "expected (" );
		SleepStatement stm = new SleepStatement( parseExpression() );
		eat( Scanner.TokenType.RPAREN, "expected )" );
		return stm;
	}
	
	private LinkInStatement parseLinkInStatement()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.LPAREN, "expected (" );
		tokenAssert( Scanner.TokenType.ID, "expected link identifier" );
		LinkInStatement stm = new LinkInStatement( token.content() );
		eat( Scanner.TokenType.RPAREN, "expected )" );
		return stm;
	}
	
	private OLSyntaxNode parseInputOperationStatement( String id )
		throws IOException, ParserException
	{
		Collection< String > inVars = parseIdListN();
		OLSyntaxNode stm;
		if ( token.is( Scanner.TokenType.LANGLE ) ) { // Request Response operation
			Collection< String > outVars = parseIdListN();
			OLSyntaxNode process;
			eat( Scanner.TokenType.LPAREN, "expected (" );
			process = parseProcess();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			stm = new RequestResponseOperationStatement( id, inVars, outVars, process );
		} else // One Way operation
			stm = new OneWayOperationStatement( id, inVars );

		return stm;
	}
	
	private OLSyntaxNode parseOutputOperationStatement( String id )
		throws IOException, ParserException
	{
		tokenAssert( Scanner.TokenType.ID, "location expected" );
		String locationId = token.content();
		getToken();
		Collection< String > outVars = parseIdListN();
		OLSyntaxNode stm;
		if ( token.is( Scanner.TokenType.LANGLE ) ) { // Request Response operation
			Collection< String > inVars = parseIdListN();
			stm = new SolicitResponseOperationStatement( id, locationId, outVars, inVars );
		} else // One Way operation
			stm = new NotificationOperationStatement( id, locationId, outVars );

		return stm;
	}
	
	private OLSyntaxNode parseWhileStatement()
		throws IOException, ParserException
	{
		OLSyntaxNode cond, process;
		getToken();
		eat( Scanner.TokenType.LPAREN, "expected (" );
		cond = parseCondition();
		eat( Scanner.TokenType.RPAREN, "expected )" );
		eat( Scanner.TokenType.LCURLY, "expected {" );
		process = parseProcess();
		eat( Scanner.TokenType.RCURLY, "expected }" );
		return new WhileStatement( cond, process );
	}
	
	private OLSyntaxNode parseCondition()
		throws IOException, ParserException
	{
		OrConditionNode orCond = new OrConditionNode();
		orCond.addChild( parseAndCondition() );
		while( token.is( Scanner.TokenType.OR ) ) {
			getToken();
			orCond.addChild( parseAndCondition() );
		}
		
		return orCond;
	}
	
	private OLSyntaxNode parseAndCondition()
		throws IOException, ParserException
	{
		AndConditionNode andCond = new AndConditionNode();
		andCond.addChild( parseBasicCondition() );
		while( token.is( Scanner.TokenType.AND ) ) {
			getToken();
			andCond.addChild( parseBasicCondition() );
		}
		
		return andCond;
	}
	
	private OLSyntaxNode parseBasicCondition()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;
		
		if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal = parseCondition();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.NOT ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			retVal = new NotConditionNode( parseCondition() );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else {
			Scanner.TokenType opType;
			OLSyntaxNode expr1;
			
			expr1 = parseExpression();
			
			opType = token.type();
			if ( opType != Scanner.TokenType.EQUAL && opType != Scanner.TokenType.LANGLE &&
				opType != Scanner.TokenType.RANGLE && opType != Scanner.TokenType.MAJOR_OR_EQUAL &&
				opType != Scanner.TokenType.MINOR_OR_EQUAL && opType != Scanner.TokenType.NOT_EQUAL ) {
				
				retVal = new ExpressionConditionNode( expr1 );
			} else {
				OLSyntaxNode expr2;
				getToken();
				expr2 = parseExpression();
			
				retVal = new CompareConditionNode( expr1, expr2, opType );
			}
		}
		
		if ( retVal == null )
			throwException( "expected condition" );
		
		return retVal;
	}
	
	private OLSyntaxNode parseExpression()
		throws IOException, ParserException
	{
		boolean keepRun = true;
		SumExpressionNode sum = new SumExpressionNode();
		sum.add( parseProductExpression() );
		
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.PLUS ) ) {
				getToken();
				sum.add( parseProductExpression() );
			} else if ( token.is( Scanner.TokenType.MINUS ) ) {
				getToken();
				sum.subtract( parseProductExpression() );
			} else
				keepRun = false;
		}
		
		return sum;
	}
	
	private OLSyntaxNode parseFactor()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;
		
		if ( token.is( Scanner.TokenType.ID ) )
			retVal = new VariableExpressionNode( token.content() );
		else if ( token.is( Scanner.TokenType.STRING ) )
			retVal = new ConstantStringExpression( token.content() );
		else if ( token.is( Scanner.TokenType.INT ) )
			retVal = new ConstantIntegerExpression( Integer.parseInt( token.content() ) );
		else if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal = parseExpression();
			tokenAssert( Scanner.TokenType.RPAREN, "expected )" );
		}
		
		getToken();
		if ( retVal == null )
			throwException( "expected expression" );
		
		return retVal;
	}
	
	private OLSyntaxNode parseProductExpression()
		throws IOException, ParserException
	{
		ProductExpressionNode product = new ProductExpressionNode();
		product.multiply( parseFactor() );
		boolean keepRun = true;
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.ASTERISK ) ) {
				getToken();
				product.multiply( parseFactor() );
			} else if ( token.is( Scanner.TokenType.DIVIDE ) ) {
				getToken();
				product.divide( parseFactor() );
			} else
				keepRun = false;
		}
		
		return product;
	}

}
