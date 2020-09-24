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

import java.util.Map;
import java.util.Optional;
import jolie.lang.Constants;
import jolie.lang.Constants.EmbeddedServiceType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;

public class ServiceNode extends OLSyntaxNode implements ImportableSymbol {

	static class ParameterConfiguration {
		private final TypeDefinition type;
		private final String variablePath;

		public ParameterConfiguration( Pair< String, TypeDefinition > parameter ) {
			this( parameter.value(), parameter.key() );
		}

		public ParameterConfiguration( TypeDefinition type, String variablePath ) {
			this.type = type;
			this.variablePath = variablePath;
		}

		public TypeDefinition type() {
			return type;
		}

		public String variablePath() {
			return variablePath;
		}
	}

	private static final long serialVersionUID = Constants.serialVersionUID();
	private final String name;
	private final Program program;
	private final Optional< ParameterConfiguration > parameter;
	private final AccessModifier accessModifier;
	private final Constants.EmbeddedServiceType type;
	private final Map< String, String > config;

	public static ServiceNode create( ParsingContext context, String name, AccessModifier accessModifier, Program p,
		Pair< String, TypeDefinition > parameter, Constants.EmbeddedServiceType technology,
		Map< String, String > implementationConfiguration ) {
		if( implementationConfiguration == null ) {
			new ServiceNode( context, name, accessModifier, p, parameter,
				Constants.EmbeddedServiceType.SERVICENODE );
		}
		Map< String, String > config = implementationConfiguration;
		if( technology == EmbeddedServiceType.SERVICENODE_JAVA && ServiceNodeJava.isConfigurationValid( config ) ) {
			return new ServiceNodeJava( context, name, accessModifier, p, parameter,
				config );
		}
		return null;
	}

	public static ServiceNode create( ParsingContext context, String name, AccessModifier accessModifier, Program p,
		Pair< String, TypeDefinition > parameter ) {
		return new ServiceNode( context, name, accessModifier, p, parameter,
			Constants.EmbeddedServiceType.SERVICENODE );
	}

	protected ServiceNode( ParsingContext context, String name, AccessModifier accessModifier, Program p,
		Pair< String, TypeDefinition > parameter,
		Constants.EmbeddedServiceType type ) {
		this( context, name, accessModifier, p, parameter, type, null );
	}

	protected ServiceNode( ParsingContext context, String name, AccessModifier accessModifier, Program p,
		Pair< String, TypeDefinition > parameter,
		Constants.EmbeddedServiceType type, Map< String, String > config ) {
		super( context );
		this.name = name;
		this.accessModifier = accessModifier;
		this.program = p;
		if( parameter != null ) {
			this.parameter = Optional.of( new ParameterConfiguration( parameter ) );
		} else {
			this.parameter = Optional.empty();
		}
		this.type = type;
		this.config = config;
	}

	public boolean hasParameter() {
		return this.parameter.isPresent();
	}

	public Optional< TypeDefinition > parameterType() {
		if( !this.hasParameter() ) {
			return Optional.empty();
		}
		return Optional.of( this.parameter.get().type() );
	}

	public Optional< String > parameterPath() {
		if( !this.hasParameter() ) {
			return Optional.empty();
		}
		return Optional.of( this.parameter.get().variablePath() );
	}

	public Program program() {
		return this.program;
	}

	public Constants.EmbeddedServiceType type() {
		return type;
	}

	public Map< String, String > implementationConfiguration() {
		return config;
	}

	@Override
	public void accept( OLVisitor visitor ) {
		visitor.visit( this );
	}

	@Override
	public AccessModifier accessModifier() {
		return this.accessModifier;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public OLSyntaxNode node() {
		return this;
	}
}
