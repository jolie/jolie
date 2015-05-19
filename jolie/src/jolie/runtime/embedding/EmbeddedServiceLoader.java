/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


package jolie.runtime.embedding;

import jolie.runtime.expression.Expression;
import jolie.runtime.*;
import jolie.lang.Constants;
import jolie.Interpreter;
import jolie.lang.parse.ast.Program;
import jolie.net.CommChannel;

public abstract class EmbeddedServiceLoader
{
	final private Expression channelDest;

	protected EmbeddedServiceLoader( Expression channelDest )
	{
		this.channelDest = channelDest;
	}

	private static EmbeddedServiceLoader createLoader(
		Interpreter interpreter,
		EmbeddedServiceConfiguration configuration,
		Expression channelDest
	)
		throws EmbeddedServiceLoaderCreationException
	{
		EmbeddedServiceLoader ret = null;
		try {
			if ( configuration.isInternal() ) {
				InternalEmbeddedServiceConfiguration internalConfiguration = (InternalEmbeddedServiceConfiguration) configuration;
				ret = new InternalJolieServiceLoader( channelDest, interpreter, internalConfiguration.serviceName(), internalConfiguration.program() );
			} else {
				ExternalEmbeddedServiceConfiguration externalConfiguration = (ExternalEmbeddedServiceConfiguration) configuration;
				switch( configuration.type() ) {
					case JAVA:
						ret = new JavaServiceLoader( channelDest, externalConfiguration.servicePath(), interpreter );
						break;
					case JOLIE:
						ret = new JolieServiceLoader( channelDest, interpreter, externalConfiguration.servicePath() );
						break;
					case JAVASCRIPT:
						ret = new JavaScriptServiceLoader( channelDest, externalConfiguration.servicePath() );
						break;
					default:
						throw new EmbeddedServiceLoaderCreationException( "Invalid embedded service type specified" );
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
		Value channelValue
	)
		throws EmbeddedServiceLoaderCreationException
	{
		return createLoader( interpreter, configuration, channelValue );
	}

	public static EmbeddedServiceLoader create(
		Interpreter interpreter,
		EmbeddedServiceConfiguration configuration,
		VariablePath channelPath
	)
		throws EmbeddedServiceLoaderCreationException
	{
		return createLoader( interpreter, configuration, channelPath );
	}

	protected void setChannel( CommChannel channel )
	{
		if ( channelDest != null ) {
			if ( channelDest instanceof VariablePath ) {
				((VariablePath) channelDest).getValue().setValue( channel );
			} else if ( channelDest instanceof Value ) {
				((Value) channelDest).setValue( channel );
			}
		}
	}

	abstract public void load()
		throws EmbeddedServiceLoadingException;

	public static abstract class EmbeddedServiceConfiguration
	{
		private final Constants.EmbeddedServiceType type;

		public EmbeddedServiceConfiguration( Constants.EmbeddedServiceType type )
		{
			this.type = type;
		}

		public Constants.EmbeddedServiceType type()
		{
			return this.type;
		}

		public boolean isInternal()
		{
			return this.type.equals( Constants.EmbeddedServiceType.INTERNAL );
		}
	}

	public static class InternalEmbeddedServiceConfiguration extends EmbeddedServiceConfiguration
	{
		private final String serviceName;
		private final Program program;

		/**
		 *
		 * @param serviceName Name of the internal service.
		 * @param program the program containing the service
		 */
		public InternalEmbeddedServiceConfiguration( String serviceName, Program program )
		{
			super( Constants.EmbeddedServiceType.INTERNAL );

			this.serviceName = serviceName;
			this.program = program;
		}

		public String serviceName()
		{
			return serviceName;
		}

		public Program program()
		{
			return program;
		}
	}

	public static class ExternalEmbeddedServiceConfiguration extends EmbeddedServiceConfiguration
	{
		private final String servicePath;

		/**
		 *
		 * @param type Type of embedded service, cannot be INTERNAL
		 * @param servicePath path of service
		 */
		public ExternalEmbeddedServiceConfiguration( Constants.EmbeddedServiceType type, String servicePath )
		{
			super( type );
			this.servicePath = servicePath;

			assert type != Constants.EmbeddedServiceType.INTERNAL;
		}

		public String servicePath()
		{
			return servicePath;
		}

	}
}