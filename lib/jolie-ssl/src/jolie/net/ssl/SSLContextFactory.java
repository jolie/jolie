/*******************************************************************************
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
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

import io.netty.util.internal.SystemPropertyUtil;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public final class SSLContextFactory {

	private final String protocol;

	private String keyStoreFormat;
	private String keyStoreFile;
	private String keyStorePassword;
	private String trustStoreFormat;
	private String trustStoreFile;
	private String trustStorePassword;

	public SSLContextFactory( String protocol ) {
		this.protocol = protocol;
	}

	public SSLContext getContext() {
		SSLContext sslCtx;
		String algorithm = SystemPropertyUtil.get( "ssl.KeyManagerFactory.algorithm" );
		if ( algorithm == null ) {
			algorithm = "SunX509";
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

			sslCtx = SSLContext.getInstance( this.protocol );
			sslCtx.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
		} catch ( IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException e ) {
			throw new Error( "Failed to initialize the server-side SSLContext", e );
		}
		return sslCtx;
	}

	public String getKeyStoreFormat() {
		return keyStoreFormat;
	}

	public SSLContextFactory setKeyStoreFormat( String keyStoreFormat ) {
		this.keyStoreFormat = keyStoreFormat;
		return this;
	}

	public String getKeyStoreFile() {
		return keyStoreFile;
	}

	public SSLContextFactory setKeyStoreFile( String keyStoreFile ) {
		this.keyStoreFile = keyStoreFile;
		return this;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public SSLContextFactory setKeyStorePassword( String keyStorePassword ) {
		this.keyStorePassword = keyStorePassword;
		return this;
	}

	public String getTrustStoreFormat() {
		return trustStoreFormat;
	}

	public SSLContextFactory setTrustStoreFormat( String trustStoreFormat ) {
		this.trustStoreFormat = trustStoreFormat;
		return this;
	}

	public String getTrustStoreFile() {
		return trustStoreFile;
	}

	public SSLContextFactory setTrustStoreFile( String trustStoreFile ) {
		this.trustStoreFile = trustStoreFile;
		return this;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public SSLContextFactory setTrustStorePassword( String trustStorePassword ) {
		this.trustStorePassword = trustStorePassword;
		return this;
	}

}
