/**********************************************************************************
 *   Copyright (C) 2016, Oliver Kleine, University of Luebeck                     *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>      *
 *                                                                                *
 *   This program is free software; you can redistribute it and/or modify         *
 *   it under the terms of the GNU Library General Public License as              *
 *   published by the Free Software Foundation; either version 2 of the           *
 *   License, or (at your option) any later version.                              *
 *                                                                                *
 *   This program is distributed in the hope that it will be useful,              *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 *   GNU General Public License for more details.                                 *
 *                                                                                *
 *   You should have received a copy of the GNU Library General Public            *
 *   License along with this program; if not, write to the                        *
 *   Free Software Foundation, Inc.,                                              *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                    *
 *                                                                                *
 *   For details about the authors of this software, see the AUTHORS file.        *
 **********************************************************************************/

package jolie.net.coap.communication.codec;

import java.net.InetSocketAddress;

/**
 * An {@link HeaderDecodingException} indicates that the header, i.e. the first
 * 4 bytes of an inbound serialized {@link CoapMessage} are malformed. This
 * exception is thrown during the decoding process and causes an RST message to
 * be sent to the inbound message origin.
 *
 * @author Oliver Kleine
 */
public class HeaderDecodingException extends Exception
{

	private static final long serialVersionUID = 1L;

	private final int messageID;
	private final InetSocketAddress remoteSocket;

	/**
	 * Creates a new instance of {@link HeaderDecodingException}.
	 *
	 * @param messageID the message ID of the message that caused
	 * @param remoteSocket the malformed message origin
	 * @param message
	 */
	public HeaderDecodingException( int messageID, InetSocketAddress remoteSocket,
		String message )
	{
		super( message );
		this.messageID = messageID;
		this.remoteSocket = remoteSocket;
	}

	/**
	 * Returns the message ID of the inbound malformed message
	 *
	 * @return the message ID of the inbound malformed message
	 */
	public int getMessageID()
	{
		return this.messageID;
	}

	/**
	 * Returns the malformed inbound messages origin CoAP endpoints
	 *
	 * @return the malformed inbound messages origin CoAP endpoints
	 */
	public InetSocketAddress getremoteSocket()
	{
		return this.remoteSocket;
	}
}
