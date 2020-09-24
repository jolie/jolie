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

package jolie.runtime.embedding;

import java.util.concurrent.atomic.AtomicLong;
import jolie.Interpreter;
import jolie.lang.Constants.EmbeddedServiceType;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.ServiceNodeJava;
import jolie.runtime.Value;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;

public abstract class ServiceNodeLoader extends EmbeddedServiceLoader {
	private final Interpreter currInterpreter;
	private final ServiceNode serviceNode;
	private final Expression passingParameter;
	private final Type acceptingType;
	protected final static AtomicLong SERVICE_LOADER_COUNTER = new AtomicLong();

	protected ServiceNodeLoader( Expression channelDest, Interpreter currInterpreter,
		ServiceNode serviceNode, Expression passingParameter, Type acceptingType ) {
		super( channelDest );
		this.currInterpreter = currInterpreter;
		this.serviceNode = serviceNode;
		this.passingParameter = passingParameter;
		this.acceptingType = acceptingType;
	}

	public abstract void load( Value v ) throws EmbeddedServiceLoadingException;

	public static ServiceNodeLoader create( Expression channelDest, Interpreter currInterpreter,
		ServiceNode serviceNode, Expression passingParameter, Type acceptingType )
		throws EmbeddedServiceCreationException {
		if( serviceNode.type() == EmbeddedServiceType.SERVICENODE ) {
			return new JolieServiceNodeLoader( channelDest, currInterpreter, serviceNode, passingParameter,
				acceptingType );
		} else if( serviceNode.type() == EmbeddedServiceType.SERVICENODE_JAVA ) {
			return new JavaServiceNodeLoader( channelDest, currInterpreter, (ServiceNodeJava) serviceNode,
				passingParameter, acceptingType );
		}
		return null;
	}

	@Override
	public void load() throws EmbeddedServiceLoadingException {

		try {
			Value passingValue = passingParameter == null ? Value.create() : passingParameter.evaluate();
			Value pathValue = Value.create();
			if( this.serviceNode.parameterType().isPresent() ) {
				this.acceptingType.check( passingValue );
				pathValue.getChildren( this.serviceNode.parameterPath().get() ).first()
					.deepCopy( passingValue );
			}

			load( pathValue );
		} catch( TypeCheckingException e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}

	public String serviceName() {
		return serviceNode.name();
	}

	public Interpreter interpreter() {
		return currInterpreter;
	}

	public ServiceNode serviceNode() {
		return serviceNode;
	}

}
