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


package jolie.net;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import jolie.Constants;
import jolie.runtime.Location;


/** A communication channel is an abstraction which permits to send and receive messages.
 * 
 * @author Fabrizio Montesi
 * @see CommProtocol
 * @see CommMessage
 */
abstract public class CommChannel
{
	public static CommChannel createCommChannel( Location location, CommProtocol protocol )
		throws IOException, URISyntaxException
	{
		CommChannel channel = null;
		URI uri = location.getURI();
		Constants.MediumId medium = Constants.stringToMediumId( uri.getScheme() );
		
		if ( medium == Constants.MediumId.SOCKET ) {
			Socket socket = new Socket( uri.getHost(), uri.getPort() );
			channel = new StreamingCommChannel(
						socket.getInputStream(),
						socket.getOutputStream(),
						protocol
						);
		} else if ( medium == Constants.MediumId.JAVA ) {
			try {
				Class<?> c =
					ClassLoader.getSystemClassLoader().loadClass( uri.getSchemeSpecificPart() );
				
				//if ( JavaService.class.isAssignableFrom( c ) == false )
				//	throw new IOException( "Specified file is not a valid JOLIE java service" );
				channel = new JavaCommChannel( c.newInstance() );
			} catch( ClassNotFoundException ce ) {
				throw new IOException( ce );
			} catch( IllegalAccessException iae ) {
				throw new IOException( iae );
			} catch( InstantiationException ie ) {
				throw new IOException( ie );
			} catch( ExceptionInInitializerError eiie ) {
				throw new IOException( eiie );
			} catch( SecurityException se ) {
				throw new IOException( se );
			}
			
		} else
			throw new IOException( "Unsupported communication medium: " + uri.getScheme() );
		
		return channel;
	}
	
	/** Receives a message from the channel. */
	abstract public CommMessage recv()
		throws IOException;
	
	/** Sends a message through the channel. */
	abstract public void send( CommMessage message )
		throws IOException;
	
	/** Closes the communication channel. */
	abstract public void close()
		throws IOException;
}