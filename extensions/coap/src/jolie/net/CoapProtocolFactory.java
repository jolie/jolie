/*
 * The MIT License
 *
 * Copyright 2017 Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jolie.net;

import java.io.IOException;
import java.net.URI;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.AndJarDeps;
import jolie.runtime.VariablePath;

/**
 * For future development of extensions: Create MqttProtocolFactory called by
 * Jolie Class Loader, update file manifest.mf with value
 * X-JOLIE-ProtocolExtension: coap:jolie.net.MqttProtocolFactory
 */
@AndJarDeps({"jolie-js.jar", "json_simple.jar", "jolie-xml.jar"})
public class CoapProtocolFactory extends CommProtocolFactory {

    public CoapProtocolFactory(CommCore commCore) {
	super(commCore);
    }

    @Override
    public CommProtocol createInputProtocol(VariablePath configurationPath,
	    URI location) throws IOException {
	return new CoapProtocol(configurationPath);
    }

    @Override
    public CommProtocol createOutputProtocol(VariablePath configurationPath,
	    URI location) throws IOException {
	return new CoapProtocol(configurationPath);
    }
}
