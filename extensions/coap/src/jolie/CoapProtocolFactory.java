package jolie;

/*
 * Copyright (C) 2017 stefanopiozingaro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.io.IOException;
import java.net.URI;
import jolie.net.CommCore;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.AndJarDeps;
import jolie.runtime.VariablePath;

/**
 * For future development of extensions: Create MqttProtocolFactory called by
 * Jolie Class Loader, update file manifest.mf with value
 * X-JOLIE-ProtocolExtension: coap:jolie.net.CoapProtocolFactory
 */
@AndJarDeps({"jolie-js.jar", "json_simple.jar", "jolie-xml.jar"})
public class CoapProtocolFactory extends CommProtocolFactory {

    private boolean isInput;

    public CoapProtocolFactory(CommCore commCore) {
	super(commCore);
    }

    @Override
    public CommProtocol createInputProtocol(VariablePath configurationPath,
	    URI location) throws IOException {
	isInput = true;
	return new CoapProtocol(configurationPath, isInput);
    }

    @Override
    public CommProtocol createOutputProtocol(VariablePath configurationPath,
	    URI location) throws IOException {
	isInput = false;
	return new CoapProtocol(configurationPath, isInput);
    }
}
