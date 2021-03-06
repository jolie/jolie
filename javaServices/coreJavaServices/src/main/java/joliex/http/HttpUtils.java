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

package joliex.http;

import jolie.Interpreter;
import jolie.js.JsUtils;
import jolie.runtime.*;
import jolie.runtime.embedding.RequestResponse;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.ClassicHttpRequests;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@AndJarDeps( { "httpclient5.jar", "httpcore5.jar", "httpcore5-h2.jar", "slf4j-api.jar", "slf4j-simple.jar" } )
public class HttpUtils extends JavaService {
	private final Map< String, AtomicBoolean > controls = new ConcurrentHashMap<>();

	public String connectStream( Value request ) {
		final HttpClient client = HttpClients.createDefault();
		ClassicHttpRequest httpReq = ClassicHttpRequests.get( request.getFirstChild( "uri" ).strValue() );

		if( request.hasChildren( "headers" ) ) {
			for( Map.Entry< String, ValueVector > entry : request.getFirstChild( "headers" ).children()
				.entrySet() ) {
				httpReq.setHeader( entry.getKey(), entry.getValue().first().strValue() );
			}
		}

		final String sid = UUID.randomUUID().toString();
		final AtomicBoolean control = new AtomicBoolean( true );
		controls.put( sid, control );
		final Embedder embedder = getEmbedder();
		final Interpreter interpreter = interpreter();
		interpreter.execute( () -> {
			try {
				client.execute( httpReq, response -> {
					HttpEntity entity = response.getEntity();
					if( entity != null ) {
						BufferedReader reader =
							new BufferedReader( new InputStreamReader( entity.getContent() ) );
						String line;
						while( control.get() && (line = reader.readLine()) != null ) {
							Value v = Value.create();
							v.setFirstChild( "sid", sid );
							JsUtils.parseJsonIntoValue( new StringReader( line ), v.getFirstChild( "data" ), false );
							embedder.callOneWay( "next", v );
						}
						entity.close();
						if( control.compareAndSet( true, false ) ) {
							Value v = Value.create();
							v.setFirstChild( "sid", sid );
							embedder.callOneWay( "end", v );
						}
					}
					return null;
				} );
			} catch( IOException e ) {
				if( control.compareAndSet( true, false ) ) {
					Value v = Value.create();
					v.setFirstChild( "sid", sid );
					v.setFirstChild( "error", e.getMessage() );
					try {
						embedder.callOneWay( "end", v );
					} catch( IOException e2 ) {
						interpreter.logWarning( e2 );
					}
				}
			}
		} );
		return sid;
	}

	@RequestResponse
	public void close( String sid )
		throws FaultException {
		AtomicBoolean control = controls.get( sid );
		if( control == null ) {
			throw new FaultException( "NotFound" );
		}

		if( control.compareAndSet( true, false ) ) {
			Value v = Value.create();
			v.setFirstChild( "sid", sid );
			try {
				getEmbedder().callOneWay( "end", v );
			} catch( IOException e ) {
				throw new FaultException( e );
			}
		}
	}
}
