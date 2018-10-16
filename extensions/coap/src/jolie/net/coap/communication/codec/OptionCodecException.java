/**********************************************************************************
 *   Copyright (C) 2017-18 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2017-18 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
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
import jolie.net.Token;
import jolie.net.coap.message.options.Option;

/**
 * Instances of {@link OptionCodecException} are thrown if either encoding of
 * decoding of a {@link CoapMessage} failed because of an invalid option (for
 * decoding only if the option was critical as non-critical options are silently
 * ignored).
 *
 * @author Oliver Kleine
 */
public class OptionCodecException extends Exception
{

	private static final String message = "Unsupported or misplaced critical "
		+ "option %s";

	private int optionNumber;
	private int messageID;
	private Token token;
	private InetSocketAddress remoteSocket;
	private int messageType;

	/**
	 * @param optionNumber the option number of the {@link OptionValue} that
	 * caused this exception
	 */
	public OptionCodecException( int optionNumber )
	{
		super();
		this.optionNumber = optionNumber;
	}

	/**
	 * Returns the number of the option that caused this exception
	 *
	 * @return the number of the option that caused this exception
	 */
	public int getOptionNumber()
	{
		return optionNumber;
	}

	/**
	 * Method to set the message ID of the message that caused this exception
	 *
	 * @param messageID the message ID of the message that caused this exception
	 */
	public void setMessageID( int messageID )
	{
		this.messageID = messageID;
	}

	/**
	 * Returns the message ID of the message that caused this exception
	 *
	 * @return the message ID of the message that caused this exception
	 */
	public int getMessageID()
	{
		return messageID;
	}

	/**
	 * Method to set the {@link Token} of the message that caused this exception
	 *
	 * @param token the {@link Token} of the message that caused this exception
	 */
	public void setToken( Token token )
	{
		this.token = token;
	}

	/**
	 * Returns the {@link Token} of the message that caused this exception
	 *
	 * @return the {@link Token} of the message that caused this exception
	 */
	public Token getToken()
	{
		return token;
	}

	/**
	 * Method to set the remote CoAP endpoints of the message that caused this
	 * exception (the remote CoAP endpoints is either the message origin if this
	 * exception was thrown by the {@link CoapMessageDecoder} or he desired
	 * recipient if this exception was thrown by the {@link CoapMessageEncoder}.
	 *
	 * @param remoteSocket the remote CoAP endpoints of the message that caused
	 * this exception
	 */
	public void setremoteSocket( InetSocketAddress remoteSocket )
	{
		this.remoteSocket = remoteSocket;
	}

	/**
	 * Returns the remote CoAP endpoints of the message that caused this exception
	 * (the remote CoAP endpoints is either the message origin if this exception
	 * was thrown by the {@link CoapMessageDecoder} or he desired recipient if
	 * this exception was thrown by the {@link CoapMessageEncoder}.
	 *
	 * @return the remote CoAP endpoints of the message that caused this exception
	 */
	public InetSocketAddress getremoteSocket()
	{
		return remoteSocket;
	}

	/**
	 * Returns the number refering to the
	 * {@link jolie.net.coap.message.MessageType} of the message that caused
	 * this exception
	 *
	 * @return the number refering to the
	 * {@link jolie.net.coap.message.MessageType} of the message that caused
	 * this exception
	 */
	public int getMessageType()
	{
		return messageType;
	}

	/**
	 * Method to set the number refering to the
	 * {@link jolie.net.coap.message.MessageType} of the message that caused
	 * this exception
	 *
	 * @param messageType the number refering to the
	 * {@link jolie.net.coap.message.MessageType} of the message that caused
	 * this exception
	 */
	public void setMessageType( int messageType )
	{
		this.messageType = messageType;
	}

	@Override
	public String getMessage()
	{
		return String.format( message, Option.asString( this.optionNumber ) );
	}
}
