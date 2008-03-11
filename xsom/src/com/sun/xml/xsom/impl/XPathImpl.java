package com.sun.xml.xsom.impl;

import com.sun.xml.xsom.XSIdentityConstraint;
import com.sun.xml.xsom.XSXPath;
import com.sun.xml.xsom.XmlString;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;
import org.xml.sax.Locator;

/**
 * @author Kohsuke Kawaguchi
 */
public class XPathImpl extends ComponentImpl implements XSXPath {
    private XSIdentityConstraint parent;
    private final XmlString xpath;

    public XPathImpl(SchemaDocumentImpl _owner, AnnotationImpl _annon, Locator _loc, ForeignAttributesImpl fa, XmlString xpath) {
        super(_owner, _annon, _loc, fa);
        this.xpath = xpath;
    }

    public void setParent(XSIdentityConstraint parent) {
        this.parent = parent;
    }

    public XSIdentityConstraint getParent() {
        return parent;
    }

    public XmlString getXPath() {
        return xpath;
    }

    public void visit(XSVisitor visitor) {
        visitor.xpath(this);
    }

    public <T> T apply(XSFunction<T> function) {
        return function.xpath(this);
    }
}
