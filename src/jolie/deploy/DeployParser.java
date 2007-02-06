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
import jolie.InputOperation;
import jolie.InvalidIdException;
import jolie.NotificationOperation;
import jolie.OneWayOperation;
import jolie.OutputOperation;
import jolie.ParserException;
import jolie.RequestResponseOperation;
import jolie.Scanner;
import jolie.SolicitResponseOperation;
import jolie.deploy.wsdl.InputPortType;
import jolie.deploy.wsdl.OutputPortType;
import jolie.deploy.wsdl.PartnerLinkType;
import jolie.deploy.wsdl.PortType;
import jolie.net.CommCore;
import jolie.net.CommProtocol;
import jolie.net.SOAPProtocol;
import jolie.net.SODEPProtocol;
import jolie.net.SocketListener;

/** Parses the deploy file
 * @author Fabrizio Montesi
 * @todo Check if a DeployScanner should be created, in order to avoid keyword collisions.
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
			parseInputPortTypes();
			parseOutputPortTypes();
			parsePartnerLinkTypes();
			parseBindings();
			parseService();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseService()
		throws IOException, ParserException
	{
		if ( token.isA( Scanner.TokenType.ID ) && token.content().equals( "service" ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.isA( Scanner.TokenType.ID ) )
				parseServiceElement();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseServiceElement()
		throws IOException, ParserException
	{
		CommProtocol protocol = null;
		if ( token.content().equals( "soap" ) )
			protocol = new SOAPProtocol();
		else if ( token.content().equals( "sodep" ) )
			protocol = new SODEPProtocol();
		else
			throwException( "Unknown protocol: " + token.content() );
		
		eat( Scanner.TokenType.COLON, ": expected" );
		tokenAssert( Scanner.TokenType.INT, "network port expected" );
		int port = Integer.parseInt( token.content() );
		if ( port < 0 || port > 65535 )
			throwException( "Invalid port specified: " + token.content() );
		getToken();

		CommCore.addListener( new SocketListener( protocol, port ) );
	}
	
	private void parseBindings()
		throws IOException, ParserException
	{
		if ( token.isA( Scanner.TokenType.ID ) && token.content().equals( "bindings" ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.isA( Scanner.TokenType.ID ) )
				parseBinding();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseBinding()
		throws IOException, ParserException
	{
		try {
			PortType pt = PortType.getById( token.content() );
			getToken();
			eat( Scanner.TokenType.COLON, ": expected" );
			tokenAssert( Scanner.TokenType.ID, "port type protocol expected" );
			if ( token.content().equals( "soap" ) )
				pt.setProtocolId( CommProtocol.Identifier.SOAP );
			else if ( token.content().equals( "sodep" ) )
				pt.setProtocolId( CommProtocol.Identifier.SODEP );
			else
				throwException( "unknown protocol specified in port type binding" );
			getToken();
		} catch( InvalidIdException iie ) {
			throwException( "invalid port type identifier: " + token.content() );
		}
	}
	
	private void parsePartnerLinkTypes()
		throws IOException, ParserException
	{
		if ( token.isA( Scanner.TokenType.ID ) && token.content().equals( "partnerLinkTypes" ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.isA( Scanner.TokenType.ID ) )
				parsePartnerLinkType();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parsePartnerLinkType()
		throws IOException, ParserException
	{
		String id = token.content();
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );
		try {
			tokenAssert( Scanner.TokenType.ID, "input port type expected" );
			InputPortType ipt = InputPortType.getById( id );
			getToken();
			eat( Scanner.TokenType.COMMA, ", expected" );
			tokenAssert( Scanner.TokenType.ID, "output port type expected" );
			OutputPortType opt = OutputPortType.getById( id );
			getToken();
			( new PartnerLinkType( id, ipt, opt ) ).register();
		} catch( InvalidIdException iie ) {
			throwException( "Invalid port type identifier: " + token.content() );
		}
	}
	
	private void parseInputPortTypes()
		throws IOException, ParserException
	{
		if ( token.isA( Scanner.TokenType.ID ) && token.content().equals( "inputPortTypes" ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.isA( Scanner.TokenType.ID ) )
				parseInputPortType();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseOutputPortTypes()
		throws IOException, ParserException
	{
		if ( token.isA( Scanner.TokenType.ID ) && token.content().equals( "inputPortTypes" ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.isA( Scanner.TokenType.ID ) )
				parseOutputPortType();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseInputPortType()
		throws IOException, ParserException
	{
		InputPortType pt = new InputPortType( token.content() );
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );
		boolean keepRun = true;
		InputOperation op;
		while( keepRun ) {
			if ( !token.isA( Scanner.TokenType.ID ) )
				keepRun = false;
			else {
				try {
					op = InputOperation.getById( token.content() );
					if ( op.wsdlInfo().portType() != null )
						throwException( "the specified operation is already present in another port type: " + token.content() );
					pt.addOperation( op );
				} catch( InvalidIdException iie ) {
					throwException( "Invalid input operation identifier: " + token.content() );
				}
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			}
		}
		pt.register();
	}
	
	private void parseOutputPortType()
		throws IOException, ParserException
	{
		OutputPortType pt = new OutputPortType( token.content() );
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );
		boolean keepRun = true;
		OutputOperation op;
		while( keepRun ) {
			if ( !token.isA( Scanner.TokenType.ID ) )
				keepRun = false;
			else {
				try {
					op = OutputOperation.getById( token.content() );
					if ( op.wsdlInfo().portType() != null )
						throwException( "the specified operation is already present in another port type: " + token.content() );
					pt.addOperation( op );
				} catch( InvalidIdException iie ) {
					throwException( "Invalid input operation identifier: " + token.content() );
				}
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			}
		}
		pt.register(); 
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
