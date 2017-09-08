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
package ncoap.application.server.resource;

import ncoap.application.linkformat.LinkParam;
import ncoap.communication.dispatching.server.RequestDispatcher;
import ncoap.message.CoapRequest;
import ncoap.message.MessageType;
import ncoap.message.options.OptionValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ncoap.message.options.OptionValue.MAX_AGE_DEFAULT;

/**
* <p>This is the abstract class to be extended by classes to represent an observable resource. The generic type T
* means, that the object that holds the status of the resource is of type T.</p>
*
* <p>Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
* (integer values). Then, your service class should extend <code>ObservableWebresource&lt;Integer&gt;</code>.</p>
*
* @author Oliver Kleine, Stefan Hüske
*/
public abstract class ObservableWebresource<T> extends Observable implements Webresource<T> {

    private static Logger log = LoggerFactory.getLogger(ObservableWebresource.class.getName());

    public static final boolean SHUTDOWN = true;
    public static final boolean UPDATE = false;

    private RequestDispatcher requestDispatcher;
    private String uriPath;
    private LinkedHashMap<String, LinkParam> linkParams;

    private T status;
    private long statusExpiryDate;
    private ReentrantReadWriteLock statusLock;

    private ScheduledExecutorService executor;


    /**
     * Creates a new instance of {@link ObservableWebresource} with {@link OptionValue#MAX_AGE_DEFAULT} as lifetime
     * of the initial resource status.
     *
     * @param uriPath the uriPath this {@link ObservableWebresource} is registered at.
     * @param initialStatus the initial status of this {@link ObservableWebresource}.
     * @param executor {@link ScheduledExecutorService} to execute internal (application and resource-specific)
     *                 tasks
     */
    protected ObservableWebresource(String uriPath, T initialStatus, ScheduledExecutorService executor) {
        this(uriPath, initialStatus, MAX_AGE_DEFAULT, executor);
        this.setLinkParam(LinkParam.createLinkParam(LinkParam.Key.OBS, null));
    }


    /**
     * Creates a new instance of {@link ObservableWebresource}.
     *
     * @param uriPath the uriPath this {@link ObservableWebresource} is registered at.
     * @param initialStatus the initial status of this {@link ObservableWebresource}.
     * @param lifetime the number of seconds the initial status may be considered fresh, i.e. cachable by
     *                        proxies or clients.
     * @param executor the {@link ScheduledExecutorService} to execute internal (application and resource-specific)
     *                 tasks
     */
    protected ObservableWebresource(String uriPath, T initialStatus, long lifetime, ScheduledExecutorService executor) {
        this.uriPath = uriPath;
        this.linkParams = new LinkedHashMap<>();
        this.statusLock = new ReentrantReadWriteLock();
        this.executor = executor;
        setResourceStatus(initialStatus, lifetime);
    }


    @Override
    public void setLinkParam(LinkParam linkParam) {
        if (this.linkParams.containsKey(linkParam.getKeyName())) {
            removeLinkParams(linkParam.getKey());
        }

        this.linkParams.put(linkParam.getKeyName(), linkParam);
    }

    @Override
    public boolean removeLinkParams(LinkParam.Key key) {
        this.linkParams.remove(key.getKeyName());
        return (this.linkParams.get(key.getKeyName()) == null);
    }

    @Override
    public boolean hasLinkAttribute(LinkParam.Key key, String value) {
        LinkParam linkParam = this.linkParams.get(key.getKeyName());
        if(linkParam == null) {
            return false;
        } else if (linkParam.getValueType() == LinkParam.ValueType.EMPTY && value == null){
            return true;
        } else {
            return linkParam.contains(value);
        }
    }

    @Override
    public Collection<LinkParam> getLinkParams() {
        return this.linkParams.values();
    }


    @Override
    public void setRequestDispatcher(RequestDispatcher requestDispatcher) {
        this.requestDispatcher = requestDispatcher;
    }


    @Override
    public RequestDispatcher getRequestDispatcher() {
        return this.requestDispatcher;
    }


