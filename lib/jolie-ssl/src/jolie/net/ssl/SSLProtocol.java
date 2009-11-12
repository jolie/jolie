/***************************************************************************
 *   Copyright (C) 2008 by Roberto La Maestra                              *
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import jolie.net.CommMessage;
import jolie.net.protocols.CommProtocol;
import jolie.net.protocols.SequentialCommProtocol;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

/**
 * Commodity class for supporting the implementation
 * of SSL-based protocols through wrapping.
 * @author Fabrizio Montesi
 */
public class SSLProtocol extends SequentialCommProtocol
{
	private static final int INITIAL_BUFFER_SIZE = 65536;
	protected Status lastSSLStatus = null;
	private boolean isClient = true;
	private boolean firstTime;
	private final CommProtocol wrappedProtocol;
	private int bufferSize = INITIAL_BUFFER_SIZE;
	private SSLResult result;
	private SSLEngine ssl;

	private class SSLResult
	{
		boolean moreToUnwrap;
		ByteBuffer buffer;
		SSLEngineResult log = null;

		public void emptyBuffer()
		{
			buffer = ByteBuffer.allocate( bufferSize );
		}

		public void enlargeBuffer()
		{
			if ( buffer.capacity() == bufferSize ) {
				bufferSize += 1024;
			}
			buffer = ByteBuffer.allocate( bufferSize );
		}

		public SSLResult( SSLEngineResult log, boolean moreToUnwrap )
		{
			this.log = log;
			emptyBuffer();
			this.moreToUnwrap = moreToUnwrap;
			lastSSLStatus = null;
		}

		public SSLResult( SSLEngineResult log )
		{
			this.log = log;
			emptyBuffer();
			this.moreToUnwrap = false;
			lastSSLStatus = null;
		}

		public SSLResult( boolean moreToUnwrap )
		{
			emptyBuffer();
			this.moreToUnwrap = moreToUnwrap;
			lastSSLStatus = null;
		}

		public SSLResult()
		{
			emptyBuffer();
			this.moreToUnwrap = false;
			lastSSLStatus = null;
		}
	}

	public String name()
	{
		return "https";
	}

	public SSLProtocol(
		VariablePath configurationPath,
		URI uri,
		CommProtocol wrappedProtocol )
	{
		super( configurationPath );
		this.wrappedProtocol = wrappedProtocol;
		firstTime = true;
	}

	// add byte array to buffer eventually enlarging its capacity to handle buffer overflow
	private ByteBuffer addToBuffer( ByteBuffer source, byte[] bytesToAdd )
	{
		int newCapacity;
		if ( source.limit() == source.capacity() ) { // limit was not set yet
			source.limit( 0 );
		} else { // otherwise go to limit
			source.position( source.limit() );
		}
		if ( bytesToAdd.length > source.capacity() - source.limit() ) {
			// create a larger buffer and copy content of old buffer
			newCapacity = bytesToAdd.length - (source.capacity() - source.limit()) + 1024;
			if ( newCapacity + source.capacity() > bufferSize ) {
				bufferSize += newCapacity;
			}
			ByteBuffer tmp = ByteBuffer.allocate( bufferSize );
			tmp.put( compactToByte( source ) );
			source = tmp;
		}
		source.limit( source.position() + bytesToAdd.length );
		source.put( bytesToAdd );
		source.flip();
		return source;
	}

	private static ByteBuffer clearUntilPosition( ByteBuffer buffer )
	{
		ByteBuffer tmp = buffer.slice();
		buffer.clear();
		buffer.put( compactToByte( tmp ) );
		buffer.flip();
		return buffer;
	}

	// performs the wrap operation taking care of buffer overflows
	private SSLResult wrap( ByteBuffer source )
	{
		result.emptyBuffer();
		try {
			result.log = ssl.wrap( source, result.buffer );
			lastSSLStatus = result.log.getStatus();
			while ( result.log.getStatus() == Status.BUFFER_OVERFLOW ) {
				result.enlargeBuffer();
				result.log = ssl.wrap( source, result.buffer );
				lastSSLStatus = result.log.getStatus();
			}
		} catch ( SSLException e ) {
			e.printStackTrace();
		}
		result.buffer.flip();
		return result;
	}

