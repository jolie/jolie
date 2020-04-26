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

import jolie.lang.parse.ast.OLSyntaxNode;


/**
 * an abstract class of Symbol declaration in Jolie
 */
public abstract class SymbolInfo
{

    /**
     * Scope of symbol,
     * LOCAL means the symbol's AST node is declared within the local execution environment
     * EXTERNAL means the symbol's AST node is declared in other exceution environment
     */
    public enum Scope {
        LOCAL, EXTERNAL
    }

    /**
     * name of the symbol
     */
    private String name;

    /**
     * scope of this symbol
     */
    private Scope scope;

    /**
     * pointer to an AST node
     */
    private OLSyntaxNode node;

    /**
     * constructor for SymbolInfo, this constructor is used when it knows the ASTnode to point to
     * corresponding to this symbol
     * 
     * @param name  Symbol name
     * @param scope scope of Symbol
     * @param node  an ASTNode
     */
    public SymbolInfo( String name, Scope scope, OLSyntaxNode node )
    {
        this( name, scope );
        this.node = node;
    }

    /**
     * constructor for SymbolInfo, this constructor is used when ASTnode is unknown
     * 
     * @param name  Symbol name
     * @param scope scope of Symbol
     */
    public SymbolInfo( String name, Scope scope )
    {
        this.name = name;
        this.scope = scope;
    }

    public String name()
    {
        return name;
    }

    /**
     * set a pointer to an AST node. the pointer should be only defined once
     */
    public void setPointer( OLSyntaxNode pointer ) throws ModuleException
    {
        if ( this.node != null ) {
            new ModuleException( "Symbol " + this.name() + " AST node pointer is already defined" );
        }
        this.node = pointer;
    }

    public OLSyntaxNode node()
    {
        return node;
    }

    public Scope scope()
    {
        return scope;
    }

    @Override
    public String toString()
    {
        return "SymbolInfo [name=" + name + ", scope=" + scope + "]";
    }


}
