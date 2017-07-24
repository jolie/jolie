/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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


package jolie.runtime.embedding;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import jolie.CommandLineException;
import jolie.Interpreter;
import jolie.runtime.expression.Expression;


public class JolieServiceLoader extends EmbeddedServiceLoader
{
	private final static Pattern servicePathSplitPattern = Pattern.compile( " " );
	private final Interpreter interpreter;
	
	public JolieServiceLoader( Expression channelDest, Interpreter currInterpreter, String servicePath )
		throws IOException, CommandLineException
	{
		super( channelDest );
		final String[] ss = servicePathSplitPattern.split( servicePath );
		final String[] options = currInterpreter.optionArgs();
				
		final String[] newArgs = new String[ 2 + options.length + ss.length ];
		newArgs[0] = "-i";
		newArgs[1] = currInterpreter.programDirectory().getAbsolutePath();
		System.arraycopy( options, 0, newArgs, 2, options.length );
		System.arraycopy( ss, 0, newArgs, 2 + options.length, ss.length );
		interpreter = new Interpreter(
                        System.currentTimeMillis(),
			newArgs,
			currInterpreter.getClassLoader(),
			currInterpreter.programDirectory()
		);
	}

	public void load()
		throws EmbeddedServiceLoadingException
	{
		Future< Exception > f = interpreter.start();
		try {
			Exception e = f.get();
			if ( e == null ) {
				setChannel( interpreter.commCore().getLocalCommChannel() );
			} else {
				throw new EmbeddedServiceLoadingException( e );
			}
		} catch( Exception e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}

	public Interpreter interpreter()
	{
		return interpreter;
	}
}