    @Override
    public final String getUriPath() {
        return this.uriPath;
    }


    @Override
    public ScheduledExecutorService getExecutor() {
        return this.executor;
    }


    /**
     * <p><b>Important:</b>To avoid synchronization issues do not use this method but
     * {@link #getWrappedResourceStatus(Set)} or {@link #getWrappedResourceStatus(long)} for status retrieval
     * (e.g. when processing an inbound {@link CoapRequest}).
     *
     * @see #getWrappedResourceStatus(Set)
     * @see #getWrappedResourceStatus(long)
     */
    @Override
    public final T getResourceStatus() {
        return this.status;
    }


    @Override
    public synchronized final void setResourceStatus(final T status, final long lifetime) {
        this.executor.submit(new Runnable() {

            @Override
            public void run() {
                try{
                    statusLock.writeLock().lock();

                    ObservableWebresource.this.status = status;
                    ObservableWebresource.this.statusExpiryDate = System.currentTimeMillis() + (lifetime * 1000);
                    ObservableWebresource.this.updateEtag(status);

                    log.debug("New status of {} successfully set (expires in {} seconds).",
                            ObservableWebresource.this.getUriPath(), lifetime);

                    setChanged();
                    notifyObservers(UPDATE);
                } catch(Exception ex) {
                    log.error("Exception while setting new resource status for \"{}\"!",
                            ObservableWebresource.this.getUriPath(), ex);
                } finally {
                    statusLock.writeLock().unlock();
                }
            }
        });
    }


    /**
     * <p>This method and {@link #getWrappedResourceStatus(java.util.Set)} are the only recommended way to retrieve
     * the actual resource status that is used for a {@link de.uzl.itm.ncoap.message.CoapResponse} to answer an inbound
     * {@link de.uzl.itm.ncoap.message.CoapRequest}.</p>
     *
     * <p>Invocation of this method read-locks the resource status, i.e. concurrent invocations of
     * {@link #setResourceStatus(Object, long)} wait for this method to finish, i.e. the read-lock to be released.
     * This is to avoid inconsistencies between the content and {@link de.uzl.itm.ncoap.message.options.Option#ETAG}, resp.
     * {@link de.uzl.itm.ncoap.message.options.Option#MAX_AGE} in a {@link de.uzl.itm.ncoap.message.CoapResponse}. Such inconsistencies could happen in case of a
     * resource update between calls of e.g. {@link #getSerializedResourceStatus(long)} and {@link #getEtag(long)},
     * resp. {@link #getMaxAge()}.</p>
     *
     * <p>However, concurrent invocations of this method are possible, as the resources read-lock can be locked multiple
     * times in parallel and {@link #setResourceStatus(Object, long)} waits for all read-locks to be released.</p>
     *
     * @param contentFormat the number representing the desired content format of the serialized resource status
     *
     * @return a {@link WrappedResourceStatus} if the content format was supported or <code>null</code> if the
     * resource status could not be serialized to the desired content format.
     */
    public final WrappedResourceStatus getWrappedResourceStatus(long contentFormat) {
        try {
            this.statusLock.readLock().lock();

            byte[] serializedResourceStatus = getSerializedResourceStatus(contentFormat);

            if (serializedResourceStatus == null) {
                return null;
            } else {
                byte[] etag = this.getEtag(contentFormat);
                return new WrappedResourceStatus(serializedResourceStatus, contentFormat, etag, this.getMaxAge());
            }
        } finally {
            this.statusLock.readLock().unlock();
        }
    }


