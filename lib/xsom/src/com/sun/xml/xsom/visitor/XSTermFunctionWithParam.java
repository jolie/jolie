package com.sun.xml.xsom.visitor;

import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSTerm;

/**
 * Function object that works on {@link XSTerm}.
 *
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSTermFunctionWithParam<T,P> {
    T wildcard( XSWildcard wc, P param );
    T modelGroupDecl( XSModelGroupDecl decl, P param );
    T modelGroup( XSModelGroup group, P param );
    T elementDecl( XSElementDecl decl, P param );
}
