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
import java.util.Vector;

import jolie.process.AssignmentProcess;
import jolie.process.CallProcess;
import jolie.process.IfProcess;
import jolie.process.InProcess;
import jolie.process.InputProcess;
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
import jolie.process.SequentialProcess;
import jolie.process.SleepProcess;
import jolie.process.SolicitResponseProcess;
import jolie.process.WhileProcess;

public class OLParser extends AbstractParser
{
	public OLParser( Scanner scanner )
	{
		super( scanner );
	}
	
	public void parse()
		throws IOException, ParserException
	{
		getToken();
		
		parseLocations();
		parseOperations();
		parseLinks();
		parseVariables();
		
		parseCode();
	}
	
	private void parseCode()
		throws IOException, ParserException
	{
		boolean mainDefined = false;
		while ( token.type() != Scanner.TokenType.EOF ) {
			if ( token.type() == Scanner.TokenType.DEFINE )
				parseDefinition();
			else if ( token.type() == Scanner.TokenType.MAIN ) {
				if ( mainDefined )
					throwException( "you must specify only one main definition" );
				parseMain();
				mainDefined = true;
			} else
				throwException( "definition expected" );
		}
		if ( !mainDefined )
			throwException( "main definition not found" );
	}
	
	private Process parseProcess()
		throws IOException, ParserException
	{
		if ( token.type() == Scanner.TokenType.LSQUARE )
			return parseChoiceProcess();
		
		return parseParallelProcess();
	}
	
	private Process parseChoiceProcess()
		throws IOException, ParserException
	{
		NDChoiceProcess choiceProc = new NDChoiceProcess();
		InputProcess inputProc = null;
		Process execProc;
		boolean stop = false;
		
		while( !stop ) {
			getToken(); // Eat [
			if ( token.type() == Scanner.TokenType.LINKIN )
				inputProc = parseLinkInProcess();
			else if ( token.type() == Scanner.TokenType.SLEEP )
				inputProc = parseSleepProcess();
			else if ( token.type() == Scanner.TokenType.IN ) {
				try {
					inputProc = parseInProcess();
				} catch( InvalidIdException e ) {
					throwException( e.getMessage() );
				}
			} else if ( token.type() == Scanner.TokenType.ID ) {
				String id = token.content();
				getToken();
				
				eat( Scanner.TokenType.LANGLE, "< expected" );

				inputProc = parseInputProcess( id );
			} else
				throwException( "input choice expected" );
		
			if ( inputProc == null )
				throwException( "input expected" );
			
			eat( Scanner.TokenType.RSQUARE, "] expected" );
			execProc = parseProcess();
			choiceProc.addChoice( inputProc, execProc );
			if ( token.type() != Scanner.TokenType.CHOICE )
				stop = true;
			else {
				getToken();
				tokenAssert( Scanner.TokenType.LSQUARE, "[ expected" );
			}
		}
		
		return choiceProc;
	}
	
	private Process parseParallelProcess()
		throws IOException, ParserException
	{
		ParallelProcess proc = new ParallelProcess();

		proc.addChild( parseSequentialProcess() );
		while ( token.type() == Scanner.TokenType.PARALLEL ) {
			getToken();
			proc.addChild( parseSequentialProcess() );
		}

		return proc;
	}
	
	private Process parseSequentialProcess()
		throws IOException, ParserException
	{
		SequentialProcess proc = new SequentialProcess();
		
		proc.addChild( parseBasicProcess() );
		while ( token.type() == Scanner.TokenType.SEQUENCE ) {
			getToken();
			proc.addChild( parseBasicProcess() );
		}
		
		return proc;
	}
	
