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
package ncoap.application.linkformat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <p>A {@link LinkValue} is a representation of a single entry in a {@link LinkValueList}. It consists of
 * one URI reference, i.e. the resource to be described, and zero or more instances of {@link LinkParam}, e.g.
 * <code>, e.g. <code>&lt;</example&gt;;ct="0 40"</code>.</p>
 *
 * <p>The notations are taken from RFC 6690</p>
 *
 * @author Oliver Kleine
 */
public class LinkValue {

    //*******************************************************************************************
    // static fields and methods
    //*******************************************************************************************

    private static Logger LOG = LoggerFactory.getLogger(LinkValue.class.getName());

    static LinkValue decode(String linkValue) {
        LinkValue result = new LinkValue(getUriReference(linkValue));
        for (String linkParam : LinkValue.getLinkParams(linkValue)) {
            result.addLinkParam(LinkParam.decode(linkParam));
        }
        return result;
    }

    private static String getUriReference(String linkValue) {
        String uriReference = linkValue.substring(linkValue.indexOf("<") + 1, linkValue.indexOf(">"));
        LOG.info("Found URI reference <{}>", uriReference);
        return uriReference;
    }

    private static List<String> getLinkParams(String linkValue) {
        List<String> result = new ArrayList<>();
        String[] linkParams = linkValue.split(";");
        result.addAll(Arrays.asList(linkParams).subList(1, linkParams.length));
        return result;
    }


    //******************************************************************************************
    // instance related fields and methods
    //******************************************************************************************

    private String uriReference;
    private Collection<LinkParam> linkParams;

    /**
     * Creates a new instance of {@link LinkValue}
     * @param uriReference the URI reference, i.e. the resource to be described
     * @param linkParams the {@link LinkParam}s to describe the resource
     */
    public LinkValue(String uriReference, Collection<LinkParam> linkParams) {
        this.uriReference = uriReference;
        this.linkParams = linkParams;
    }

    private LinkValue(String uriReference) {
        this(uriReference, new ArrayList<LinkParam>());
    }

    private void addLinkParam(LinkParam linkParams) {
        this.linkParams.add(linkParams);
    }

    /**
     * Returns the URI reference of this {@link LinkValue}
     * @return the URI reference of this {@link LinkValue}
     */
    public String getUriReference() {
        return this.uriReference;
    }

    /**
     * Returns the {@link LinkParam}s describing the resource identified by the URI reference
     * @return the {@link LinkParam}s describing this resource identified by the URI reference
     */
    public Collection<LinkParam> getLinkParams() {
        return this.linkParams;
    }

    /**
     * Returns <code>true</code> if this {@link LinkValue} contains a {@link LinkParam} that matches the given
     * criterion, i.e. the given key-value-pair and <code>false</code> otherwise.
     *
     * @param key the key of the criterion
     * @param value the value of the criterion
     *
     * @return <code>true</code> if this {@link LinkValue} contains a {@link LinkParam} that matches the given
     * criterion, i.e. the given key-value-pair and <code>false</code> otherwise.
     */
    public boolean containsLinkParam(LinkParam.Key key, String value) {
        for (LinkParam linkParam : this.linkParams) {
            if (key.equals(linkParam.getKey())) {
                return value == null || linkParam.contains(value);
            }
        }
        return false;
    }

    /**
     * Returns a string representation of this {@link LinkValue}.
     * @return a string representation of this {@link LinkValue}.
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(uriReference).append(">");
        for (LinkParam linkParam : this.getLinkParams()) {
            builder.append(";").append(linkParam.toString());
        }
        return builder.toString();
    }
}
