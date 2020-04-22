package jolie.lang.parse.module;

import jolie.lang.parse.ast.OLSyntaxNode;

public class SymbolInfoLocal extends SymbolInfo
{

    public SymbolInfoLocal( String name )
    {
        super( name, Scope.LOCAL );
    }

}