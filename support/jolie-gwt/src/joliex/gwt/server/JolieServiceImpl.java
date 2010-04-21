/***************************************************************************
 *   Copyright (C) 2008-2010 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package joliex.gwt.server;

import com.google.gwt.core.client.GWT;
import joliex.gwt.client.JolieService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import jolie.net.CommMessage;
import jolie.net.SodepProtocol;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.ClosedVariablePath;
import jolie.util.Pair;
import joliex.gwt.client.FaultException;
import joliex.gwt.client.Value;

public class JolieServiceImpl extends RemoteServiceServlet implements JolieService
{
	private final String location;
	private final CommProtocol protocol;

	public JolieServiceImpl()
	{
		super();
		location = getServletConfig().getInitParameter( "location" );
		protocol = new SodepProtocol( new ClosedVariablePath( new Pair[0], jolie.runtime.Value.create() ) );
	}

	public Value call( String operationName, Value value )
		throws FaultException
	{
		Value ret = new Value();
		if ( location.isEmpty() ) {
			return ret;
		}
		jolie.runtime.Value jolieValue = jolie.runtime.Value.create();
		JolieGWTConverter.gwtToJolieValue( value, jolieValue );
		Socket socket = null;
		try {
			URI uri = new URI( location );
			socket = new Socket( uri.getHost(), uri.getPort() );
			OutputStream ostream = socket.getOutputStream();
			InputStream istream = socket.getInputStream();
			CommMessage message = CommMessage.createRequest( operationName, "/", jolieValue );
			protocol.send( ostream, message, istream );
			message = protocol.recv( istream, ostream );
			if ( message.isFault() ) {
				throw JolieGWTConverter.jolieToGwtFault( message.fault() );
			} else {
				JolieGWTConverter.jolieToGwtValue( message.value(), ret );
			}
		} catch( URISyntaxException e ) {
			GWT.log( "The provided servlet location parameter is not valid.", e );
		} catch( IOException e ) {
			GWT.log( "An IOException occurred when trying to communicate with the remote Jolie service.", e );
		} finally {
			if ( socket != null ) {
				try {
					socket.close();
				} catch( IOException e ) {
					GWT.log( "An IOException occurred when trying to close the communication socket with the remote Jolie service.", e );
				}
			}
		}
		return ret;
	}
}
