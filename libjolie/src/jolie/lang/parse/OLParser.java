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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import jolie.Constants;
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
import jolie.lang.parse.ast.StatementChannelInfo;
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
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePath;
import jolie.lang.parse.ast.WhileStatement;
import jolie.util.Pair;

/** Parser for a .ol file. 
 * @author Fabrizio Montesi
 *
 */
public class OLParser extends AbstractParser
{
	private Program program = new Program( new ParsingContext() );
	private HashMap< String, Scanner.Token > constantsMap =
					new HashMap< String, Scanner.Token > ();

	public OLParser( Scanner scanner )
	{
		super( scanner );
	}
	
	public Program parse()
		throws IOException, ParserException
	{
		getToken();
		Scanner.Token t;
		do {
			t = token;
			parseInclude();
			parseConstants();
			parseInclude();
			parseExecution();
			parseInclude();
			parseCorrelationSet();
			parseInclude();
			parsePortTypes();
			parseInclude();
			parsePorts();
			parseInclude();
			parseService();
			parseInclude();
			parseCode();
		} while( t != token && t.isNot( Scanner.TokenType.EOF ) );

		return program;
	}
	
	private void parseCorrelationSet()
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.CSET ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			String varId;
			VariablePath path;
			HashSet< VariablePath > cPaths = new HashSet< VariablePath >();
			while( token.is( Scanner.TokenType.ID ) ) {
				varId = token.content();
				getToken();
				path = parseVariablePath( varId );
				cPaths.add( path );
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					break;
			}
			program.addChild( new CorrelationSetInfo( getContext(), cPaths ) );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}
	
	private void parseExecution()
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.EXECUTION ) ) {
			Constants.ExecutionMode mode = Constants.ExecutionMode.SEQUENTIAL;
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			if ( token.is( Scanner.TokenType.SEQUENTIAL ) ) {
				mode = Constants.ExecutionMode.SEQUENTIAL;
			} else if ( token.is( Scanner.TokenType.CONCURRENT ) ) {
				mode = Constants.ExecutionMode.CONCURRENT;
			} else if ( token.is( Scanner.TokenType.SINGLE ) ) {
				mode = Constants.ExecutionMode.SINGLE;
			} else
				throwException( "Expected execution mode, found " + token.content() );

			program.addChild( new ExecutionInfo( getContext(), mode ) );
			getToken();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseConstants()
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.CONSTANTS ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			boolean keepRun = true;
			while( token.is( Scanner.TokenType.ID ) && keepRun ) {
				String cId = token.content();
				getToken();
				eat( Scanner.TokenType.ASSIGN, "expected =" );
				if ( 	token.isNot( Scanner.TokenType.STRING ) &&
						token.isNot( Scanner.TokenType.INT ) &&
						token.isNot( Scanner.TokenType.ID ) ) {
					throwException( "expected string, integer or identifier constant" );
				}
				constantsMap.put( cId, token );
				getToken();
				if ( token.isNot( Scanner.TokenType.COMMA ) )
					keepRun = false;
				else
					getToken();
			}
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}
	
	private void parseInclude()
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.INCLUDE ) ) {
			getToken();
			Scanner oldScanner = scanner();
			tokenAssert( Scanner.TokenType.STRING, "expected filename to include" );
			setScanner( new Scanner( new FileInputStream( token.content() ), token.content() ) );
			parse();
			setScanner( oldScanner );
			getToken();
		}
	}

	private boolean checkConstant()
	{
		if ( token.is( Scanner.TokenType.ID ) ) {
			Scanner.Token t = constantsMap.get( token.content() );
			if ( t != null ) {
				token = t;
				return true;
			}
		}
		return false;
	}
	
	private void parsePortTypes()
		throws IOException, ParserException
	{
		while ( token.isKeyword( "inputPortType" ) ) {
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected input port type identifier" );
			InputPortTypeInfo pt = new InputPortTypeInfo( getContext(), token.content() );
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			parseInputOperations( pt );
			program.addChild( pt );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
		
		while ( token.isKeyword( "outputPortType" ) ) {
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected output port type identifier" );
			OutputPortTypeInfo pt = new OutputPortTypeInfo( getContext(), token.content() );
			getToken();
			if ( token.is( Scanner.TokenType.COLON ) ) {
				getToken();
				tokenAssert( Scanner.TokenType.STRING, "expected port type namespace" );
				pt.setNamespace( token.content() );
				getToken();
			}		
			eat( Scanner.TokenType.LCURLY, "expected {" );
			parseOutputOperations( pt );
			program.addChild( pt );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}
	
	private void parsePorts()
		throws IOException, ParserException
	{
		if ( token.isKeyword( "ports" ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "expected {" );
			Constants.ProtocolId protocolId;
			String portTypeId, portId;
			while( token.is( Scanner.TokenType.ID ) ) {
				portTypeId = token.content();
				getToken();
				tokenAssert( Scanner.TokenType.COLON, ": expected" );
				do {
					getToken();
					tokenAssert( Scanner.TokenType.ID, "port id expected" );
					portId = token.content();
					getToken();
					eat( Scanner.TokenType.LSQUARE, "expected [" );
					tokenAssert( Scanner.TokenType.ID, "port protocol expected" );
					protocolId = Constants.stringToProtocolId( token.content() );
					if ( protocolId == Constants.ProtocolId.UNSUPPORTED )
						throwException( "unknown protocol specified in port binding" );
					getToken();
					eat( Scanner.TokenType.RSQUARE, "expected ]" );
					program.addChild( new PortInfo( getContext(), portId, portTypeId, protocolId ) );
				} while( token.is( Scanner.TokenType.COMMA ) );
			}
			eat( Scanner.TokenType.RCURLY, "expected }" );
		}
	}
	
	private void parseService()
		throws IOException, ParserException
	{
		if ( token.isKeyword( "service" ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.isNot( Scanner.TokenType.RCURLY ) )
				parseServiceElement();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseServiceElement()
		throws IOException, ParserException
	{
		checkConstant();
		tokenAssert( Scanner.TokenType.STRING, "expected service location" );
		URI serviceUri = null;
		try {
			serviceUri = new URI( token.content() );
		} catch( URISyntaxException e ) {
			throwException( e );
		}
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );
		Collection< String > ports = parseIdListN( false );
		if ( ports.size() < 1 )
			throwException( "expected at least one port identifier" );
		program.addChild( new ServiceInfo( getContext(), serviceUri, ports ) );
	}
	
	private void parseInputOperations( InputPortTypeInfo pt )
		throws IOException, ParserException
	{
		boolean keepRun = true;
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.OP_OW ) )
				parseOneWayOperations( pt );
			else if ( token.is( Scanner.TokenType.OP_RR ) )
				parseRequestResponseOperations( pt );
			else
				keepRun = false;
		}
	}
	
	private void parseOutputOperations( OutputPortTypeInfo pt )
		throws IOException, ParserException
	{
		boolean keepRun = true;
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.OP_N ) )
				parseNotificationOperations( pt );
			else if ( token.is( Scanner.TokenType.OP_SR ) )
				parseSolicitResponseOperations( pt );
			else
				keepRun = false;
		}
	}
	
	private void parseOneWayOperations( InputPortTypeInfo pt )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );
		
		boolean keepRun = true;
		
		while( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.ID ) ) {
				pt.addOperation( new OneWayOperationDeclaration( getContext(), token.content() ) );
				getToken();
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
	}
	
	private void parseRequestResponseOperations( InputPortTypeInfo pt )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );
		
		boolean keepRun = true;
		String opId;
		//Collection< Constants.VariableType > inVarTypes, outVarTypes; 
		
		while( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.ID ) ) {
				opId = token.content();
				getToken();
				/*inVarTypes = parseVarTypes();
				outVarTypes = parseVarTypes();*/
				Vector< String > faultNames = new Vector< String >();
				if ( token.is( Scanner.TokenType.THROWS ) ) {
					getToken();
					while( token.is( Scanner.TokenType.ID ) ) {
						faultNames.add( token.content() );
						getToken();
						if ( token.isNot( Scanner.TokenType.COMMA ) )
							break;
						else getToken();
					}
				}
				pt.addOperation(
					new RequestResponseOperationDeclaration( getContext(), opId, faultNames )
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
	
	private void parseNotificationOperations( OutputPortTypeInfo pt )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );
		
		boolean keepRun = true;
		//String opId;
		//Collection< VariableType > outVarTypes; 
		
		while( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.ID ) ) {
				/*opId = token.content();
				getToken();*/
				//outVarTypes = parseVarTypes();
				/*eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.ID, "bound input operation name expected" );*/
				pt.addOperation(
					new NotificationOperationDeclaration( getContext(), token.content() )
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
	
	private void parseSolicitResponseOperations( OutputPortTypeInfo pt )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, "expected :" );
		
		boolean keepRun = true;
		//String opId;
		//Collection< VariableType > outVarTypes, inVarTypes; 
		
		while( keepRun ) {
			checkConstant();
			if ( token.is( Scanner.TokenType.ID ) ) {
				//opId = token.content();
				//getToken();
				/*outVarTypes = parseVarTypes();
				inVarTypes = parseVarTypes();*/
				/*eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.ID, "bound input operation name expected" );*/
				pt.addOperation(
					new SolicitResponseOperationDeclaration( getContext(), token.content() )
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
	
	private void parseCode()
		throws IOException, ParserException
	{
		boolean mainDefined = false;
		boolean keepRun = true;
		do {
			if ( token.is( Scanner.TokenType.DEFINE ) )
				program.addChild( parseProcedure() );
			else if ( token.is( Scanner.TokenType.MAIN ) ) {
				if ( mainDefined )
					throwException( "you must specify only one main definition" );
				program.addChild( parseMain() );
				mainDefined = true;
			} else
				keepRun = false;
		} while( keepRun );
	}
	
	private Procedure parseMain()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.LCURLY, "expected { after procedure identifier" );
		Procedure retVal = new Procedure( getContext(), "main", parseProcess() );
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
		Procedure retVal = new Procedure( getContext(), procedureId, parseProcess() );
		eat( Scanner.TokenType.RCURLY, "expected } after procedure definition" );
		
		return retVal;
	}
	
	public OLSyntaxNode parseProcess()
		throws IOException, ParserException
	{
		return parseParallelStatement();
	}
	
	private ParallelStatement parseParallelStatement()
		throws IOException, ParserException
	{
		ParallelStatement stm = new ParallelStatement( getContext() );
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
		SequenceStatement stm = new SequenceStatement( getContext() );
		
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
		
		if ( token.is( Scanner.TokenType.LSQUARE ) ) {
			retVal = parseNDChoiceStatement();
		} else if ( token.is( Scanner.TokenType.ID ) ) {
			checkConstant();
			String id = token.content();
			getToken();
			if ( token.is( Scanner.TokenType.COLON )
					|| token.is( Scanner.TokenType.LSQUARE )
					|| token.is( Scanner.TokenType.DOT )
					|| token.is( Scanner.TokenType.ASSIGN )
					|| token.is( Scanner.TokenType.POINTS_TO )
					|| token.is( Scanner.TokenType.DEEP_COPY_LEFT )
					|| token.is( Scanner.TokenType.DECREMENT )
					|| token.is( Scanner.TokenType.CHOICE )
					) {
				retVal = parseAssignOrDeepCopyOrPointerStatement( id );
			} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
				retVal = parseInputOperationStatement( id );
			} else if ( token.is( Scanner.TokenType.AT ) ) {
				getToken();
				retVal = parseOutputOperationStatement( id );
			} else
				retVal = new ProcedureCallStatement( getContext(), id );
		} else if ( token.is( Scanner.TokenType.CHOICE ) ) { // Pre increment: ++i
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new PreIncrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.DECREMENT ) ) { // Pre decrement
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new PreDecrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.SYNCHRONIZED ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected lock id" );
			String id = token.content();
			getToken();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			eat( Scanner.TokenType.LCURLY, "expected {" );
			retVal = new SynchronizedStatement( getContext(), id, parseProcess() );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		} else if ( token.is( Scanner.TokenType.UNDEF ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String id = token.content();
			getToken();
			retVal = new UndefStatement( getContext(), parseVariablePath( id ) );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.FOR ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			OLSyntaxNode init = parseProcess();
			eat( Scanner.TokenType.COMMA, "expected ," );
			OLSyntaxNode condition = parseCondition();
			eat( Scanner.TokenType.COMMA, "expected ," );
			OLSyntaxNode post = parseProcess();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			
			OLSyntaxNode body = parseBasicStatement();
			
			retVal = new ForStatement( getContext(), init, condition, post, body );
		} else if ( token.is( Scanner.TokenType.FOREACH ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected variable path" );
			String varId = token.content();
			getToken();
			VariablePath keyPath = parseVariablePath( varId );
			eat( Scanner.TokenType.COMMA, "expected ," );
			tokenAssert( Scanner.TokenType.ID, "expected variable path" );
			varId = token.content();
			getToken();
			VariablePath valuePath = parseVariablePath( varId );
			eat( Scanner.TokenType.IN, "expected in" );
			tokenAssert( Scanner.TokenType.ID, "expected variable path" );
			varId = token.content();
			getToken();
			VariablePath targetPath = parseVariablePath( varId );
			eat( Scanner.TokenType.RPAREN, "expected )" );
			
			OLSyntaxNode body = parseBasicStatement();
			
			retVal = new ForEachStatement( getContext(), keyPath, valuePath, targetPath, body );
		} else if ( token.is( Scanner.TokenType.OUT ) ) { // out( <Expression> )
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			retVal = new OutStatement( getContext(), parseExpression() );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.RUN ) ) { // run( <Expression> )
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			retVal = new RunStatement( getContext(), parseExpression() );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IN ) ) // in( <id> )
			retVal = parseInStatement();
		else if ( token.is( Scanner.TokenType.LINKIN ) )
			retVal = parseLinkInStatement();
		else if ( token.is( Scanner.TokenType.NULL_PROCESS ) ) {
			getToken();
			retVal = new NullProcessStatement( getContext() );
		} else if ( token.is( Scanner.TokenType.EXIT ) ) {
			getToken();
			retVal = new ExitStatement( getContext() );
		} else if ( token.is( Scanner.TokenType.WHILE ) )
			retVal = parseWhileStatement();
		else if ( token.is( Scanner.TokenType.SLEEP ) )
			retVal = parseSleepStatement();
		else if ( token.is( Scanner.TokenType.LINKOUT ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected link identifier" );
			retVal = new LinkOutStatement( getContext(), token.content() );
			getToken();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal = parseProcess();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.LCURLY ) ) {
			getToken();
			retVal = parseProcess();
			eat( Scanner.TokenType.RCURLY, "expected }" );
		} else if ( token.is( Scanner.TokenType.SCOPE ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();
			tokenAssert( Scanner.TokenType.ID, "expected scope identifier" );
			String id = token.content();
			getToken();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			eat( Scanner.TokenType.LCURLY, "expected {" );
			retVal = new Scope( getContext(), id, parseProcess() );
			eat( Scanner.TokenType.RCURLY, "expected }" );
		} else if ( token.is( Scanner.TokenType.COMPENSATE ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();
			tokenAssert( Scanner.TokenType.ID, "expected scope identifier" );
			retVal = new CompensateStatement( getContext(), token.content() );
			getToken();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.THROW ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			checkConstant();
			tokenAssert( Scanner.TokenType.ID, "expected fault identifier" );
			retVal = new ThrowStatement( getContext(), token.content() );
			getToken();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.INSTALL ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			retVal = new InstallStatement( getContext(), parseInstallFunction() );
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IF ) ) {
			IfStatement stm = new IfStatement( getContext() );
			OLSyntaxNode cond;
			OLSyntaxNode node;
			
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			cond = parseCondition();
			eat( Scanner.TokenType.RPAREN, "expected )" );
			node = parseBasicStatement();
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
					node = parseBasicStatement();
					stm.addChild(
						new Pair< OLSyntaxNode, OLSyntaxNode >( cond, node )
					);
				} else { // else branch
					keepRun = false;
					stm.setElseProcess( parseBasicStatement() );
				}
			}
			
			retVal = stm;
		}
		
		if ( retVal == null )
			throwException( "expected basic statement" );
		
		return retVal;
	}
	
	private InstallFunctionNode parseInstallFunction()
		throws IOException, ParserException
	{
		Vector< Pair< String, OLSyntaxNode > > vec =
				new Vector< Pair< String, OLSyntaxNode > >();

		String id;
		// @todo: this is buggy, as it allows lists ending with a comma
		while( token.is( Scanner.TokenType.ID ) || token.is( Scanner.TokenType.THIS ) ) {
			if ( token.is( Scanner.TokenType.ID ) )
				id = token.content();
			else
				id = null;
			getToken();
			eat( Scanner.TokenType.ARROW, "expected =>" );
			vec.add( new Pair< String, OLSyntaxNode >( id, parseProcess() ) );
			if ( token.isNot( Scanner.TokenType.COMMA ) )
				break;
			else
				getToken();
		}
		
		return new InstallFunctionNode( vec );
	}
	
	private OLSyntaxNode parseAssignOrDeepCopyOrPointerStatement( String varName )
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;
		VariablePath path = parseVariablePath( varName );
		
		if ( token.is( Scanner.TokenType.ASSIGN ) ) {
			getToken();
			retVal = new AssignStatement( getContext(), path, parseExpression() );
		} else if ( token.is( Scanner.TokenType.CHOICE ) ) {
			getToken();
			retVal = new PostIncrementStatement( getContext(), path );
		} else if ( token.is( Scanner.TokenType.DECREMENT ) ) {
			getToken();
			retVal = new PostDecrementStatement( getContext(), path );
		} else if ( token.is( Scanner.TokenType.POINTS_TO ) ) {
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String id = token.content();
			getToken();
			retVal = new PointerStatement( getContext(), path, parseVariablePath( id ) );
		} else if ( token.is( Scanner.TokenType.DEEP_COPY_LEFT ) ) {
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String id = token.content();
			getToken();
			retVal = new DeepCopyStatement( getContext(), path, parseVariablePath( id ) );
		} else
			throwException( "expected = or -> or << or -- or ++" );
		
		
		return retVal;
	}
	
	private VariablePath parseVariablePath( String varId )
		throws IOException, ParserException
	{
		OLSyntaxNode expr = null;
		VariablePath path = null;
		
		if ( varId.equals( Constants.GLOBAL ) ) {
			path = new VariablePath( true );
		} else {
			path = new VariablePath( false );
			if ( token.is( Scanner.TokenType.LSQUARE ) ) {
				getToken();
				expr = parseExpression();
				eat( Scanner.TokenType.RSQUARE, "expected ]" );
			}
			path.append( new Pair< String, OLSyntaxNode >( varId, expr ) );
		}
		
		String node;
		while( token.is( Scanner.TokenType.DOT ) ) {
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected nested node identifier" );
			node = token.content();
			getToken();
			if ( token.is( Scanner.TokenType.LSQUARE ) ) {
				getToken();
				expr = parseExpression();
				eat( Scanner.TokenType.RSQUARE, "expected ]" );
			} else {
				expr = null;
			}
			
			path.append( new Pair< String, OLSyntaxNode >( node, expr ) );
		}
		
		if ( token.is( Scanner.TokenType.COLON ) ) {
			getToken();
			path.setAttribute( parseExpression() );
		}
		return path;
	}
	
	private InStatement parseInStatement()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.LPAREN, "expected (" );
		tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
		String varId = token.content();
		getToken();
		InStatement stm = new InStatement( getContext(), parseVariablePath( varId ) );
		eat( Scanner.TokenType.RPAREN, "expected )" );
		return stm;
	}
	
	private NDChoiceStatement parseNDChoiceStatement()
		throws IOException, ParserException
	{
		NDChoiceStatement stm = new NDChoiceStatement( getContext() );
		OLSyntaxNode inputGuard = null;
		OLSyntaxNode process;
		boolean keepRun = true;
		while( keepRun ) {
			getToken(); // Eat [
			if ( token.is( Scanner.TokenType.LINKIN ) )
				inputGuard = parseLinkInStatement();
			else if ( token.is( Scanner.TokenType.SLEEP ) )
				inputGuard = parseSleepStatement();
			else if ( token.is( Scanner.TokenType.IN ) )
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
		SleepStatement stm = new SleepStatement( getContext(), parseExpression() );
		eat( Scanner.TokenType.RPAREN, "expected )" );
		return stm;
	}
	
	private LinkInStatement parseLinkInStatement()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.LPAREN, "expected (" );
		tokenAssert( Scanner.TokenType.ID, "expected link identifier" );
		LinkInStatement stm = new LinkInStatement( getContext(), token.content() );
		getToken();
		eat( Scanner.TokenType.RPAREN, "expected )" );
		return stm;
	}
	
	private OLSyntaxNode parseInputOperationStatement( String id )
		throws IOException, ParserException
	{
		ParsingContext context = getContext();
		VariablePath inputVarPath = parseOperationStatementParameter();
		OLSyntaxNode stm;
		StatementChannelInfo info;
		if ( token.is( Scanner.TokenType.LPAREN ) ) { // Request Response operation
			VariablePath outputVarPath = parseOperationStatementParameter();
			info = parseInputChannelInfo();
			OLSyntaxNode process;
			eat( Scanner.TokenType.LCURLY, "expected {" );
			process = parseProcess();
			eat( Scanner.TokenType.RCURLY, "expected }" );
			stm = new RequestResponseOperationStatement(
					context, id, inputVarPath, outputVarPath, process, info
					);
		} else { // One Way operation
			info = parseInputChannelInfo();
			stm = new OneWayOperationStatement( context, id, inputVarPath, info );
		}

		return stm;
	}
	
	private StatementChannelInfo parseInputChannelInfo()
		throws IOException, ParserException
	{
		Constants.ChannelOperator type = null;
		
		if ( token.is( Scanner.TokenType.IN ) )
			type = Constants.ChannelOperator.IN;
		else if ( token.is( Scanner.TokenType.FROM ) )
			type = Constants.ChannelOperator.FROM;
		else if ( token.is( Scanner.TokenType.PICK ) )
			type = Constants.ChannelOperator.PICK;
		else
			return null;
		
		VariablePath channelPath = null;
		VariablePath indexPath = null;
		
		getToken();
		tokenAssert( Scanner.TokenType.ID, "expected variable path after operation statement" );
		String varId = token.content();
		channelPath = parseVariablePath( varId );
		if ( type == Constants.ChannelOperator.PICK && token.is( Scanner.TokenType.IN ) ) {
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected variable path after pick/in command" );
			String indexId = token.content();
			indexPath = parseVariablePath( indexId );
		}
		
		return new StatementChannelInfo( type, channelPath, indexPath );
	}
	
	/**
	 * @return The VariablePath parameter of the statement. May be null.
	 * @throws IOException
	 * @throws ParserException
	 */
	private VariablePath parseOperationStatementParameter()
		throws IOException, ParserException
	{
		VariablePath ret = null;
		
		eat( Scanner.TokenType.LPAREN, "expected (" );
		if ( token.is( Scanner.TokenType.ID ) ) {
			String varId = token.content();
			getToken();
			ret = parseVariablePath( varId );
		}
		eat( Scanner.TokenType.RPAREN, "expected )" );
		
		return ret;
	}
	
	private OLSyntaxNode parseOutputOperationStatement( String id )
		throws IOException, ParserException
	{
		ParsingContext context = getContext();
		OLSyntaxNode locExpr = parseExpression();
		VariablePath outputVarPath = parseOperationStatementParameter();

		OLSyntaxNode stm;
		if ( token.is( Scanner.TokenType.LPAREN ) ) { // Solicit Response operation
			VariablePath inputVarPath = parseOperationStatementParameter();
			InstallFunctionNode function = null;
			if ( token.is( Scanner.TokenType.LSQUARE ) ) {
				eat( Scanner.TokenType.LSQUARE, "expected [" );
				function = parseInstallFunction();
				eat( Scanner.TokenType.RSQUARE, "expected ]" );
			}
			stm = new SolicitResponseOperationStatement(
					getContext(),
					id,
					locExpr,
					outputVarPath,
					inputVarPath,
					function );
		} else // Notification operation
			stm = new NotificationOperationStatement( context, id, locExpr, outputVarPath );

		return stm;
	}
	
	private OLSyntaxNode parseWhileStatement()
		throws IOException, ParserException
	{
		ParsingContext context = getContext();
		OLSyntaxNode cond, process;
		getToken();
		eat( Scanner.TokenType.LPAREN, "expected (" );
		cond = parseCondition();
		eat( Scanner.TokenType.RPAREN, "expected )" );
		eat( Scanner.TokenType.LCURLY, "expected {" );
		process = parseProcess();
		eat( Scanner.TokenType.RCURLY, "expected }" );
		return new WhileStatement( context, cond, process );
	}
	
	private OLSyntaxNode parseCondition()
		throws IOException, ParserException
	{
		OrConditionNode orCond = new OrConditionNode( getContext() );
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
		AndConditionNode andCond = new AndConditionNode( getContext() );
		andCond.addChild( parseBasicCondition() );
		while( token.is( Scanner.TokenType.AND ) ) {
			getToken();
			andCond.addChild( parseBasicCondition() );
		}
		
		return andCond;
	}
	
	private OLSyntaxNode parseNotCondition()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;
		
		if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal = parseCondition();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else
			retVal = new ExpressionConditionNode( getContext(), parseExpression() );

		return new NotConditionNode( getContext(), retVal );
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
			retVal = parseNotCondition();
		} else {
			Scanner.TokenType opType;
			OLSyntaxNode expr1;
			
			expr1 = parseExpression();
			
			opType = token.type();
			if ( opType != Scanner.TokenType.EQUAL && opType != Scanner.TokenType.LANGLE &&
				opType != Scanner.TokenType.RANGLE && opType != Scanner.TokenType.MAJOR_OR_EQUAL &&
				opType != Scanner.TokenType.MINOR_OR_EQUAL && opType != Scanner.TokenType.NOT_EQUAL ) {
				
				retVal = new ExpressionConditionNode( getContext(), expr1 );
			} else {
				OLSyntaxNode expr2;
				getToken();
				expr2 = parseExpression();
			
				retVal = new CompareConditionNode( getContext(), expr1, expr2, opType );
			}
		}
		
		if ( retVal == null )
			throwException( "expected condition" );
		
		return retVal;
	}
	
	/**
	 * @todo Check if negative integer handling is appropriate
	 */
	private OLSyntaxNode parseExpression()
		throws IOException, ParserException
	{
		boolean keepRun = true;
		SumExpressionNode sum = new SumExpressionNode( getContext() );
		sum.add( parseProductExpression() );
		
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.PLUS ) ) {
				getToken();
				sum.add( parseProductExpression() );
			} else if ( token.is( Scanner.TokenType.MINUS ) ) {
				getToken();
				sum.subtract( parseProductExpression() );
			} else if ( token.is( Scanner.TokenType.INT ) ) { // e.g. i -1
				int value = Integer.parseInt( token.content() );
				// We add it, because it's already negative.
				if ( value < 0 )
					sum.add( parseProductExpression() );
				else // e.g. i 1
					throwException( "expected expression operator" );
			} else if ( token.is( Scanner.TokenType.REAL ) ) { // e.g. i -1
				double value = Double.parseDouble( token.content() );
				// We add it, because it's already negative.
				if ( value < 0 )
					sum.add( parseProductExpression() );
				else // e.g. i 1
					throwException( "expected expression operator" );
			} else
				keepRun = false;
		}
		
		return sum;
	}
	
	private Collection< String > parseIdListN( boolean enclosed )
		throws IOException, ParserException
	{
		Vector< String > idList = new Vector< String >();
		if ( enclosed )
			eat( Scanner.TokenType.LPAREN, "expected (" );
		boolean keepRun = true;
		while( token.is( Scanner.TokenType.ID ) && keepRun ) {
			idList.add( token.content() );
			getToken();
			if ( token.is( Scanner.TokenType.COMMA ) )
				getToken();
			else
				keepRun = false;
		}
		if ( enclosed )
			eat( Scanner.TokenType.RPAREN, "expected )" );
		return idList;
	}
	
	private OLSyntaxNode parseFactor()
		throws IOException, ParserException
	{
		OLSyntaxNode retVal = null;
		
		checkConstant();
		if ( token.is( Scanner.TokenType.ID ) ) {
			String varId = token.content();
			getToken();
			VariablePath path = parseVariablePath( varId );
			if ( token.is( Scanner.TokenType.CHOICE ) ) { // Post increment
				getToken();
				retVal = new PostIncrementStatement( getContext(), path );
			} else if ( token.is( Scanner.TokenType.DECREMENT ) ) {
				getToken();
				retVal = new PostDecrementStatement( getContext(), path );
			} else if ( token.is( Scanner.TokenType.ASSIGN ) ) {
				getToken();
				retVal = new AssignStatement( getContext(), path, parseExpression() );
			} else {
				retVal = new VariableExpressionNode( getContext(), path );
			}
		} else if ( token.is( Scanner.TokenType.STRING ) ) {
			retVal = new ConstantStringExpression( getContext(), token.content() );
			getToken();
		} else if ( token.is( Scanner.TokenType.INT ) ) {
			retVal = new ConstantIntegerExpression( getContext(), Integer.parseInt( token.content() ) );
			getToken();
		} else if ( token.is( Scanner.TokenType.REAL ) ) {
			retVal = new ConstantRealExpression( getContext(), Double.parseDouble( token.content()) );
			getToken();
		} else if ( token.is( Scanner.TokenType.LPAREN ) ) {
			getToken();
			retVal = parseExpression();
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.HASH ) ) {
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new ValueVectorSizeExpressionNode( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.CHOICE ) ) { // Pre increment: ++i
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new PreIncrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.DECREMENT ) ) { // Pre decrement
			getToken();
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new PreDecrementStatement( getContext(), parseVariablePath( varId ) );
		} else if ( token.is( Scanner.TokenType.IS_DEFINED ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new IsTypeExpressionNode(
						getContext(), IsTypeExpressionNode.CheckType.DEFINED, parseVariablePath( varId )
						);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IS_INT ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new IsTypeExpressionNode(
						getContext(), IsTypeExpressionNode.CheckType.INT, parseVariablePath( varId )
						);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IS_REAL ) ) {
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
				String varId = token.content();
				getToken();
				retVal = new IsTypeExpressionNode(
							getContext(), IsTypeExpressionNode.CheckType.REAL, parseVariablePath( varId )
							);
				eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.IS_STRING ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new IsTypeExpressionNode(
						getContext(), IsTypeExpressionNode.CheckType.STRING, parseVariablePath( varId )
						);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.CAST_INT ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new TypeCastExpressionNode(
						getContext(), Constants.VariableType.INT, parseVariablePath( varId )
						);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.CAST_REAL ) ) {
				getToken();
				eat( Scanner.TokenType.LPAREN, "expected (" );
				tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
				String varId = token.content();
				getToken();
				retVal = new TypeCastExpressionNode(
							getContext(), Constants.VariableType.REAL, parseVariablePath( varId )
							);
				eat( Scanner.TokenType.RPAREN, "expected )" );
		} else if ( token.is( Scanner.TokenType.CAST_STRING ) ) {
			getToken();
			eat( Scanner.TokenType.LPAREN, "expected (" );
			tokenAssert( Scanner.TokenType.ID, "expected variable identifier" );
			String varId = token.content();
			getToken();
			retVal = new TypeCastExpressionNode(
						getContext(), Constants.VariableType.STRING, parseVariablePath( varId )
						);
			eat( Scanner.TokenType.RPAREN, "expected )" );
		}

		if ( retVal == null )
			throwException( "expected expression" );
		
		return retVal;
	}
	
	private OLSyntaxNode parseProductExpression()
		throws IOException, ParserException
	{
		ProductExpressionNode product = new ProductExpressionNode( getContext() );
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
