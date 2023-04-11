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

package jolie.net;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

public abstract class AbstractCommChannel extends CommChannel {
	private final Map< Long, CommMessage > pendingResponses = new HashMap<>();
	private final List< CommMessage > pendingGenericResponses = new LinkedList<>();
	private final Map< Long, CompletableFuture< CommMessage > > waiters = new HashMap<>();
	private ResponseReceiver responseReceiver = null;
	private final Object responseRecvMutex = new Object();

	private Optional< CommMessage > removeResponseFromCache( CommMessage request ) {
		final Optional< CommMessage > result;

		var response = pendingResponses.remove( request.requestId() );
		if( response != null ) {
			result = Optional.of( response );
		} else if( !pendingGenericResponses.isEmpty() ) {
			result = Optional.of( pendingGenericResponses.remove( 0 ) );
		} else {
			result = Optional.empty();
		}

		return result;
	}

	private void startResponseReceiver() {
		final var ethread = ExecutionThread.currentThread();
		if( responseReceiver == null ) {
			// If there is no receiver yet, start a new one
			responseReceiver = new ResponseReceiver( this, ethread );
			ethread.interpreter().commCore().startCommChannelHandler( responseReceiver );
		} else {
			// If there is already a receiver running, reset its timeout
			responseReceiver.resetTimeout();
		}
	}

	@Override
	public Future< CommMessage > recvResponseFor( CommMessage request )
		throws IOException {
		synchronized( responseRecvMutex ) {
			var optResponse = removeResponseFromCache( request );
			if( optResponse.isPresent() ) {
				return CompletableFuture.completedFuture( optResponse.get() );
			} else {
				assert !waiters.containsKey( request.requestId() );
				var responseFuture = new CompletableFuture< CommMessage >();
				waiters.put( request.requestId(), responseFuture );

				startResponseReceiver();

				return responseFuture;
			}
		}
	}

	private static class ResponseReceiver implements Runnable {
		private final AbstractCommChannel parent;
		private final ExecutionThread ethread;
		private boolean keepRun;
		private Future< ? > timeoutHandler;

		private ResponseReceiver( AbstractCommChannel parent, ExecutionThread ethread ) {
			this.ethread = ethread;
			this.parent = parent;
			this.keepRun = true;
			this.timeoutHandler = null;
		}

		// Requires: synchronized on parent.responseRecvMutex
		private void resetTimeout() {
			if( timeoutHandler != null ) {
				timeoutHandler.cancel( false ); // TODO: What if this fails?
			}
			timeoutHandler = ethread.interpreter().schedule( this::timeout, ethread.interpreter().responseTimeout() );
		}

		private void timeout() {
			synchronized( parent.responseRecvMutex ) {
				parent.waiters.forEach( ( requestId, messageFuture ) -> {
					messageFuture.completeExceptionally( new TimeoutException( "Channel timed out" ) );
				} );
				parent.waiters.clear();
				try {
					parent.close();
				} catch( IOException e ) {
					Interpreter.getInstance().logWarning( e );
				}
			}
		}

		private void handleGenericMessage( CommMessage response ) {
			if( parent.waiters.isEmpty() ) {
				parent.pendingGenericResponses.add( response );
			} else {
				var entry =
					parent.waiters.entrySet().iterator().next();
				var responseFuture = entry.getValue();
				parent.waiters.remove( entry.getKey() );
				responseFuture.complete(
					new CommMessage(
						entry.getKey(),
						response.operationName(),
						response.resourcePath(),
						response.value(),
						response.fault() ) );
			}
		}

		private void handleMessage( CommMessage response ) {
			var responseFuture = parent.waiters.remove( response.requestId() );
			if( responseFuture != null ) {
				responseFuture.complete( response );
			} else {
				parent.pendingResponses.put( response.requestId(), response );
			}
		}

		private void throwIOExceptionFault( IOException e ) {
			for( var entry : parent.waiters.entrySet() ) {
				entry.getValue().complete( new CommMessage(
					entry.getKey(),
					"",
					Constants.ROOT_RESOURCE_PATH,
					Value.create(),
					new FaultException( "IOException", e ) ) );
			}
			parent.waiters.clear();
		}

		@Override
		public void run() {
			/*
			 * Warning: the following line implies that this whole thing is safe iff the CommChannel is used
			 * only for outputs, otherwise we are messing with correlation set checking.
			 */
			CommChannelHandler.currentThread().setExecutionThread( ethread ); // TODO: this is hacky..

			CommMessage response;
			while( keepRun ) {
				synchronized( parent.responseRecvMutex ) {
					resetTimeout();
				}
				try {
					response = parent.recv();
					if( response != null ) {
						synchronized( parent.responseRecvMutex ) {
							if( response.hasGenericRequestId() ) {
								handleGenericMessage( response );
							} else {
								handleMessage( response );
							}
						}
					}
					synchronized( parent.responseRecvMutex ) {
						if( parent.waiters.isEmpty() ) {
							keepRun = false;
							parent.responseReceiver = null;
							timeoutHandler.cancel( false );
						}
					}
				} catch( IOException e ) {
					synchronized( parent.responseRecvMutex ) {
						throwIOExceptionFault( e );
						keepRun = false;
						parent.responseReceiver = null;
						timeoutHandler.cancel( false );
					}
					// TODO: close the channel?
				}
			}
		}
	}
}
