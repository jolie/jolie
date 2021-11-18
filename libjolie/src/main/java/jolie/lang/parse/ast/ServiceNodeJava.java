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

package jolie.lang.parse.ast;

import java.util.HashMap;
import java.util.Map;
import jolie.lang.Constants;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;

public class ServiceNodeJava extends ServiceNode {

	private static final long serialVersionUID = Constants.serialVersionUID();
	private final InputPortInfo inputPortInfo;
	private final Map< String, OutputPortInfo > outputPortInfos;

	protected static boolean isConfigurationValid( Map< String, String > config ) {
		return config.containsKey( "class" );
	}

	protected ServiceNodeJava( ParsingContext context, String name, AccessModifier accessModifier, Program p,
		Pair< String, TypeDefinition > parameter, Map< String, String > config ) {
		super( context, name, accessModifier, p, parameter, Constants.EmbeddedServiceType.SERVICENODE_JAVA, config );
		this.outputPortInfos = new HashMap<>();
		InputPortInfo in = null;
		for( OLSyntaxNode node : p.children() ) {
			if( node instanceof OutputPortInfo ) {
				OutputPortInfo op = (OutputPortInfo) node;
				outputPortInfos.put( op.id(), op );
			} else if( node instanceof InputPortInfo ) {
				in = (InputPortInfo) node;
			}
		}
		this.inputPortInfo = in;
	}

	public String classPath() {
		return super.implementationConfiguration().get( "class" );
	}

	public InputPortInfo inputPortInfo() {
		return inputPortInfo;
	}

	public Map< String, OutputPortInfo > outputPortInfos() {
		return outputPortInfos;
	}

}
