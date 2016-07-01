/**
 * *************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com> * * This program
 * is free software; you can redistribute it and/or modify * it under the terms
 * of the GNU Library General Public License as * published by the Free Software
 * Foundation; either version 2 of the * License, or (at your option) any later
 * version. * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General
 * Public License for more details. * * You should have received a copy of the
 * GNU Library General Public * License along with this program; if not, write
 * to the * Free Software Foundation, Inc., * 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 * *************************************************************************
 */
package jolie.net;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

public abstract class AbstractCommChannel extends CommChannel
{
	private static final long RECEIVER_KEEP_ALIVE = 20000; // msecs

	private final Map< Long, CommMessage> pendingResponses = new HashMap<>();
	private final Map< Long, ResponseContainer> waiters = new HashMap<>();
	private final List< CommMessage> pendingGenericResponses = new LinkedList<>();

	private final Object responseRecvMutex = new Object();

	private static class ResponseContainer
	{
		private ResponseContainer()
		{
		}
		private CommMessage response = null;
	}

	@Override
	public CommMessage recvResponseFor( CommMessage request )
		throws IOException
	{
		CommMessage response;
		ResponseContainer monitor = null;
		synchronized( responseRecvMutex ) {
			response = pendingResponses.remove( request.id() );
			if ( response == null ) {
				if ( pendingGenericResponses.isEmpty() ) {
					assert (waiters.containsKey( request.id() ) == false);
					monitor = new ResponseContainer();
					waiters.put( request.id(), monitor );
					//responseRecvMutex.notify();
				} else {
					response = pendingGenericResponses.remove( 0 );
				}
			}
		}
		if ( response == null ) {
			synchronized( monitor ) {
				if ( monitor.response == null ) {
					try {
						monitor.wait();
					} catch( InterruptedException e ) {
						Interpreter.getInstance().logSevere( e );
					}
				}
				response = monitor.response;
			}
		}
		return response;
	}

	protected void recievedResponse( CommMessage response )
	{
		if ( response.hasGenericId() ) {
			handleGenericMessage( response );
		} else {
			handleMessage( response );
		}
	}

	private void handleGenericMessage( CommMessage response )
	{
		ResponseContainer monitor;
		if ( waiters.isEmpty() ) {
			pendingGenericResponses.add( response );
		} else {
			Entry< Long, ResponseContainer> entry
				= waiters.entrySet().iterator().next();
			monitor = entry.getValue();
			waiters.remove( entry.getKey() );
			synchronized( monitor ) {
				monitor.response = new CommMessage(
					entry.getKey(),
					response.operationName(),
					response.resourcePath(),
					response.value(),
					response.fault()
				);
				monitor.notify();
			}
		}
	}

	private void handleMessage( CommMessage response )
	{
		ResponseContainer monitor;
		if ( (monitor = waiters.remove( response.id() )) == null ) {
			pendingResponses.put( response.id(), response );
		} else {
			synchronized( monitor ) {
				monitor.response = response;
				monitor.notify();
			}
		}
	}

	private void throwIOExceptionFault( IOException e )
	{
		if ( waiters.isEmpty() == false ) {
			ResponseContainer monitor;
			for( Entry< Long, ResponseContainer> entry : waiters.entrySet() ) {
				monitor = entry.getValue();
				synchronized( monitor ) {
					monitor.response = new CommMessage(
						entry.getKey(),
						"",
						Constants.ROOT_RESOURCE_PATH,
						Value.create(),
						new FaultException( "IOException", e )
					);
					monitor.notify();
				}
			}
			waiters.clear();
		}
	}
}
