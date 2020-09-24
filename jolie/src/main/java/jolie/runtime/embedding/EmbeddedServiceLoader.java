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

import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ServiceNode;
import jolie.net.CommChannel;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.Type;

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
					serviceNodeConfiguration.parameter(), serviceNodeConfiguration.acceptingParameterType() );
			} else if( configuration.isInternal() ) {
				InternalEmbeddedServiceConfiguration internalConfiguration =
					(InternalEmbeddedServiceConfiguration) configuration;
				ret = new InternalJolieServiceLoader( channelDest, interpreter, internalConfiguration.serviceName(),
					internalConfiguration.program() );
			} else {
				ExternalEmbeddedServiceConfiguration externalConfiguration;
				switch( configuration.type() ) {
				case JAVA:
					externalConfiguration = (ExternalEmbeddedServiceConfiguration) configuration;
					ret = new JavaServiceLoader( channelDest, externalConfiguration.servicePath(), interpreter );
					break;
				case JOLIE:
					if( configuration instanceof ExternalEmbeddedServiceConfiguration ) {
						externalConfiguration = (ExternalEmbeddedServiceConfiguration) configuration;
						ret = new JolieServiceLoader( channelDest, interpreter, externalConfiguration.servicePath() );
					} else {
						ExternalEmbeddedNativeCodeConfiguration externalEmbeddedNativeCodeConfiguration =
							(ExternalEmbeddedNativeCodeConfiguration) configuration;
						ret = new JolieServiceLoader( externalEmbeddedNativeCodeConfiguration.code(), channelDest,
							interpreter );
					}
					break;
				default:
					String serviceType = configuration.type().toString();
					EmbeddedServiceLoaderFactory factory = interpreter.getEmbeddedServiceLoaderFactory( serviceType );
					if( factory == null ) {
						throw new EmbeddedServiceLoaderCreationException(
							"Could not find extension to load services of type " + serviceType );
					}
					externalConfiguration = (ExternalEmbeddedServiceConfiguration) configuration;
					ret = factory.createLoader( interpreter, serviceType, externalConfiguration.servicePath(),
						channelDest );
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

		/**
		 *
		 * @param type Type of embedded service, cannot be INTERNAL
		 * @param servicePath path of service
		 */
		public ExternalEmbeddedServiceConfiguration( Constants.EmbeddedServiceType type, String servicePath ) {
			super( type );
			this.servicePath = servicePath;

			assert type != Constants.EmbeddedServiceType.INTERNAL;
		}

		public String servicePath() {
			return servicePath;
		}

	}

	public static class ExternalEmbeddedNativeCodeConfiguration extends EmbeddedServiceConfiguration {
		private final String code;

		/**
		 *
		 * @param type Type of embedded service, cannot be INTERNAL
		 * @param code code of service
		 */
		public ExternalEmbeddedNativeCodeConfiguration( Constants.EmbeddedServiceType type, String code ) {
			super( type );
			this.code = code;

			assert type != Constants.EmbeddedServiceType.INTERNAL;
		}

		public String code() {
			return code;
		}
	}

	public static class ServiceNodeEmbeddedConfiguration extends EmbeddedServiceConfiguration {
		private final ServiceNode serviceNode;
		private final Expression parameter;
		private final Type acceptingParameterType;

		public ServiceNodeEmbeddedConfiguration( Constants.EmbeddedServiceType type, ServiceNode node,
			Expression parameter, Type acceptingParameterType ) {
			super( type );
			this.serviceNode = node;
			this.parameter = parameter;
			this.acceptingParameterType = acceptingParameterType;
		}

		public ServiceNode serviceNode() {
			return this.serviceNode;
		}

		public Expression parameter() {
			return this.parameter;
		}

		public Type acceptingParameterType() {
			return this.acceptingParameterType;
		}

	}
}
