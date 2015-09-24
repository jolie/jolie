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


import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import jolie.lang.Constants;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

/**
 * A <code>CommMessage</code> represents a generic communication message.
 * A message is composed by the following parts:
 * <ul>
 * <li>a numeric identifier, which can be used to relate a response to its request;</li>
 * <li>an operation name,
 * to identify the operation that the message is meant for
 * (in case of a request) or the operation that generated the message
 * (in case of a response);</li>
 * <li>a resource path, used for redirection;</li>
 * <li>a value, holding the message data;</li>
 * <li>potentially, a fault.</li>
 * </ul>
 *
 * Message instances destined to be used in a Request-Response pattern
 * should always be created using the static methods
 * {@link #createRequest(java.lang.String, java.lang.String, jolie.runtime.Value) createRequest}
 * and {@link #createResponse(jolie.net.CommMessage, jolie.runtime.Value) createResponse}.
 *
 * @author Fabrizio Montesi
 */
public class CommMessage implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final AtomicLong idCounter = new AtomicLong( 1L );
	public static final long GENERIC_ID = 0L;
	public static final CommMessage UNDEFINED_MESSAGE = new CommMessage( GENERIC_ID, "", Constants.ROOT_RESOURCE_PATH, Value.UNDEFINED_VALUE, null );
	
	private final long id;
	private final String operationName;
	private final String resourcePath;
	private final Value value;
	private final FaultException fault;

	/**
	 * Returns the resource path of this message.
	 * @return the resource path of this message
	 */
	public String resourcePath()
	{
		return resourcePath;
	}

	/**
	 * Returns <code>true</code> if this message has a generic identifier, <code>false</code> otherwise.
	 *
	 * A message with a generic identifier cannot be related to other messages.
	 *
	 * A message can have a generic identifier if it is meant to be used in a Notification.
	 * Also, communication channels not supporting message identifiers could be generating
	 * messages equipped with a generic identifier every time.
	 * @return <code>true</code> if this message has a generic identifier, <code>false</code> otherwise
	 */
	public boolean hasGenericId()
	{
		return id == GENERIC_ID;
	}

	/**
	 * Returns the identifier of this message.
	 * @return the identifier of this message
	 */
	public long id()
	{
		return id;
	}

	public static long getNewMessageId()
	{
		return idCounter.getAndIncrement();
	}

	/**
	 * Creates a request message.
	 * @param operationName the name of the operation this request is meant for
	 * @param resourcePath the resource path of this message
	 * @param value the message data
	 * @return a request message as per specified by the parameters
	 */
	public static CommMessage createRequest( String operationName, String resourcePath, Value value )
	{
		return new CommMessage( getNewMessageId(), operationName, resourcePath, Value.createDeepCopy( value ), null );
	}

	/**
	 * Creates an empty (i.e. without data) response for the passed request.
	 * @param request the request message that caused this response
	 * @return an empty response for the passed request
	 */
	public static CommMessage createEmptyResponse( CommMessage request )
	{
		return createResponse( request, Value.create() );
	}

	/**
	 * Creates a response for the passed request.
	 * @param request the request message that caused this response
	 * @param value the data to equip the response with
	 * @return a response for the passed request
	 */
	public static CommMessage createResponse( CommMessage request, Value value )
	{
		//TODO support resourcePath
		return new CommMessage( request.id, request.operationName, "/", Value.createDeepCopy( value ), null );
	}

	/**
	 * Creates a response message equipped with the passed fault.
	 * @param request the request message that caused this response
	 * @param fault the fault to equip the response with
	 * @return a response message equipped with the specified fault
	 */
	public static CommMessage createFaultResponse( CommMessage request, FaultException fault )
	{
		//TODO support resourcePath
		return new CommMessage( request.id, request.operationName, "/", Value.create(), fault );
	}

	/**
	 * Constructor
	 * @param id the identifier for this message
	 * @param operationName the operation name for this message
	 * @param resourcePath the resource path for this message
	 * @param value the message data to equip the message with
	 * @param fault the fault to equip the message with
	 */
	public CommMessage( long id, String operationName, String resourcePath, Value value, FaultException fault )
	{
		this.id = id;
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		this.value = value;
		this.fault = fault;
	}

	/**
	 * Constructor. The identifier of this message will be generic.
	 * @param operationName the operation name for this message
	 * @param resourcePath the resource path for this message
	 * @param value the message data to equip the message with
	 * @param fault the fault to equip the message with
	 */
	/*private CommMessage( String operationName, String resourcePath, Value value, FaultException f )
	{
		this( GENERIC_ID, operationName, resourcePath, value, f );
	}*/

	/**
	 * Constructor. The identifier of this message will be generic.
	 * @param operationName the operation name of this message
	 * @param resourcePath the resource path of this message
	 */
	/*private CommMessage( String operationName, String resourcePath )
	{
		this( GENERIC_ID, operationName, resourcePath, Value.create(), null );
	}

	private CommMessage( long id, String operationName, String resourcePath, Value value )
	{
		this( id, operationName, resourcePath, value, null );
	}

	private CommMessage( long id, String operationName, String resourcePath, FaultException fault )
	{
		this( id, operationName, resourcePath, Value.create(), fault );
	}*/

	/**
	 * Constructor. The identifier of this message will be generic.
	 * @param operationName the operation name for this message
	 * @param resourcePath the resource path for this message
	 * @param value the message data to equip the message with
	 */
	/*private CommMessage( String operationName, String resourcePath, Value value )
	{
		this( GENERIC_ID, operationName, resourcePath, value, null );
	}*/

	/**
	 * Returns the value representing the data contained in this message.
	 * @return the value representing the data contained in this message
	 */
	public Value value()
	{
		return value;
	}

	/**
	 * The operation name of this message.
	 * @return the operation name of this message
	 */
	public String operationName()
	{
		return operationName;
	}

	/**
	 * Returns <code>true</code> if this message contains a fault, <code>false</code> otherwise.
	 * @return <code>true</code> if this message contains a fault, <code>false</code> otherwise
	 */
	public boolean isFault()
	{
		return ( fault != null );
	}

	/**
	 * Returns the fault contained in this message.
	 *
	 * If this message does not contain a fault, <code>null</code> is returned.
	 * @return the fault contained in this message
	 */
	public FaultException fault()
	{
		return fault;
	}
}