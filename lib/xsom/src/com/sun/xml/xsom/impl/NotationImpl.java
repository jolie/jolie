/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.xsom.impl;

import com.sun.xml.xsom.XSNotation;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;
import org.xml.sax.Locator;

/**
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class NotationImpl extends DeclarationImpl implements XSNotation {
    
    public NotationImpl( SchemaDocumentImpl owner, AnnotationImpl _annon,
        Locator _loc, ForeignAttributesImpl _fa, String _name,
        String _publicId, String _systemId ) {
        super(owner,_annon,_loc,_fa,owner.getTargetNamespace(),_name,false);
        
        this.publicId = _publicId;
        this.systemId = _systemId;
    }
    
    private final String publicId;
    private final String systemId;
    
    public String getPublicId() { return publicId; }
    public String getSystemId() { return systemId; }

    public void visit(XSVisitor visitor) {
        visitor.notation(this);
    }

    public Object apply(XSFunction function) {
        return function.notation(this);
    }

}
