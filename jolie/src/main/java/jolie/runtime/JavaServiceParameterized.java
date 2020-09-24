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

package jolie.runtime;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.runtime.embedding.java.Inject;
import jolie.runtime.embedding.java.OutputPort;
import jolie.util.Pair;

public abstract class JavaServiceParameterized extends JavaService {

	private Value receivedValue;

	public Value receivedValue() {
		return receivedValue;
	}

	public void setReceivedValue( Value v ) {
		this.receivedValue = v;
	}

	public void applyServiceOutputPorts( Map< String, OutputPortInfo > ops )
		throws IllegalArgumentException, IllegalAccessException, FaultException, URISyntaxException {

		for( Field field : this.getClass().getDeclaredFields() ) {
			final boolean isOutputPort = field.getAnnotation( Inject.class ) != null;
			if( isOutputPort ) {
				String outputPortName = field.getName();
				field.setAccessible( true );
				try {
					if( !ops.containsKey( outputPortName ) ) {
						throw new FaultException( "Missing outputPort",
							"unable to locate outputPort \""
								+ outputPortName + "\" in service " + this.getClass().getSimpleName() + "." );
					}
					OutputPortInfo op = ops.get( outputPortName );
					URI location = null;
					if( op.location() instanceof ConstantStringExpression ) {
						location = new URI( op.location().toString() );
					} else if( op.location() instanceof VariableExpressionNode ) {
						VariableExpressionNode expr = (VariableExpressionNode) op.location();
						VariablePathBuilder builder = new VariablePathBuilder( false );
						for( Pair< OLSyntaxNode, OLSyntaxNode > path : expr.variablePath().path() ) {
							builder.add( path.key().toString(), 0 );
						}
						location = new URI( builder.toClosedVariablePath( receivedValue ).evaluate().strValue() );
						if( !location.toString().startsWith( "local" ) ) {
							throw new FaultException( "Invalid scheme " + location.toString(),
								"expected output port with location \"local\" for a Java service." );
						}
					}
					field.set( this, OutputPort.create( super.interpreter(), location ) );
				} catch( IllegalArgumentException | IllegalAccessException | URISyntaxException e ) {
					throw e;
				}
			}
		}
	}
}