	private Process parseBasicProcess()
		throws IOException, ParserException
	{
		Process proc = null;
		try {
			if ( token.type() == Scanner.TokenType.ID ) {
				String id = token.content();
				getToken();
				if ( token.type() == Scanner.TokenType.ASSIGN ) {
					getToken();
					proc = new AssignmentProcess( id, parseExpression() );
				} else if ( token.type() == Scanner.TokenType.LANGLE ) {
					proc = parseInputProcess( id );
				} else if ( token.type() == Scanner.TokenType.AT ) {
					getToken();
					proc = parseOutputProcess( id );
				} else { // It's a definition call
					proc = new CallProcess( id );
				}
			} else if ( token.type() == Scanner.TokenType.CALL ) {
				getToken();
				eat( Scanner.TokenType.LPAREN, "( expected" );
				tokenAssert( Scanner.TokenType.ID, "call id expected" );
				proc = new CallProcess( token.content() );
				getToken();
				eat( Scanner.TokenType.RPAREN, ") expected" );
			} else if ( token.type() == Scanner.TokenType.OUT ) {	// out( <Expression> )
				getToken();
				eat( Scanner.TokenType.LPAREN, "( expected" );
				proc = new OutProcess( parseExpression() );
				eat( Scanner.TokenType.RPAREN, ") expected" );
			} else if ( token.type() == Scanner.TokenType.IN ) {
				proc = parseInProcess();
			} else if ( token.type() == Scanner.TokenType.LINKIN ) {
				proc = parseLinkInProcess();
			} else if ( token.type() == Scanner.TokenType.WHILE ) {
				proc = parseWhileProcess();
			} else if ( token.type() == Scanner.TokenType.SLEEP ) {
				proc = parseSleepProcess();
			} else if ( token.type() == Scanner.TokenType.LINKOUT ) {
				getToken();
				eat( Scanner.TokenType.LPAREN, "( expected" );
				if ( token.type() != Scanner.TokenType.ID )
					throwException( "linkOut id expected" );
				proc = new LinkOutProcess( token.content() );
				getToken();
				eat( Scanner.TokenType.RPAREN, ") expected" );
			} else if ( token.type() == Scanner.TokenType.LPAREN ) {
				getToken();
				proc = parseProcess();
				eat( Scanner.TokenType.RPAREN, ") expected" );
			} else if ( token.type() == Scanner.TokenType.IF ) {
				Condition currCond;
				Process currProc;
				IfProcess ifProc = new IfProcess();
				boolean stop = false;
				
				getToken();
				eat( Scanner.TokenType.LPAREN, "( expected" );
				currCond = parseCondition();
				eat( Scanner.TokenType.RPAREN, ") expected" );
				eat( Scanner.TokenType.LCURLY, "{ expected" );
				currProc = parseProcess();
				tokenAssert( Scanner.TokenType.RCURLY, "} expected" );
				ifProc.addPair( currCond, currProc );
				getToken();
				while( token.type() == Scanner.TokenType.ELSE && !stop ) {
					getToken();
					if ( token.type() == Scanner.TokenType.LCURLY ) {	// else block
						stop = true;
						getToken();
						ifProc.setElseProcess( parseProcess() );
						eat( Scanner.TokenType.RCURLY, "} expected" );
					} else if ( token.type() == Scanner.TokenType.IF ) { // else if block
						getToken();
						eat( Scanner.TokenType.LPAREN, "( expected" );
						currCond = parseCondition();
						eat( Scanner.TokenType.RPAREN, ") expected" );
						eat( Scanner.TokenType.LCURLY, "{ expected" );
						currProc = parseProcess();
						tokenAssert( Scanner.TokenType.RCURLY, "} expected" );
						ifProc.addPair( currCond, currProc );
						getToken();
					}
				}
						
				proc = ifProc;
			} else if ( token.type() == Scanner.TokenType.NULL_PROCESS ) {
				proc = NullProcess.getInstance();
				getToken();
			}
		} catch( InvalidIdException e ) {
			throwException( e.getMessage() );
		}

		if ( proc == null )
			throwException( "basic process expected" );

		return proc;
	}
	
