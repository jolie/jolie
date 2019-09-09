/***************************************************************************
 *   Copyright (C) 2010 by Claudio Guidi <cguidi@italianasoftware.com>     *
 *   Copyright (C) 2014 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.doc;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;

/**
 *
 * @author Claudio Guidi, Fabrizio Montesi
 */
public class JolieDocCommandLineParser extends CommandLineParser
{
	private final boolean outputPortEnabled;
	private final String inputPortName;

	public boolean outputPortEnabled()
	{
		return outputPortEnabled;
	}

	public String inputPortName()
	{
		return inputPortName;
	}

	private static class JolieDocArgumentHandler implements CommandLineParser.ArgumentHandler
	{
		private boolean outputPortEnabled = false;
		private String inputPortName = "";

		public int onUnrecognizedArgument( List< String> argumentsList, int index )
			throws CommandLineException
		{
			if ( "--outputPortEnabled".equals( argumentsList.get( index ) ) ) {
				index++;

				outputPortEnabled = new Boolean( argumentsList.get( index ) );
				//index++;
			} else if ( "--port".equals( argumentsList.get( index ) ) ) {
				index++;
				inputPortName = argumentsList.get( index );
			} else {
				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
			}

			return index;
		}
	}

	public static JolieDocCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException
	{
		return new JolieDocCommandLineParser( args, parentClassLoader, new JolieDocArgumentHandler() );
	}

	private JolieDocCommandLineParser( String[] args, ClassLoader parentClassLoader, JolieDocArgumentHandler argHandler )
		throws CommandLineException, IOException
	{
		super( args, parentClassLoader, argHandler );
		inputPortName = argHandler.inputPortName;
		outputPortEnabled = argHandler.outputPortEnabled;
	}

	@Override
	protected String getHelpString()
	{
		return "Usage: joliedoc [ --outputPortEnabled true|false ] [ --port input_port_name ] <filename.ol|filename.iol>";
	}
}
