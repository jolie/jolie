/********************************************************************************
  * Copyright (C) 2014-2016 Fabrizio Montesi <famontesi@gmail.com>              *
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

package jolie.net.auto;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.CommCore;
import jolie.net.CommListener;
import jolie.net.UnsupportedCommMediumException;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.runtime.AndJarDeps;
import jolie.util.Helpers;

/**
 * Listener factory for the auto communication medium.
 *
 * @author Fabrizio Montesi
 */
@AndJarDeps({"ini4j.jar"})
public class AutoListenerFactory extends CommListenerFactory {

    public AutoListenerFactory(CommCore commCore) {
        super(commCore);
    }

    @Override
    public CommListener createListener(
            Interpreter interpreter,
            CommProtocolFactory protocolFactory,
            InputPort inputPort
    )
            throws IOException {
        // Format: "auto:autoconf_uri"
        URI locationURI = inputPort.location();

        String[] ss = locationURI.getSchemeSpecificPart().split(":", 2);
        String location = null;
        if ("ini".equals(ss[0])) {
            location = AutoHelper.getLocationFromIni(ss[1]);
        } else {
            AutoHelper.throwIOException("unsupported scheme: " + locationURI.getScheme());
        }

        AutoHelper.assertIOException(location == null, "internal error: location is null");
        //AutoHelper.assertIOException( location.equals( Constants.LOCAL_LOCATION_KEYWORD ), "autoconf does not support local locations" );

        if (location.equals(Constants.LOCAL_LOCATION_KEYWORD)) {
            interpreter.commCore().addLocalInputPort(inputPort);
            inputPort.setLocation(location);
            return interpreter.commCore().localListener();
        } else {
            try {
                URI uri = new URI(location);
                inputPort.setLocation(location);
                CommListenerFactory factory = interpreter.commCore().getCommListenerFactory(uri.getScheme());
                Helpers.condThrow(factory == null, new UnsupportedCommMediumException(uri.getScheme()));
                return factory.createListener(interpreter, protocolFactory, inputPort);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
    }
}
