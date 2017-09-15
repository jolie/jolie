/*****************************************************************************
 * Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                           *
 *   This program is free software; you can redistribute it and/or modify    *
 *   it under the terms of the GNU Library General Public License as         *  
 *   published by the Free Software Foundation; either version 2 of the      *
 *   License, or (at your option) any later version.                         *
 *                                                                           *
 *   This program is distributed in the hope that it will be useful,         *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 *   GNU General Public License for more details.                            *
 *                                                                           *
 *   You should have received a copy of the GNU Library General Public       *
 *   License along with this program; if not, write to the                   *
 *   Free Software Foundation, Inc.,                                         *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.               *
 *                                                                           *
 *   For details about the authors of this software, see the AUTHORS file.   *
 *****************************************************************************/
package jolie.net;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.net.protocols.CommProtocol;

public class PubSubCommChannel extends StreamingCommChannel {

  private final CommChannel channel;
  private final Map< Long, CompletableFuture<Void>> sendRelease;

  public PubSubCommChannel( URI location, CommProtocol protocol, CommChannel channel, Map sendRelease )
      throws IOException {
    super( location, protocol );
    this.channel = channel;
    this.sendRelease = sendRelease;
  }

  public PubSubCommChannel( CommChannel channel, Map sendRelease ) {
    super( null, null );
    this.channel = channel;
    this.sendRelease = sendRelease;
  }

  @Override
  protected void sendImpl( CommMessage message )
      throws IOException {
    CompletableFuture<Void> cf = new CompletableFuture<>();
    sendRelease.put( message.id(), cf );
    channel.send( message );
    try {
      cf.get();
    } catch ( InterruptedException | ExecutionException ex ) {
      Logger.getLogger( PubSubCommChannel.class.getName() ).log( Level.SEVERE, null, ex );
    }
  }

  @Override
  public void sendRelease( long id ) {
    if ( sendRelease.containsKey( id ) ) {
      sendRelease.get( id ).complete( null );
      sendRelease.remove( id );
    } else {
      Logger.getLogger(
          PubSubCommChannel.class.getName()
      ).log( Level.SEVERE, sendRelease.toString(),
          new IOException( "Tried to remove missing future " + id )
      );
    }
  }

  @Override
  protected CommMessage recvImpl()
      throws IOException {
    System.out.println( "Requiring the reception of a message" );
    return CommMessage.UNDEFINED_MESSAGE;
  }

  @Override
  public StreamingCommChannel createWithSideChannel( CommChannel channel ) {
    Map< Long, CompletableFuture<Void>> subPubSendRelease = new ConcurrentHashMap<>();
    return new PubSubCommChannel( channel, subPubSendRelease );
  }

  @Override
  protected void closeImpl()
      throws IOException {
    System.out.println( "Received close request" );
  }

  public synchronized boolean isReady()
      throws IOException {
    System.out.println( "Return ready when the forwarding channel is ready" );
    return true;
  }

}
