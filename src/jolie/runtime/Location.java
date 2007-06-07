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


package jolie.runtime;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import jolie.net.CommChannel;
import jolie.net.CommProtocol;
import jolie.net.UnsupportedCommMediumException;

abstract public class Location
{
	abstract protected String value();
	abstract public void setValue( String value );
	
	/**
	 * @todo Implement the communication medium choice (socket, pipe, file) through uri.getProtocol().
	 * @param protocol
	 * @return
	 * @throws IOException
	 */
	public CommChannel createCommChannel( CommProtocol protocol )
		throws IOException, URISyntaxException, UnsupportedCommMediumException
	{
		URI uri = getURI();
		//String urlProtocol = url.getProtocol();
		if ( !uri.getScheme().equals( "socket" ) )
			throw new UnsupportedCommMediumException( uri.getScheme() );
		
		Socket socket = new Socket( uri.getHost(), uri.getPort() );
		return new CommChannel( socket.getInputStream(), socket.getOutputStream(), protocol );
	}
	
	public URI getURI()
		throws URISyntaxException
	{
		return new URI( value() );
	}
}