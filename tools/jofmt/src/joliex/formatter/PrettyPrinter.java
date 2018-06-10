/*
 * Copyright (C) 2011-2018 Fabrizio Montesi <famontesi@gmail.com>
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

package joliex.formatter;

import java.io.IOException;
import java.io.Writer;

/**
 * 
 * @author Fabrizio Montesi
 */
public class PrettyPrinter
{
	private int indentation = 0;
	private final Writer writer;
	private final StringBuilder builder = new StringBuilder();

	public PrettyPrinter( Writer writer )
	{
		this.writer = writer;
	}

	protected void setIndentationLevel( int level )
	{
		this.indentation = level;
	}

	protected int indentationLevel()
	{
		return indentation;
	}

	protected void indent()
	{
		indentation++;
	}

	protected void unindent()
	{
		indentation--;
	}
	
	protected void indented( Runnable runnable )
	{
		indent();
		runnable.run();
		unindent();
	}

	protected void writeIndented( String s )
	{
		for( int i = 0; i < indentation; i++ ) {
			write( "\t" );
		}
		write( s );
	}

	protected void writeLineIndented( String... lines )
	{
		for( String line : lines ) {
			writeIndented( line );
			newLine();
		}
	}
	
	protected void writeLine( String s )
	{
		builder.append( s );
		builder.append( '\n' );
	}

	protected void newLine()
	{
		builder.append( '\n' );
	}

	protected void write( String s )
	{
		builder.append( s );
	}

	protected void flush()
		throws IOException
	{
		writer.write( builder.toString() );
		writer.flush();
	}
}
