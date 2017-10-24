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

import java.net.*;
import jolie.net.coap.options.Option;
import jolie.net.coap.options.OptionValue;
import jolie.net.coap.options.StringOptionValue;
import jolie.net.coap.options.UintOptionValue;

/**
 * Instances of {@link CoapRequest} are created by {@link CoapClient}s to
 * request remote CoAP resource.
 *
 * @author Oliver Kleine
 */
public class CoapRequest extends CoapMessage {

  private static final String NO_REQUEST_TYPE = "Message type %d is not a "
      + "suitable type for requests (only CON and NON)!";
  private static final String NO_REQUEST_CODE = "Message code %d is "
      + "not a request code!";
  private static final String URI_SCHEME = "URI scheme must be set "
      + "to \"coap\" (but given URI is: %s)!";
  private static final String URI_FRAGMENT = "URI must not have a "
      + "fragment (but given URI is: %s)!";

  /**
   * Creates a {@link CoapRequest} instance with initial CoAP header and options
   * according to the given parameters.
   *
   * @param messageType the number representing the message type for the
   * {@link CoapRequest}.
   * @param messageCode the number representing the message code for the
   * {@link CoapRequest}.
   * @param targetUri the {@link URI} representing the webresource this
   * {@link CoapRequest} is to be sent to.
   * @param useProxy indicates if this {@link CoapRequest} is supposed to be
   * sent to its final destination via a forward-proxy (if set to
   * <code>true</code> the given target URI is set as {@link Option#PROXY_URI},
   * if set to <code>false</code> the given target URI is set as combination of
   * {@link Option#URI_HOST}, {@link Option#URI_PORT}, {@link Option#URI_PATH},
   * and {@link Option#URI_QUERY}.
   *
   * @throws java.lang.IllegalArgumentException if at least one of the given
   * arguments causes an error
   */
  public CoapRequest(int messageType, int messageCode, URI targetUri,
      boolean useProxy) throws IllegalArgumentException {

    super(messageType, messageCode, UNDEFINED_MESSAGE_ID,
        new Token(new byte[0]));

    if (messageType < MessageType.CON || messageType > MessageType.NON) {
      throw new IllegalArgumentException(String.format(NO_REQUEST_TYPE,
          messageType));
    }

    if (!MessageCode.isRequest(messageCode)) {
      throw new IllegalArgumentException(String.format(NO_REQUEST_CODE,
          messageCode));
    }

    if (useProxy) {
      setProxyURIOption(targetUri);
    } else {
      setTargetUriOptions(targetUri);
    }
  }

  private void setProxyURIOption(URI targetUri)
      throws IllegalArgumentException {
    this.addStringOption(Option.PROXY_URI, targetUri.toString());
  }

  private void setTargetUriOptions(URI targetUri)
      throws IllegalArgumentException {
    targetUri = targetUri.normalize();

    String scheme = targetUri.getScheme();
    if (scheme == null) {
      throw new IllegalArgumentException(String.format(
          URI_SCHEME, targetUri.toString()));
    }

    //Target URI must not have fragment part
    if (targetUri.getFragment() != null) {
      throw new IllegalArgumentException(String.format(
          URI_FRAGMENT, targetUri.toString()));
    }

    //Create target URI options
    if (!(OptionValue.isDefaultValue(Option.URI_HOST,
        targetUri.getHost().getBytes(CoapMessage.CHARSET)))) {
      addUriHostOption(targetUri.getHost());
    }

    if (targetUri.getPort() != -1 && targetUri.getPort()
        != OptionValue.URI_PORT_DEFAULT) {
      addUriPortOption(targetUri.getPort());
    }

    addUriPathOptions(targetUri.getPath());
    addUriQueryOptions(targetUri.getQuery());
  }

  private void addUriQueryOptions(String uriQuery)
      throws IllegalArgumentException {
    if (uriQuery != null) {
      for (String queryComponent : uriQuery.split("&")) {
        this.addStringOption(Option.URI_QUERY, queryComponent);
      }
    }
  }

  private void addUriPathOptions(String uriPath)
      throws IllegalArgumentException {
    if (uriPath != null) {
      //Path must not start with "/" to be further processed
      if (uriPath.startsWith("/")) {
        uriPath = uriPath.substring(1);
      }

      if ("".equals(uriPath)) {
        return;
      }

      for (String pathComponent : uriPath.split("/")) {
        this.addStringOption(Option.URI_PATH, pathComponent);
      }
    }
  }

