/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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
import java.util.Map.Entry;

/**
 * Pretty printer for {@link Value} objects.
 * This class is not thread safe.
 * @author Fabrizio Montesi
 */
public class ValuePrettyPrinter
{
	private final Value root;
	private final Writer writer;
	private final String header;
	private int indentation = 0;
	private int byteTruncation = -1; // Default: no truncation

	public ValuePrettyPrinter( Value root, Writer writer, String header )
	{
		this.root = root;
		this.writer = writer;
		this.header = header;
	}

	private void indent()
	{
		indentation++;
	}

	private void unindent()
	{
		indentation--;
	}
	
	public void setByteTruncation( int truncation )
	{
		this.byteTruncation = truncation;
	}

	public void run()
		throws IOException
	{
		writeIndented( header );
		writeNativeValue( root );
		indent();
		writeChildren( root );
		unindent();
	}
	
	public void setIndentationOffset( int offset )
	{
		this.indentation = offset;
	}

	private void writeNativeValue( Value value )
		throws IOException
	{
		if ( value.isUsedInCorrelation() ) {
			writer.write( " (cset)" );
		}
		if ( value.valueObject() != null ) {
			writer.write( " = " );
			if ( value.valueObject() instanceof ByteArray && byteTruncation > -1 ) {
				String s = value.byteArrayValue().toString();
				writer.write( s.substring( 0, Math.min( byteTruncation, s.length() ) ) + "..." );
			} else {
				writer.write( value.valueObject().toString() );
			}
			writer.write( " : " );
			writer.write( value.valueObject().getClass().getName() );
		}
		writer.write( '\n' );
	}

	private void writeChildren( Value value )
		throws IOException
	{
		Integer i;
		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			if ( entry.getValue().isEmpty() ) {
				writeIndented( "." );
				writer.write( entry.getKey() );
				writer.write( " (empty)" );
			} else {
				i = 0;
				for( Value child : entry.getValue() ) {
					writeIndented( "." );
					writer.write( entry.getKey() );
					writer.write( '[' );
					writer.write( i.toString() );
					writer.write( ']' );
					writeNativeValue( child );
					indent();
					writeChildren( child );
					unindent();
					i++;
				}
			}
		}
	}

	private void writeIndented( String s )
		throws IOException
	{
		for( int i = 0; i < indentation; i++ ) {
			writer.write( '\t' );
		}
		writer.write( s );
	}
}
