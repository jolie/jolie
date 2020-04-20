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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.types.TypeDefinition;



public class ImportResult
{
    private final List< OLSyntaxNode > nodes;
    private final Map< String, TypeDefinition > types;
    private final Map< String, InterfaceDefinition > interfaces;
    private final Map< String, DefinitionNode > procedures;

    public ImportResult()
    {
        this.nodes = new ArrayList<>();
        this.types = new HashMap< String, TypeDefinition >();
        this.interfaces = new HashMap< String, InterfaceDefinition >();
        this.procedures = new HashMap< String, DefinitionNode >();
    }

    public void prependResult( ImportResult re )
    {
        for (OLSyntaxNode n : re.nodes) {
            this.nodes.add( 0, n );
        }
        for (Map.Entry< String, TypeDefinition > entry : re.types.entrySet()) {
            this.types.put( entry.getKey(), entry.getValue() );
        }
        for (Map.Entry< String, InterfaceDefinition > entry : re.interfaces.entrySet()) {
            this.interfaces.put( entry.getKey(), entry.getValue() );
        }
        for (Map.Entry< String, DefinitionNode > entry : re.procedures.entrySet()) {
            this.procedures.put( entry.getKey(), entry.getValue() );
        }
    }

    public void addResult( ImportResult re )
    {
        for (OLSyntaxNode n : re.nodes) {
            this.nodes.add( n );
        }
        for (Map.Entry< String, TypeDefinition > entry : re.types.entrySet()) {
            this.types.put( entry.getKey(), entry.getValue() );
        }
        for (Map.Entry< String, InterfaceDefinition > entry : re.interfaces.entrySet()) {
            this.interfaces.put( entry.getKey(), entry.getValue() );
        }
        for (Map.Entry< String, DefinitionNode > entry : re.procedures.entrySet()) {
            this.procedures.put( entry.getKey(), entry.getValue() );
        }
    }

    public void addNode( OLSyntaxNode n )
    {
        if ( n instanceof TypeDefinition ) {
            this.addType( (TypeDefinition) n );
        }
        if ( n instanceof InterfaceDefinition ) {
            this.addInterface( (InterfaceDefinition) n );
        }
        if ( n instanceof DefinitionNode ) {
            this.addProcedure( (DefinitionNode) n );
        }
        this.nodes.add( n );
    }

    public List< OLSyntaxNode > nodes()
    {
        return nodes;
    }

    public void addType( TypeDefinition td )
    {
        this.types.put( td.id(), td );
    }

    public Map< String, TypeDefinition > types()
    {
        return types;
    }

    public void addInterface( InterfaceDefinition id )
    {
        this.interfaces.put( id.name(), id );
    }

    public Map< String, InterfaceDefinition > interfaces()
    {
        return interfaces;
    }

    public void addProcedure( DefinitionNode dn )
    {
        this.procedures.put( dn.id(), dn );
    }

    public Map< String, DefinitionNode > procedures()
    {
        return procedures;
    }
}
