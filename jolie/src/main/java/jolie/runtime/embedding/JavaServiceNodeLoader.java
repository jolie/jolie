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

import jolie.Interpreter;
import jolie.JolieClassLoader;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ServiceNodeJava;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.Type;
import jolie.tracer.EmbeddingTraceAction;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Map;

public class JavaServiceNodeLoader extends ServiceNodeLoader {

	private final JavaService service;
	private final Map< String, OutputPortInfo > outputPortInfos;

	protected JavaServiceNodeLoader( Expression channelDest, Interpreter interpreter, ServiceNodeJava serviceNode,
		Expression passingParameter, Type acceptingType ) throws EmbeddedServiceCreationException {
		super( channelDest, interpreter, serviceNode, passingParameter, acceptingType );
		JavaService service = null;
		Map< String, OutputPortInfo > ops = null;
		try {
			service = loadJavaClass( serviceNode.classPath() );
			ops = serviceNode.outputPortInfos();
		} catch( EmbeddedServiceCreationException e ) {
			throw e;
		}
		this.service = service;
		this.outputPortInfos = ops;
	}

	private JavaService loadJavaClass( String classname )
		throws EmbeddedServiceCreationException {
		try {
			final JolieClassLoader cl = super.interpreter().getClassLoader();
			Class< ? > c;
			c = cl.loadClass( classname );
			final Constructor< ? > obj = c.getDeclaredConstructor();
			Object o =
				obj.newInstance();
			if( !(o instanceof JavaService) ) {
				throw new EmbeddedServiceCreationException( classname + " is not a valid JavaService" );
			}
			return (JavaService) o;
		} catch( ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
			| IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
			throw new EmbeddedServiceCreationException( e );
		}
	}

	@Override
	public void load( Value v ) throws EmbeddedServiceLoadingException {
		try {
			service.setInterpreter( super.interpreter() );
			service.setReceivedValue( v );
			service.applyServiceOutputPorts( this.outputPortInfos );
			setChannel( new JavaCommChannel( service ) );
			super.interpreter().tracer().trace( () -> new EmbeddingTraceAction(
				EmbeddingTraceAction.Type.SERVICE_LOAD,
				"Java Service Loader",
				service.getClass().getSimpleName(),
				null ) );
		} catch( IllegalAccessException | IllegalArgumentException | SecurityException | FaultException
			| URISyntaxException e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}

}
