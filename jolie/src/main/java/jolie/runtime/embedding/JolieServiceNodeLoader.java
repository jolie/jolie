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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jolie.Interpreter;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InputPortInfo.AggregationItemInfo;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.module.ModuleSource;
import jolie.lang.parse.util.ProgramBuilder;
import jolie.runtime.Value;
import jolie.runtime.expression.Expression;

public class JolieServiceNodeLoader extends ServiceNodeLoader {
	private volatile Interpreter loadedInterpreter = null;

	protected JolieServiceNodeLoader( Expression channelDest, Interpreter currInterpreter, ServiceNode serviceNode,
		Expression passingParameter ) {
		super( channelDest, currInterpreter, serviceNode, passingParameter );
	}

	@Override
	public void load( Value v ) throws EmbeddedServiceLoadingException {
		ModuleSource source = ModuleSource.create( serviceNode() );
		Interpreter.Configuration configuration = Interpreter.Configuration.create(
			super.interpreter().configuration(), source );

		Interpreter interpreter;
		try {
			ProgramBuilder builder = new ProgramBuilder( serviceNode().context() );
			builder.addChild( serviceNode() );

			// add all interface extender definitions to program builder
			for( OLSyntaxNode node : serviceNode().program().children() ) {
				if( node instanceof InputPortInfo ) {
					for( AggregationItemInfo aggregationItemInfo : ((InputPortInfo) node).aggregationList() ) {
						if( aggregationItemInfo.interfaceExtender() != null ) {
							builder.addChild( aggregationItemInfo.interfaceExtender() );
						}
					}
				}
			}
			interpreter = new Interpreter(
				configuration,
				interpreter().symbolTables(),
				builder.toProgram(),
				v,
				interpreter().logPrefix() );

			Future< Exception > f = interpreter.start();
			Exception e = f.get();
			if( e == null ) {
				setChannel( interpreter.commCore().getLocalCommChannel() );
				loadedInterpreter = interpreter;
			}
		} catch( IOException | InterruptedException | ExecutionException e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}

	@Override
	public void exit() {
		if( loadedInterpreter != null ) {
			loadedInterpreter.exit();
		}
	}
}
