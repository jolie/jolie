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
package ncoap.communication.identification;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import ncoap.communication.dispatching.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The TokenFactory generates endpointIDs to match inbound responses with open requests and enable the
 * {@link de.uzl.itm.ncoap.communication.dispatching.client.ResponseDispatcher} to invoke the correct callback method.
 *
 * The CoAP specification makes no assumptions how to interpret the bytes returned by {@link Token#getBytes()()}, i.e.
 * a {@link Token} instances where {@link Token#getBytes()} returns an empty byte array is different from a
 * {@link Token} where {@link Token#getBytes()} returns a byte array containing one "zero byte" (all bits set to 0).
 * Furthermore, both of these {@link Token}s differ from another {@link Token} that is backed by a byte array
 * containing two "zero bytes" and so on...
 *
 * This leads to 257 (<code>(2^8) + 1</code>) different endpointIDs for a maximum endpointID length of 1 or 65793 different
 * endpointIDs (<code>(2^16) + (2^8) + 1</code>) for a maximum endpointID length of 2 and so on and so forth...
 *
 * @author Oliver Kleine
 */
public class EndpointIDFactory {

    private static Logger LOG = LoggerFactory.getLogger(EndpointIDFactory.class.getName());


    private SortedSet<EndpointID> activeIDs;
    private ReentrantReadWriteLock lock;
    private Random random;

    /**
     * Creates a new instance of
     * {@link ncoap.communication.identification.EndpointIDFactory}     * producing {@link Token}s where the length of
     * {@link Token#getBytes()} is not longer than the given
     * maximum length.
     */
    public EndpointIDFactory() {
        this.lock = new ReentrantReadWriteLock();
        activeIDs = new TreeSet<>();
        this.random = new Random(System.currentTimeMillis());
    }

    public EndpointID getNextEndpointID() {
        try{
            this.lock.writeLock().lock();
            EndpointID endpointID;
            do {
                endpointID = new EndpointID(Ints.toByteArray(this.random.nextInt()));
            } while (!this.activeIDs.add(endpointID));

            return endpointID;
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

//    public EndpointID getNextEndpointID() {
//        try{
//            this.lock.writeLock().lock();
//            EndpointID endpointID;
//            if (activeIDs.isEmpty()) {
//                endpointID = new EndpointID(new byte[1]);
//            }
//            else{
//                endpointID = getSuccessor(activeIDs.last());
//            }
//
//            activeIDs.add(endpointID);
//            LOG.debug("Created new Endpoint ID: {}", endpointID);
//            return endpointID;
//        }
//        finally {
//            this.lock.writeLock().unlock();
//        }
//    }


    public synchronized boolean passBackEndpointID(EndpointID endpointID) {
        try{
            this.lock.readLock().lock();
            if (!this.activeIDs.contains(endpointID)) {
                LOG.error("Could not pass pack (unknown) Endpoint ID ({})", endpointID);
                return false;
            }
        }
        finally {
            this.lock.readLock().unlock();
        }

        try{
            this.lock.writeLock().lock();
            if (this.activeIDs.remove(endpointID)) {
                LOG.info("Passed back Endpoint ID ({})", endpointID);
                return true;
            }
            else{
                LOG.error("Could not pass pack Endpoint ID (remote endpoint: {}, endpointID: {})", endpointID);
                return false;
            }
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }


    private static EndpointID getSuccessor(EndpointID endpointID) {

        boolean allBitsSet = true;

        //Check if all bits in the given byte array are set to 1
        for(byte b : endpointID.getBytes()) {
            if (b != -1) {
                allBitsSet = false;
                break;
            }
        }

        if (allBitsSet) {
            //make e.g. ([00000000], [00000000]) the successor of ([11111111])
            if (endpointID.getBytes().length < EndpointID.MAX_LENGTH) {
                return new EndpointID(new byte[endpointID.getBytes().length + 1]);
            }

            //make [00000000] the successor of the byte array with 8 [11111111] bytes
            else{
                return new EndpointID(new byte[1]);
            }
        }

        long tmp = Longs.fromByteArray(Bytes.concat(new byte[8 - endpointID.getBytes().length], endpointID.getBytes())) + 1;
        byte[] result = Longs.toByteArray(tmp);
        EndpointID successor = new EndpointID(Arrays.copyOfRange(result, 8 - endpointID.getBytes().length, 8));
        LOG.debug("Successor of {} is {}.", endpointID, successor);
        return successor;
    }
}
