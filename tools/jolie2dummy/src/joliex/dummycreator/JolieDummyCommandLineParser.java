/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package joliex.dummycreator;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;

public class JolieDummyCommandLineParser extends CommandLineParser
{
	private String nameOperation;

	public String getNameOperation()
	{
		return nameOperation;
	}

	private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler
	{
		private String nameOperation;

		public int onUnrecognizedArgument( List< String> argumentsList, int index )
			throws CommandLineException
		{
			if ( "--output".equals( argumentsList.get( index ) ) ) {
				index++;

				nameOperation = argumentsList.get( index );
				index++;
			}
			//			} else {
			//				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
			//			}

			return index;
		}
	}

	public static JolieDummyCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException
	{
		return new JolieDummyCommandLineParser( args, parentClassLoader, new JolieDummyArgumentHandler() );
	}

	private JolieDummyCommandLineParser( String[] args, ClassLoader parentClassLoader, JolieDummyArgumentHandler argHandler )
		throws CommandLineException, IOException
	{
		super( args, parentClassLoader, argHandler );
		nameOperation = argHandler.nameOperation;
	}
}
