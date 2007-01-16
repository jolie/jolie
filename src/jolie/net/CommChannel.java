/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/

package jolie.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;


/** A communication channel is an abstraction which permits to send and receive messages.
 * 
 * @author Fabrizio Montesi
 * @see CommProtocol
 * @see CommMessage
 */
public class CommChannel
{
	private InputStream istream;
	private OutputStream ostream;
	private CommProtocol protocol;
	
	/** Constructor.
	 * 
	 * @param istream the channel input stream.
	 * @param ostream the channel output stream.
	 * @param protocol the protocol to use to send and receive messages.
	 */
	public CommChannel( InputStream istream, OutputStream ostream, CommProtocol protocol )
	{
		this.istream = istream;
		this.ostream = ostream;
		this.protocol = protocol;
	}
	
	/*public InputStream inputStream()
	{
		return istream;
	}
	
	public OutputStream outputStream()
	{
		return ostream;
	}*/
	
	/** Receives a message from the channel. */
	public CommMessage recv()
		throws IOException
	{
		return protocol.recv( istream );
	}
	
	/** Sends a message through the channel. */
	public void send( CommMessage message )
		throws IOException
	{
		protocol.send( ostream, message );
	}
	
	/** Closes the communication channel. */
	public void close()
		throws IOException
	{
		istream.close();
		ostream.close();
	}
}