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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.Iteration;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.Wrapper;
import jolie.Interpreter;
import jolie.net.CommMessage;
import jolie.net.LocalCommChannel;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.java.OutputPort;

public class JolieMustacheObjectHandler implements ObjectHandler {
	public final static class LocationHolder {
		private final OutputPort functionProvider;

		public LocationHolder( Object location ) {
			if( location instanceof LocalCommChannel ) {
				this.functionProvider = new OutputPort( ((LocalCommChannel) location).interpreter() );
			} else {
				throw new IllegalArgumentException(
					"Location passed to Mustache must be a LocalCommChannel. Please refer to getLocalLocation@runtime." );
			}
		}
	}

	private record FunctionCallHolder(String functionName, OutputPort provider) {
	}

	/**
	 * Find a value named "name" in the array of scopes in reverse order.
	 *
	 * @param name the variable name
	 * @param scopes0 the ordered list of scopes
	 * @return a wrapper that can be used to extract a value
	 */
	@Override
	public Wrapper find( String name, List< Object > scopes0 ) {
		return scopes -> {
			Object scope = scopes.get( scopes.size() - 1 );
			if( scope != null ) {
				Value value = (Value) scope;

				String[] keys = name.split( "\\." );
				for( int i = 0; i < keys.length - 1; i++ ) {
					if( value.hasChildren( keys[ i ] ) ) {
						value = value.getFirstChild( keys[ i ] );
					} else {
						return null;
					}
				}

				if( value.hasChildren( keys[ keys.length - 1 ] ) ) {
					// It's a data node
					return value.getChildren( keys[ keys.length - 1 ] );
				} else if( value.valueObject() instanceof LocationHolder ) {
					// We have a function provider. Return a function call with this key as function name.
					return new FunctionCallHolder( keys[ keys.length - 1 ],
						((LocationHolder) value.valueObject()).functionProvider );
				}
				// Value value = (Value) scope;
				// if( value.hasChildren( name ) ) {
				// return value.getChildren( name );
				// // ValueVector vec = value.getChildren( name );
				// // return vec.size() > 1 ? vec : vec.first().valueObject();
				// }
			}

			return null;
		};
	}

	/**
	 * Coerce results to Java native iterables, functions, callables.
	 *
	 * @param object transform an unknown type to a known type
	 * @return the new object
	 */
	@Override
	public Object coerce( Object object ) {
		return object;
	}

	/**
	 * Iterate over an object by calling Iteration.next for each value.
	 *
	 * @param iteration callback for the next iteration
	 * @param writer the writer to write to
	 * @param object the current object
	 * @param scopes the scopes present
	 * @return the current writer
	 */
	@Override
	public Writer iterate( Iteration iteration, Writer writer, Object object, List< Object > scopes ) {
		if( object instanceof ValueVector ) {
			ValueVector vec = (ValueVector) object;
			if( vec.size() > 0 && vec.first().isBool() && !vec.first().boolValue() ) {
				return writer;
			} else {
				Writer w = writer;
				for( Value value : vec ) {
					scopes.add( value );
					w = iteration.next( w, value, scopes );
					scopes.remove( scopes.size() - 1 );
				}
				return w;
			}
		} else if( object instanceof FunctionCallHolder ) {
			FunctionCallHolder holder = (FunctionCallHolder) object;
			try {
				Value requestData = Value.create();
				requestData.getFirstChild( "name" ).setValue( holder.functionName );

				StringWriter requestWriter = new StringWriter();
				iteration.next( requestWriter, null, scopes );
				requestData.getFirstChild( "template" ).setValue( requestWriter.toString() );

				Value responseData =
					holder.provider.callRequestResponse( CommMessage.createRequest( "call", "/", requestData ) );
				if( responseData.isString() ) {
					try {
						writer.write( responseData.strValue() );
					} catch( IOException e ) {
						Interpreter.getInstance().logWarning( e );
					}
				} else {
					scopes.add( responseData );
					Writer w = iteration.next( writer, responseData, scopes );
					System.out.println( "TBD" );
					return w;
				}
				// scopes.add( responseData );
				// Writer w = iteration.next( writer, responseData, scopes );
				// scopes.remove( scopes.size() - 1 );
				return writer;
			} catch( FaultException e ) {
				return writer;
			}
		}

		return writer;
	}

	/**
	 * Call Iteration.next() either 0 (true) or 1 (false) times.
	 *
	 * @param iteration callback for the next iteration
	 * @param writer the writer to write to
	 * @param object the current object
	 * @param scopes the scopes present
	 * @return the current writer
	 */
	@Override
	public Writer falsey( Iteration iteration, Writer writer, Object object, List< Object > scopes ) {
		if( object == null ) {
			iteration.next( writer, object, scopes );
		} else if( object instanceof ValueVector ) {
			ValueVector vec = (ValueVector) object;
			if( vec.size() == 1 ) {
				Value first = vec.first();
				if( first.isBool() && !first.boolValue() ) {
					iteration.next( writer, object, scopes );
				}
			}
		}

		return writer;
	}

	/**
	 * Each call site has its own binding to allow for fine grained caching without a separate parallel
	 * hierarchy of objects.
	 *
	 * @param name the name that we bound
	 * @param tc the textual context of the binding site
	 * @param code the code that was bound
	 * @return the binding
	 */
	@Override
	public Binding createBinding( String name, TemplateContext tc, Code code ) {
		return scopes -> find( name, null ).call( scopes );
	}

	/**
	 * Turns an object into the string representation that should be displayed in templates.
	 *
	 * @param object the object to be displayed
	 * @return a string representation of the object.
	 */
	@Override
	public String stringify( Object object ) {
		switch( object ) {
		case Value v:
			return v.strValue();
		case ValueVector vec:
			return vec.first().strValue();
		case FunctionCallHolder holder:
			try {
				Value requestData = Value.create();
				requestData.getFirstChild( "name" ).setValue( holder.functionName );
				return holder.provider.callRequestResponse( CommMessage.createRequest( "call", "/", requestData ) )
					.strValue();
			} catch( FaultException e ) {
				return e.toString();
			}
		default:
			return object.toString();
		}
	}
}
