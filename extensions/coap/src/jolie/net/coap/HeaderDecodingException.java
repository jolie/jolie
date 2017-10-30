/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *                                                                             
 *   This program is free software; you can redistribute it and/or modify      
 *   it under the terms of the GNU Library General Public License as           
 *   published by the Free Software Foundation; either version 2 of the        
 *   License, or (at your option) any later version.                           
 *                                                                             
 *   This program is distributed in the hope that it will be useful,           
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             
 *   GNU General Public License for more details.                              
 *                                                                             
 *   You should have received a copy of the GNU Library General Public         
 *   License along with this program; if not, write to the                     
 *   Free Software Foundation, Inc.,                                           
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 
 *                                                                             
 *   For details about the authors of this software, see the AUTHORS file.     
 */
package jolie.net.coap;

import java.net.InetSocketAddress;

/**
 * An {@link HeaderDecodingException} indicates that the header, i.e. the first
 * 4 bytes of an inbound serialized {@link CoapMessage} are malformed. This
 * exception is thrown during the decoding process and causes an RST message to
 * be sent to the inbound message origin.
 *
 * @author Oliver Kleine
 */
public class HeaderDecodingException extends Exception {

  private int messageID;
  private InetSocketAddress remoteSocket;

  /**
   * Creates a new instance of {@link HeaderDecodingException}.
   *
   * @param messageID the message ID of the message that caused
   * @param remoteSocket the malformed message origin
   */
  public HeaderDecodingException(int messageID, InetSocketAddress remoteSocket,
      String message) {
    super(message);
    this.messageID = messageID;
    this.remoteSocket = remoteSocket;
  }

  /**
   * Returns the message ID of the inbound malformed message
   *
   * @return the message ID of the inbound malformed message
   */
  public int getMessageID() {
    return messageID;
  }

  /**
   * Returns the malformed inbound messages origin CoAP endpoints
   *
   * @return the malformed inbound messages origin CoAP endpoints
   */
  public InetSocketAddress getremoteSocket() {
    return remoteSocket;
  }
}
