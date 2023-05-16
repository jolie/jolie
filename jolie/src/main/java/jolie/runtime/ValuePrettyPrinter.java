/***************************************************************************
 *   Copyright (C) 2009-2019 by Fabrizio Montesi <famontesi@gmail.com>     *
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


package jolie.runtime;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import jolie.lang.NativeType;

/**
 * Pretty printer for {@link Value} objects. This class is not thread safe.
 * 
 * @author Fabrizio Montesi
 */
public class ValuePrettyPrinter {
	private final Value root;
	private final Writer writer;
	private final String header;
	private int indentation = 0;
	private int byteTruncation = -1; // Default: no truncation

	public ValuePrettyPrinter( Value root, Writer writer, String header ) {
		this.root = root;
		this.writer = writer;
		this.header = header;
	}

	private void indent() {
		indentation++;
	}

	private void unindent() {
		indentation--;
	}

	public void setByteTruncation( int truncation ) {
		this.byteTruncation = truncation;
	}

	public void run()
		throws IOException {
		writeIndented( header, List.of() );
		writeNativeValue( root );
		writeChildren( root, List.of() );
	}

	public void setIndentationOffset( int offset ) {
		this.indentation = offset;
	}

	private void writeNativeValue( Value value )
		throws IOException {
		if( value.isUsedInCorrelation() ) {
			writer.write( " (cset)" );
		}
		if( value.valueObject() != null ) {
			writer.write( ":" );
			writer.write( prettyNativeTypeName( value ) );
			writer.write( " = " );
			if( value.valueObject() instanceof ByteArray && byteTruncation > -1 ) {
				String s = value.byteArrayValue().toString();
				writer.write( s.substring( 0, Math.min( byteTruncation, s.length() ) ) + "..." );
			} else if( value.isString() ) {
				writer.write( '"' );
				writer.write( value.valueObject().toString() );
				writer.write( '"' );
			} else {
				writer.write( value.valueObject().toString() );
			}
		}
		writer.write( '\n' );
	}

	private static String prettyNativeTypeName( Value v ) {
		if( v.isBool() ) {
			return NativeType.BOOL.id();
		} else if( v.isByteArray() ) {
			return NativeType.RAW.id();
		} else if( v.isChannel() ) {
			return v.valueObject().getClass().getName();
		} else if( v.isDouble() ) {
			return NativeType.DOUBLE.id();
		} else if( v.isInt() ) {
			return NativeType.INT.id();
		} else if( v.isLong() ) {
			return NativeType.LONG.id();
		} else if( v.isString() ) {
			return NativeType.STRING.id();
		} else {
			return v.valueObject().getClass().getName();
		}
	}

	private void writeChildren( Value value, List< Boolean > hasMore )
		throws IOException {
		Iterator< Entry< String, ValueVector > > childrenIt = value.children().entrySet().iterator();
		while( childrenIt.hasNext() ) {
			Entry< String, ValueVector > entry = childrenIt.next();
			if( entry.getValue().isEmpty() ) {
				writeIndented( childrenIt.hasNext() ? "├─── " : "╰─── ", hasMore );
				writer.write( entry.getKey() );
				writer.write( " (empty array)\n" );
			} else {
				int size = entry.getValue().size();
				Iterator< Value > elementsIt = entry.getValue().iterator();
				int i = 0;
				while( elementsIt.hasNext() ) {
					Value child = elementsIt.next();
					writeIndented( (elementsIt.hasNext() || childrenIt.hasNext()) ? "├─── " : "╰─── ",
						hasMore );
					writer.write( entry.getKey() );
					if( size > 1 ) {
						writer.write( '[' );
						writer.write( Integer.toString( i ) );
						writer.write( ']' );
					}
					writeNativeValue( child );

					indent();
					List< Boolean > l = new ArrayList<>( hasMore );
					l.add( childrenIt.hasNext() || elementsIt.hasNext() );
					writeChildren( child, l );
					unindent();

					i++;
				}
			}
		}
	}

	private void writeIndented( String s, List< Boolean > hasMore )
		throws IOException {
		for( int i = 0; i < indentation; i++ ) {
			if( hasMore.size() > i && hasMore.get( i ) ) {
				writer.write( '│' );
			}
			writer.write( '\t' );
		}
		writer.write( s );
	}
}
