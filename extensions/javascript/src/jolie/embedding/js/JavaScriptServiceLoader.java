/***************************************************************************
 *   Copyright (C) 2007-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.embedding.js;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoaderCreationException;
import jolie.runtime.embedding.EmbeddedServiceLoadingException;
import jolie.runtime.expression.Expression;

/**
 *
 * @author Fabrizio Montesi
 */
public class JavaScriptServiceLoader extends EmbeddedServiceLoader
{
	private final ScriptEngine engine;

	public JavaScriptServiceLoader( Expression channelDest, String jsPath )
		throws EmbeddedServiceLoaderCreationException
	{
		super( channelDest );
		final ScriptEngineManager manager = new ScriptEngineManager();
		this.engine = manager.getEngineByName( "JavaScript" );
		if ( engine == null ) {
			throw new EmbeddedServiceLoaderCreationException( "JavaScript engine not found. Check your system." );
		}

		final Compilable compilable = (Compilable)engine;
		try {
			final Reader reader = new BufferedReader( new FileReader( jsPath ) );
			try {
				final CompiledScript compiledScript = compilable.compile( reader );
				compiledScript.eval();
			} catch( ScriptException e ) {
				throw new EmbeddedServiceLoaderCreationException( e );
			} finally {
				try {
					reader.close();
				} catch( IOException e ) {
					throw new EmbeddedServiceLoaderCreationException( e );
				}
			}
		} catch( FileNotFoundException e ) {
			throw new EmbeddedServiceLoaderCreationException( e );
		}
	}

	@Override
	public void load()
		throws EmbeddedServiceLoadingException
	{
		try {
			final Object json = engine.eval( "JSON" );
			setChannel( new JavaScriptCommChannel( (Invocable)engine, json ) );
		} catch( ScriptException e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}
}
