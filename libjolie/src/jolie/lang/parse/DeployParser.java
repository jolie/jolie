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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Vector;

import jolie.Constants;
import jolie.lang.parse.ast.deploy.CorrelationSetInfo;
import jolie.lang.parse.ast.deploy.DeployInfo;
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

/** Parser for the deploy information file (.dol)
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

	public DeployInfo parse()
		throws IOException, ParserException
	{
		DeployInfo deployInfo = new DeployInfo();
		getToken();
		parseState( deployInfo );
		parseExecution( deployInfo );
		parseCorrSet( deployInfo );
		parseLocations( deployInfo );
		parseWSDL( deployInfo );
		return deployInfo;
	}
	
	private void parseState( DeployInfo deployInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.STATE ) ) {
			Constants.StateMode mode = Constants.StateMode.PERSISTENT;
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			if ( token.is( Scanner.TokenType.PERSISTENT ) ) {
				mode = Constants.StateMode.PERSISTENT;
			} else if ( token.is( Scanner.TokenType.NOT_PERSISTENT ) ) {
				mode = Constants.StateMode.NOT_PERSISTENT;
			} else
				throwException( "Expected state mode, found " + token.content() );

			deployInfo.addChild( new StateInfo( mode ) );
			getToken();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseExecution( DeployInfo deployInfo )
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

			deployInfo.addChild( new ExecutionInfo( mode ) );
			getToken();
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseCorrSet( DeployInfo deployInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.CSET ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			deployInfo.addChild( new CorrelationSetInfo( parseIdListN( false ) ) );
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseLocations( DeployInfo deployInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.LOCATIONS ) ) {
			boolean keepRun = true;
			
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			String id;
			while ( !token.is( Scanner.TokenType.RCURLY ) && keepRun ) {
				tokenAssert( Scanner.TokenType.ID, "location id expected" );
				id = token.content();
				getToken();
				eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.STRING, "location value expected" );
				
				try {
					deployInfo.addChild( new LocationDeployInfo( id, new URI( token.content() ) ) );
				} catch( URISyntaxException e ) {
					throwException( "Invalid URI specified for location " + id );
				}
				
				getToken();
				if ( token.is( Scanner.TokenType.COMMA ) )
					getToken();
				else
					keepRun = false;					
			}
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseWSDL( DeployInfo deployInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.ID ) && "interface".equals( token.content() ) ) {
			WSDLInfo wsdlInfo = new WSDLInfo();
			
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			parseWSDLOperations( wsdlInfo );
			parseInputPortTypes( wsdlInfo );
			parseOutputPortTypes( wsdlInfo );
			parsePartnerLinkTypes( wsdlInfo );
			parseBindings( wsdlInfo );
			parseService( wsdlInfo );
			eat( Scanner.TokenType.RCURLY, "} expected" );
			
			deployInfo.addChild( wsdlInfo );
		}
	}
	
	private void parseService( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.ID ) && "service".equals( token.content() ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.is( Scanner.TokenType.STRING ) )
				parseServiceElement( wsdlInfo );
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseServiceElement( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
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
		wsdlInfo.addChild( new ServiceInfo( serviceUri, ports ) );
	}
	
	private void parseBindings( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.ID ) && "bindings".equals( token.content() ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.is( Scanner.TokenType.ID ) )
				parseBinding( wsdlInfo );
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseBinding( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		Constants.ProtocolId protocolId;
		String portId = token.content();

		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );
		tokenAssert( Scanner.TokenType.ID, "port type id expected" );
		String portType = token.content();
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );
		tokenAssert( Scanner.TokenType.ID, "port type protocol expected" );
		protocolId = Constants.stringToProtocolId( token.content() );
		if ( protocolId == Constants.ProtocolId.UNSUPPORTED )
			throwException( "unknown protocol specified in port binding" );
		getToken();
		
		wsdlInfo.addChild( new PortBindingInfo( portId, portType, protocolId ) );
	}
	
	private void parsePartnerLinkTypes( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.ID ) && "partnerLinkTypes".equals( token.content() ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.is( Scanner.TokenType.ID ) )
				parsePartnerLinkType( wsdlInfo );
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parsePartnerLinkType( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		String id = token.content();
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );
		tokenAssert( Scanner.TokenType.ID, "input port type expected" );
		String ipt = token.content();
		getToken();
		eat( Scanner.TokenType.COMMA, ", expected" );
		tokenAssert( Scanner.TokenType.ID, "output port type expected" );
		String opt = token.content();
		getToken();
		wsdlInfo.addChild( new PartnerLinkTypeInfo( id, ipt, opt ) );
	}
	
	private void parseInputPortTypes( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.ID ) && "inputPortTypes".equals( token.content() ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.is( Scanner.TokenType.ID ) )
				parseInputPortType( wsdlInfo );
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseOutputPortTypes( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.ID ) && "outputPortTypes".equals( token.content() ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while( token.is( Scanner.TokenType.ID ) )
				parseOutputPortType( wsdlInfo );
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseInputPortType( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		String id = token.content();
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );
		boolean keepRun = true;
		Vector< String > operations = new Vector< String >(); 
		while( keepRun ) {
			if ( token.is( Scanner.TokenType.ID ) ) {
				operations.add( token.content() );
				getToken();
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
		wsdlInfo.addChild( new InputPortTypeInfo( id, operations ) );
	}
	
	private void parseOutputPortType( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		String id = token.content();
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );
		boolean keepRun = true;
		Vector< String > operations = new Vector< String >(); 
		while( keepRun ) {
			if ( token.is(Scanner.TokenType.ID) ) {
				operations.add( token.content() );
				getToken();
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
		wsdlInfo.addChild( new OutputPortTypeInfo( id, operations ) );
	}
	
	private void parseWSDLOperations( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		if ( token.is( Scanner.TokenType.OPERATIONS ) ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			boolean keepRun = true;
			while ( keepRun ) {
				if ( token.type() == Scanner.TokenType.OP_OW )
					parseOneWayOperations( wsdlInfo );
				else if ( token.type() == Scanner.TokenType.OP_RR )
					parseRequestResponseOperations( wsdlInfo );
				else if ( token.type() == Scanner.TokenType.OP_N )
					parseNotificationOperations( wsdlInfo );
				else if ( token.type() == Scanner.TokenType.OP_SR )
					parseSolicitResponseOperations( wsdlInfo );
				else
					keepRun = false;
			}
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
	}
	
	private void parseOneWayOperations( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		String op, boundName;

		while ( keepRun ) {
			if ( token.is( Scanner.TokenType.ID ) ) {
				op = token.content();
				getToken();
				eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.ID, "bound operation name expected" );
				boundName = token.content();
				getToken();
				wsdlInfo.addChild(
						new OneWayOperationDeployInfo( op, boundName, parseIdListN() )
						);
				
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
	}
	
	private void parseNotificationOperations( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		
		String op, boundName;

		while ( keepRun ) {
			if ( token.is( Scanner.TokenType.ID ) ) {
				op = token.content();
				getToken();
				eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.ID, "bound operation name expected" );
				boundName = token.content();
				getToken();
				wsdlInfo.addChild(
						new NotificationOperationDeployInfo( op, boundName, parseIdListN() )
						);
				
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
	}
	
	private void parseRequestResponseOperations( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		Collection< String > varNames;
		
		String op, boundName;

		while ( keepRun ) {
			if ( token.is( Scanner.TokenType.ID ) ) {
				op = token.content();
				getToken();
				eat( Scanner.TokenType.ASSIGN, "expected =" );
				tokenAssert( Scanner.TokenType.ID, "bound operation name expected" );
				boundName = token.content();
				getToken();
				varNames = parseIdListN();
				wsdlInfo.addChild(
					new RequestResponseOperationDeployInfo( op, boundName, varNames, parseIdListN() )
					);
				
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
	}
	
	private void parseSolicitResponseOperations( WSDLInfo wsdlInfo )
		throws IOException, ParserException
	{
		getToken();
		eat( Scanner.TokenType.COLON, ": expected" );

		boolean keepRun = true;
		Collection< String > varNames;
		
		String op, boundName;

		while ( keepRun ) {
			if ( token.is( Scanner.TokenType.ID ) ) {
				op = token.content();
				getToken();
				eat( Scanner.TokenType.ASSIGN, "= expected" );
				tokenAssert( Scanner.TokenType.ID, "bound operation name expected" );
				boundName = token.content();
				getToken();
				varNames = parseIdListN();
				
				wsdlInfo.addChild(
					new SolicitResponseOperationDeployInfo( op, boundName, varNames, parseIdListN() )
					);
				
				if ( token.type() == Scanner.TokenType.COMMA )
					getToken();
				else
					keepRun = false;
			} else
				keepRun = false;
		}
	}
}
