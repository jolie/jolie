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
package ncoap.application.server.resource;

public class WrappedResourceStatus {

    private byte[] content;
    private long contentFormat;
    private byte[] etag;
    private long maxAge;

    /**
     * Creates a new instance of {@link WrappedResourceStatus}
     *
     * @param content the serialized resource status
     * @param contentFormat the number representing the serialization format
     * @param etag the ETAG value of the actual status
     * @param maxAge the number of seconds this status is allowed to be cached
     */
    public WrappedResourceStatus(byte[] content, long contentFormat, byte[] etag, long maxAge) {
	this.content = content;
	this.contentFormat = contentFormat;
	this.etag = etag;
	this.maxAge = maxAge;
    }

    /**
     * Returns the serialized resource status, i.e. a particular representation
     *
     * @return the serialized resource status, i.e. a particular representation
     */
    public byte[] getContent() {
	return content;
    }

    /**
     * Returns the number referring to the format of the serialized resource
     * status returned by {@link #getContent()}.
     *
     * @return the number referring to the format of the serialized resource
     * status
     */
    public long getContentFormat() {
	return contentFormat;
    }

    /**
     * Returns the ETAG value of the serialized resource status returned by
     * {@link #getContent()}.
     *
     * @return the ETAG value of the serialized resource status
     */
    public byte[] getEtag() {
	return etag;
    }

    /**
     * Returns the number of seconds a cache is allowed to cache this status
     *
     * @return the number of seconds a cache is allowed to cache this status
     */
    public long getMaxAge() {
	return maxAge;
    }
}
