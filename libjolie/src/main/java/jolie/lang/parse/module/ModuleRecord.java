package jolie.lang.parse.module;

import java.net.URI;
import jolie.lang.parse.ast.Program;

public class ModuleRecord
{
    private final URI source;
    private final Program program;
    private SymbolTable symbolTable;

    public ModuleRecord( URI source, Program program )
    {
        this.source = source;
        this.program = program;
    }

    /**
     * @param program
     * @param symbolTable
     */
    public ModuleRecord( URI source, Program program, SymbolTable symbolTable )
    {
        this.source = source;
        this.program = program;
        this.symbolTable = symbolTable;
    }

    /**
     * @return the absolute URI to the Module
     */
    public URI source()
    {
        return source;
    }

    /**
     * @return the program
     */
    public Program program()
    {
        return program;
    }

    public void setSymbolTable(SymbolTable symbolTable)
    {
        this.symbolTable = symbolTable;
    }

    /**
     * @return the symbolTable
     */
    public SymbolTable symbolTable()
    {
        return symbolTable;
    }

    public SymbolInfo symbol(String name){
        return this.symbolTable.symbol(name);
    }

    public SymbolInfoExternal[] externalSymbols(){
        return this.symbolTable.externalSymbols();
    }

    public SymbolInfoLocal[] localSymbols(){
        return this.symbolTable.localSymbols();
    }

    public SymbolInfo[] symbols(){
        return this.symbolTable.symbols();
    }

    @Override
    public String toString()
    {
        return "ModuleRecord [source=" + source + ", symbolTable="
                + symbolTable + "]";
    }
    

}
