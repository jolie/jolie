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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
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
 * 2015: major fixups
 */
public class SSLProtocol extends SequentialCommProtocol
{
	private static final int INITIAL_BUFFER_SIZE = 32768;

	private final boolean isClient;
	private boolean firstTime;
	private final CommProtocol wrappedProtocol;
	private SSLEngine sslEngine;

	private OutputStream outputStream;
	private InputStream inputStream;

	private SSLInputStream sslInputStream = new SSLInputStream();
	private SSLOutputStream sslOutputStream = new SSLOutputStream();

	private class SSLInputStream extends InputStream
	{
		private ByteBuffer clearInputBuffer = ByteBuffer.allocate( 0 );

		@Override
		public int read()
			throws IOException
		{
			if ( !clearInputBuffer.hasRemaining() ) {
				handshake();
				unwrap( this );
				// EOF reached?
				if ( !clearInputBuffer.hasRemaining() ) {
					return -1;
				}
			}

			try {
				return clearInputBuffer.get();
			} catch( BufferUnderflowException e ) {
				return -1;
			}
		}

		@Override
		public int read( byte[] b, int off, int len )
			throws IOException
		{
			if ( len == 0 )
				return 0;

			if ( !clearInputBuffer.hasRemaining() ) {
				handshake();
				unwrap( this );
				// EOF reached?
				if ( !clearInputBuffer.hasRemaining() ) {
					return -1;
				}
			}

			try {
				clearInputBuffer.get( b, off, len );
				return len;
			} catch( BufferUnderflowException e ) {
				// okay, just return the maximum possible
				len = clearInputBuffer.remaining();
				clearInputBuffer.get( b, off, len );
				return len;
			}
		}

		@Override
		public long skip( long n )
			throws IOException
		{
			if ( n <= 0 ) {
				return 0;
			}

			long skipped = 0;
			while ( skipped < n && clearInputBuffer.hasRemaining() ) {
				clearInputBuffer.get();
				++skipped;
			}
			return skipped;
		}

		@Override
		public int available()
			throws IOException
		{
			return clearInputBuffer.remaining();
		}

		// close() not necessary, does nothing
	}

	private class SSLOutputStream extends OutputStream
	{
		private final ByteBuffer internalBuffer = ByteBuffer.allocate( INITIAL_BUFFER_SIZE );

		private void writeCache()
			throws IOException
		{
			if ( internalBuffer.hasRemaining() ) {
				handshake();
				internalBuffer.flip();
				wrap( internalBuffer );
				internalBuffer.clear();
			}
		}

		@Override
		public void write( int b )
			throws IOException
		{
			try {
				internalBuffer.put( (byte)b );
			} catch ( BufferOverflowException e ) {
				// let us retry after freeing the buffer
				writeCache();
				internalBuffer.put( (byte)b );
			}
		}

		@Override
		public void write( byte[] b, int off, int len )
			throws IOException
		{
			try {
				internalBuffer.put( b, off, len );
			} catch ( BufferOverflowException e ) {
				// let us retry after freeing the buffer
				writeCache();
				internalBuffer.put( b, off, len );
			}
		}

		@Override
		public void flush()
			throws IOException
		{
			writeCache();
		}

		// close() not necessary, does nothing
	}

	private class SSLResult
	{
		private ByteBuffer buffer;
		private SSLEngineResult log;

		public SSLResult( int capacity )
		{
			buffer = ByteBuffer.allocate( capacity );
		}
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
		this.firstTime = true;
	}

	@Override
	public String name()
	{
		return wrappedProtocol.name() + "s";
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
				wrap( ByteBuffer.allocate( INITIAL_BUFFER_SIZE ) );
				break;
			case NEED_UNWRAP:
				unwrap( null );
				break;
			}
		}
	}

	private void unwrap( SSLInputStream sslInputStream )
		throws IOException
	{
		ByteBuffer cryptBuffer = ByteBuffer.allocate( 0 );
		final SSLResult result = new SSLResult( INITIAL_BUFFER_SIZE );

		boolean keepRun = true;

		while( keepRun ) {
			result.log = sslEngine.unwrap( cryptBuffer, result.buffer );
			switch( result.log.getStatus() ) {
			case BUFFER_OVERFLOW:
				final int appSize = sslEngine.getSession().getApplicationBufferSize();
				// Resize "result.buffer" if needed
				if ( appSize > result.buffer.capacity() ) {
					final ByteBuffer b = ByteBuffer.allocate( appSize );
					result.buffer.flip();
					b.put( result.buffer );
					result.buffer = b;
				} else {
					result.buffer.compact();
				}
				break;
			case BUFFER_UNDERFLOW:
				final int netSize = sslEngine.getSession().getPacketBufferSize();
				// Resize "cryptBuffer" if needed
				if ( netSize > cryptBuffer.capacity() ) {
					final ByteBuffer b = ByteBuffer.allocate( netSize );
					cryptBuffer.flip();
					b.put( cryptBuffer );
					cryptBuffer = b;
				} else {
					cryptBuffer.compact();
				}

				int currByte = inputStream.read();
				if ( currByte >= 0 ) {
					cryptBuffer.put( (byte)currByte );
					cryptBuffer.flip();
				} else {
					// input stream EOF reached, we may not continue
					keepRun = false;
				}
				break;
			case OK:
			case CLOSED:
				if ( sslInputStream != null && result.log.bytesProduced() > 0 ) {
					sslInputStream.clearInputBuffer = result.buffer;
					sslInputStream.clearInputBuffer.flip();
				}
				keepRun = false;
				break;
			}
		}
	}

	private void wrap( ByteBuffer source )
		throws IOException
	{
		final SSLResult result = new SSLResult( source.capacity() );
		result.log = sslEngine.wrap( source, result.buffer );
		while ( result.log.getStatus() == Status.BUFFER_OVERFLOW ) {
			final int appSize = sslEngine.getSession().getApplicationBufferSize();

			// Resize "result.buffer" if needed
			if ( appSize > result.buffer.capacity() ) {
				final ByteBuffer b = ByteBuffer.allocate( appSize );
				result.buffer.flip();
				b.put( result.buffer );
				result.buffer = b;
			} else {
				result.buffer.compact();
			}

			result.log = sslEngine.wrap( source, result.buffer );
		}
		// must be Status.OK or Status.CLOSED here
		if ( result.log.bytesProduced() > 0 ) {
			final WritableByteChannel outputChannel = Channels.newChannel( outputStream );
			result.buffer.flip();
			while ( result.buffer.hasRemaining() )
				outputChannel.write( result.buffer );
			outputStream.flush();
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

		wrappedProtocol.send( sslOutputStream, message, sslInputStream );
		sslOutputStream.flush();
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

		CommMessage message = wrappedProtocol.recv( sslInputStream, sslOutputStream );
		sslOutputStream.flush();
		return message;
	}
}
