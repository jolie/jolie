package jolie.lang.parse.ast;

import jolie.lang.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

public class EmbedServiceNode extends OLSyntaxNode {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final OLSyntaxNode passingParameter;
	private final OutputPortInfo bindingPort;
	private final boolean isNewPort;
	private final String serviceName;

	private ServiceNode service;

	public EmbedServiceNode( ParsingContext context, String serviceName,
		OutputPortInfo bindingPort, boolean isNewPort, OLSyntaxNode passingParam ) {
		super( context );
		this.serviceName = serviceName;
		this.passingParameter = passingParam;
		this.bindingPort = bindingPort;
		this.isNewPort = isNewPort;
	}

	public String serviceName() {
		return this.serviceName;
	}

	public void setService( ServiceNode node ) {
		this.service = node;
	}

	public boolean hasBindingPort() {
		return this.bindingPort != null;
	}

	public OutputPortInfo bindingPort() {
		return this.bindingPort;
	}

	public ServiceNode service() {
		return this.service;
	}

	public OLSyntaxNode passingParameter() {
		return this.passingParameter;
	}

	/**
	 * @return the flag indicates the present of 'new' keyword on embedding
	 */
	public boolean isNewPort() {
		return isNewPort;
	}

	@Override
	public void accept( OLVisitor visitor ) {
		visitor.visit( this );
	}

}
