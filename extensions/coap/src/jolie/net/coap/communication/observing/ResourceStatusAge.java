/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jolie.net.coap.communication.observing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of {@link ResourceStatusAge} represent the data
 * which is necessary to make the age of received update notifications comparable.
 *
 * @author Oliver Kleine
 */
public class ResourceStatusAge {

    public static final long MODULUS = (long) Math.pow(2, 24);
    private static final long THRESHOLD = (long) Math.pow(2, 23);

    private static Logger log = LoggerFactory.getLogger(ResourceStatusAge.class.getName());

    private long sequenceNo;
    private long timestamp;

    /**
     * Creates a new instance of {@link ResourceStatusAge}
     * @param sequenceNo the sequence number, i.e. {@link de.uzl.itm.ncoap.message.options.Option#OBSERVE} of an update
     *                   notification
     * @param timestamp the reception timestamp of an update notification
     */
    public ResourceStatusAge(long sequenceNo, long timestamp) {
        this.sequenceNo = sequenceNo;
        this.timestamp = timestamp;
    }

    /**
     * Returns <code>true</code> if <code>received</code> is newer than <code>latest</code> or <code>false</code>
     * otherwise
     *
     * @param latest the {@link ResourceStatusAge} of the latest
     *               update notification received so far
     * @param received the {@link ResourceStatusAge} of the newly
     *                 received update notification
     *
     * @return <code>true</code> if <code>received</code> is newer than <code>latest</code> or <code>false</code>
     * otherwise
     */
    public static boolean isReceivedStatusNewer(ResourceStatusAge latest, ResourceStatusAge received) {
        if (latest.sequenceNo < received.sequenceNo && received.sequenceNo - latest.sequenceNo < THRESHOLD) {
            log.debug("Criterion 1 matches: received ({}) is newer than latest ({}).", received, latest);
            return true;
        }

        if (latest.sequenceNo > received.sequenceNo && latest.sequenceNo - received.sequenceNo > THRESHOLD) {
            log.debug("Criterion 2 matches: received  ({}) is newer than latest ({}).", received, latest);
            return true;
        }

        if (received.timestamp > latest.timestamp + 128000L) {
            log.debug("Criterion 3 matches: received ({}) is newer than latest ({}).", received, latest);
            return true;
        }

        log.debug("No criterion matches: received({}) is older than latest ({}).", received, latest);
        return false;
    }

    @Override
    public String toString() {
        return "STATUS AGE (Sequence No: " + this.sequenceNo + ", Reception Timestamp: " + this.timestamp + ")";
    }
}
