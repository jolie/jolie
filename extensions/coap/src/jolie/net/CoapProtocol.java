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
package jolie.net;

import io.netty.channel.ChannelPipeline;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.VariablePath;

/**
 *
 * @author stefanopiozingaro
 */
public class CoapProtocol extends AsyncCommProtocol {

    public CoapProtocol(VariablePath configurationPath) {
        super(configurationPath);
    }

    @Override
    public void setupPipeline(ChannelPipeline pipeline) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isThreadSafe() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
