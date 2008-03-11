package com.sun.xml.xsom.impl;

import java.util.Comparator;

/**
 * UName.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class UName {
    /**
     * @param _nsUri
     *      Use "" to indicate the no namespace.
     */
    public UName( String _nsUri, String _localName, String _qname ) {
        if(_nsUri==null || _localName==null || _qname==null) {
            throw new NullPointerException(_nsUri+" "+_localName+" "+_qname);
        }
        this.nsUri = _nsUri.intern();
        this.localName = _localName.intern();
        this.qname = _qname.intern();
    }
    
    public UName( String nsUri, String localName ) {
        this(nsUri,localName,localName);
    }
    
    private final String nsUri;
    private final String localName;
    private final String qname;
    
    public String getName() { return localName; }
    public String getNamespaceURI() { return nsUri; }
    public String getQualifiedName() { return qname; }
    
    /**
     * Compares {@link UName}s by their names.
     */
    public static final Comparator comparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            UName lhs = (UName)o1;
            UName rhs = (UName)o2;
            int r = lhs.nsUri.compareTo(rhs.nsUri);
            if(r!=0)    return r;
            return lhs.localName.compareTo(rhs.localName);
        }
    };
}
