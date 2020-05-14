/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang.parse.module;

import java.net.URI;
import java.util.Optional;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.exceptions.DuplicateSymbolException;

/**
 * A class represent a Jolie module record, which contain an URI of source, a AST tree and
 * Symboltable
 */
public class ModuleRecord
{
    private final URI source;
    private final Program program;
    private SymbolTable symbolTable;

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

    public void setSymbolTable( SymbolTable symbolTable )
    {
        this.symbolTable = symbolTable;
    }

    public void addWildcardImportedRecord( SymbolWildCard symbol, SymbolInfo ... symbolsFromWildcard ) throws DuplicateSymbolException
    {
        this.symbolTable.replaceWildCardSymbol(symbol, symbolsFromWildcard);
    }

    /**
     * @return the symbolTable
     */
    public SymbolTable symbolTable()
    {
        return symbolTable;
    }

    public Optional<SymbolInfo> symbol( String name )
    {
        return this.symbolTable.symbol( name );
    }

    public SymbolInfoExternal[] externalSymbols()
    {
        return this.symbolTable.externalSymbols();
    }

    public SymbolInfoLocal[] localSymbols()
    {
        return this.symbolTable.localSymbols();
    }

    public SymbolInfo[] symbols()
    {
        return this.symbolTable.symbols();
    }

    @Override
    public String toString()
    {
        return "ModuleRecord [source=" + source + ", symbolTable=" + symbolTable + "]";
    }


}
