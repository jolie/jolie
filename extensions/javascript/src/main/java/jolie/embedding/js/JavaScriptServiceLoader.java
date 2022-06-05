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

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class JavaScriptServiceLoader extends EmbeddedServiceLoader {
	private final ScriptEngine engine;

	public JavaScriptServiceLoader( Expression channelDest, String jsPathString )
		throws EmbeddedServiceLoaderCreationException {
		super( channelDest );
		try {
			final Path jsPath = Paths.get( jsPathString ).toAbsolutePath();
			if( !Files.isRegularFile( jsPath ) ) {
				throw new EmbeddedServiceLoaderCreationException( jsPathString + " is not a regular file" );
			}

			final ScriptEngineManager manager = new ScriptEngineManager();
			this.engine = manager.getEngineByName( "nashorn" );
			if( engine == null ) {
				throw new EmbeddedServiceLoaderCreationException(
					"JavaScript engine (nashorn) not found. Check your system." );
			}

			try {
				// The JS engine on Windows requires '/' as a path separator. Hence use
				// jsPath.toUri().toString() instead of jsPath().toString()
				engine.eval( "load('" + jsPath.toUri().toString() + "');" );
			} catch( ScriptException e ) {
				throw new EmbeddedServiceLoaderCreationException( e );
			}
		} catch( InvalidPathException e ) {
			throw new EmbeddedServiceLoaderCreationException( e );
		}
	}

	@Override
	public void load()
		throws EmbeddedServiceLoadingException {
		try {
			final Object json = engine.eval( "JSON" );
			setChannel( new JavaScriptCommChannel( (Invocable) engine, json ) );
		} catch( ScriptException e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}
}
