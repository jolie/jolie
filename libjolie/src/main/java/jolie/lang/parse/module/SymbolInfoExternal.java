package jolie.lang.parse.module;

public class SymbolInfoExternal extends SymbolInfo
{

    private Source module;
    private String moduleSymbol;

    public SymbolInfoExternal( String name, Source module, String moduleSymbol )
    {
        super( name, Scope.EXTERNAL );
        this.module = module;
        this.moduleSymbol = moduleSymbol;
    }

    public Source module()
    {
        return this.module;
    }

    public String moduleSymbol()
    {
        return this.moduleSymbol;
    }


}
