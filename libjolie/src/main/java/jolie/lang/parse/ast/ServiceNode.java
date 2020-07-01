package jolie.lang.parse.ast;

import java.util.Optional;
import jolie.lang.Constants;
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

	public ServiceNode( ParsingContext context, String name, AccessModifier accessModifier, Program p,
		Pair< String, TypeDefinition > parameter,
		Constants.EmbeddedServiceType type ) {
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
