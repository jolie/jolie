/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
import java.util.Iterator;
import java.util.Map;
import jolie.net.CommMessage;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;

public class ConsoleService extends JavaService
{
	private Map< String, String > sessionTokens;
	private boolean sessionListeners = false;
	private boolean enableTimestamp = false;
	private final String timestampFormatDefault = "dd/MM/yyyy HH:mm:ss";
	private String timestampFormat = timestampFormatDefault;

	private class ConsoleInputThread extends Thread
	{
		private boolean keepRun = true;

		public void kill()
		{
			keepRun = false;
			this.interrupt();
		}

		@Override
		public void run()
		{
			BufferedReader stdin
				= new BufferedReader(
					new InputStreamReader(
						Channels.newInputStream(
							(new FileInputStream( FileDescriptor.in )).getChannel() ) ) );
			try {
				String line;
				while( keepRun ) {
					line = stdin.readLine();

					if ( sessionListeners ) {
						Iterator it = sessionTokens.keySet().iterator();

						while( it.hasNext() ) {
							Value v = Value.create();
							v.getFirstChild( "token" ).setValue( it.next() );
							v.setValue( line );
							sendMessage( CommMessage.createRequest( "in", "/", v ) );
						}
					} else {
						sendMessage( CommMessage.createRequest( "in", "/", Value.create( line ) ) );
					}
				}
			} catch( ClosedByInterruptException ce ) {
			} catch( Exception e ) {
				e.printStackTrace();
			} finally {
				try {
					stdin.close();
				} catch( IOException e ) {
					interpreter().logWarning( e );
				}
			}
		}
	}
	private ConsoleInputThread consoleInputThread;

	@RequestResponse
	public void registerForInput( Value request )
	{
		if ( request.getFirstChild( "enableSessionListener" ).isDefined() ) {
			if ( request.getFirstChild( "enableSessionListener" ).boolValue() ) {
				sessionListeners = true;
				sessionTokens = new HashMap<>();
			}
		}
		consoleInputThread = new ConsoleInputThread();
		consoleInputThread.start();
	}

	@Override
	protected void finalize()
		throws Throwable
	{
		try {
			consoleInputThread.kill();
		} finally {
			super.finalize();
		}
	}

	@RequestResponse
	public void print( String s )
	{
		if ( enableTimestamp ) {
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
	public void println( String s )
	{
		if ( enableTimestamp ) {
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
	public void enableTimestamp( Value request )
	{
		boolean enable = request.boolValue();
		if ( enable ) {
			enableTimestamp = true;
			if ( request.getFirstChild( "format" ).isDefined() ) {
				timestampFormat = request.getFirstChild( "format" ).strValue();
			} else {
				timestampFormat = timestampFormatDefault;
			}
		} else {
			enableTimestamp = false;
			timestampFormat = timestampFormatDefault;
		}

	}

	@RequestResponse
	public void subscribeSessionListener( Value request )
	{
		String token = request.getFirstChild( "token" ).strValue();

		if ( sessionListeners ) {
			sessionTokens.put( token, token );

		}
	}

	@RequestResponse
	public void unsubscribeSessionListener( Value request )
	{
		String token = request.getFirstChild( "token" ).strValue();

		if ( sessionListeners ) {
			sessionTokens.remove( token );
		}
	}
}
