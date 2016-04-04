/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.PollableCommChannel;
import jolie.runtime.InvalidIdException;
import jolie.runtime.JavaService;


// TODO: this should not be polled

/**
 * @author Fabrizio Montesi
 */
public class JavaCommChannel extends CommChannel implements PollableCommChannel
{
	private final JavaService javaService;
	private final Map< Long, CommMessage > messages = new ConcurrentHashMap<>();
	
	public JavaCommChannel( JavaService javaService )
	{
		this.javaService = javaService;
	}

	@Override
	public boolean isReady()
	{
		return messages.isEmpty() == false;
	}

	@Override
	protected void disposeForInputImpl()
		throws IOException
	{
		Interpreter.getInstance().commCore().registerForPolling( this );
	}

	@Override
	public CommChannel createDuplicate()
	{
		return new JavaCommChannel( javaService );
	}

	@Override
	public void send( CommMessage message )
		throws IOException
	{
		sendImpl( message );
	}

	@Override
	protected void sendImpl( CommMessage message )
		throws IOException
	{
		try {
			final CommMessage response = javaService.callOperation( message );
			messages.put( message.id(), response );
		} catch( IllegalAccessException | InvalidIdException e ) {
			throw new IOException( e );
		}
	}

	@Override
	protected CommMessage recvImpl()
		throws IOException
	{
		throw new IOException( "Unsupported operation" );
	}

	@Override
	public CommMessage recvResponseFor( CommMessage request )
		throws IOException
	{
		return messages.remove( request.id() );
	}

	@Override
	protected void closeImpl()
	{}
}