	// performs the unwrap operation taking care of these events:
	//
	// - if the source contains more than a request, unwraps the first request, removes it from source and returns moreToUnwrap=true
	// - (BUFFER UNDERFLOW) retrieves more data from the channel and tries unwrapping again
	// - (BUFFER OVERFLOW)  inizialize a larger buffer for containing results
	// return:
	// moreToUnwrap=true if there is more data to be processed in the source
	// moreToUnwrap=false if all data in the source has been processed
	private SSLResult unwrap( ByteBuffer source, InputStream istream )
		throws IOException, SSLException
	{
		result.emptyBuffer();
		source.rewind();
		do {
			result.log = ssl.unwrap( source, result.buffer );
			lastSSLStatus = result.log.getStatus();
			switch ( result.log.getStatus() ) {
				case BUFFER_UNDERFLOW:
					source.position( source.limit() );
					byte[] justRead = compactToByte( readFromChannel( istream ) );
					source = addToBuffer( source, justRead );
					break;
				case BUFFER_OVERFLOW:
					result.enlargeBuffer();
					break;
			}
		} while ( (result.log.getStatus() != Status.OK) && (result.log.getStatus() != Status.CLOSED) );
		if ( result.log.bytesConsumed() < source.limit() ) { // if the buffer still contains data to be processed
			source.position( result.log.bytesConsumed() );
			result.moreToUnwrap = true;
		} else {
			result.moreToUnwrap = false;
		}
		result.buffer.flip();
		return result;
	}

	private static byte[] compactToByte( ByteBuffer a )
	{
		byte[] ba = new byte[a.limit()];
		a.rewind();
		int i;
		for ( i = 0; i < a.limit(); i++ ) {
			ba[i] = a.get();
		}
		return ba;
	}

	private String getSSLStringParameter( String parameterName, String defaultValue )
	{
		if ( hasParameter( "ssl" ) ) {
			Value sslParams = getParameterFirstValue( "ssl" );
			if ( sslParams.hasChildren( parameterName ) ) {
				return sslParams.getFirstChild( parameterName ).strValue();
			}
		}
		return defaultValue;
	}

	private void init()
		throws IOException
	{
		// Set default parameters
		String protocol = getSSLStringParameter( "protocol", "SSLv3" ),
			keyStoreFormat = getSSLStringParameter( "keyStoreFormat", "JKS" ),
			trustStoreFormat = getSSLStringParameter( "trustStoreFormat", "JKS" ),
			keyStoreFile = getSSLStringParameter( "keyStore", null ),
			keyStorePassword = getSSLStringParameter( "keyStorePassword", null ),
			trustStoreFile = getSSLStringParameter( "trustStore", System.getProperty( "java.home" ) + "/lib/security/cacerts" ),
			trustStorePassword = getSSLStringParameter( "trustStorePassword", null );
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

			ssl = context.createSSLEngine();
			ssl.setUseClientMode( isClient );
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

	private void startHandshake( OutputStream ostream, InputStream istream )
		throws IOException, SSLException
	{
		boolean handshakeRunning = true;
		result = new SSLResult( false );
		ByteBuffer inbound = ByteBuffer.allocate( bufferSize );

		if ( ssl == null ) {
			wrappedProtocol.setChannel( this.channel() );
		}

		if ( firstTime == true ) {
			init();
			ssl.beginHandshake();
			firstTime = false;
		}

		ByteBuffer receivedData = ByteBuffer.allocate( bufferSize );
		while ( (ssl.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) && (ssl.getHandshakeStatus() != HandshakeStatus.FINISHED) && (handshakeRunning != false) ) {
			switch ( ssl.getHandshakeStatus() ) {
				case NEED_TASK:
					Runnable runnable;
					while ( (runnable = ssl.getDelegatedTask()) != null ) {
						runnable.run();
					}
					break;
				case NEED_WRAP:
					inbound.clear();
					result = wrap( inbound );
					if ( result.log.bytesProduced() > 0 ) { //need to send result to other side
						ostream.write( result.buffer.array(), 0, result.buffer.limit() );
						ostream.flush();
					}
					break;
				case NEED_UNWRAP:
					if ( result.moreToUnwrap == false ) {
						receivedData = readFromChannel( istream );
					} else {
						receivedData = clearUntilPosition( receivedData );    // drops data already processed
					}
					try {
						result = unwrap( receivedData, istream );
					} catch ( SSLException e ) {
						handshakeRunning = false;
						throw e;
					}
					inbound = result.buffer;
					break;
			}
		}
	}

	private ByteBuffer readFromChannel( InputStream istream )
		throws IOException
	{
		ByteBuffer receivedData = null;
		byte[] byteBuffer;
		int bufferLength;
		// handling buffer_overflow
		while ( istream.available() > bufferSize ) {
			bufferSize += 1024;
		}

		byteBuffer = new byte[bufferSize];
		bufferLength = istream.read( byteBuffer );
		if ( bufferLength != -1 ) {
			receivedData = ByteBuffer.wrap( byteBuffer, 0, bufferLength );
			receivedData.rewind();
		}
		return receivedData;
	}

	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		if ( (ssl != null) && ((ssl.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) || (ssl.getHandshakeStatus() == HandshakeStatus.FINISHED)) ) {
			ByteArrayOutputStream bostream = new ByteArrayOutputStream();

			// sending request to httpProtocol
			wrappedProtocol.send( bostream, message, null );
			sendAux( bostream, ostream );
		} else { // We need to handshake first
			if ( ssl == null ) {
				isClient = true;
			}
			startHandshake( ostream, istream );
			send( ostream, message, istream );
		}
	}

