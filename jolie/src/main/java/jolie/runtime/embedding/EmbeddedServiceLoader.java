/*
 * Copyright (C) 2007-2015 Fabrizio Montesi <famontesi@gmail.com>
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

import java.util.Optional;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ServiceNode;
import jolie.net.CommChannel;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;

public abstract class EmbeddedServiceLoader {
	private final Expression channelDest;

	protected EmbeddedServiceLoader( Expression channelDest ) {
		this.channelDest = channelDest;
	}

	private static EmbeddedServiceLoader createLoader(
		Interpreter interpreter,
		EmbeddedServiceConfiguration configuration,
		Expression channelDest )
		throws EmbeddedServiceLoaderCreationException {
		EmbeddedServiceLoader ret = null;
		try {
			if( configuration.isServiceNode() ) {
				ServiceNodeEmbeddedConfiguration serviceNodeConfiguration =
					(ServiceNodeEmbeddedConfiguration) configuration;
				ret = ServiceNodeLoader.create( channelDest, interpreter, serviceNodeConfiguration.serviceNode,
					serviceNodeConfiguration.parameter() );
			} else {
				String serviceFactoryType = configuration.type().toString();
				switch( configuration.type() ) {
				case JAVA:
				case INTERNAL:
					// Handle JAVA and INTERNAL services with the JolieServiceLoaderFactory
					serviceFactoryType = Constants.EmbeddedServiceType.JOLIE.toString();
				default:
					EmbeddedServiceLoaderFactory factory =
						interpreter.getEmbeddedServiceLoaderFactory( serviceFactoryType );
					if( factory == null ) {
						throw new EmbeddedServiceLoaderCreationException(
							"Could not find extension to load services of type " + serviceFactoryType );
					}
					ret = factory.createLoader( interpreter, configuration, channelDest );
					break;
				}
			}
		} catch( Exception e ) {
			throw new EmbeddedServiceLoaderCreationException( e );
		}

		return ret;
	}

	public static EmbeddedServiceLoader create(
		Interpreter interpreter,
		EmbeddedServiceConfiguration configuration,
		Value channelValue )
		throws EmbeddedServiceLoaderCreationException {
		return createLoader( interpreter, configuration, channelValue );
	}

	public static EmbeddedServiceLoader create(
		Interpreter interpreter,
		EmbeddedServiceConfiguration configuration,
		VariablePath channelPath )
		throws EmbeddedServiceLoaderCreationException {
		return createLoader( interpreter, configuration, channelPath );
	}

	protected void setChannel( CommChannel channel ) {
		if( channelDest != null ) {
			if( channelDest instanceof VariablePath ) {
				((VariablePath) channelDest).getValue().setValue( channel );
			} else if( channelDest instanceof Value ) {
				((Value) channelDest).setValue( channel );
			}
		}
	}

	public void exit() {}

	public abstract void load()
		throws EmbeddedServiceLoadingException;

	public static abstract class EmbeddedServiceConfiguration {
		private final Constants.EmbeddedServiceType type;

		public EmbeddedServiceConfiguration( Constants.EmbeddedServiceType type ) {
			this.type = type;
		}

		public Constants.EmbeddedServiceType type() {
			return this.type;
		}

		public boolean isInternal() {
			return this.type.equals( Constants.EmbeddedServiceType.INTERNAL );
		}

		public boolean isServiceNode() {
			return this.type.equals( Constants.EmbeddedServiceType.SERVICENODE )
				|| this.type.equals( Constants.EmbeddedServiceType.SERVICENODE_JAVA );
		}
	}

	public static class InternalEmbeddedServiceConfiguration extends EmbeddedServiceConfiguration {
		private final String serviceName;
		private final Program program;

		/**
		 *
		 * @param serviceName Name of the internal service.
		 * @param program the program containing the service
		 */
		public InternalEmbeddedServiceConfiguration( String serviceName, Program program ) {
			super( Constants.EmbeddedServiceType.INTERNAL );
			this.serviceName = serviceName;
			this.program = program;
		}

		public String serviceName() {
			return serviceName;
		}

		public Program program() {
			return program;
		}
	}

	public static class ExternalEmbeddedServiceConfiguration extends EmbeddedServiceConfiguration {
		private final String servicePath;
		private final Optional< String > serviceName;
		private final Optional< Value > params;

		/**
		 *
		 * @param type Type of embedded service, cannot be INTERNAL
		 * @param servicePath path of service
		 * @param params the actual parameters to be passed to the service, if any
		 */
		public ExternalEmbeddedServiceConfiguration( Constants.EmbeddedServiceType type, String servicePath,
			Optional< String > serviceName, Optional< Value > params ) {
			super( type );
			this.servicePath = servicePath;
			this.serviceName = serviceName;
			this.params = params;

			assert type != Constants.EmbeddedServiceType.INTERNAL;
		}

		public Optional< String > serviceName() {
			return serviceName;
		}

		public String servicePath() {
			return servicePath;
		}

		public Optional< Value > params() {
			return params;
		}
	}

	public static class ExternalEmbeddedNativeCodeConfiguration extends EmbeddedServiceConfiguration {
		private final String code;

		/**
		 * @param code code of service
		 */
		public ExternalEmbeddedNativeCodeConfiguration( String code ) {
			super( Constants.EmbeddedServiceType.JOLIE );
			this.code = code;
		}

		public String code() {
			return code;
		}
	}

	public static class ServiceNodeEmbeddedConfiguration extends EmbeddedServiceConfiguration {
		private final ServiceNode serviceNode;
		private final Expression parameter;

		public ServiceNodeEmbeddedConfiguration( Constants.EmbeddedServiceType type, ServiceNode node,
			Expression parameter ) {
			super( type );
			this.serviceNode = node;
			this.parameter = parameter;
		}

		public ServiceNode serviceNode() {
			return this.serviceNode;
		}

		public Expression parameter() {
			return this.parameter;
		}
	}
}
