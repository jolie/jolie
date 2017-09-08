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
package ncoap.message.options;

/**
 * Helper class to provide constants for several content formats.
 *
 * @author Oliver Kleine
 */
public abstract class ContentFormat {

    /**
     * Corresponds to number -1
     */
    public static final long    UNDEFINED           = -1;

    /**
     * Corresponds to number 0
     */
    public static final long    TEXT_PLAIN_UTF8     = 0;

    /**
     * Corresponds to number 40
     */
    public static final long    APP_LINK_FORMAT     = 40;

    /**
     * Corresponds to number 41
     */
    public static final long    APP_XML             = 41;

    /**
     * Corresponds to number 42
     */
    public static final long    APP_OCTET_STREAM    = 42;

    /**
     * Corresponds to number 47
     */
    public static final long    APP_EXI             = 47;

    /**
     * Corresponds to number 50
     */
    public static final long    APP_JSON            = 50;

    /**
     * Corresponds to number 201 (no standard but defined for very selfish reasons)
     */
    public static final long    APP_RDF_XML         = 201;

    /**
     * Corresponds to number 202 (no standard but defined for very selfish reasons)
     */
    public static final long    APP_TURTLE          = 202;

    /**
     * Corresponds to number 203 (no standard but defined for very selfish reasons)
     */
    public static final long    APP_N3              = 203;

    /**
     * Corresponds to number 205 (no standard but defined for very selfish reasons)
     */
    public static final long    APP_SHDT            = 205;
}
