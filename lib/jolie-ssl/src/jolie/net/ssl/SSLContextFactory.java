/*
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
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

/**
 * Creates a SSL context both for client and server accordingly.
 *
 * @author spz
 */
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
		char[] keyPassphrase = this.keyStorePassword.toCharArray();
		char[] trustPassphrase = this.trustStorePassword.toCharArray();

		try {
			KeyStore ks = KeyStore.getInstance( this.keyStoreFormat );
			ks.load( new FileInputStream( this.keyStoreFile ), keyPassphrase );

			KeyStore ts = KeyStore.getInstance( this.trustStoreFormat );
			ts.load( new FileInputStream( this.trustStoreFile ), trustPassphrase );

			KeyManagerFactory kmf = KeyManagerFactory.getInstance( algorithm );
			kmf.init( ks, keyPassphrase );

			TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
			tmf.init( ts );

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

	public void setKeyStoreFormat( String keyStoreFormat ) {
		this.keyStoreFormat = keyStoreFormat;
	}

	public String getKeyStoreFile() {
		return keyStoreFile;
	}

	public void setKeyStoreFile( String keyStoreFile ) {
		this.keyStoreFile = keyStoreFile;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword( String keyStorePassword ) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getTrustStoreFormat() {
		return trustStoreFormat;
	}

	public void setTrustStoreFormat( String trustStoreFormat ) {
		this.trustStoreFormat = trustStoreFormat;
	}

	public String getTrustStoreFile() {
		return trustStoreFile;
	}

	public void setTrustStoreFile( String trustStoreFile ) {
		this.trustStoreFile = trustStoreFile;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword( String trustStorePassword ) {
		this.trustStorePassword = trustStorePassword;
	}

}
