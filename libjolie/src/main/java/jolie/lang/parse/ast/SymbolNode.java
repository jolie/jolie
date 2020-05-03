package jolie.lang.parse.ast;

import jolie.lang.parse.module.SymbolInfo.Privacy;

public interface SymbolNode
{
    public Privacy privacy();
    public void setPrivacy(Privacy privacy);
    public String name();
    public OLSyntaxNode node();
}