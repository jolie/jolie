package com.sun.xml.xsom.impl;

import com.sun.xml.xsom.ForeignAttributes;
import org.relaxng.datatype.ValidationContext;
import org.xml.sax.Locator;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Remembers foreign attributes.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ForeignAttributesImpl extends AttributesImpl implements ForeignAttributes {
    private final ValidationContext context;
    private final Locator locator;
    /**
     * {@link ForeignAttributes} forms a linked list.
     */
    final ForeignAttributesImpl next;

    public ForeignAttributesImpl(ValidationContext context, Locator locator, ForeignAttributesImpl next) {
        this.context = context;
        this.locator = locator;
        this.next = next;
    }

    public ValidationContext getContext() {
        return context;
    }

    public Locator getLocator() {
        return locator;
    }
}
