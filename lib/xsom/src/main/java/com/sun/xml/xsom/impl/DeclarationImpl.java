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

import com.sun.xml.xsom.XSDeclaration;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.util.NameGetter;
import org.xml.sax.Locator;

abstract class DeclarationImpl extends ComponentImpl implements XSDeclaration
{
    DeclarationImpl( SchemaDocumentImpl owner,
        AnnotationImpl _annon, Locator loc, ForeignAttributesImpl fa,
        String _targetNamespace, String _name,    boolean _anonymous ) {
        
        super(owner,_annon,loc,fa);
        this.targetNamespace = _targetNamespace;
        this.name = _name;
        this.anonymous = _anonymous;
    }
    
    private final String name;
    public String getName() { return name; }
    
    private final String targetNamespace;
    public String getTargetNamespace() { return targetNamespace; }
    
    private final boolean anonymous;
    /** @deprecated */
    public boolean isAnonymous() { return anonymous; }
    
    public final boolean isGlobal() { return !isAnonymous(); }
    public final boolean isLocal() { return isAnonymous(); }
}
