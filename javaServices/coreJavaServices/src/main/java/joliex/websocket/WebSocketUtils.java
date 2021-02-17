/*
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package joliex.websocket;

import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AndJarDeps( { "Java-WebSocket.jar", "slf4j-api.jar", "slf4j-simple.jar" } )
public class WebSocketUtils extends JavaService {
	private class JolieWebSocketClient extends WebSocketClient {
		private final String id;
		private final Embedder embedder;
		private final Value correlationData;

		public JolieWebSocketClient( String id, URI serverUri, Embedder embedder, Value correlationData ) {
			super( serverUri );
			this.id = id;
			this.embedder = embedder;
			this.correlationData = correlationData;
		}

		private Value buildNotificationValue() {
			Value v = Value.create();
			v.deepCopy( correlationData );
			v.setFirstChild( "id", id );
			return v;
		}

		@Override
		public void onOpen( ServerHandshake handshake ) {
			try {
				embedder.callOneWay( "onOpen", buildNotificationValue() );
			} catch( IOException e ) {
				interpreter().logWarning( e );
			}
		}

		@Override
		public void onClose( int code, String reason, boolean remote ) {
			try {
				Value v = buildNotificationValue();
				v.setFirstChild( "code", code );
				v.setFirstChild( "reason", reason );
				v.setFirstChild( "remote", remote );
				embedder.callOneWay( "onClose", v );
			} catch( IOException e ) {
				interpreter().logWarning( e );
			}
		}

		@Override
		public void onMessage( ByteBuffer message ) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void onMessage( String message ) {
			try {
				Value v = buildNotificationValue();
				v.setFirstChild( "message", message );
				embedder.callOneWay( "onMessage", v );
			} catch( IOException e ) {
				interpreter().logWarning( e );
			}
		}

		@Override
		public void onError( Exception ex ) {
			try {
				Value v = buildNotificationValue();
				v.setFirstChild( "error", ex.getMessage() );
				embedder.callOneWay( "onError", v );
			} catch( IOException e ) {
				interpreter().logWarning( e );
			}
		}
	}

	private final Map< String, JolieWebSocketClient > clients = new ConcurrentHashMap<>();

	@RequestResponse
	public void connect( Value request )
		throws FaultException {
		try {
			String id = request.getFirstChild( "id" ).strValue();
			final JolieWebSocketClient client =
				new JolieWebSocketClient( id, new URI( request.getFirstChild( "uri" ).strValue() ),
					getEmbedder(), request.getFirstChild( "data" ) );
			clients.put( id, client );
			client.connect();
		} catch( URISyntaxException e ) {
			throw new FaultException( "URISyntaxException", e );
		}
	}

	@RequestResponse
	public void send( Value request )
		throws FaultException {
		JolieWebSocketClient client = clients.get( request.getFirstChild( "id" ).strValue() );
		if( client != null ) {
			client.send( request.getFirstChild( "message" ).strValue() );
		} else {
			throw new FaultException( "NotFound" );
		}
	}

	public void close( Value request ) {
		JolieWebSocketClient client = clients.get( request.getFirstChild( "id" ).strValue() );
		if( client != null ) {
			client.close();
		}
	}
}
