/*
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer <maan511@student.sdu.dk>
 *                                                                             
 *   This program is free software; you can redistribute it and/or modify      
 *   it under the terms of the GNU Library General Public License as           
 *   published by the Free Software Foundation; either version 2 of the        
 *   License, or (at your option) any later version.                           
 *                                                                             
 *   This program is distributed in the hope that it will be useful,           
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             
 *   GNU General Public License for more details.                              
 *                                                                             
 *   You should have received a copy of the GNU Library General Public         
 *   License along with this program; if not, write to the                     
 *   Free Software Foundation, Inc.,                                           
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 
 *                                                                             
 *   For details about the authors of this software, see the AUTHORS file.     
 */
package jolie.net.ssl;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import java.net.URI;
import javax.net.ssl.SSLEngine;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

/**
 *
 * @author spz
 */
public class SSLProtocol extends AsyncCommProtocol {

	final AsyncCommProtocol wrappedProtocol;
	final boolean isInput;
	final String host;
	final int port;

	public SSLProtocol( VariablePath configurationPath, URI uri, CommProtocol wrappedProtocol, boolean isInput ) {
		super( configurationPath );
		this.wrappedProtocol = ( AsyncCommProtocol ) wrappedProtocol;
		this.host = uri.getHost();
		this.port = uri.getPort();
		this.isInput = isInput;
	}

	@Override
	public void setupPipeline( ChannelPipeline pipeline ) {
		pipeline.addLast( "ssl", new SslHandler( engine() ) );
		this.wrappedProtocol.setupPipeline( pipeline );
	}

	@Override
	public boolean isThreadSafe() {
		return this.wrappedProtocol.isThreadSafe();
	}

	@Override
	public String name() {
		return this.wrappedProtocol.name() + "s";
	}

	/**
	 *
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
	String getSSLStringParameter( String parameterName, String defaultValue ) {
		if ( hasParameter( "ssl" ) ) {
			Value sslParams = getParameterFirstValue( "ssl" );
			if ( sslParams.hasChildren( parameterName ) ) {
				return sslParams.getFirstChild( parameterName ).strValue();
			}
		}
		return defaultValue;
	}

	private SSLEngine engine() {
		SSLContextFactory sslCtx = new SSLContextFactory( getSSLStringParameter( Parameters.PROTOCOL, DefaultParameters.PROTOCOL ) );
		sslCtx.setKeyStoreFormat( getSSLStringParameter( Parameters.KEY_STORE_FORMAT, DefaultParameters.KEY_STORE_FORMAT ) );
		sslCtx.setKeyStoreFile( getSSLStringParameter( Parameters.KEY_STORE_FILE, DefaultParameters.KEY_STORE_FILE ) );
		sslCtx.setKeyStorePassword( getSSLStringParameter( Parameters.KEY_STORE_PASSWORD, DefaultParameters.KEY_STORE_PASSWORD ) );
		sslCtx.setTrustStoreFormat( getSSLStringParameter( Parameters.TRUST_STORE_FORMAT, DefaultParameters.TRUST_STORE_FORMAT ) );
		sslCtx.setTrustStoreFile( getSSLStringParameter( Parameters.TRUST_STORE_FILE, DefaultParameters.TRUST_STORE_FILE ) );
		sslCtx.setTrustStorePassword( getSSLStringParameter( Parameters.TRUST_STORE_PASSWORD, DefaultParameters.TRUST_STORE_PASSWORD ) );

		SSLEngine engine = sslCtx.getContext().createSSLEngine();
		if ( isInput ) {
			engine.setUseClientMode( false );
		} else {
			engine.setUseClientMode( true );
		}

		return engine;
	}

	private static class Parameters {

		private static final String PROTOCOL = "protocol";
		private static final String KEY_STORE_FORMAT = "keyStoreFormat";
		private static final String KEY_STORE_FILE = "keyStore";
		private static final String KEY_STORE_PASSWORD = "keyStorePassword";
		private static final String TRUST_STORE_FORMAT = "trustStoreFormat";
		private static final String TRUST_STORE_FILE = "trustStore";
		private static final String TRUST_STORE_PASSWORD = "trustStorePassword";
	}

	private static class DefaultParameters {

		private static final String PROTOCOL = "TLSv1";
		private static final String KEY_STORE_FORMAT = "JKS";
		private static final String KEY_STORE_FILE = null;
		private static final String KEY_STORE_PASSWORD = null;
		private static final String TRUST_STORE_FORMAT = "JKS";
		private static final String TRUST_STORE_FILE = System.getProperty( "java.home" ) + "/lib/security/cacerts";
		private static final String TRUST_STORE_PASSWORD = null;
	}
}
