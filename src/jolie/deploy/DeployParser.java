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

package jolie.deploy;

import java.io.IOException;
import java.util.Vector;

import jolie.AbstractParser;
import jolie.GlobalLocation;
import jolie.InvalidIdException;
import jolie.NotificationOperation;
import jolie.OneWayOperation;
import jolie.ParserException;
import jolie.RequestResponseOperation;
import jolie.Scanner;
import jolie.SolicitResponseOperation;

/** Parses the deploy file
 * @author Fabrizio Montesi
 */
public class DeployParser extends AbstractParser
{
	/** Constructor */
	public DeployParser( Scanner scanner )
	{
		super( scanner );
	}
	
	public void parse()
		throws IOException, ParserException
	{
		getToken();
		parseState();
		/*parseExecution();
		parseCorrSet();*/
		parseLocations();
		parseWSDL();
	}
	
	private void parseWSDL()
		throws IOException, ParserException
	{
		if ( token.isA( Scanner.TokenType.ID ) && token.content().equals( "wsdl" ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			parseWSDLOperations();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseWSDLOperations()
		throws IOException, ParserException
	{
		if ( token.isA( Scanner.TokenType.OPERATIONS ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			boolean keepRun = true;
			while ( keepRun ) {
				if ( token.type() == Scanner.TokenType.OP_OW )
					parseOneWayOperations();
				else if ( token.type() == Scanner.TokenType.OP_RR )
					parseRequestResponseOperations();
				else if ( token.type() == Scanner.TokenType.OP_N )
					parseNotificationOperations();
				else if ( token.type() == Scanner.TokenType.OP_SR )
					parseSolicitResponseOperations();
				else
					keepRun = false;
			}
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseOneWayOperations()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		Vector< String > inVarNames;
		
		OneWayOperation op;

		while ( keepRun ) {
			if ( !token.isA( Scanner.TokenType.ID ) )
				keepRun = false;
			else {
				try {
					op = OneWayOperation.getById( token.content() );
					getToken();
					eat( Scanner.TokenType.ASSIGN, "= expected" );
					tokenAssert( Scanner.TokenType.ID, "bound operation name expected" );
					op.wsdlInfo().setBoundName( token.content() );
					getToken();
					inVarNames = parseIdListN();
					if ( inVarNames.size() != op.inVarTypes().size() )
						throwException( "invalid operation arguments" );
					
					op.wsdlInfo().setInVarNames( inVarNames );
					
					if ( token.type() == Scanner.TokenType.COMMA )
						getToken();
					else
						keepRun = false;
				} catch( InvalidIdException e ) {
					throwException( "wrong operation name: " + token.content() );
				}
			}
		}
	}
	
	private void parseNotificationOperations()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		Vector< String > varNames;
		
		NotificationOperation op;

		while ( keepRun ) {
			if ( !token.isA( Scanner.TokenType.ID ) )
				keepRun = false;
			else {
				try {
					op = NotificationOperation.getById( token.content() );
					getToken();
					eat( Scanner.TokenType.ASSIGN, "= expected" );
					tokenAssert( Scanner.TokenType.ID, "bound operation name expected" );
					op.wsdlInfo().setBoundName( token.content() );
					getToken();
					varNames = parseIdListN();
					if ( varNames.size() != op.outVarTypes().size() )
						throwException( "invalid operation arguments" );
					
					op.wsdlInfo().setOutVarNames( varNames );
					
					if ( token.type() == Scanner.TokenType.COMMA )
						getToken();
					else
						keepRun = false;
				} catch( InvalidIdException e ) {
					throwException( "wrong operation name: " + token.content() );
				}
			}
		}
	}
	
	private void parseRequestResponseOperations()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		Vector< String > varNames;
		
		RequestResponseOperation op;

		while ( keepRun ) {
			if ( !token.isA( Scanner.TokenType.ID ) )
				keepRun = false;
			else {
				try {
					op = RequestResponseOperation.getById( token.content() );
					getToken();
					eat( Scanner.TokenType.ASSIGN, "= expected" );
					tokenAssert( Scanner.TokenType.ID, "bound operation name expected" );
					op.wsdlInfo().setBoundName( token.content() );
					getToken();
					varNames = parseIdListN();
					if ( varNames.size() != op.inVarTypes().size() )
						throwException( "invalid operation arguments" );
					
					op.wsdlInfo().setInVarNames( varNames );
					
					varNames = parseIdListN();
					if ( varNames.size() != op.outVarTypes().size() )
						throwException( "invalid operation arguments" );
					
					op.wsdlInfo().setOutVarNames( varNames );
					
					if ( token.type() == Scanner.TokenType.COMMA )
						getToken();
					else
						keepRun = false;
				} catch( InvalidIdException e ) {
					throwException( "wrong operation name: " + token.content() );
				}
			}
		}
	}
	
	private void parseSolicitResponseOperations()
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		Vector< String > varNames;
		
		SolicitResponseOperation op;

		while ( keepRun ) {
			if ( !token.isA( Scanner.TokenType.ID ) )
				keepRun = false;
			else {
				try {
					op = SolicitResponseOperation.getById( token.content() );
					getToken();
					eat( Scanner.TokenType.ASSIGN, "= expected" );
					tokenAssert( Scanner.TokenType.ID, "bound operation name expected" );
					op.wsdlInfo().setBoundName( token.content() );
					getToken();
					varNames = parseIdListN();
					if ( varNames.size() != op.outVarTypes().size() )
						throwException( "invalid operation arguments" );
					
					op.wsdlInfo().setOutVarNames( varNames );
					
					varNames = parseIdListN();
					if ( varNames.size() != op.inVarTypes().size() )
						throwException( "invalid operation arguments" );
					
					op.wsdlInfo().setInVarNames( varNames );
					
					if ( token.type() == Scanner.TokenType.COMMA )
						getToken();
					else
						keepRun = false;
				} catch( InvalidIdException e ) {
					throwException( "wrong operation name: " + token.content() );
				}
			}
		}
	}

	
	private Vector< String > parseIdListN()
		throws IOException, ParserException
	{
		Vector< String > idVector = new Vector< String >();
		eat( Scanner.TokenType.LANGLE, "< expected" );
		boolean keepRun = true;
		
		while( keepRun ) {
			if ( token.isA( Scanner.TokenType.RANGLE ) )
				keepRun = false;
			else {
				if ( token.isA( Scanner.TokenType.ID ) )
					idVector.add( token.content() );
				else
					throwException( "expected variable name" );
				
				getToken();
				if ( token.isA( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;
			}
		}
		
		eat( Scanner.TokenType.RANGLE, "> expected" );
		return idVector;
	}
	
	private void parseState() 
		throws IOException, ParserException
	{
		//if (token.isA(Scanner.))
	}
	
	private void parseLocations()
		throws IOException, ParserException
	{
		int checkedLocations = 0;
		
		if ( token.isA( Scanner.TokenType.LOCATIONS ) ) {
			boolean keepRun = true;
			GlobalLocation loc;
			
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while ( !token.isA( Scanner.TokenType.RCURLY ) && keepRun ) {
				tokenAssert( Scanner.TokenType.ID, "location id expected" );
				try {
					loc = GlobalLocation.getById( token.content() );
					getToken();
					eat( Scanner.TokenType.ASSIGN, "= expected" );
					tokenAssert( Scanner.TokenType.STRING, "location value expected" );
					loc.setValue( token.content() );
					checkedLocations++;
				} catch( InvalidIdException e ) {
					throwException( "invalid location identifier" );
				}
				getToken();
				if ( !token.isA( Scanner.TokenType.COMMA ) )
					keepRun = false;
				else
					getToken();
			}
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}

		if ( checkedLocations != GlobalLocation.getAll().size() )
			throwException( "locations deployment block is not complete" ); 
	}
}
