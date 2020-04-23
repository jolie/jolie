package jolie.lang.parse.module;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.module.SymbolInfo.Scope;

public class SymbolTable
{
    private final URI source;
    private final Map< String, SymbolInfo > symbols;

    /**
     * @param source
     * @param symbols
     */
    public SymbolTable( URI source )
    {
        this.source = source;
        this.symbols = new HashMap<>();
    }

    public URI source()
    {
        return this.source;
    }

    public void addSymbol( String name, OLSyntaxNode node ) throws ModuleException
    {
        if ( isDuplicateSymbol( name ) ) {
            throw new ModuleException( "detected redeclaration of symbol " + name );
        }
        this.symbols.put( name, new SymbolInfoLocal( name, node ) );
    }

    public void addSymbol( String name, String[] moduleTargetStrings ) throws ModuleException
    {
        if ( isDuplicateSymbol( name ) ) {
            throw new ModuleException( "detected redeclaration of symbol " + name );
        }
        this.symbols.put( name, new SymbolInfoExternal( name, moduleTargetStrings, name ) );
    }

    public void addSymbol( String name, String[] moduleTargetStrings, String moduleSymbol )
            throws ModuleException
    {
        if ( isDuplicateSymbol( name ) ) {
            throw new ModuleException( "detected redeclaration of symbol " + name );
        }
        this.symbols.put( name, new SymbolInfoExternal( name, moduleTargetStrings, moduleSymbol ) );
    }

    public void addNamespaceSymbol( String[] moduleTargetStrings )
    {
        this.symbols.put( Arrays.toString( moduleTargetStrings ),
                new SymbolInfoExternal( "*", moduleTargetStrings, "*" ) );
    }

    public SymbolInfo[] symbols()
    {
        return this.symbols.values().toArray( new SymbolInfo[0] );
    }

    public SymbolInfoLocal[] localSymbols()
    {
        return this.symbols.values().stream().filter( symbol -> symbol.scope() == Scope.LOCAL )
                .toArray( SymbolInfoLocal[]::new );
    }

    public SymbolInfoExternal[] externalSymbols()
    {
        return this.symbols.values().stream().filter( symbol -> symbol.scope() == Scope.EXTERNAL )
                .toArray( SymbolInfoExternal[]::new );
    }

    public SymbolInfo symbol( String name )
    {
        return this.symbols.get( name );
    }

    private boolean isDuplicateSymbol( String name )
    {
        if ( symbols.containsKey( name ) && symbols.get( name ).scope() != Scope.LOCAL ) {
            return true;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "SymbolTable [source=" + source + ", symbols=" + symbols + "]";
    }

}