	private InputProcess parseInputProcess( String id )
		throws IOException, ParserException
	{
		InputProcess proc = null;
		try {
			InputOperation operation;
		
			operation = InputOperation.getById( id );
			Vector< Variable > inVars = parseOperationVariables();
		
			if ( token.type() == Scanner.TokenType.LANGLE ) { // Request Response operation
				Vector< Variable > outVars = parseOperationVariables();
				if ( operation instanceof RequestResponseOperation ) {
					RequestResponseOperation rro = (RequestResponseOperation) operation;
					if ( inVars.size() != rro.inVarTypes().size() )
						throwException( "wrong operation input parameters" );
					if ( outVars.size() != rro.outVarTypes().size() )
						throwException( "wrong operation output parameters" );
				} else
					throwException( "wrong operation type" );
				
				eat( Scanner.TokenType.LPAREN, "( expected" );
				Process rrProc = parseProcess();
				eat( Scanner.TokenType.RPAREN, ") expected" );				
				proc = new RequestResponseProcess( operation, inVars, outVars, rrProc );
			} else { // One Way operation
				if ( inVars.size() != operation.inVarTypes().size() )
					throwException( "wrong operation input parameters" );
				proc = new OneWayProcess( operation, inVars );
			}
		} catch( InvalidIdException e ) {
			throwException( e.getMessage() );
		}
		if ( proc == null )
			throwException( "input operation expected" );
		return proc;
	}
	
	private Vector< Variable > parseOperationVariables()
		throws IOException, ParserException, InvalidIdException
	{
		Vector< Variable > vars = new Vector< Variable >();
		eat( Scanner.TokenType.LANGLE, "< expected" );
		boolean keepRun = true;
		
		while( token.type() == Scanner.TokenType.ID && keepRun ) {
			vars.add( GlobalVariable.getById( token.content() ) );
			getToken();
			if ( token.type() == Scanner.TokenType.COMMA )
				getToken();
			else
				keepRun = false;
		}
		eat( Scanner.TokenType.LANGLE, "> expected" );

		return vars;
	}
	
	private Process parseOutputProcess( String id )
		throws IOException, ParserException
	{
		Process proc = null;
		tokenAssert( Scanner.TokenType.ID, "location expected" );
		Location location = null;
		
		try {
			location = GlobalLocation.getById( token.content() );
		} catch( InvalidIdException e ) {
			Variable var = null;
			try {
				var = GlobalVariable.getById( token.content() );
			} catch( InvalidIdException iie ) {
				throwException( iie.getMessage() );
			}
			location = new VariableLocation( var );
		}

		try {			
			OutputOperation operation = OutputOperation.getById( id );
						
			getToken();
			if ( token.type() != Scanner.TokenType.LANGLE )
				throwException( "< expected" );
			getToken();
						
			Vector< Variable > outVars = parseOperationVariables();
			
			if ( token.type() == Scanner.TokenType.LANGLE ) { // Solicit Response
				Vector< Variable > inVars = parseOperationVariables();
				if ( operation instanceof SolicitResponseOperation ) {
					SolicitResponseOperation sro = (SolicitResponseOperation) operation;
					if ( outVars.size() != sro.outVarTypes().size() )
						throwException( "wrong operation output parameters" );
					if ( inVars.size() != sro.inVarTypes().size() )
						throwException( "wrong operation input parameters" );
					proc = new SolicitResponseProcess( sro, location, outVars, inVars );
				} else
					throwException( "wrong operation type" );
			} else { // Notification
				if ( outVars.size() != operation.outVarTypes().size() )
					throwException( "wrong operation output parameters" );
				proc = new NotificationProcess( operation, location, outVars );
			}
		} catch( InvalidIdException e ) {
			throwException( e.getMessage() );
		}
		if ( proc == null )
			throwException( "output operation expected" );
		return proc;
	}
	
	private InProcess parseInProcess()
		throws IOException, ParserException, InvalidIdException
	{
		InProcess proc;
		getToken();
		eat( Scanner.TokenType.LPAREN, "( expected" );
		tokenAssert( Scanner.TokenType.ID, "in id expected" );
		proc = new InProcess( token.content() );
		getToken();
		eat( Scanner.TokenType.RPAREN, ") expected" );

		return proc;
	}
	
