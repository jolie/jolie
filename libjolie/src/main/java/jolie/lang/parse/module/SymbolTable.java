package jolie.lang.parse.module;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.module.SymbolInfo.Scope;

public class SymbolTable
{
    private final URI source;
    private final List<Source> dependency;
    private final Map< String, SymbolInfo > symbols;

    /**
     * @param source
     * @param symbols
     */
    public SymbolTable( URI source )
    {
        this.source = source;
        this.symbols = new HashMap<>();
        this.dependency = new ArrayList<>();
    }

    public URI source()
    {
        return this.source;
    }

    public void addSymbol( String name, OLSyntaxNode node ) throws ModuleException
    {
        if (isDuplicateSymbol( name )){
            throw new ModuleException("detected redeclaration of symbol " + name);
        }
        this.symbols.put( name, new SymbolInfoLocal( name, node ) );
    }

    public void addSymbol( String name, Source module ) throws ModuleException
    {
        if (isDuplicateSymbol( name )){
            throw new ModuleException("detected redeclaration of symbol " + name);
        }
        this.symbols.put( name, new SymbolInfoExternal( name, module, name ) );
    }

    public void addSymbol( String name, Source module, String moduleSymbol )
    {
        dependency.add(module);
        this.symbols.put( name, new SymbolInfoExternal( name, module, moduleSymbol ) );
    }

    public void addNamespaceSymbol( Source module ){
        dependency.add(module);
        this.symbols.put( module.source().toString(), new SymbolInfoExternal( "*", module, "*" ) );
    }

    public Source[] dependency(){
        return this.dependency.toArray( new Source[0] );
    }

    public SymbolInfo[] symbols(){
        return this.symbols.values().toArray( new SymbolInfo[0] );
    }

    public SymbolInfo symbol(String name){
        return this.symbols.get(name);
    }

    private boolean isDuplicateSymbol( String name )
    {
        if (symbols.containsKey(name) && symbols.get(name).scope() != Scope.LOCAL){
            return true;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "SymbolTable [source=" + source + ", dependency=" + dependency + ", symbols="
                + symbols + "]";
    }

}
