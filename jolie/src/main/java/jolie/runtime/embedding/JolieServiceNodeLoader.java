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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jolie.Interpreter;
import jolie.lang.parse.ast.ServiceNode;
import jolie.runtime.Value;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.Type;

public class JolieServiceNodeLoader extends ServiceNodeLoader {

	protected JolieServiceNodeLoader( Expression channelDest, Interpreter currInterpreter, ServiceNode serviceNode,
		Expression passingParameter, Type acceptingType ) {
		super( channelDest, currInterpreter, serviceNode, passingParameter, acceptingType );
	}

	@Override
	public void load( Value v ) throws EmbeddedServiceLoadingException {

		Interpreter.Configuration configuration = Interpreter.Configuration.create(
			super.interpreter().configuration(),
			new File( "#service_node_" + SERVICE_LOADER_COUNTER.getAndIncrement() ),
			new ByteArrayInputStream( "".getBytes() ) );

		Interpreter interpreter;
		try {
			interpreter = new Interpreter(
				configuration,
				super.interpreter().programDirectory(),
				super.interpreter(),
				super.serviceNode().program(),
				v );
			Future< Exception > f = interpreter.start();
			Exception e = f.get();
			if( e == null ) {
				setChannel( interpreter.commCore().getLocalCommChannel() );
			}
		} catch( IOException | InterruptedException | ExecutionException e ) {
			throw new EmbeddedServiceLoadingException( e );

		}

	}
}