	private void sendAux( ByteArrayOutputStream bostream, OutputStream ostream )
		throws IOException
	{
		// now wrapping the message
		ByteBuffer inbound = ByteBuffer.wrap( "".getBytes() );
		if ( bostream != null ) {
			inbound = ByteBuffer.wrap( bostream.toByteArray() );
		}
		result = wrap( inbound );
		if ( result.log.bytesProduced() > 0 ) { //need to send result to client
			ostream.write( result.buffer.array(), 0, result.buffer.limit() );
			ostream.flush();
		}
	}

	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		byte[] recvBytes = recvAux( istream, ostream );
		if ( recvBytes != null ) {
			return wrappedProtocol.recv( new SSLInputStream( recvBytes, this, istream, ostream ), ostream );
		} else {
			return null;
		}
	}

	public byte[] recvAux( InputStream istream, OutputStream ostream )
		throws IOException
	{
		ByteBuffer recvBuffer = ByteBuffer.allocate( bufferSize );
		ByteBuffer receivedData = null;
		if ( (ssl != null) && ((ssl.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) || (ssl.getHandshakeStatus() == HandshakeStatus.FINISHED)) ) {
			result = new SSLResult( false );
			byte[] justRead = null;
			do {
				if ( result.moreToUnwrap == false ) {
					receivedData = readFromChannel( istream );
				} else {
					receivedData = clearUntilPosition( receivedData ); // drops data already processed
				}
				if ( receivedData == null ) {
					return null;
				}
				try {
					result = unwrap( receivedData, istream );
					if ( result.buffer.limit() >= 0 ) {
						// merges the received data with the previous ones
						justRead = compactToByte( result.buffer );
						recvBuffer = addToBuffer( recvBuffer, justRead );
					}
				} catch ( SSLException e ) {
					result.moreToUnwrap = false;
					throw e;
				}
			} while ( result.moreToUnwrap == true );

			if ( lastSSLStatus == Status.CLOSED && result.log.getHandshakeStatus() == HandshakeStatus.NEED_WRAP ) {
				// Connection is about to gracefully shut down. Sending message to other peer to gracefully shutdown too.
				sendAux( null, ostream );
			}
			if ( recvBuffer.limit() == 0 ) {
				return null;
			}
			return compactToByte( recvBuffer );
		} else {
			if ( ssl == null ) {
				isClient = false;
			}
			startHandshake( ostream, istream );
			return recvAux( istream, ostream ); // TODO after handshaking go again in recv mode
		}
	}
}

