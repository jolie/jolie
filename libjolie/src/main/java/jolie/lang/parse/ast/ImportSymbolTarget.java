package jolie.lang.parse.ast;

/**
 * A class for holding information of symbol tar
 */
public class ImportSymbolTarget
{
    private final String moduleSymbol;
    private final String localSymbol;

    /**
     * @param moduleSymbol a symbol of taring module
     * @param localSymbol  a symbol of to place in local execution
     */
    public ImportSymbolTarget( String moduleSymbol, String localSymbol )
    {
        this.moduleSymbol = moduleSymbol;
        this.localSymbol = localSymbol;
    }

    @Override
    public String toString()
    {
        if ( this.moduleSymbol.equals( this.localSymbol ) ) {
            return this.moduleSymbol;
        }
        return this.moduleSymbol + " as " + this.localSymbol;
    }

    public String moduleSymbol()
    {
        return moduleSymbol;
    }

    public String localSymbol()
    {
        return localSymbol;
    }
}