	private LinkInProcess parseLinkInProcess()
		throws IOException, ParserException
	{
		LinkInProcess proc = null;
		
		eat( Scanner.TokenType.LINKIN, "linkIn expected" );
		eat( Scanner.TokenType.LPAREN, "( expected" );
		tokenAssert( Scanner.TokenType.ID, "link id expected" );
		try {
			proc = new LinkInProcess( token.content() );
		} catch( InvalidIdException e ) {
			throwException( e.getMessage() );
		}
		getToken();
		eat( Scanner.TokenType.RPAREN, ") expected" );

		return proc;
	}
	
	private SleepProcess parseSleepProcess()
		throws IOException, ParserException
	{
		eat( Scanner.TokenType.SLEEP, "expected sleep call" );
		SleepProcess proc = null;
		eat( Scanner.TokenType.LPAREN, "( expected" );
		
		proc = new SleepProcess( parseExpression() );
		
		eat( Scanner.TokenType.RPAREN, ") expected" );
		
		return proc;
	}	
	
	private WhileProcess parseWhileProcess()
		throws IOException, ParserException
	{
		eat( Scanner.TokenType.WHILE, "expected while block" );
		eat( Scanner.TokenType.LPAREN, "( expected" );
		Condition condition = parseCondition();
		eat( Scanner.TokenType.RPAREN, ") expected" );
		eat( Scanner.TokenType.LCURLY, "{ expected" );
		Process process = parseProcess();
		eat( Scanner.TokenType.RCURLY, "} expected" );
		
		return new WhileProcess( condition, process );
	}
	
	private Condition parseCondition()
		throws IOException, ParserException
	{
		OrCondition orCond = new OrCondition();
		orCond.addChild( parseAndCondition() );
		while( token.type() == Scanner.TokenType.OR ) {
			getToken();
			orCond.addChild( parseBasicCondition() );
		}
				
		return orCond;
	}
	
	private Condition parseAndCondition()
		throws IOException, ParserException
	{
		AndCondition andCond = new AndCondition();
		andCond.addChild( parseBasicCondition() );
		while( token.type() == Scanner.TokenType.AND ) {
			getToken();
			andCond.addChild( parseBasicCondition() );
		}
		
		return andCond;
	}
	
	private Condition parseBasicCondition()
		throws IOException, ParserException
	{
		Condition retval = null;

		if ( token.type() == Scanner.TokenType.LPAREN ) {
			getToken();
			retval = parseCondition();
			if ( token.type() != Scanner.TokenType.RPAREN )
				throwException( ") expected" );
			getToken();
		} else if ( token.type() == Scanner.TokenType.NOT ) {
			getToken();
			if ( token.type() != Scanner.TokenType.LPAREN )
				throwException( "( expected" );
			getToken();
			retval = new NotCondition( parseCondition() );
			if ( token.type() != Scanner.TokenType.RPAREN )
				throwException( ") expected" );
			getToken();
		} else {
			Scanner.TokenType opType;
			Expression expr1;
			
			expr1 = parseExpression();
			
			opType = token.type();
			if ( opType != Scanner.TokenType.EQUAL && opType != Scanner.TokenType.LANGLE &&
				opType != Scanner.TokenType.RANGLE && opType != Scanner.TokenType.MAJOR_OR_EQUAL &&
				opType != Scanner.TokenType.MINOR_OR_EQUAL && opType != Scanner.TokenType.NOT_EQUAL ) {
				
				retval = new ExpressionCondition( expr1 );
			} else {
				Expression expr2;
				getToken();
				expr2 = parseExpression();
			
				retval = new CompareCondition( expr1, expr2, opType );
			}
		}
		
		if ( retval == null )
			throwException( "condition expected" );
		
		return retval;
	}
	
	private Expression parseExpression()
		throws IOException, ParserException
	{
		boolean stop = false;
		SumExpression sum = new SumExpression();
		sum.add( parseProductExpression() );
		
		while( !stop ) {
			if ( token.type() == Scanner.TokenType.PLUS ) {
				getToken();
				sum.add( parseProductExpression() );
			} else if ( token.type() == Scanner.TokenType.MINUS ) {
				getToken();
				sum.subtract( parseProductExpression() );
			} else
				stop = true;
		}

		return sum;
	}
	
