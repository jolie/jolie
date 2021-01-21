/*
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2021 Valentino Picotti <valentino.picotti@gmail.com>
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


package jolie.embedding.jolie;

import jolie.cli.CommandLineException;
import jolie.Interpreter;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoader.ExternalEmbeddedServiceConfiguration;
import jolie.runtime.embedding.EmbeddedServiceLoader.ExternalEmbeddedNativeCodeConfiguration;
import jolie.runtime.embedding.EmbeddedServiceLoader.InternalEmbeddedServiceConfiguration;
import jolie.runtime.embedding.EmbeddedServiceLoaderCreationException;
import jolie.runtime.embedding.EmbeddedServiceLoaderFactory;
import jolie.runtime.expression.Expression;

import java.io.IOException;



public class JolieServiceLoaderFactory implements EmbeddedServiceLoaderFactory {
	@Override
	public EmbeddedServiceLoader createLoader( Interpreter interpreter,
		EmbeddedServiceLoader.EmbeddedServiceConfiguration configuration,
		Expression channelDest )
		throws EmbeddedServiceLoaderCreationException {
		EmbeddedServiceLoader ret;
		try {
			switch( configuration.type() ) {
			case INTERNAL:
				InternalEmbeddedServiceConfiguration internalConf =
					(InternalEmbeddedServiceConfiguration) configuration;
				ret = new InternalJolieServiceLoader( channelDest, interpreter,
					internalConf.serviceName(), internalConf.program() );
				break;
			case JOLIE:
				if( configuration instanceof EmbeddedServiceLoader.ExternalEmbeddedServiceConfiguration ) {
					ExternalEmbeddedServiceConfiguration externalConf =
						(ExternalEmbeddedServiceConfiguration) configuration;
					ret = new JolieServiceLoader( channelDest, interpreter, externalConf.servicePath(),
						externalConf.serviceName(), externalConf.params() );
				} else {
					ExternalEmbeddedNativeCodeConfiguration externalNativeCodeConf =
						(ExternalEmbeddedNativeCodeConfiguration) configuration;
					ret = new JolieServiceLoader( externalNativeCodeConf.code(), channelDest, interpreter );
				}
				break;
			case JAVA:
				ExternalEmbeddedServiceConfiguration externalConfiguration =
					(ExternalEmbeddedServiceConfiguration) configuration;
				ret = new JavaServiceLoader( channelDest, externalConfiguration.servicePath(), interpreter );
				break;
			default:
				throw new EmbeddedServiceLoaderCreationException(
					"Could not create Jolie service loader from configuration of type  " + configuration.type() );
			}
		} catch( IOException | CommandLineException e ) {
			throw new EmbeddedServiceLoaderCreationException( e );
		}
		return ret;
	}
}
