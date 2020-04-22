package jolie.lang.parse.module;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import jolie.lang.parse.ast.OLSyntaxNode;

public class SymbolTable
{
    private URI context;
    private Map< String, SymbolInfo > symbols;

    /**
     * @param context
     * @param symbols
     */
    public SymbolTable( URI context )
    {
        this.context = context;
        this.symbols = new HashMap<>();
    }

    public URI context()
    {
        return this.context;
    }

    public void addSymbol( String name )
    {
        this.symbols.put( name, new SymbolInfoLocal( name ) );
    }

    public void addSymbol( String name, Source module )
    {
        this.symbols.put( name, new SymbolInfoExternal( name, module, name ) );
    }

    public void addSymbol( String name, Source module, String moduleSymbol )
    {
        this.symbols.put( name, new SymbolInfoExternal( name, module, moduleSymbol ) );
    }

}
