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
package jolie.net.coap.message;

import jolie.net.coap.message.options.ContentFormat;
import io.netty.buffer.Unpooled;
import java.net.URI;
import java.net.URISyntaxException;
import jolie.net.coap.communication.blockwise.BlockSize;
import jolie.net.coap.message.options.OpaqueOptionValue;

import jolie.net.coap.message.options.Option;
import jolie.net.coap.message.options.OptionValue;
import jolie.net.coap.message.options.StringOptionValue;
import jolie.net.coap.message.options.UintOptionValue;

/**
 * <p>
 * Instances of {@link CoapResponse} are created to answer requests.</p>
 *
 * <p>
 * <b>Note:</b> The given {@link MessageType} (one of {@link MessageType#CON} or
 * {@link MessageType#NON}) may be changed by the framework before it is sent to
 * the other CoAP endpoints. Such a change might e.g. happen if this
 * {@link CoapResponse} was created with {@link MessageType#CON} to answer a
 * {@link CoapRequest} with {@link MessageType#CON} and the framework did not
 * yet send an empty {@link CoapMessage} with {@link MessageType#ACK}. Then the
 * framework will ensure the {@link MessageType} of this {@link CoapResponse} to
 * be set to {@link de.uzl.itm.ncoap.message.MessageType#ACK} to make it a
 * piggy-backed response.</p>
 *
 * @author Oliver Kleine
 */
public class CoapResponse extends CoapMessage {

  private long MODULUS = (long) Math.pow(2, 24);

  /**
   * Creates a new instance of {@link CoapResponse}.
   *
   * @param messageType
   * <p>
   * the number representing the {@link MessageType}</p>
   *
   * <p>
   * <b>Note:</b> the {@link MessageType} might be changed by the framework (see
   * class description).</p>
   *
   * @param messageCode the {@link MessageCode} for this {@link CoapResponse}
   *
   * @throws java.lang.IllegalArgumentException if at least one of the given
   * arguments causes an error
   */
  public CoapResponse(int messageType, int messageCode)
      throws IllegalArgumentException {

    super(messageType, messageCode);
    if (!MessageCode.isResponse(messageCode)) {
      throw new IllegalArgumentException("Message code no."
          + messageCode + " is no response code.");
    }
  }

  /**
   * Sets the {@link jolie.net.coap.message.options.Option#ETAG} of this
   * {@link CoapResponse}.
   *
   * @param etag the byte array that is supposed to represent the ETAG of the
   * content returned by {@link #getContent()}.
   *
   * @throws IllegalArgumentException if the given byte array is invalid to be
   * considered an ETAG
   */
  public void setEtag(byte[] etag) throws IllegalArgumentException {
    this.addOpaqueOption(Option.ETAG, etag);
  }

  /**
   * Returns the byte array representing the ETAG of the content returned by
   * {@link #getContent()}
   *
   * @return the byte array representing the ETAG of the content returned by
   * {@link #getContent()}
   */
  public byte[] getEtag() {
    if (options.containsKey(Option.ETAG)) {
      return ((OpaqueOptionValue) options.get(Option.ETAG).iterator().next()).getDecodedValue();
    } else {
      return null;
    }
  }

  public void setPreferredBlock2Size(BlockSize block2Size) {
    if (BlockSize.UNBOUND == block2Size || block2Size == null) {
      this.removeOptions(Option.BLOCK_2);
    } else {
      this.setBlock2(0, false, block2Size.getSzx());
    }
  }

  /**
   * Sets the BLOCK2 option in this {@link CoapRequest} and returns
   * <code>true</code> if the option is set after method returns (may already
   * have been set beforehand in a prior method invocation) or
   * <code>false</code> if the option is not set, e.g. because that option has
   * no meaning with the message code of this {@link CoapRequest}.
   *
   * @param number The relative number of the block sent or requested
   * @param more Whether more blocks are following;
   * @param szx The block size (can assume values between 0 and 6, the actual
   * block size is then 2^(szx + 4)).
   *
   * @throws IllegalArgumentException if the block number is greater than
   * 1048575 (2^20 - 1)
   */
  public void setBlock2(long number, boolean more, long szx) throws IllegalArgumentException {
    try {
      this.removeOptions(Option.BLOCK_2);
      if (number > 1048575) {
        throw new IllegalArgumentException("Max. BLOCK2NUM is 1048575");
      }
      //long more = ((more) ? 1 : 0) << 3;
      this.addUintOption(Option.BLOCK_2, ((number & 0xFFFFF) << 4) + ((more ? 1 : 0) << 3) + szx);
    } catch (IllegalArgumentException e) {
      this.removeOptions(Option.BLOCK_2);
    }
  }

  public void setBlock1(long number, long szx) throws IllegalArgumentException {
    try {
      this.removeOptions(Option.BLOCK_1);
      if (number > 1048575) {
        throw new IllegalArgumentException("Max. BLOCK1NUM is 1048575");
      }
      //long more = ((more) ? 1 : 0) << 3;
      this.addUintOption(Option.BLOCK_1, ((number & 0xFFFFF) << 4) + (1 << 3) + szx);
    } catch (IllegalArgumentException e) {
      this.removeOptions(Option.BLOCK_1);
    }
  }

  /**
   * Sets the observe option to a proper value automatically. This method is to
   * be invoked if an inbound {@link CoapRequest} to start a new observation is
   * accepted.
   */
  public void setObserve() {
    this.setObserve(System.currentTimeMillis() % MODULUS);
  }

