package jolie.lang.parse.module;

import java.util.Optional;

public class SymbolInfoExternal extends SymbolInfo
{

    private final String[] moduleTargets;
    private final String moduleSymbol;
    private Source moduleSource;

    public SymbolInfoExternal( String name, String[] moduleTargets, String moduleSymbol )
    {
        super( name, Scope.EXTERNAL );
        this.moduleTargets = moduleTargets;
        this.moduleSymbol = moduleSymbol;
    }

    public void setModuleSource( Source moduleSource ) throws ModuleException
    {
        if ( this.moduleSource != null ) {
            new ModuleException( "Symbol " + this.name() + " has already defined moduleSource at "
                    + this.moduleSource.source().toString() );
        }
        this.moduleSource = moduleSource;
    }

    public String[] moduleTargets()
    {
        return this.moduleTargets;
    }

    public Optional<Source> moduleSource()
    {
        return Optional.of(this.moduleSource);
    }

    public String moduleSymbol()
    {
        return this.moduleSymbol;
    }


}
