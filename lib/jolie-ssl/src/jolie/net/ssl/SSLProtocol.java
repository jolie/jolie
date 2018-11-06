/*******************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>              *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer <maan511@student.sdu.dk>  *
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package jolie.net.ssl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.ssl.SslHandler;
import java.net.URI;
import java.util.List;
import javax.net.ssl.SSLEngine;
import jolie.ExecutionThread;
import jolie.net.CommMessage;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

public class SSLProtocol extends AsyncCommProtocol {

	final AsyncCommProtocol wrappedProtocol;
	final boolean isInput;
	final String host;
	final int port;
	final String SSL_HANDLER_NAME = "sslHandler";
	final String SSL_HANDLER_PLACEHOLDER_NAME = "placeholderSslHandler";
	final static ChannelHandler sslHandlerPlaceholder = new DummyHandler();
		
	@ChannelHandler.Sharable
	private static class DummyHandler extends MessageToMessageCodec<Void,Void>
	{
		@Override
		protected void decode( ChannelHandlerContext chc, Void inbndn, List<Object> list ) throws Exception
		{
			throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		protected void encode( ChannelHandlerContext chc, Void otbndn, List<Object> list ) throws Exception
		{
			throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
		}
	}

	public SSLProtocol( VariablePath configurationPath, URI uri, CommProtocol wrappedProtocol, boolean isInput ) {
		super( configurationPath );
		this.wrappedProtocol = ( AsyncCommProtocol ) wrappedProtocol;
		this.host = uri.getHost();
		this.port = uri.getPort();
		this.isInput = isInput;
	}
	
	private class SSLStartHandler extends MessageToMessageCodec<CommMessage,CommMessage>{
		private final SSLProtocol parent;
		
		public SSLStartHandler( SSLProtocol parent ){
			this.parent = parent;
		}
		
		@Override
		protected void encode( ChannelHandlerContext chc, CommMessage msg, List<Object> out ) throws Exception
		{
			if ( parent.channel().parentPort() instanceof OutputPort ) {
				if( !chc.pipeline().names().contains( SSL_HANDLER_NAME ) ){
					chc.pipeline().replace(
						SSL_HANDLER_PLACEHOLDER_NAME,
						SSL_HANDLER_NAME, 
						new SslHandler( parent.engine( msg.id() ) )			
					);
				} else {
					chc.pipeline().replace(
						SSL_HANDLER_PLACEHOLDER_NAME,
						SSL_HANDLER_NAME, 
						new SslHandler( parent.engine( msg.id() ) )			
					);
				}
			}
			out.add( msg );
		}

		@Override
		protected void decode( ChannelHandlerContext chc, CommMessage msg, List<Object> out ) throws Exception
		{
			out.add( msg );
		}
	}

	@Override
	public void setInitExecutionThread( ExecutionThread t )
	{
		wrappedProtocol.setInitExecutionThread( t );
		super.setInitExecutionThread( t );
	}
	
	
	@Override
	public void setupPipeline( ChannelPipeline pipeline ) {
		if( channel().parentPort() instanceof InputPort ){
			pipeline.addLast( SSL_HANDLER_NAME, new SslHandler( engine( 0L ) ) );
			this.wrappedProtocol.setupPipeline( pipeline );
		} else {
			pipeline.addLast( SSL_HANDLER_PLACEHOLDER_NAME, sslHandlerPlaceholder );
			this.wrappedProtocol.setupPipeline( pipeline );
			pipeline.addLast( new SSLStartHandler( this ) );	
		}
	}

	@Override
	public boolean isThreadSafe() {
		return false;
//		return this.wrappedProtocol.isThreadSafe();
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
	
	private SSLEngine engine( Long k ) {
		if( channel().parentPort() instanceof InputPort ) {
			setReceiveExecutionThread( k );
		} else {
			setSendExecutionThread( k );
		}
		SSLContextFactory sslCtx = new SSLContextFactory( getSSLStringParameter( Parameters.PROTOCOL, DefaultParameters.PROTOCOL ) );
		sslCtx
			.setKeyStoreFormat(		getSSLStringParameter( Parameters.KEY_STORE_FORMAT,		DefaultParameters.KEY_STORE_FORMAT ) )
			.setKeyStoreFile(		getSSLStringParameter( Parameters.KEY_STORE_FILE,		DefaultParameters.KEY_STORE_FILE ) )
			.setKeyStorePassword(	getSSLStringParameter( Parameters.KEY_STORE_PASSWORD,	DefaultParameters.KEY_STORE_PASSWORD ) )
			.setTrustStoreFormat(	getSSLStringParameter( Parameters.TRUST_STORE_FORMAT,	DefaultParameters.TRUST_STORE_FORMAT ) )
			.setTrustStoreFile(		getSSLStringParameter( Parameters.TRUST_STORE_FILE,		DefaultParameters.TRUST_STORE_FILE ) )
			.setTrustStorePassword( getSSLStringParameter( Parameters.TRUST_STORE_PASSWORD,	DefaultParameters.TRUST_STORE_PASSWORD ) );

		SSLEngine engine = sslCtx.getContext().createSSLEngine();
		if ( channel().parentPort() instanceof InputPort ) {
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
