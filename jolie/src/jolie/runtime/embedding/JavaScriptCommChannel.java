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
import java.util.LinkedList;
import java.util.List;
import javax.script.Invocable;
import javax.script.ScriptException;
import jolie.Interpreter;
import jolie.net.AbstractCommChannel;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.PollableCommChannel;
import jolie.runtime.Value;

/**
 * @TODO this shouldn't be polled
 * @author Fabrizio Montesi
 */
public class JavaScriptCommChannel extends AbstractCommChannel implements PollableCommChannel
{
	private final Invocable invocable;
	private final List< CommMessage > messages = new LinkedList< CommMessage >();

	public JavaScriptCommChannel( Invocable invocable )
	{
		this.invocable = invocable;
	}

	@Override
	public CommChannel createDuplicate()
	{
		return new JavaScriptCommChannel( invocable );
	}

	protected void sendImpl( CommMessage message )
		throws IOException
	{
		Object returnValue = null;
		try {
			returnValue = invocable.invokeFunction( message.operationName(), message.value() );
		} catch( ScriptException e ) {
			throw new IOException( e );
		} catch( NoSuchMethodException e ) {
			throw new IOException( e );
		}
		if ( returnValue != null ) {
			Value value = Value.create();
			value.setValue( returnValue );
			CommMessage response = new CommMessage(
						message.id(),
						message.operationName(),
						message.resourcePath(),
						value,
						null
					);
			synchronized( messages ) {
				messages.add( response );
				messages.notifyAll();
			}
		}
	}

	protected CommMessage recvImpl()
		throws IOException
	{
		CommMessage ret = null;
		synchronized( messages ) {
			while( messages.isEmpty() ) {
				try {
					messages.wait();
				} catch( InterruptedException e ) {}
			}
			ret = messages.remove( 0 );
		}
		if ( ret == null ) {
			throw new IOException( "Unknown exception occurred during communications with a Java Service" );
		}
		return ret;
	}

	@Override
	protected void disposeForInputImpl()
		throws IOException
	{
		Interpreter.getInstance().commCore().registerForPolling( this );
	}

	protected void closeImpl()
	{}

	public boolean isReady()
	{
		return( !messages.isEmpty() );
	}
}
