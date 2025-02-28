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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.util.Lazy;
import jolie.util.metadata.Metadata;

/**
 * A <code>CommMessage</code> represents a generic communication message. A message is composed by
 * the following parts:
 * <ul>
 * <li>a numeric identifier, which can be used to relate a response to its request;</li>
 * <li>an operation name, to identify the operation that the message is meant for (in case of a
 * request) or the operation that generated the message (in case of a response);</li>
 * <li>a resource path, used for redirection;</li>
 * <li>a value, holding the message data;</li>
 * <li>potentially, a fault.</li>
 * </ul>
 *
 * Message instances destined to be used in a Request-Response pattern should always be created
 * using the static methods
 * {@link #createRequest(java.lang.String, java.lang.String, jolie.runtime.Value) createRequest} and
 * {@link #createResponse(jolie.net.CommMessage, jolie.runtime.Value) createResponse}.
 *
 * @author Fabrizio Montesi
 */
public class CommMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final AtomicLong REQUEST_ID_COUNTER = new AtomicLong( 1L );
	private static final AtomicLong ID_COUNTER = new AtomicLong( 1L );

	public static final long GENERIC_REQUEST_ID = 0L;
	public static final CommMessage UNDEFINED_MESSAGE =
		new CommMessage( GENERIC_REQUEST_ID, "", Constants.ROOT_RESOURCE_PATH, Value.UNDEFINED_VALUE, null, null );

	private final long requestId;
	private final String operationName;
	private final String resourcePath;
	private final Value value;
	private final FaultException fault;
	private final long id;
	private final Lazy< Metadata > metadata = new Lazy<>( Metadata::new );
	private final CommMessage originalRequest;

	/**
	 * Returns the resource path of this message.
	 *
	 * @return the resource path of this message
	 */
	public String resourcePath() {
		return resourcePath;
	}

	/**
	 * Returns <code>true</code> if this message has a generic identifier, <code>false</code> otherwise.
	 *
	 * A message with a generic identifier cannot be related to other messages.
	 *
	 * A message can have a generic identifier if it is meant to be used in a Notification. Also,
	 * communication channels not supporting message identifiers could be generating messages equipped
	 * with a generic identifier every time.
	 *
	 * @return <code>true</code> if this message has a generic identifier, <code>false</code> otherwise
	 */
	public boolean hasGenericRequestId() {
		return requestId == GENERIC_REQUEST_ID;
	}

	/**
	 * Returns the request identifier of this message.
	 *
	 * @return the request identifier of this message
	 */
	public long requestId() {
		return requestId;
	}

	public static long getNewRequestId() {
		int cellId = 0;
		final Interpreter interpreter = Interpreter.getInstance();
		if( interpreter != null ) {
			cellId = interpreter.configuration().cellId();
		}
		return (((long) cellId) << 32) | (REQUEST_ID_COUNTER.getAndIncrement() & 0xffffffffL);
	}

	/**
	 * Creates a request message.
	 *
	 * @param operationName the name of the operation this request is meant for
	 * @param resourcePath the resource path of this message
	 * @param value the message data
	 * @return a request message as per specified by the parameters
	 */
	public static CommMessage createRequest( String operationName, String resourcePath, Value value ) {
		return new CommMessage( getNewRequestId(), operationName, resourcePath, Value.createDeepCopy( value ), null,
			null );
	}

	/**
	 * Creates an empty (i.e. without data) response for the passed request.
	 *
	 * @param request the request message that caused this response
	 * @return an empty response for the passed request
	 */
	public static CommMessage createEmptyResponse( CommMessage request ) {
		return createResponse( request, Value.create() );
	}

	/**
	 * Creates a response for the passed request.
	 *
	 * @param request the request message that caused this response
	 * @param value the data to equip the response with
	 * @return a response for the passed request
	 */
	public static CommMessage createResponse( CommMessage request, Value value ) {
		// TODO support resourcePath
		return new CommMessage( request.requestId, request.operationName, "/", Value.createDeepCopy( value ), null,
			request );
	}

	/**
	 * Creates a response message equipped with the passed fault.
	 *
	 * @param request the request message that caused this response
	 * @param fault the fault to equip the response with
	 * @return a response message equipped with the specified fault
	 */
	public static CommMessage createFaultResponse( CommMessage request, FaultException fault ) {
		// TODO support resourcePath
		return new CommMessage( request.requestId, request.operationName, "/", Value.create(), fault, request );
	}

	/**
	 * Constructor
	 *
	 * @param requestId the identifier for the request
	 * @param operationName the operation name for this message
	 * @param resourcePath the resource path for this message
	 * @param value the message data to equip the message with
	 * @param fault the fault to equip the message with
	 * @param originalRequest the original request that this message is a response for
	 */
	public CommMessage( long requestId, String operationName, String resourcePath, Value value, FaultException fault,
		CommMessage originalRequest ) {
		this.requestId = requestId;
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		this.value = value;
		this.fault = fault;
		this.id = ID_COUNTER.getAndIncrement();
		this.originalRequest = originalRequest;
	}

	/**
	 * Constructor
	 *
	 * @param requestId the identifier for the request
	 * @param operationName the operation name for this message
	 * @param resourcePath the resource path for this message
	 * @param value the message data to equip the message with
	 * @param fault the fault to equip the message with
	 */
	public CommMessage( long requestId, String operationName, String resourcePath, Value value, FaultException fault ) {
		this( requestId, operationName, resourcePath, value, fault, null );
	}

	/**
	 * Returns the id associated with the message
	 *
	 * @return the id associated with the message
	 */
	public long id() {
		return id;
	}

	/**
	 * Returns the value representing the data contained in this message.
	 *
	 * @return the value representing the data contained in this message
	 */
	public Value value() {
		return value;
	}

	/**
	 * The operation name of this message.
	 *
	 * @return the operation name of this message
	 */
	public String operationName() {
		return operationName;
	}

	/**
	 * Returns <code>true</code> if this message contains a fault, <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if this message contains a fault, <code>false</code> otherwise
	 */
	public boolean isFault() {
		return (fault != null);
	}

	/**
	 * Returns the fault contained in this message.
	 *
	 * If this message does not contain a fault, <code>null</code> is returned.
	 *
	 * @return the fault contained in this message
	 */
	public FaultException fault() {
		return fault;
	}

	/** Returns the metadata for this message. */
	public Metadata metadata() {
		return metadata.get();
	}

	public Optional< CommMessage > originalRequest() {
		return Optional.ofNullable( originalRequest );
	}
}
