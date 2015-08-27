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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.BufferUnderflowException;
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
 * 2010: complete rewrite
 */
public class SSLProtocol extends SequentialCommProtocol
{
	private static final int INITIAL_BUFFER_SIZE = 32768;
	private static final int MAX_SSL_CONTENT_SIZE = 16384;

	private final boolean isClient;
	private boolean firstTime;
	private final CommProtocol wrappedProtocol;
	private SSLEngine sslEngine = null;

	private OutputStream outputStream;
	private InputStream inputStream;

	private ByteBuffer clearInputBuffer = ByteBuffer.allocate( INITIAL_BUFFER_SIZE );

	private class SSLInputStream extends InputStream
	{
		@Override
		public int read()
			throws IOException
		{
			handshakeIfNeeded();

			if ( clearInputBuffer.remaining() <= 0 ) {
				unwrapFromInputStream( false );
			}

			try {
				return clearInputBuffer.get();
			} catch( BufferUnderflowException e ) {
				throw new IOException( e );
			}
		}

		@Override
		public int read( byte[] b, int off, int len )
			throws IOException
		{
			// in this case it is the best to use InputStream's method, which calls read()
			return super.read( b, off, len );
		}

		@Override
		public long skip( long n )
			throws IOException
		{
			if ( n <= 0 ) {
				return 0;
			}

			long skipped = 0;
			while ( skipped < n && clearInputBuffer.position() < clearInputBuffer.limit() ) {
				clearInputBuffer.get();
				++skipped;
			}
			return skipped;
		}

		@Override
		public int available()
			throws IOException
		{
			return clearInputBuffer.limit() - clearInputBuffer.position();
		}

		// close() not necessary, does nothing
	}

	private class SSLOutputStream extends OutputStream
	{
		private final ByteArrayOutputStream internalStreamBuffer = new ByteArrayOutputStream();

		private void writeCache()
			throws IOException
		{
			SSLProtocol.this.write( ByteBuffer.wrap( internalStreamBuffer.toByteArray() ) );
			internalStreamBuffer.reset();
		}

		@Override
		public void write( int b )
			throws IOException
		{
			internalStreamBuffer.write( b );
			if ( internalStreamBuffer.size() >= MAX_SSL_CONTENT_SIZE ) {
				writeCache();
			}
		}

		@Override
		public void write( byte[] b, int off, int len )
			throws IOException
		{
			internalStreamBuffer.write( b, off, len );
			if ( internalStreamBuffer.size() >= MAX_SSL_CONTENT_SIZE ) {
				writeCache();
			}
		}

		@Override
		public void flush()
			throws IOException
		{
			writeCache();
			SSLProtocol.this.flushOutputStream();
		}

		// close() not necessary, does nothing
	}

	private class SSLResult
	{
		private ByteBuffer buffer;
		private SSLEngineResult log = null;

		public void enlargeBuffer()
		{
			buffer = ByteBuffer.allocate( buffer.capacity() + INITIAL_BUFFER_SIZE );
		}

		public SSLResult( int capacity )
		{
			buffer = ByteBuffer.allocate( capacity );
		}
	}

	@Override
	public String name()
	{
		return wrappedProtocol.name() + "s";
	}

	public SSLProtocol(
		VariablePath configurationPath,
		URI uri,
		CommProtocol wrappedProtocol,
		boolean isClient
	) {
		super( configurationPath );
		this.wrappedProtocol = wrappedProtocol;
		this.isClient = isClient;
		firstTime = true;
		clearInputBuffer.limit( 0 );
	}

