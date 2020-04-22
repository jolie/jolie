package jolie.lang.parse.module;

import jolie.lang.parse.ast.OLSyntaxNode;

public abstract class SymbolInfo
{

    enum Scope {
        LOCAL, EXTERNAL
    }

    private String name;
    private Scope scope;
    private OLSyntaxNode node;

    public SymbolInfo( String name, Scope scope, OLSyntaxNode node )
    {
        this(name, scope);
        this.node = node;
    }

    public SymbolInfo( String name, Scope scope )
    {
        this.name = name;
        this.scope = scope;
    }

    public String name()
    {
        return name;
    }

    public void setPointer(OLSyntaxNode pointer){
        this.node = pointer;
    }

    public OLSyntaxNode node()
    {
        return node;
    }

    public Scope scope()
    {
        return scope;
    }

    @Override
    public String toString()
    {
        return "SymbolInfo [name=" + name + ", scope=" + scope + "]";
    }
    

}
