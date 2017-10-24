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

import java.net.URI;
import java.net.URISyntaxException;

import static jolie.net.coap.message.CoapMessage.UNDEFINED_MESSAGE_ID;
import jolie.net.coap.options.Option;
import jolie.net.coap.options.OptionValue;
import jolie.net.coap.options.StringOptionValue;
import jolie.net.coap.options.UintOptionValue;

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

    super(messageType, messageCode, UNDEFINED_MESSAGE_ID,
        new Token(new byte[0]));
    if (!MessageCode.isResponse(messageCode)) {
      throw new IllegalArgumentException("Message code no."
          + messageCode + " is no response code.");
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
}
