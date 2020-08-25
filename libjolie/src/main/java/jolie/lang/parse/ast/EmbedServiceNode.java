package jolie.lang.parse.ast;

import jolie.lang.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

public class EmbedServiceNode extends OLSyntaxNode {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final OLSyntaxNode passingParameter;
	private final String bindingPortName;
	private final Constants.EmbeddedServiceType type;
	private final boolean isNewPort;

	private ServiceNode service;

	public EmbedServiceNode( ParsingContext context, String serviceName,
		String bindingPortName, boolean isNewPort, OLSyntaxNode passingParam ) {
		super( context );
		this.type = Constants.EmbeddedServiceType.JOLIE;
		this.passingParameter = passingParam;
		this.bindingPortName = bindingPortName;
		this.isNewPort = isNewPort;
	}

	public String serviceName() {
		return service.name();
	}

	public void setService( ServiceNode node ) {
		this.service = node;
	}

	public boolean hasBindingPort() {
		return this.bindingPortName != null;
	}

	public String bindingPortName() {
		return this.bindingPortName;
	}

	public ServiceNode service() {
		return this.service;
	}

	public OLSyntaxNode passingParameter() {
		return this.passingParameter;
	}

	public Constants.EmbeddedServiceType type() {
		return type;
	}
	
	/**
	 * @return the flag indicates the present of 'new' keyword on embedding
	 */
	public boolean isNewPort() {
		return isNewPort;
	}

	@Override
	public void accept( OLVisitor visitor ) {
		// TODO Auto-generated method stub
	}

}