	private Expression parseFactor()
		throws IOException, ParserException, InvalidIdException
	{
		Expression retval = null;
		
		if ( token.type() == Scanner.TokenType.ID )
			retval = GlobalVariable.getById( token.content() );
		else if ( token.type() == Scanner.TokenType.STRING )
			retval = new TempVariable( token.content() );
		else if ( token.type() == Scanner.TokenType.INT )
			retval = new TempVariable( Integer.parseInt( token.content() ) );
		else if ( token.type() == Scanner.TokenType.LPAREN ) {
			getToken();
			retval = parseExpression();
			tokenAssert( Scanner.TokenType.RPAREN, ") expected" );
		}
		
		getToken();
		
		if ( retval == null )
			throwException( "expression expected" );
		
		return retval;
	}
	
	private ProductExpression parseProductExpression()
		throws IOException, ParserException
	{
		ProductExpression product = new ProductExpression();
		
		try {
			boolean stop = false;
			
			product.multiply( parseFactor() );

			while( !stop ) {
				if ( token.type() == Scanner.TokenType.ASTERISK ) {
					getToken();
					product.multiply( parseFactor() );
				} else if ( token.type() == Scanner.TokenType.DIVIDE ) {
					getToken();
					product.divide( parseFactor() );
				} else
					stop = true;
			}
		} catch( InvalidIdException e ) {
			throwException( e.getMessage() );
		}
		
		return product;
	}
	
	private void parseDefinition()
		throws IOException, ParserException
	{
		String id;
		
		getToken();
		if ( token.type() != Scanner.TokenType.ID )
			throwException( "define id expected" );
		id = token.content();
		
		getToken();
		if ( token.type() != Scanner.TokenType.LCURLY )
			throwException( "{ expected" );

		getToken();
		/*
		 	In order to allow recursion, we register the definition before
		 	reading the contained process.
		*/
		Definition def = new Definition( id, null );
		def.register();
		def.setProcess( parseProcess() );
		
		if ( token.type() != Scanner.TokenType.RCURLY )
			throwException( "} expected" );
		getToken();
	}
	
	private void parseMain()
		throws IOException, ParserException
	{
		getToken();
		if ( token.type() != Scanner.TokenType.LCURLY )
			throwException( "{ expected" );

		getToken();
		Definition def = new Definition( "main", null );
		def.register();
		def.setProcess( parseProcess() );
		
		if ( token.type() != Scanner.TokenType.RCURLY )
			throwException( "} expected" );
		getToken();
	}
	
	private void parseLocations()
		throws IOException, ParserException
	{
		boolean stop = false;
		
		if ( token.type() != Scanner.TokenType.LOCATIONS )
			return;
		
		getToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );
		
		while( token.type() != Scanner.TokenType.RCURLY && !stop ) {
			tokenAssert( Scanner.TokenType.ID, "location id expected" );
			(new GlobalLocation( token.content() )).register();
			getToken();
			if ( token.type() != Scanner.TokenType.COMMA )
				stop = true;
			else
				getToken();
		}
		