    /**
     * <p>This method and {@link #getWrappedResourceStatus(long)} are the only recommended ways to retrieve
     * the actual resource status that is used for a {@link de.uzl.itm.ncoap.message.CoapResponse} to answer an
     * inbound {@link de.uzl.itm.ncoap.message.CoapRequest}.</p>
     *
     * <p>Invocation of this method read-locks the resource status, i.e. concurrent invocations of
     * {@link #setResourceStatus(Object, long)} wait for this method to finish, i.e. the read-lock to be released.
     * This is to avoid inconsistencies between the content and {@link de.uzl.itm.ncoap.message.options.Option#ETAG}, resp.
     * {@link de.uzl.itm.ncoap.message.options.Option#MAX_AGE} in a {@link de.uzl.itm.ncoap.message.CoapResponse}. Such inconsistencies could happen in case of a
     * resource update between calls of e.g. {@link #getSerializedResourceStatus(long)} and {@link #getEtag(long)},
     * resp. {@link #getMaxAge()}.</p>
     *
     * <p>However, concurrent invocations of this method are possible, as the resources read-lock can be locked multiple
     * times in parallel and {@link #setResourceStatus(Object, long)} waits for all read-locks to be released.</p>
     *
     * <p><b>Note:</b> This method iterates over the given {@link Set} and tries to serialize the status in the order
     * given by the {@link java.util.Set#iterator()}. The first supported content format, i.e. where
     * {@link #getSerializedResourceStatus(long)} does return a value other than <code>null</code> is the content
     * format of the {@link WrappedResourceStatus} returned by this method.</p>
     *
     * @param contentFormats A {@link Set} containing the numbers representing the accepted content formats
     *
     * @return a {@link WrappedResourceStatus} if any of the given content formats was supported or
     * <code>null</code> if the resource status could not be serialized to any accepted content format.
     */
    @Override
    public final WrappedResourceStatus getWrappedResourceStatus(Set<Long> contentFormats) {
        try {
            this.statusLock.readLock().lock();

            WrappedResourceStatus result = null;

            for(long contentFormat : contentFormats) {
                result = getWrappedResourceStatus(contentFormat);

                if (result != null)
                    break;
            }

            return result;
        } finally {
            this.statusLock.readLock().unlock();
        }
    }


    /**
     * <p>This method is invoked by the framework for every observer after every resource update. Classes that extend
     * {@link ObservableWebresource} may implement this method just by returning one of
     * {@link MessageType#CON} or {@link MessageType#NON}.</p>
     *
     * <p>However, this method also gives {@link ObservableWebresource}s the opportunity
     * to e.g. distinguish between observers or implement some other arbitrary logic...</p>
     *
     * @param remoteSocket the remote CoAP endpoints that observes this {@link ObservableWebresource}
     *
     * @return the message type for the next update notification for the observer identified by the given parameters
     */
    public abstract boolean isUpdateNotificationConfirmable(InetSocketAddress remoteSocket);

    public abstract void removeObserver(InetSocketAddress remoteAddress);


    /**
     * <p>Returns the number of seconds the actual resource state can be considered fresh for status caching on proxies
     * or clients. The returned number is calculated using the parameter <code>lifetime</code> on
     * invocation of {@link #setResourceStatus(Object, long)} or
     * {@link #ObservableWebresource(String, Object, long, ScheduledExecutorService)}
     * (which internally invokes {@link #setResourceStatus(Object, long)}).</p>
     *
     * <p>If the number of seconds passed after the last invocation of {@link #setResourceStatus(Object, long)} is larger
     * than the number of seconds given as parameter <code>lifetime</code>, this method returns zero.</p>
     *
     * @return the number of seconds the actual resource state can be considered fresh for status caching on proxies
     * or clients.
     */
    @Override
    public final long getMaxAge() {
        return Math.max(this.statusExpiryDate - System.currentTimeMillis(), 0) / 1000;
    }


    /**
     * {@inheritDoc}
     *
     * <p><b>Important:</b> Make sure to invoke <code>super.shutdown()</code> if you override this method in an
     * extending class.</p>
     */
    @Override
    public void shutdown() {
        getExecutor().submit(new Runnable() {
            @Override
            public void run() {
                log.warn("Shutdown service \"{}\"!", getUriPath());
                statusLock.writeLock().lock();
                setChanged();
                notifyObservers(SHUTDOWN);
            }
        });
    }
}
