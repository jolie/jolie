/***************************************************************************
 *   Copyright (C) 2014 by Claudio Guidi <cguidi@italianasoftware.com>     *
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



package jolie.tracer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.function.Supplier;
import jolie.Interpreter;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;

/**
 *
 * @author Claudio Guidi
 * 15 Sep 2014 - Fabrizio Montesi: Updated interface
 */
public class PrintingTracer implements Tracer
{
	private int actionCounter = 0;
	private final Interpreter interpreter;
	
	public PrintingTracer( Interpreter interpreter )
	{
		this.interpreter = interpreter;
	}

	@Override
	public synchronized void trace( Supplier< ? extends TraceAction > supplier )
	{
		final TraceAction action = supplier.get();
		actionCounter++;
		if ( action instanceof MessageTraceAction ) {
			trace( (MessageTraceAction) action );
		} else if ( action instanceof EmbeddingTraceAction ) {
			trace( (EmbeddingTraceAction) action );
		}
	}
	
	private void trace( EmbeddingTraceAction action )
	{
		StringBuilder stBuilder = new StringBuilder();
		stBuilder.append( interpreter.logPrefix() ).append( "\t" );
		stBuilder.append( Integer.toString( actionCounter ) ).append( ".\t" );
		switch( action.type() ) {
			case SERVICE_LOAD:
				stBuilder.append( "^ LOAD" );
				break;
			default:
				break;
		}
		stBuilder
			.append( "\t" ).append( action.name() )
			.append( "\t\t\t" ).append( action.description() );
		System.out.println( stBuilder.toString() );
	}
	
	private void trace( MessageTraceAction action )
	{
		StringBuilder stBuilder = new StringBuilder();
		stBuilder.append( interpreter.logPrefix() ).append( "\t" );
		stBuilder.append( Integer.toString( actionCounter ) ).append( ".\t" );
		switch( action.type() ) {
			case SOLICIT_RESPONSE:
				stBuilder.append( "<< SR" );
				break;
			case NOTIFICATION:
				stBuilder.append( "< N" );
				break;
			case ONE_WAY:
				stBuilder.append( "> OW" );
				break;
			case REQUEST_RESPONSE:
				stBuilder.append( ">> RR" );
				break;
			case COURIER_NOTIFICATION:
				stBuilder.append( ">> CN" );
				break;
			case COURIER_SOLICIT_RESPONSE:
				stBuilder.append( ">> CSR" );
				break;
			default:
				break;
		}
		stBuilder
			.append( "\t" ).append( action.name() )
			.append( "\t\t\t" ).append( action.description() );
		if ( action.message() != null ) {
			stBuilder.append( "\tMSG_ID:" ).append( action.message().id() ).append( "\n" );
			Writer writer = new StringWriter();
			Value messageValue = action.message().value();
			if ( action.message().isFault() ) {
				messageValue = action.message().fault().value();
			}
			ValuePrettyPrinter printer = new ValuePrettyPrinter(
				messageValue,
				writer,
				"Value:"
			);
			printer.setByteTruncation( 50 );
			printer.setIndentationOffset( 6 );
			try {
				printer.run();
			} catch( IOException e ) {} // Should never happen
			stBuilder.append( writer.toString() );
		}
		System.out.println( stBuilder.toString() );
	}
}
