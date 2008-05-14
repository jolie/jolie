package com.sun.xml.xsom;

/**
 * Selector or field of {@link XSIdentityConstraint}.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface XSXPath extends XSComponent  {

    /**
     * Returns the {@link XSIdentityConstraint} to which
     * this XPath belongs to.
     *
     * @return
     *      never null.
     */
    XSIdentityConstraint getParent();

    /**
     * Gets the XPath as a string.
     *
     * @return
     *      never null.
     */
    XmlString getXPath();
}
