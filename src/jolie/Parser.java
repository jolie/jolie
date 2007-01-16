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

public class Parser extends AbstractParser
{
	public Parser( Scanner scanner )
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
					getToken();
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
			boolean stop = false;
			Vector< Variable > vars = new Vector< Variable >();
			InputOperation operation;
		
			operation = InputOperation.getById( id );
			
			while( token.type() == Scanner.TokenType.ID && !stop ) {
				vars.add( GlobalVariable.getById( token.content() ) );
				getToken();
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					stop = true;
			}
			if ( token.type() != Scanner.TokenType.RANGLE )
				throwException( "> expected" );
			getToken();
			
			if ( token.type() == Scanner.TokenType.LANGLE ) { // Request Response operation
				Vector< Variable > outVars = new Vector < Variable >();
				stop = false;
				getToken();
				while( token.type() == Scanner.TokenType.ID && !stop ) {
					outVars.add( GlobalVariable.getById( token.content() ) );
					getToken();
					if ( token.type() == Scanner.TokenType.COMMA )
						getToken();
					else
						stop = true;
				}
				if ( token.type() != Scanner.TokenType.RANGLE )
					throwException( "> expected" );
				getToken();
				if ( token.type() != Scanner.TokenType.LPAREN )
					throwException( "( expected" );
				getToken();
				Process rrProc = parseProcess();
				if ( token.type() != Scanner.TokenType.RPAREN )
					throwException( ") expected" );
				getToken();
				
				proc = new RequestResponseProcess( operation, vars, outVars, rrProc );
			} else { // One Way operation
				proc = new OneWayProcess( operation, vars );
			}
		} catch( InvalidIdException e ) {
			throwException( e.getMessage() );
		}
		if ( proc == null )
			throwException( "input operation expected" );
		return proc;
	}
	
	private Process parseOutputProcess( String id )
		throws IOException, ParserException
	{
		Process proc = null;
		boolean stop = false;
		if ( token.type() != Scanner.TokenType.ID )
			throwException( "location expected" );
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
			Operation operation = Operation.getById( id );
						
			getToken();
			if ( token.type() != Scanner.TokenType.LANGLE )
				throwException( "< expected" );
			getToken();
						
			Vector< Variable > vars = new Vector< Variable >();
						
			while( token.type() == Scanner.TokenType.ID && !stop ) {
				vars.add( GlobalVariable.getById( token.content() ) );
				getToken();
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					stop = true;
			}
			if ( token.type() != Scanner.TokenType.RANGLE )
				throwException( "> expected" );
			getToken();
			if ( token.type() == Scanner.TokenType.LANGLE ) { // Solicit Response
				Vector< Variable > inVars = new Vector< Variable >();
				stop = false;
				getToken();
				while( token.type() == Scanner.TokenType.ID && !stop ) {
					inVars.add( GlobalVariable.getById( token.content() ) );
					getToken();
					if ( token.type() == Scanner.TokenType.COMMA )
						getToken();
					else
						stop = true;
				}
				if ( token.type() != Scanner.TokenType.RANGLE )
					throwException( "> expected" );
				getToken();
				
				proc = new SolicitResponseProcess( operation, location, vars, inVars );
			} else { // Notification
				proc = new NotificationProcess( operation, location, vars );
			}
		} catch( InvalidIdException e ) {
			throwException( e.getMessage() );
		}
		if ( proc == null )
			throwException( "input operation expected" );
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
	
	private InputProcess parseLinkInProcess()
		throws IOException, ParserException
	{
		InputProcess proc = null;

		if ( token.type() == Scanner.TokenType.LINKIN ) {
			getToken();
			if ( token.type() != Scanner.TokenType.LPAREN )
				throwException( "( expected" );
			getToken();
			if ( token.type() != Scanner.TokenType.ID )
				throwException( "call id expected" );
			try {
				proc = new LinkInProcess( token.content() );
			} catch( InvalidIdException e ) {
				throwException( e.getMessage() );
			}
			getToken();
			if ( token.type() != Scanner.TokenType.RPAREN )
				throwException( ") expected" );
			getToken();
		}
		
		if ( proc == null )
			throwException( "expected input" );
		
		return proc;
	}
	
	private SleepProcess parseSleepProcess()
		throws IOException, ParserException
	{
		SleepProcess proc = null;

		if ( token.type() == Scanner.TokenType.SLEEP ) {
			getToken();
			if ( token.type() != Scanner.TokenType.LPAREN )
				throwException( "( expected" );
			getToken();
			if ( token.type() == Scanner.TokenType.INT ) {
				proc = new SleepProcess( new TempVariable( token.content() ) );
			} else if ( token.type() == Scanner.TokenType.ID ) {
				try {
					proc = new SleepProcess( GlobalVariable.getById( token.content() ) );
				} catch( InvalidIdException e ) {
					throwException( e.getMessage() );
				}
			} else
				throwException( "sleep integer or variable parameter expected" );
			
			getToken();
			if ( token.type() != Scanner.TokenType.RPAREN )
				throwException( ") expected" );
			getToken();
		}
		
		if ( proc == null )
			throwException( "expected sleep call" );
		
		return proc;
	}	
	
	private WhileProcess parseWhileProcess()
		throws IOException, ParserException
	{
		WhileProcess proc = null;

		if ( token.type() == Scanner.TokenType.WHILE ) {
			getToken();
			if ( token.type() != Scanner.TokenType.LPAREN )
				throwException( "( expected" );
			getToken();
			Condition condition = parseCondition();
			if ( token.type() != Scanner.TokenType.RPAREN )
				throwException( ") expected" );
			getToken();
			if ( token.type() != Scanner.TokenType.LCURLY )
				throwException( "{ expected" );
			getToken();
			Process process = parseProcess();
			if ( token.type() != Scanner.TokenType.RCURLY )
				throwException( "} expected" );
			proc = new WhileProcess( condition, process );
			getToken();
		}
		
		if ( proc == null )
			throwException( "expected while block" );
		
		return proc;
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
		
		if ( token.type() == Scanner.TokenType.ID ) {
			retval = GlobalVariable.getById( token.content() );
		} else if ( token.type() == Scanner.TokenType.STRING ) {
			retval = new TempVariable( token.content() );
		} else if ( token.type() == Scanner.TokenType.INT ) {
			int value = Integer.parseInt( token.content() );
			retval = new TempVariable( value );
		} else if ( token.type() == Scanner.TokenType.LPAREN ) {
			getToken();
			retval = parseExpression();
			if ( token.type() != Scanner.TokenType.RPAREN )
				throwException( ") expected" );
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
		String id, value;
		boolean stop = false;
		
		if ( token.type() != Scanner.TokenType.LOCATIONS )
			throwException( "locations expected" );
		getToken();
		if ( token.type() != Scanner.TokenType.LCURLY )
			throwException( "{ expected");
		getToken();
		while ( token.type() != Scanner.TokenType.RCURLY && !stop ) {
			if ( token.type() != Scanner.TokenType.ID )
				throwException( "location id expected" );
			id = token.content();
			getToken();
			if ( token.type() != Scanner.TokenType.ASSIGN )
				throwException( "= expected" );
			getToken();
			if ( token.type() != Scanner.TokenType.STRING )
				throwException( "string expected" );
			value = token.content();
			(new GlobalLocation( id, value )).register();
			getToken();
			if ( token.type() != Scanner.TokenType.COMMA )
				stop = true;
			else
				getToken();
		}
		if ( token.type() != Scanner.TokenType.RCURLY )
			throwException( "} expected");
		getToken();
	}
	
	private void parseOperations()
		throws IOException, ParserException
	{
		boolean stop = false;
	
		if ( token.type() != Scanner.TokenType.OPERATIONS )
			throwException( "operations expected" );
		getToken();
		if ( token.type() != Scanner.TokenType.LCURLY )
			throwException( "{ expected");
		getToken();
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
		if ( token.type() != Scanner.TokenType.RCURLY )
			throwException( "} expected");
		getToken();
	}
	
	private void parseOneWayOperations()
		throws IOException, ParserException
	{
		boolean stop = false;

		getToken();
		if ( token.type() != Scanner.TokenType.COLON )
			throwException( ": expected" );
		while ( !stop ) {
			getToken();
			if ( token.type() != Scanner.TokenType.ID )
				stop = true;
			else {
				(new OneWayOperation( token.content() )).register();
				getToken();
				if ( token.type() != Scanner.TokenType.COMMA )
					stop = true;
			}
		}
	}
	
	private void parseRequestResponseOperations()
		throws IOException, ParserException
	{
		boolean stop = false;

		getToken();
		if ( token.type() != Scanner.TokenType.COLON )
			throwException( ": expected" );
		while ( !stop ) {
			getToken();
			if ( token.type() != Scanner.TokenType.ID )
				stop = true;
			else {
				(new RequestResponseOperation( token.content() )).register();
				getToken();
				if ( token.type() != Scanner.TokenType.COMMA )
					stop = true;
			}
		}
	}
	
	private void parseNotificationOperations()
		throws IOException, ParserException
	{
		boolean stop = false;
		String id;

		getToken();
		if ( token.type() != Scanner.TokenType.COLON )
			throwException( ": expected" );
		while ( !stop ) {
			getToken();
			if ( token.type() != Scanner.TokenType.ID )
				stop = true;
			else {
				id = token.content();
				getToken();
				if ( token.type() != Scanner.TokenType.ASSIGN )
					throwException( "= expected" );
				getToken();
				if ( token.type() != Scanner.TokenType.ID )
					throwException( "value for operation expected" );
				(new NotificationOperation( id, token.content() )).register();
				getToken();
				if ( token.type() != Scanner.TokenType.COMMA )
					stop = true;
			}
		}
	}
	
	private void parseSolicitResponseOperations()
		throws IOException, ParserException
	{
		boolean stop = false;
		String id;

		getToken();
		if ( token.type() != Scanner.TokenType.COLON )
			throwException( ": expected" );
		while ( !stop ) {
			getToken();
			if ( token.type() != Scanner.TokenType.ID )
				stop = true;
			else {
				id = token.content();
				getToken();
				if ( token.type() != Scanner.TokenType.ASSIGN )
					throwException( "= expected" );
				getToken();
				if ( token.type() != Scanner.TokenType.ID )
					throwException( "value for operation expected" );
				(new SolicitResponseOperation( id, token.content() )).register();
				getToken();
				if ( token.type() != Scanner.TokenType.COMMA )
					stop = true;
			}
		}
	}
	
	private void parseVariables()
		throws IOException, ParserException
	{
		boolean stop = false;
		
		if ( token.type() != Scanner.TokenType.VARIABLES )
			throwException( "variables expected" );
		getToken();
		if ( token.type() != Scanner.TokenType.LCURLY )
			throwException( "{ expected");
		getToken();
		
		while( token.type() != Scanner.TokenType.RCURLY && !stop ) {
			if ( token.type() != Scanner.TokenType.ID )
				throwException( "variable id expected" );
			(new GlobalVariable( token.content() )).register();
			getToken();
			if ( token.type() != Scanner.TokenType.COMMA )
				stop = true;
			else
				getToken();
		}

		if ( token.type() != Scanner.TokenType.RCURLY )
			throwException( "} expected" );
		getToken();
	}
	
	private void parseLinks()
		throws IOException, ParserException
	{
		boolean stop = false;
		
		if ( token.type() != Scanner.TokenType.LINKS )
			throwException( "links expected" );
		getToken();
		if ( token.type() != Scanner.TokenType.LCURLY )
			throwException( "{ expected");
		getToken();
		
		while( token.type() != Scanner.TokenType.RCURLY && !stop ) {
			if ( token.type() != Scanner.TokenType.ID )
				throwException( "variable id expected" );
			(new InternalLink( token.content() )).register();
			getToken();
			if ( token.type() != Scanner.TokenType.COMMA )
				stop = true;
			else
				getToken();
		}

		if ( token.type() != Scanner.TokenType.RCURLY )
			throwException( "} expected" );
		getToken();
	}
}