		eat( Scanner.TokenType.RCURLY, "} expected" );
	}
	
	private void parseOperations()
		throws IOException, ParserException
	{
		boolean stop = false;
	
		if ( token.type() != Scanner.TokenType.OPERATIONS )
			return;

		getToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );

		while ( !stop ) {
			if ( token.type() == Scanner.TokenType.OP_OW )
				parseOneWayOperations();
			else if ( token.type() == Scanner.TokenType.OP_RR )
				parseRequestResponseOperations();
			else if ( token.type() == Scanner.TokenType.OP_N )
				parseNotificationOperations();
			else if ( token.type() == Scanner.TokenType.OP_SR )
				parseSolicitResponseOperations();
			else
				stop = true;
		}
		
		eat( Scanner.TokenType.RCURLY, "} expected" );		
	}
	
	private void parseOneWayOperations()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		String opName;
		Vector< Variable.Type > inVarTypes;

		while ( keepRun ) {
			if ( token.type() != Scanner.TokenType.ID )
				keepRun = false;
			else {
				opName = token.content();
				getToken();
				inVarTypes = parseVarTypes();
				(new OneWayOperation( opName, inVarTypes )).register();
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			}
		}
	}
	
	private Vector< Variable.Type > parseVarTypes()
		throws IOException, ParserException
	{
		eat( Scanner.TokenType.LANGLE, "< expected" );
		
		Vector< Variable.Type > varTypes = new Vector< Variable.Type > ();
		boolean keepRun = true;
		
		while( keepRun ) {
			if ( token.type() == Scanner.TokenType.RANGLE )
				keepRun = false;
			else {
				if ( token.type() == Scanner.TokenType.VAR_TYPE_INT )
					varTypes.add( Variable.Type.INT );
				else if ( token.type() == Scanner.TokenType.VAR_TYPE_STRING )
					varTypes.add( Variable.Type.STRING );
				else if ( token.type() == Scanner.TokenType.VAR_TYPE_VARIANT )
					varTypes.add( Variable.Type.VARIANT );
				else
					throwException( "expected variable type" );
				
				getToken();
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			}
		}

		eat( Scanner.TokenType.RANGLE, "> expected" );
		return varTypes;
	}
	
	private void parseRequestResponseOperations()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		String opName;
		Vector< Variable.Type > inVarTypes, outVarTypes;

		while ( keepRun ) {
			if ( token.type() != Scanner.TokenType.ID )
				keepRun = false;
			else {
				opName = token.content();
				getToken();
				inVarTypes = parseVarTypes();
				outVarTypes = parseVarTypes();
				(new RequestResponseOperation( opName, inVarTypes, outVarTypes )).register();
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			}
		}
	}
	
	private void parseNotificationOperations()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		String opName;
		Vector< Variable.Type > outVarTypes;

		while ( keepRun ) {
			if ( token.type() != Scanner.TokenType.ID )
				keepRun = false;
			else {
				opName = token.content();
				getToken();
				outVarTypes = parseVarTypes();
				eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.ID, "bound input operation expected" );
				(new NotificationOperation( opName, outVarTypes, token.content() )).register();
				getToken();
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			}
		}
	}
	
	private void parseSolicitResponseOperations()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		String opName;
		Vector< Variable.Type > outVarTypes, inVarTypes;

		while ( keepRun ) {
			if ( token.type() != Scanner.TokenType.ID )
				keepRun = false;
			else {
				opName = token.content();
				getToken();
				outVarTypes = parseVarTypes();
				inVarTypes = parseVarTypes();
				eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.ID, "bound input operation expected" );
				(new SolicitResponseOperation( opName, outVarTypes, inVarTypes, token.content() )).register();
				getToken();
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			}
		}
	}
	
	private void parseVariables()
		throws IOException, ParserException
	{
		boolean stop = false;
		
		if ( token.type() != Scanner.TokenType.VARIABLES )
			return;
		
		getToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );
		while( token.type() != Scanner.TokenType.RCURLY && !stop ) {
			tokenAssert( Scanner.TokenType.ID, "variable id expected" );
			(new GlobalVariable( token.content() )).register();
			getToken();
			if ( token.type() != Scanner.TokenType.COMMA )
				stop = true;
			else
				getToken();
		}
		eat( Scanner.TokenType.RCURLY, "} expected" );
	}
	
	private void parseLinks()
		throws IOException, ParserException
	{
		boolean stop = false;
		
		if ( token.type() != Scanner.TokenType.LINKS )
			return;
		
		getToken();
		eat( Scanner.TokenType.LCURLY, "{ expected" );
		while( token.type() != Scanner.TokenType.RCURLY && !stop ) {
			tokenAssert( Scanner.TokenType.ID, "variable id expected" );
			(new InternalLink( token.content() )).register();
			getToken();
			if ( token.type() != Scanner.TokenType.COMMA )
				stop = true;
			else
				getToken();
		}
		
		eat( Scanner.TokenType.RCURLY, "} expected" );
	}
}


