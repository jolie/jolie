/*
 * Copyright (C) 2006-2022 Fabrizio Montesi <famontesi@gmail.com>
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

package joliex.io;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;

public class ConsoleService extends JavaService {
	private Map< String, String > sessionTokens;
	private boolean sessionListeners = false;
	private boolean enableTimestamp = false;
	private static final String TIMESTAMP_DEFAULT_FORMAT = "dd/MM/yyyy HH:mm:ss";
	private String timestampFormat = TIMESTAMP_DEFAULT_FORMAT;

	private class ConsoleInputThread extends Thread {
		private volatile boolean keepRun = true;

		public void kill() {
			keepRun = false;
			this.interrupt();
		}

		@Override
		public void run() {
			try(
				FileInputStream fis = new FileInputStream( FileDescriptor.in );
				BufferedReader stdin = new BufferedReader(
					new InputStreamReader(
						Channels.newInputStream(
							fis.getChannel() ) ) ) ) {
				String line;
				do {
					line = stdin.readLine();

					if( sessionListeners ) {
						for( String s : sessionTokens.keySet() ) {
							Value v = Value.create();
							v.getFirstChild( "token" ).setValue( s );
							v.setValue( line );
							getEmbedder().callOneWay( "in", v );
						}
					} else {
						getEmbedder().callOneWay( "in", Value.create( line ) );
					}
				} while( line != null && keepRun );
			} catch( ClosedByInterruptException ce ) {
			} catch( IOException e ) {
				interpreter().logWarning( e );
			}
		}
	}

	private ConsoleInputThread consoleInputThread;

	@RequestResponse
	public void registerForInput( Value request ) {
		if( request.getFirstChild( "enableSessionListener" ).isDefined() ) {
			if( request.getFirstChild( "enableSessionListener" ).boolValue() ) {
				sessionListeners = true;
				sessionTokens = new HashMap<>();
			}
		}
		consoleInputThread = new ConsoleInputThread();
		consoleInputThread.start();
	}

	@Override
	protected void finalize()
		throws Throwable {
		try {
			consoleInputThread.kill();
		} finally {
			super.finalize();
		}
	}

	@RequestResponse
	public void print( String s ) {
		if( enableTimestamp ) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat( timestampFormat );
				final Date now = new Date();
				String ts = sdf.format( now );
				System.out.print( ts + " " + s );
			} catch( Exception e ) {
				System.out.print( "Bad Format " + s );
			}
		} else {
			System.out.print( s );
		}
	}

	@RequestResponse
	public void println( String s ) {
		if( enableTimestamp ) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat( timestampFormat );
				final Date now = new Date();
				String ts = sdf.format( now );
				System.out.println( ts + " " + s );
			} catch( Exception e ) {
				System.out.println( "Bad Format " + s );
			}
		} else {
			System.out.println( s );
		}
	}

	@RequestResponse
	public void enableTimestamp( Value request ) {
		boolean enable = request.boolValue();
		if( enable ) {
			enableTimestamp = true;
			if( request.getFirstChild( "format" ).isDefined() ) {
				timestampFormat = request.getFirstChild( "format" ).strValue();
			} else {
				timestampFormat = TIMESTAMP_DEFAULT_FORMAT;
			}
		} else {
			enableTimestamp = false;
			timestampFormat = TIMESTAMP_DEFAULT_FORMAT;
		}

	}

	@RequestResponse
	public void subscribeSessionListener( Value request ) {
		String token = request.getFirstChild( "token" ).strValue();

		if( sessionListeners ) {
			sessionTokens.put( token, token );

		}
	}

	@RequestResponse
	public void unsubscribeSessionListener( Value request ) {
		String token = request.getFirstChild( "token" ).strValue();

		if( sessionListeners ) {
			sessionTokens.remove( token );
		}
	}

	@RequestResponse
	public String readLine( Value request ) {
		if( request.getFirstChild( "secret" ).isDefined() && request.getFirstChild( "secret" ).boolValue() ) {
			char passwordArray[] = System.console().readPassword();
			return new String( passwordArray );
		} else {
			return System.console().readLine();
		}
	}
}
