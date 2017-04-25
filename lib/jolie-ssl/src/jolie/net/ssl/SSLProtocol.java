/***************************************************************************
 *   Copyright (C) 2010-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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

package jolie.net.ssl;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import jolie.StatefulContext;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

/**
 * Commodity class for supporting the implementation
 * of SSL-based protocols through wrapping.
 * @author Fabrizio Montesi
 * 2010: complete rewrite
 * 2015: major fixups
 */
public class SSLProtocol extends AsyncCommProtocol
{
	private final AsyncCommProtocol wrappedProtocol;
	private final boolean isClient;
	private SSLEngine sslEngine;

	public SSLProtocol( 
		VariablePath configurationPath,
		URI uri,
		CommProtocol wrappedProtocol,
		boolean isClient) 
		throws IOException
	{
		super( configurationPath );
		this.wrappedProtocol = (AsyncCommProtocol) wrappedProtocol;
		this.isClient = isClient;
	}

	@Override
	public void initialize( StatefulContext ctx )
	{
		try {
			init( ctx );
		} catch( IOException ex ) {
			Logger.getLogger( SSLProtocol.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}
	
	@Override
	public void setupPipeline( ChannelPipeline pipeline )
	{		
		pipeline.addLast( new SslHandler( this.sslEngine ) );
		wrappedProtocol.setupPipeline( pipeline );
		wrappedProtocol.setChannel( this.channel() );
	}

	@Override
	public String name()
	{
		return wrappedProtocol.name() + 's';
	}

	@Override
	public boolean isThreadSafe()
	{
		return false;
	}
		
	private String getSSLStringParameter( StatefulContext ctx, String parameterName, String defaultValue )
	{
		if ( hasParameter( ctx, "ssl" ) ) {
			Value sslParams = getParameterFirstValue( ctx, "ssl" );
			if ( sslParams.hasChildren( parameterName ) ) {
				return sslParams.getFirstChild( parameterName ).strValue();
			}
		}
		return defaultValue;
	}

	private int getSSLIntegerParameter( StatefulContext ctx, String parameterName, int defaultValue )
	{
		if ( hasParameter( ctx, "ssl" ) ) {
			Value sslParams = getParameterFirstValue( ctx, "ssl" );
			if ( sslParams.hasChildren( parameterName ) ) {
				return sslParams.getFirstChild( parameterName ).intValue();
			}
		}
		return defaultValue;
	}
	
	public void init( StatefulContext ctx )
		throws IOException
	{
		// Set default parameters
		String protocol = getSSLStringParameter( ctx, "protocol", "TLSv1" ),
			keyStoreFormat = getSSLStringParameter( ctx, "keyStoreFormat", "JKS" ),
			trustStoreFormat = getSSLStringParameter( ctx, "trustStoreFormat", "JKS" ),
			keyStoreFile = getSSLStringParameter( ctx, "keyStore", null ),
			keyStorePassword = getSSLStringParameter( ctx, "keyStorePassword", null ),
			trustStoreFile = getSSLStringParameter( ctx, "trustStore", System.getProperty( "java.home" ) + "/lib/security/cacerts" ),
			trustStorePassword = getSSLStringParameter( ctx, "trustStorePassword", null );
		if ( keyStoreFile == null && isClient == false ) {
			throw new IOException( "Compulsory parameter needed for server mode: ssl.keyStore" );
		}
		try {
			SSLContext context = SSLContext.getInstance( protocol );
			KeyStore ks = KeyStore.getInstance( keyStoreFormat );
			KeyStore ts = KeyStore.getInstance( trustStoreFormat );

			char[] passphrase;
			if ( keyStorePassword != null ) {
				passphrase = keyStorePassword.toCharArray();
			} else {
				passphrase = null;
			}

			if ( keyStoreFile != null ) {
				ks.load( new FileInputStream( keyStoreFile ), passphrase );
			} else {
				ks.load( null, null );
			}

			KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
			kmf.init( ks, passphrase );

			if ( trustStorePassword != null ) {
				passphrase = trustStorePassword.toCharArray();
			} else {
				passphrase = null;
			}
			ts.load( new FileInputStream( trustStoreFile ), passphrase );

			TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
			tmf.init( ts );

			context.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );

			sslEngine = context.createSSLEngine();
			sslEngine.setEnabledProtocols( new String[] { protocol } );
			sslEngine.setUseClientMode( isClient );
			if ( isClient == false ) {
				if ( getSSLIntegerParameter( ctx, "wantClientAuth", 1 ) > 0 ) {
					sslEngine.setWantClientAuth( true );
				} else {
					sslEngine.setWantClientAuth( false );
				}
			}
		} catch ( NoSuchAlgorithmException e ) {
			throw new IOException( e );
		} catch ( KeyManagementException e ) {
			throw new IOException( e );
		} catch ( KeyStoreException e ) {
			throw new IOException( e );
		} catch ( UnrecoverableKeyException e ) {
			throw new IOException( e );
		} catch ( CertificateException e ) {
			throw new IOException( e );
		}
	}
}