  private void addUriPortOption(int uriPort) throws IllegalArgumentException {
    if (uriPort > 0 && uriPort != OptionValue.URI_PORT_DEFAULT) {
      this.addUintOption(Option.URI_PORT, uriPort);
    }
  }

  private void addUriHostOption(String uriHost)
      throws IllegalArgumentException {
    addStringOption(Option.URI_HOST, uriHost);
  }

  /**
   * Returns the value of the URI port option or
   * {@link OptionValue#URI_PORT_DEFAULT} if the URI port option is not present
   * in this {@link CoapRequest}.
   *
   * @return the value of the URI port option or
   * {@link OptionValue#URI_PORT_DEFAULT} if the URI port option is not present
   * in this {@link CoapRequest}.
   */
  public String getUriHost() {
    String result = "";

    if (options.containsKey(Option.URI_HOST)) {
      result = ((StringOptionValue) options.get(Option.URI_HOST))
          .getDecodedValue();
    }

    return result;
  }

  /**
   * Returns the full path of the request URI reconstructed from the URI path
   * options present in this {@link CoapRequest}. If no such option is set, the
   * returned value is "/".
   *
   * @return the full path of the request URI reconstructed from the URI path
   * options present in this {@link CoapRequest}.
   */
  public String getUriPath() {

    String result = "/";

    if (options.containsKey(Option.URI_PATH)) {
      result = ((StringOptionValue) options.get(Option.URI_PATH))
          .getDecodedValue();
    }

    return result;
  }

  /**
   * Returns the full query of the request URI reconstructed from the URI query
   * options present in this {@link CoapRequest} or the empty string ("") if no
   * such option is present.
   *
   * @return the full query of the request URI reconstructed from the URI query
   * options present in this {@link CoapRequest} or the empty string ("") if no
   * such option is present.
   */
  public String getUriQuery() {

    String result = "";

    if (options.containsKey(Option.URI_QUERY)) {
      result = ((StringOptionValue) options.get(Option.URI_QUERY))
          .getDecodedValue();
    }

    return result;
  }

  /**
   * Returns the value of the Proxy URI option if such an option is present in
   * this {@link CoapRequest}. If no such option is present but a Proxy Scheme
   * option, then the returned {@link java.net.URI} is reconstructed from the
   * Proxy Scheme option and the URI host, URI port, URI path and URI query
   * options.
   *
   * If both options, Proxy URI and Proxy Scheme are not present in this
   * {@link CoapRequest} this method returns <code>null</code>.
   *
   * @return the URI of the requested resource if this {@link CoapRequest} was
   * (or is supposed to be) sent via a proxy or <code>null</code> if the request
   * was (or is supposed to be) sent directly.
   *
   * @throws URISyntaxException if the value of the proxy URI option or the
   * reconstruction from Proxy Scheme, URI host, URI port, URI path, and URI
   * query options is invalid.
   */
  public URI getProxyURI() throws URISyntaxException {

    if (options.containsKey(Option.PROXY_URI)) {
      OptionValue proxyUriOptionValue = options.get(Option.PROXY_URI);
      return new URI(((StringOptionValue) proxyUriOptionValue)
          .getDecodedValue());
    }

    if (options.containsKey(Option.PROXY_SCHEME)) {
      OptionValue proxySchemeOptionValue = options
          .get(Option.PROXY_SCHEME);
      String scheme = ((StringOptionValue) proxySchemeOptionValue)
          .getDecodedValue();
      String uriHost = getUriHost();
      OptionValue uriPortOptionValue = options
          .get(Option.URI_PORT);
      int uriPort = ((UintOptionValue) uriPortOptionValue)
          .getDecodedValue().intValue();
      String uriPath = getUriPath();
      String uriQuery = getUriQuery();

      return new URI(scheme, null, uriHost, uriPort
          == OptionValue.URI_PORT_DEFAULT ? -1 : uriPort, uriPath,
          uriQuery, null);
    }

    return null;
  }

  /**
   * Returns <code>true</code> if the observing option is set on this
   * {@link CoapRequest} or <code>false</code> otherwise.
   *
   * @return <code>true</code> if the observing option is set on this
   * {@link CoapRequest} or <code>false</code> otherwise.
   */
  public boolean isObservationRequest() {
    return (options.containsKey(Option.OBSERVE));
  }
}
