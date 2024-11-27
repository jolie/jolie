/*
 * Copyright (C) 2022 Fabrizio Montesi <famontesi@gmail.com>
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

package joliex.mustache;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import jolie.runtime.AndJarDeps;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

@AndJarDeps( "compiler.jar" )
public class MustacheService extends JavaService {
	public String render( Value request ) {
		final Map< String, Integer > compiledPartialsDepth = new HashMap<>();
		final DefaultMustacheFactory mustacheFactory;
		final int partialsRecursionLimit;

		partialsRecursionLimit = request.firstChildOrDefault( "partialsRecursionLimit", Value::intValue, 10 );

		if( request.hasChildren( "dir" ) ) {
			mustacheFactory = new DefaultMustacheFactory( new File( request.getFirstChild( "dir" ).strValue() ) );
		} else if( request.hasChildren( "partials" ) ) {
			@SuppressWarnings( "unchecked" )
			final Map< String, String > templateMap = Map.ofEntries(
				request.getChildren( "partials" ).stream()
					.map( ( Value partialValue ) -> new AbstractMap.SimpleImmutableEntry< String, String >(
						partialValue.getFirstChild( "name" ).strValue(),
						partialValue.getFirstChild( "template" ).strValue() ) )
					.toArray( Map.Entry[]::new ) );
			mustacheFactory = new DefaultMustacheFactory() {
				int globalDepthCounter = 0;

				@Override
				public Mustache compilePartial( String name ) {
					if( !templateMap.containsKey( name ) ) {
						throw new IllegalArgumentException( "Partial template '" + name + "' not found in memory" );
					}
					globalDepthCounter++;
					compiledPartialsDepth.computeIfAbsent( name, k -> 0 );
					StringReader reader = new StringReader( "" );
					if( compiledPartialsDepth.get( name ) <= partialsRecursionLimit
						&& globalDepthCounter <= getRecursionLimit() ) {
						reader = new StringReader( templateMap.get( name ) );
					}
					compiledPartialsDepth.put( name, compiledPartialsDepth.get( name ) + 1 );
					Mustache mustache = compile( reader, name );
					compiledPartialsDepth.put( name, compiledPartialsDepth.get( name ) - 1 );
					globalDepthCounter--;
					return mustache;
				}
			};
		} else {
			mustacheFactory = new DefaultMustacheFactory();
		}

		mustacheFactory.setObjectHandler( new JolieMustacheObjectHandler() );
		if( request.hasChildren( "recursionLimit" ) ) {
			mustacheFactory.setRecursionLimit( request.getFirstChild( "recursionLimit" ).intValue() );
		}
		Mustache mustache = mustacheFactory.compile(
			new StringReader( request.getFirstChild( "template" ).strValue() ),
			"Jolie" );
		StringWriter outputWriter = new StringWriter();
		mustache.execute( outputWriter, request.getFirstChild( "data" ) );
		outputWriter.flush();
		return outputWriter.toString();
	}
}
