/***************************************************************************
 *   Copyright (C) 2008-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.runtime.embedding.js;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.Invocable;
import javax.script.ScriptException;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.PollableCommChannel;
import jolie.runtime.Value;

/**
 * @author Fabrizio Montesi
 * 
 * TODO: this shouldn't be polled
 */
public class JavaScriptCommChannel extends CommChannel implements PollableCommChannel
{
	private final Invocable invocable;
	private final Map< Long, CommMessage > messages = new ConcurrentHashMap< Long, CommMessage >();

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
		CommMessage response;
		if ( returnValue != null ) {
			Value value = Value.create();
			
			if ( returnValue instanceof Value ) {
				value.refCopy( (Value)returnValue );
			} else {
				value.setValue( returnValue );
			}
			
			response = new CommMessage(
				message.id(),
				message.operationName(),
				message.resourcePath(),
				value,
				null
			);
		} else {
			response = CommMessage.createEmptyResponse( message );
		}
		
		messages.put( message.id(), response );
	}
	
	protected CommMessage recvImpl()
		throws IOException
	{
		throw new IOException( "Unsupported operation" );
	}

	/* protected CommMessage recvImpl()
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
	*/
	
	@Override
	public CommMessage recvResponseFor( CommMessage request )
		throws IOException
	{
		return messages.remove( request.id() );
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
