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
import java.net.URI;
import java.net.URISyntaxException;
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
import jolie.deploy.wsdl.InputPort;
import jolie.deploy.wsdl.InputPortType;
import jolie.deploy.wsdl.OutputPortType;
import jolie.deploy.wsdl.PartnerLinkType;
import jolie.deploy.wsdl.PortCreationException;
import jolie.deploy.wsdl.PortType;
import jolie.net.CommCore;
import jolie.net.CommProtocol;
import jolie.net.SOAPProtocol;
import jolie.net.SODEPProtocol;
import jolie.net.UnsupportedCommMediumException;

/** Parses the deploy file
 * @author Fabrizio Montesi
 * @todo Check if an ad-hoc DeployScanner should be created, in order to avoid keyword collisions.
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
			while( token.isA( Scanner.TokenType.STRING ) )
				parseServiceElement();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseServiceElement()
		throws IOException, ParserException
	{
		CommProtocol protocol = null;
		try {
			Vector< InputPort > inputPorts = new Vector< InputPort >();
			URI serviceUri = new URI( token.content() );
			
			getToken();
			eat( Scanner.TokenType.COLON, ": expected" );
			tokenAssert( Scanner.TokenType.ID, "expected port identifier" );
			
			InputPort port = InputPort.getById( token.content() );
			CommProtocol.Identifier pId = port.protocolId();
			if ( pId == CommProtocol.Identifier.SOAP )
				protocol = new SOAPProtocol( serviceUri );
			else if ( pId == CommProtocol.Identifier.SODEP )
				protocol = new SODEPProtocol();
			else
				throwException( "Unhandled protocol specified for port " + port.id() );
			
			inputPorts.add( port );
			
			getToken();
			while( token.isA( Scanner.TokenType.COMMA ) ) {
				port = InputPort.getById( token.content() );
				if ( port.protocolId() != pId )
					throwException( "Protocol incompatible ports specified in service element" );
				inputPorts.add( port );
				getToken();
			}
			
			CommCore.addService( serviceUri, protocol, inputPorts );
		} catch( URISyntaxException uriex ) {
			throwException( uriex );
		} catch( UnsupportedCommMediumException ucm ) {
			throwException( ucm );
		} catch( InvalidIdException iie ) {
			throwException( iie );
		}
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
			CommProtocol.Identifier protocolId = CommProtocol.Identifier.SODEP;
			String portId = token.content();
			/*try {
				Port.getById( portId );
				throwException( "Port identifier " + portId + " already in use" );
			} catch( InvalidIdException iie ) {}*/
			getToken();
			eat( Scanner.TokenType.COLON, ": expected" );
			tokenAssert( Scanner.TokenType.ID, "port type id expected" );
			PortType pt = PortType.getById( token.content() );
			getToken();
			eat( Scanner.TokenType.COLON, ": expected" );
			tokenAssert( Scanner.TokenType.ID, "port type protocol expected" );
			if ( token.content().equals( "soap" ) )
				protocolId = CommProtocol.Identifier.SOAP;
			else if ( token.content().equals( "sodep" ) )
				protocolId = CommProtocol.Identifier.SODEP;
			else
				throwException( "unknown protocol specified in port binding" );
			getToken();
			try {
				( pt.createPort( portId, protocolId ) ).register();
			} catch( PortCreationException ex ) {
				throwException( ex.getMessage() );
			}
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
			if ( ipt.partnerLinkType() != null )
				throwException( "specified input port type is already present in another partner link type" );
			getToken();
			eat( Scanner.TokenType.COMMA, ", expected" );
			tokenAssert( Scanner.TokenType.ID, "output port type expected" );
			OutputPortType opt = OutputPortType.getById( id );
			if ( opt.partnerLinkType() != null )
				throwException( "specified output port type is already present in another partner link type" );
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
		if ( token.isA( Scanner.TokenType.ID ) && token.content().equals( "outputPortTypes" ) ) {
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
					getToken();
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
					getToken();
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