	private SSLResult wrap( ByteBuffer source )
		throws IOException
	{
		SSLResult result = new SSLResult( source.capacity() );
		result.log = sslEngine.wrap( source, result.buffer );
		while ( result.log.getStatus() == Status.BUFFER_OVERFLOW ) {
			result.enlargeBuffer();
			result.log = sslEngine.wrap( source, result.buffer );
		}
		if ( result.log.getStatus() == Status.CLOSED ) {
			throw new IOException( "Remote party closed SSL connection" );
		}
		result.buffer.flip();
		return result;
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

	private int getSSLIntegerParameter( String parameterName, int defaultValue )
	{
		if ( hasParameter( "ssl" ) ) {
			Value sslParams = getParameterFirstValue( "ssl" );
			if ( sslParams.hasChildren( parameterName ) ) {
				return sslParams.getFirstChild( parameterName ).intValue();
			}
		}
		return defaultValue;
	}

	private void init()
		throws IOException
	{
		// Set default parameters
		String protocol = getSSLStringParameter( "protocol", "TLSv1" ),
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

			sslEngine = context.createSSLEngine();
			sslEngine.setEnabledProtocols( new String[] { protocol } );
			sslEngine.setUseClientMode( isClient );
			if ( isClient == false ) {
				if ( getSSLIntegerParameter( "wantClientAuth", 1 ) > 0 ) {
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

	private void handshake()
		throws IOException, SSLException
	{
		if ( firstTime ) {
			init();
			sslEngine.beginHandshake();
			firstTime = false;
		}

		SSLResult result;
		Runnable runnable;
		while (
			sslEngine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING
			&& sslEngine.getHandshakeStatus() != HandshakeStatus.FINISHED
		) {
			switch ( sslEngine.getHandshakeStatus() ) {
			case NEED_TASK:
				while ( (runnable = sslEngine.getDelegatedTask()) != null ) {
					runnable.run();
				}
				break;
			case NEED_WRAP:
				result = wrap( ByteBuffer.allocate( INITIAL_BUFFER_SIZE ) );
				if ( result.log.bytesProduced() > 0 ) { //need to send result to other side
					outputStream.write( result.buffer.array(), result.buffer.position(), result.buffer.remaining() );
					outputStream.flush();
				}
				break;
			case NEED_UNWRAP:
				unwrapFromInputStream( true );
				break;
			}
		}
	}

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		outputStream = ostream;
		inputStream = istream;

		if ( firstTime ) {
			wrappedProtocol.setChannel( this.channel() );
		}

		SSLOutputStream sslOutputStream = new SSLOutputStream();
		InputStream sslInputStream = new SSLInputStream();
		wrappedProtocol.send( sslOutputStream, message, sslInputStream );
		sslOutputStream.writeCache();
	}

	private void handshakeIfNeeded()
		throws IOException
	{
		if ( sslEngine == null ||
			( sslEngine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING &&
			sslEngine.getHandshakeStatus() != HandshakeStatus.FINISHED )
		) {
			handshake();
		}
	}

	/* private void enlargeClearInputBuffer()
	{
		// TODO: Maybe we should also check if some compacting would suffice.
		ByteBuffer tmp = ByteBuffer.allocate( clearInputBuffer.capacity() + INITIAL_BUFFER_SIZE );
		tmp.put( clearInputBuffer );
		tmp.flip();
		clearInputBuffer = tmp;
	} */

	private void unwrapFromInputStream( boolean forHandshake )
		throws IOException
	{
		SSLEngineResult result;
		ByteBuffer cryptBuffer;
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		boolean keepRun = true;
		boolean closed = false;

		int oldPosition = clearInputBuffer.position();
		clearInputBuffer.position( clearInputBuffer.limit() );
		clearInputBuffer.limit( clearInputBuffer.capacity() );

		byteOutputStream.write( inputStream.read() );
		while( keepRun ) {
			cryptBuffer = ByteBuffer.wrap( byteOutputStream.toByteArray() );
			result = sslEngine.unwrap( cryptBuffer, clearInputBuffer );
			switch( result.getStatus() ) {
			case BUFFER_OVERFLOW:
				/*enlargeClearInputBuffer();
				oldPosition = clearInputBuffer.position();
				clearInputBuffer.position( clearInputBuffer.limit() );
				clearInputBuffer.limit( clearInputBuffer.capacity() );*/
                                int appSize = sslEngine.getSession().getApplicationBufferSize();
                                ByteBuffer tmpBuffer1 = ByteBuffer.allocate(appSize + clearInputBuffer.position());
                                clearInputBuffer.flip();
                                tmpBuffer1.put( clearInputBuffer );
                                clearInputBuffer = tmpBuffer1;
				break;
			case BUFFER_UNDERFLOW:
                                int netSize = sslEngine.getSession().getPacketBufferSize();
                                if ( netSize > clearInputBuffer.capacity() ) {
                                    ByteBuffer tmpBuffer2 = ByteBuffer.allocate( netSize );
                                    cryptBuffer.flip();
                                    tmpBuffer2.put( cryptBuffer );
                                    cryptBuffer = tmpBuffer2;
                                }
				byteOutputStream.write( inputStream.read() );
				break;
			case CLOSED:
				keepRun = false;
				closed = true;
				break;
			case OK:
				clearInputBuffer.limit( clearInputBuffer.position() );
				clearInputBuffer.position( oldPosition );
				if ( forHandshake ) {
					keepRun = false;
				} else {
					if ( cryptBuffer.position() >= cryptBuffer.limit() ) {
						// If we are here, it means that there are no more packets to receive
						keepRun = false;
					} else {
						cryptBuffer = cryptBuffer.slice();
						byteOutputStream = new ByteArrayOutputStream();
						byteOutputStream.write( cryptBuffer.array() );
					}
				}
				break;
			}
		}
		if ( closed ) {
			throw new IOException( "Other party closed the SSL connection" );
		}
	}

	private void write( ByteBuffer b )
		throws IOException
	{
		handshakeIfNeeded();
		SSLResult wrapResult = wrap( b );
		if ( wrapResult.log.bytesProduced() > 0 ) {
			outputStream.write( wrapResult.buffer.array(), 0, wrapResult.buffer.limit() );
			//outputStream.flush();
		}
	}

	private void flushOutputStream()
		throws IOException
	{
		outputStream.flush();
	}

	@Override
	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		outputStream = ostream;
		inputStream = istream;

		if ( firstTime ) {
			wrappedProtocol.setChannel( this.channel() );
		}

		return wrappedProtocol.recv( new SSLInputStream(), new SSLOutputStream() );
	}
}

