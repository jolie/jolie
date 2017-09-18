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
  private final Map< String, MessageContainer> MCS = new ConcurrentHashMap<>();  // MessageContainer Storage

  private class MessageContainer {

    final Queue< CompletableFuture< CommMessage>> generic_FromReception;
    final Queue< CompletableFuture< CommMessage>> generic_FromRequest;
    final Map< Long, CompletableFuture< CommMessage>> directMap;

    public MessageContainer() {
      generic_FromReception = new ConcurrentLinkedQueue<>();
      generic_FromRequest = new ConcurrentLinkedQueue<>();
      directMap = new ConcurrentHashMap<>();
    }

    protected boolean hasDirectFuture( Long id ) {
      return directMap.containsKey( id );
    }

    protected CompletableFuture< CommMessage> getDirectFuture( Long id ) {
      return directMap.get( id );
    }

    private void addDirectFuture( Long id, CompletableFuture<CommMessage> f ) {
      directMap.put( id, f );
    }

    private boolean hasGenericReceptionFuture() {
      return !generic_FromReception.isEmpty();
    }

    private CompletableFuture<CommMessage> getGenericReceptionFuture() {
      return generic_FromReception.peek();
    }

    private void addGenericReceptionFuture( CompletableFuture<CommMessage> f ) {
      generic_FromReception.add( f );
    }

    private void addGenericRequestFuture( CompletableFuture<CommMessage> f ) {
      generic_FromRequest.add( f );
    }

    private boolean hasGenericRequestFuture() {
      return !generic_FromRequest.isEmpty();
    }

    private CompletableFuture<CommMessage> getGenericRequestFuture() {
      return generic_FromRequest.peek();
    }

    private void removeFuture( Long id, CompletableFuture<CommMessage> f ) {
      directMap.remove( id );
      generic_FromReception.remove( f );
      generic_FromRequest.remove( f );
    }

    private void removeFuture( CompletableFuture<CommMessage> f ) {
      generic_FromReception.remove( f );
      generic_FromRequest.remove( f );
    }

  }

  private MessageContainer retrieveMessageContainer( String operation ) {
    // we check if the operation of the request is already indexed into the MCS,
    // if not, we add it
    synchronized ( MCS ) {
      if ( !MCS.containsKey( operation ) ) {
        MessageContainer mc = new MessageContainer();
        MCS.put( operation, mc );
        return mc;
      } else {
        return MCS.get( operation );
      }
    }
  }

  @Override
  public CommMessage recvResponseFor( CommMessage request )
      throws IOException {

    String operation = request.operationName();
    Long id = request.id();
    MessageContainer mc = retrieveMessageContainer( operation );
    CompletableFuture< CommMessage> futureResponse;
    CommMessage response = null;

    synchronized ( mc ) {
      // we check if there is already a response for this request: 
      // first by ID, 
      if ( mc.hasDirectFuture( id ) ) {
        // we get it
        futureResponse = mc.getDirectFuture( id );
        // and we remove it from the storage
        mc.removeFuture( id, futureResponse );
      } // then in generic receptions
      else if ( mc.hasGenericReceptionFuture() ) {
        // we get it
        futureResponse = mc.getGenericReceptionFuture();
        // and we remove it from the storage
        mc.removeFuture( id, futureResponse );
      } // if no response arrived yet, we set up the future
      else {
        futureResponse = new CompletableFuture<>();
        mc.addDirectFuture( id, futureResponse );
        mc.addGenericRequestFuture( futureResponse );
      }
    }

    try {
      // DO WE HAVE TO CHANGE THE ID OF A GENERIC RESPONSE TO THE ONE OF THIS REQUEST?
      response = futureResponse.get();

    } catch ( InterruptedException | ExecutionException ex ) {
      Logger.getLogger( AbstractCommChannel.class.getName() ).log( Level.SEVERE, null, ex );
    }

    return response;
  }

  protected void receiveResponse( CommMessage response ) {

    if ( response.hasGenericId() ) {
      handleGenericMessage( response );
    } else {
      handleMessage( response );
    }
  }

  private void handleGenericMessage( CommMessage response ) {

    String operation = response.operationName();
    MessageContainer mc = retrieveMessageContainer( operation );
    CompletableFuture< CommMessage> future;

    synchronized ( mc ) {
      // if a request is already in the storage, we complete it
      if ( mc.hasGenericRequestFuture() ) {
        future = mc.getGenericRequestFuture();
        mc.removeFuture( future );
      } // else we add it in the received generic responses
      else {
        future = new CompletableFuture<>();
        mc.addGenericReceptionFuture( future );
      }
    }

    future.complete( response );
    
  }

  private void handleMessage( CommMessage response ) {

    String operation = response.operationName();
    Long id = response.id();
    MessageContainer mc = retrieveMessageContainer( operation );
    CompletableFuture< CommMessage> future;

    synchronized ( mc ) {
      // if a request is already in the storage, we complete it
      if ( mc.hasDirectFuture( id ) ) {
        future = mc.getDirectFuture( id );
        mc.removeFuture( future );
      } // else we add it in the received generic responses
      else {
        future = new CompletableFuture<>();
        mc.addDirectFuture( id, future );
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
