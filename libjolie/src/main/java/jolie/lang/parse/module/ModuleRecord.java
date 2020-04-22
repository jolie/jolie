package jolie.lang.parse.module;

import java.net.URI;
import jolie.lang.parse.ast.Program;

public class ModuleRecord
{
    private final URI source;
    private final Program program;
    private final SymbolTable symbolTable;

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

    /**
     * @return the symbolTable
     */
    public SymbolTable symbolTable()
    {
        return symbolTable;
    }

    public Source[] dependency()
    {
        return this.symbolTable.dependency();
    }

    @Override
    public String toString()
    {
        return "ModuleRecord [source=" + source + ", symbolTable="
                + symbolTable + "]";
    }
    

}