  /**
   * Returns <code>true</code> if this {@link CoapResponse} is an update
   * notification and <code>false</code> otherwise. A {@link CoapResponse} is
   * considered an update notification if the invocation of
   * {@link #getObserve()} returns a value other than <code>null</code>.
   *
   * @return <code>true</code> if this {@link CoapResponse} is an update
   * notification and <code>false</code> otherwise.
   */
  public boolean isUpdateNotification() {
    return this.getObserve() != UintOptionValue.UNDEFINED;
  }

  /**
   * Adds all necessary location URI related options to the list. This causes
   * eventually already contained location URI related options to be removed
   * from the list even in case of an exception.
   *
   * @param locationURI The location URI of the newly created resource. The
   * parts scheme, host, and port are ignored anyway and thus may not be
   * included in the URI object
   *
   * @throws java.lang.IllegalArgumentException if at least one of the options
   * to be added is not valid. Previously to throwing the exception possibly
   * contained options of {@link Option#LOCATION_PATH} and
   * {@link Option#LOCATION_QUERY} are removed from this {@link CoapResponse}.
   */
  public void setLocationURI(URI locationURI)
      throws IllegalArgumentException {

    options.remove(Option.LOCATION_PATH);
    options.remove(Option.LOCATION_QUERY);

    String locationPath = locationURI.getRawPath();
    String locationQuery = locationURI.getRawQuery();

    try {
      if (locationPath != null) {
        //Path must not start with "/" to be further processed
        if (locationPath.startsWith("/")) {
          locationPath = locationPath.substring(1);
        }

        for (String pathComponent : locationPath.split("/")) {
          this.addStringOption(Option.LOCATION_PATH, pathComponent);
        }
      }

      if (locationQuery != null) {
        for (String queryComponent : locationQuery.split("&")) {
          this.addStringOption(Option.LOCATION_QUERY, queryComponent);
        }
      }
    } catch (IllegalArgumentException ex) {
      options.remove(Option.LOCATION_PATH);
      options.remove(Option.LOCATION_QUERY);
      throw ex;
    }
  }

  /**
   * Returns the URI reconstructed from the location URI related options
   * contained in the message
   *
   * @return the URI reconstructed from the location URI related options
   * contained in the message or null if there are no location URI related
   * options
   *
   * @throws java.net.URISyntaxException if the URI to be reconstructed from
   * options is invalid
   */
  public URI getLocationURI() throws URISyntaxException {

    //Reconstruct path
    String locationPath = "";

    if (options.containsKey(Option.LOCATION_PATH)) {
      locationPath = ((StringOptionValue) options.get(Option.LOCATION_PATH))
          .getDecodedValue();
    }

    //Reconstruct query
    String locationQuery = "";

    if (options.containsKey(Option.LOCATION_QUERY)) {
      locationQuery = ((StringOptionValue) options.get(Option.LOCATION_QUERY))
          .getDecodedValue();
    }

    if (locationPath.length() == 0 && locationQuery.length() == 0) {
      return null;
    }

    return new URI(null, null, null, (int) UintOptionValue.UNDEFINED,
        locationPath, locationQuery, null);
  }

  /**
   * Sets the Max-Age option of this {@link CoapResponse}. If there was a
   * Max-Age * option set prior to the invocation of this method, the previous
   * value is overwritten.
   *
   * @param maxAge the value for the Max-Age option to be set
   */
  public void setMaxAge(long maxAge) {
    try {
      this.options.remove(Option.MAX_AGE);
      this.addUintOption(Option.MAX_AGE, maxAge);
    } catch (IllegalArgumentException e) {
    }
  }

  /**
   * Returns the value of the Max-Age option of this {@link CoapResponse}. If no
   * such option exists, this method returns
   * {@link OptionValue#MAX_AGE_DEFAULT}.
   *
   * @return the value of the Max-Age option of this {@link CoapResponse}. If no
   * such option exists, this method returns
   * {@link OptionValue#MAX_AGE_DEFAULT}.
   */
  public long getMaxAge() {
    if (options.containsKey(Option.MAX_AGE)) {
      return ((UintOptionValue) options.get(Option.MAX_AGE)).getDecodedValue();
    } else {
      return OptionValue.MAX_AGE_DEFAULT;
    }
  }

  /**
   * Creates a new instance of {@link CoapResponse} with
   * {@link MessageCode#INTERNAL_SERVER_ERROR_500} and the stacktrace of the
   * given {@link Throwable} as payload (this is particularly useful for
   * debugging). Basically, this can be considered a shortcut to create error
   * responses.
   *
   * @param messageType
   * <p>
   * the {@link MessageType} (one of {@link MessageType#CON} or
   * {@link MessageType#NON}).</p>
   *
   * <p>
   * <b>Note:</b> the {@link MessageType} might be changed by the framework (see
   * class description).</p>
   * @param messageCode the {@link MessageCode} for this {@link CoapResponse}
   *
   * @return a new instance of {@link CoapResponse} with the
   * {@link Throwable#getMessage} as content (payload).
   *
   * @throws java.lang.IllegalArgumentException if at least one of the given
   * arguments causes an error
   */
  public static CoapResponse createErrorResponse(int messageType,
      int messageCode, String content)
      throws IllegalArgumentException {

    if (!MessageCode.isErrorMessage(messageCode)) {
      throw new IllegalArgumentException(String.format("Code no. %s is "
          + "no error code!", MessageCode.asString(messageCode)));
    }

    CoapResponse errorResponse = new CoapResponse(messageType, messageCode);
    errorResponse.setContent(Unpooled.wrappedBuffer(
        content.getBytes(CoapMessage.CHARSET)), ContentFormat.TEXT_PLAIN_UTF8);

    return errorResponse;
  }
}
