/*
 * Copyright (C) 2019 Fabrizio Montesi <famontesi@gmail.com>
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

package joliex.surface;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;

/**
 *
 * @author Fabrizio Montesi
 */
public class JolieToSurfaceCommandLineParser extends CommandLineParser
{
	private final String inputPortName;
	private final boolean noOutputPort, noLocation, noProtocol;

	public boolean noOutputPort()
	{
		return noOutputPort;
	}
	
	public boolean noLocation()
	{
		return noLocation;
	}
	
	public boolean noProtocol()
	{
		return noProtocol;
	}

	public String inputPortName()
	{
		return inputPortName;
	}

	private static class JolieDocArgumentHandler implements CommandLineParser.ArgumentHandler
	{
		private boolean noOutputPort, noLocation, noProtocol;

		@Override
		public int onUnrecognizedArgument( List< String > argumentsList, int index )
			throws CommandLineException
		{
			switch( argumentsList.get( index ) ) {
			case "--noOutputPort":
				noOutputPort = true;
				break;
			case "--noProtocol":
				noProtocol = true;
				break;
			case "--noLocation":
				noLocation = true;
				break;
			default:
				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
			}

			return index;
		}
	}

	public static JolieToSurfaceCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException
	{
		return new JolieToSurfaceCommandLineParser( args, parentClassLoader, new JolieDocArgumentHandler() );
	}

	private JolieToSurfaceCommandLineParser( String[] args, ClassLoader parentClassLoader, JolieDocArgumentHandler argHandler )
		throws CommandLineException, IOException
	{
		super( args, parentClassLoader, argHandler );
		if ( arguments().length < 1 ) {
			throw new CommandLineException( "no input port name provided (try jolie2surface --help for help)" );
		}
		inputPortName = arguments()[0];
		noLocation = argHandler.noLocation;
		noOutputPort = argHandler.noOutputPort;
		noProtocol = argHandler.noProtocol;
	}

	@Override
	protected String getHelpString()
	{
		return "Usage: jolie2surface [ --noOutputPort ] [ --noLocation ] [ --noProtocol ] <filename.ol|filename.iol> <input port name>";
	}
}
