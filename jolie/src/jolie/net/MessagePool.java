/*******************************************************************************
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/
package jolie.net;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagePool
{

	private static final long RECEIVER_KEEP_ALIVE = 20000; // msecs

	// TODO: add some cleaning routine to remove outdated mappings
	private final Map< Long, CompletableFuture< CommMessage > > specificMap = new ConcurrentHashMap<>();
	private final Map< String, GenericMessages > genericMap = new ConcurrentHashMap<>();
	private final Map< CommChannel, CommMessage > synchronousResponseMap = new ConcurrentHashMap<>();
	private final Map< Long, String > asynchronousResponseMap = new ConcurrentHashMap<>();



	private class GenericMessages
	{

		public final Queue< CompletableFuture< CommMessage>> responses = new ConcurrentLinkedQueue<>();
		public final Queue< CompletableFuture< CommMessage>> requests = new ConcurrentLinkedQueue<>();
	}

	private GenericMessages getGenericMessages( String operation )
	{
		assert operation != null;
		if ( !genericMap.containsKey( operation ) ) {
			genericMap.put( operation, new GenericMessages() );
		}
		return genericMap.get( operation );
	}

	public CommMessage recvResponseFor( CommMessage request )
		throws IOException
	{

		CompletableFuture< CommMessage> futureResponse = null;
		CommMessage response = null;

		Long id = request.id();
		String operation = request.operationName();

		synchronized( this ) {
			if ( request.hasGenericId() ) {
				if ( operation != null ) {
					GenericMessages gm = getGenericMessages( operation );
					if ( gm.responses.isEmpty() ) {
						futureResponse = new CompletableFuture<>();
						gm.requests.add( futureResponse );
					} else {
						futureResponse = gm.responses.poll();
					}
				} else {
					Logger.getLogger( AbstractCommChannel.class.getName() )
						.log( Level.SEVERE, null,
							new Exception( "Requested reception for generic response without operation. "
								+ "Impossible to handle." ) );
				}

			} else {
				if ( specificMap.containsKey( id ) ) {
					futureResponse = specificMap.remove( id );
				} else {
					if ( operation != null ) {
						GenericMessages gm = getGenericMessages( operation );
						if ( gm.responses.isEmpty() ) {
							futureResponse = new CompletableFuture<>();
							specificMap.put( id, futureResponse );
							gm.requests.add( futureResponse );
						} else {
							futureResponse = gm.responses.poll();
						}
					} else {
						futureResponse = new CompletableFuture<>();
						specificMap.put( id, futureResponse );
					}
				}
			}
		}

		if ( futureResponse != null ) {
			try {

				// DO WE HAVE TO CHANGE THE ID OF A GENERIC RESPONSE TO THE ONE OF THIS REQUEST?
				response = futureResponse.get();
				// if we polled a generic response, we remove the specific request
				synchronized( this ) {
					if ( !request.hasGenericId() ) {
						specificMap.remove( id );
					}
				}
			} catch( InterruptedException | ExecutionException ex ) {
				Logger.getLogger( AbstractCommChannel.class.getName() ).log( Level.SEVERE, null, ex );
			}
		}

		return response;
	}

	protected void receiveResponse( CommMessage response )
	{
		if ( response.hasGenericId() ) {
			handleGenericMessage( response );
		} else {
			handleMessage( response );
		}
	}

	private void handleGenericMessage( CommMessage response )
	{

		String operation = response.operationName();
		CompletableFuture future = null;
		if ( operation == null ) {
			Logger.getLogger( AbstractCommChannel.class.getName() )
				.log( Level.SEVERE, null,
					new Exception( "Requested handling of generic response without operation. "
						+ "Impossible to handle." ) );
		} else {
			synchronized( this ) {
				GenericMessages gm = getGenericMessages( operation );
				if ( gm.requests.isEmpty() ) {
					future = new CompletableFuture();
					gm.responses.add( future );
				} else {
					future = gm.requests.poll();
				}
			}
		}

		if ( future != null ) {
			future.complete( response );
		}
	}

	private void handleMessage( CommMessage response )
	{

		String operation = response.operationName();
		Long id = response.id();
		CompletableFuture< CommMessage> future = null;

		synchronized( this ) {
			if ( specificMap.containsKey( id ) ) {
				if ( !specificMap.get( id ).isDone() ) {
					// if it is not done, the future has been put by a recvResponseFor
					// hence we can remove it and complete it later on
					future = specificMap.remove( id );
				} else {
					// otherwise we have received another message with the same id
					// we consider the last one as valid and proceed to complete 
					// it with the new message
					future = specificMap.get( id );
				}
				if ( operation != null ) {
					// removes related generic request
					getGenericMessages( operation ).requests.remove( future );
				}
			} else {
				future = new CompletableFuture<>();
				specificMap.put( id, future );
			}
		}

		future.complete( response );

	}

	public void registerForSynchronousResponse( CommChannel channel, CommMessage request ) {
		synchronousResponseMap.put( channel, request );
	}
	
	public CommMessage retrieveSynchronousRequest( CommChannel channel ) {
		return synchronousResponseMap.remove( channel );
	}
	
	public void registerForAsynchronousResponse( long id, String operationName )
	{
		asynchronousResponseMap.put( id, operationName );
	}
	 
	public String retrieveAsynchronousRequest( long id )
	{
		return asynchronousResponseMap.remove( id );
	}
	
	
}
