/*******************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>              *
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
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

public abstract class AbstractCommChannel extends CommChannel {

	private static final long RECEIVER_KEEP_ALIVE = 20000; // msecs

	// TODO: add some cleaning routine to remove outdated mappings
	private final Map< Long, CompletableFuture< CommMessage>> specificMap = new ConcurrentHashMap<>();
	private final Map< String, GenericMessages> genericMap = new ConcurrentHashMap<>();

	private class GenericMessages {

		public final Queue< CompletableFuture< CommMessage>> responses = new ConcurrentLinkedQueue<>();
		public final Queue< CompletableFuture< CommMessage>> requests = new ConcurrentLinkedQueue<>();
	}

	private GenericMessages getGenericMessages( String operation ) {
		assert operation != null;
		if ( !genericMap.containsKey( operation ) ) {
			genericMap.put( operation, new GenericMessages() );
		}
		return genericMap.get( operation );
	}

	@Override
	public CommMessage recvResponseFor( CommMessage request )
		throws IOException {

	    try {
		Thread.sleep(2000);
	    } catch (InterruptedException ex) {
		Logger.getLogger(AbstractCommChannel.class.getName()).log(Level.SEVERE, null, ex);
	    }
	    System.out.println("Request Response for: " + request.operationName() + " " + request.id());

		CompletableFuture< CommMessage> futureResponse = null;
		CommMessage response = null;

		Long id = request.id();
		String operation = request.operationName();

		synchronized ( this ) {
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
			    System.out.println("Found response " + response.operationName() + " " + response.id());
				// if we polled a generic response, we remove the specific request
				synchronized ( this ) {
					if ( !request.hasGenericId() ) {
						specificMap.remove( id );
					}
				}
			} catch ( InterruptedException | ExecutionException ex ) {
				Logger.getLogger( AbstractCommChannel.class.getName() ).log( Level.SEVERE, null, ex );
			}
		}

		return response;
	}

    protected void receiveResponse(CommMessage response) {
	System.out.println("Received response: " + response.operationName() + " " + response.id());
		if ( response.hasGenericId() ) {
			handleGenericMessage( response );
		} else {
			handleMessage( response );
		}
	}

	private void handleGenericMessage( CommMessage response ) {

		String operation = response.operationName();
		CompletableFuture future = null;
		if ( operation == null ) {
			Logger.getLogger( AbstractCommChannel.class.getName() )
				.log( Level.SEVERE, null,
					new Exception( "Requested handling of generic response without operation. "
						+ "Impossible to handle." ) );
		} else {
			synchronized ( this ) {
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

	private void handleMessage( CommMessage response ) {

		String operation = response.operationName();
		Long id = response.id();
		CompletableFuture< CommMessage> future = null;

		synchronized ( this ) {
			if ( specificMap.containsKey( id ) ) {
				future = specificMap.remove( id );
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

//    private void throwIOExceptionFault( IOException e ) {
//        System.out.println( "throwIOException " + e.getMessage() );
//        if ( !waiters.isEmpty() ) {
//            ResponseContainer monitor;
//            for ( Entry< Long, ResponseContainer> entry : waiters.entrySet() ) {
//                monitor = entry.getValue();
//                synchronized ( monitor ) {
//                    monitor.response = new CommMessage(
//                      entry.getKey(),
//                      "",
//                      Constants.ROOT_RESOURCE_PATH,
//                      Value.create(),
//                      new FaultException( "IOException", e )
//                    );
//                    monitor.notify();
//                }
//            }
//        }
//        waiters.clear();
//    }
}
