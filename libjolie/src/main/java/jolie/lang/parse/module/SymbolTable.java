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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.module.SymbolInfo.Scope;

/**
 * A class represent the Symbol table of a Jolie module
 */
public class SymbolTable
{
    /**
     * Symbol target source
     */
    private final URI source;

    /**
     * Maps of Symbolname an corresponding SymbolInfo object
     */
    private final Map< String, SymbolInfo > symbols;

    /**
     * A constructor of SymbolTable
     * 
     * @param source source of Jolie module
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

    /**
     * add a local Symbol with it's ASTNode to the table
     * 
     * @throws ModuleException when adding name duplicate name to the symbol
     */
    public void addSymbol( String name, OLSyntaxNode node ) throws ModuleException
    {
        if ( isDuplicateSymbol( name ) ) {
            throw new ModuleException( "detected redeclaration of symbol " + name );
        }
        this.symbols.put( name, new SymbolInfoLocal( name, node ) );
    }

    /**
     * add an external Symbol with it's module target to the table
     * 
     * @throws ModuleException when adding name duplicate name to the symbol
     */
    public void addSymbol( String name, String[] moduleTargetStrings ) throws ModuleException
    {
        if ( isDuplicateSymbol( name ) ) {
            throw new ModuleException( "detected redeclaration of symbol " + name );
        }
        this.symbols.put( name, new SymbolInfoExternal( name, moduleTargetStrings, name ) );
    }

    /**
     * add an external Symbol
     * 
     * @param name                Symbol name
     * @param moduleTargetStrings an array of String defined at import statement
     * @param moduleSymbol        a name for local environment
     * 
     * @throws ModuleException when adding name duplicate name to the symbol
     */
    public void addSymbol( String name, String[] moduleTargetStrings, String moduleSymbol )
            throws ModuleException
    {
        if ( isDuplicateSymbol( name ) ) {
            throw new ModuleException( "detected redeclaration of symbol " + name );
        }
        this.symbols.put( name, new SymbolInfoExternal( name, moduleTargetStrings, moduleSymbol ) );
    }

    /**
     * add an wildcard Symbol to table
     * 
     * @param moduleTargetStrings an array of String defined at import statement
     */
    public void addWildCardSymbol( String[] moduleTargetStrings )
    {
        this.symbols.put( Arrays.toString( moduleTargetStrings ),
                new SymbolWildCard( moduleTargetStrings ) );
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

    public Optional<SymbolInfo> symbol( String name )
    {
        return Optional.of(this.symbols.get( name ));
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
