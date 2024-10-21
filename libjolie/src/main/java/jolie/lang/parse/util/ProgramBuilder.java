/*
 * Copyright (C) 2019 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.lang.parse.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ImportStatement;
import jolie.lang.parse.ast.ImportableSymbol;
import jolie.lang.parse.ast.ImportableSymbol.AccessModifier;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.context.ParsingContext;

public class ProgramBuilder {
	private final ParsingContext context;
	private final List< OLSyntaxNode > children = new ArrayList<>();

	public ProgramBuilder( ParsingContext context ) {
		this.context = context;
	}

	public void addChild( OLSyntaxNode node ) {
		children.add( node );
	}

	public List< OLSyntaxNode > children() {
		return children;
	}

	public Program toProgram() {
		return new Program( context, children );
	}

	public boolean isJolieModuleSystem() {
		for( OLSyntaxNode node : children ) {
			if( node instanceof DefinitionNode ) {
				if( ((DefinitionNode) node).id().equals( "main" ) ) {
					return false;
				}
			} else if( node instanceof ServiceNode ) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Transform a Jolie execution program into a Jolie Module system program by collects every node
	 * that defining a service and injects those to a newly create "main" service
	 */
	public void transformProgramToModuleSystem() {
		// main service program
		ProgramBuilder mainServiceProgramBuilder = new ProgramBuilder( context );

		ListIterator< OLSyntaxNode > it = children.listIterator();
		while( it.hasNext() ) {
			OLSyntaxNode node = it.next();
			if( !(node instanceof ImportableSymbol) && !(node instanceof ImportStatement) ) {
				mainServiceProgramBuilder.addChild( node );
				it.remove();
			}
		}

		ServiceNode mainService =
			ServiceNode.create( context, ServiceNode.DEFAULT_MAIN_SERVICE_NAME, AccessModifier.PUBLIC,
				mainServiceProgramBuilder.toProgram(), null );

		children.add( mainService );
	}


	/**
	 * Utility function for remove deployment instruction nodes declared in module scope from program
	 * This function is called when parsing a module which contains an include directive
	 */
	public void removeModuleScopeDeploymentInstructions() {
		Set< OLSyntaxNode > toRemove = new HashSet<>();
		for( OLSyntaxNode node : children() ) {
			if( node instanceof OutputPortInfo || node instanceof InputPortInfo
				|| node instanceof EmbeddedServiceNode || node instanceof CourierDefinitionNode ) {
				toRemove.add( node );
			}
		}
		children.removeAll( toRemove );
	}
}
