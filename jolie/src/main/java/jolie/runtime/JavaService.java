/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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


package jolie.runtime;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import jolie.Interpreter;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.LocalCommChannel;
import jolie.runtime.embedding.JavaServiceHelpers;
import jolie.runtime.embedding.RequestResponse;
import jolie.runtime.embedding.java.Inject;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.OutputPort;
import jolie.util.Pair;

/**
 *
 * @author Fabrizio Montesi
 */
public abstract class JavaService {
	@FunctionalInterface
	private interface JavaOperationCallable {
		CommMessage call( JavaService service, JavaOperation javaOperation, CommMessage message )
			throws IllegalAccessException;
	}

	public interface ValueConverter {
	}

	private static class JavaOperation {
		private final Method method;
		private final Method parameterConstructor;
		private final Method returnValueConstructor;
		private final JavaOperationCallable callable;

		private JavaOperation(
			Method method,
			Method parameterConstructor,
			Method returnValueConstructor,
			JavaOperationCallable callable ) {
			this.method = method;
			this.parameterConstructor = parameterConstructor;
			this.returnValueConstructor = returnValueConstructor;
			this.callable = callable;
		}
	}

	protected static class Embedder {
		private final Interpreter interpreter;

		private Embedder( Interpreter interpreter ) {
			this.interpreter = interpreter;
		}

		public void callOneWay( CommMessage request )
			throws IOException {
			LocalCommChannel c = interpreter.commCore().getLocalCommChannel();
			try {
				c.send( request );
				c.recvResponseFor( request ).get();
			} catch( ExecutionException | InterruptedException | IOException e ) {
				throw new IOException( e );
			}
		}

		public void callOneWay( String operationName, Value requestValue )
			throws IOException {
			callOneWay( CommMessage.createRequest( operationName, "/", requestValue ) );
		}

		public void callOneWay( String operationName, JolieValue requestValue ) throws IOException {
			callOneWay( CommMessage.createRequest( operationName, "/", JolieValue.toValue( requestValue ) ) );
		}

		public Value callRequestResponse( CommMessage request )
			throws IOException, FaultException {
			LocalCommChannel c = interpreter.commCore().getLocalCommChannel();
			try {
				c.send( request );
				CommMessage response = c.recvResponseFor( request ).get();
				if( response.isFault() ) {
					throw response.fault();
				}
				return response.value();
			} catch( ExecutionException | InterruptedException | IOException e ) {
				throw new IOException( e );
			}
		}

		public Value callRequestResponse( String operationName, Value requestValue )
			throws IOException, FaultException {
			return callRequestResponse( CommMessage.createRequest( operationName, "/", requestValue ) );
		}

		public JolieValue callRequestResponse( String operationName, JolieValue requestValue )
			throws IOException, FaultException {
			return JolieValue.fromValue( callRequestResponse(
				CommMessage.createRequest( operationName, "/", JolieValue.toValue( requestValue ) ) ) );
		}
	}

	private Interpreter interpreter;
	private final Map< String, JavaOperation > operations;
	private Value receivedValue;

	public JavaService() {
		Map< String, JavaOperation > ops = new HashMap<>();

		Class< ? >[] params;
		for( Method method : this.getClass().getDeclaredMethods() ) {
			if( Modifier.isPublic( method.getModifiers() ) ) {
				params = method.getParameterTypes();
				if( params.length == 1 ) {
					checkMethod( ops, method, getFromValueConverter( params[ 0 ] ) );

				} else if( params.length == 0 ) {
					checkMethod( ops, method, null );
				}
			}
		}
		this.operations = Collections.unmodifiableMap( ops );
	}

	private static String getMethodName( Method method ) {
		Identifier identifier = method.getAnnotation( Identifier.class );
		if( identifier == null ) {
			return method.getName();
		}

		return identifier.value();
	}

	// Warning: this MUST NOT contain void.
	/*
	 * When you add something here, be sure that you define the appropriate "create..." static method in
	 * JavaServiceHelpers.
	 */
	private static final Class< ? >[] SUPPORTED_TYPES = new Class[] {
		Value.class, String.class, Integer.class, Double.class, Boolean.class,
		Long.class, ByteArray.class
	};
	private static final Method[] TO_VALUE_CONVERTERS;
	private static final Method[] FROM_VALUE_CONVERTERS;

	static {
		TO_VALUE_CONVERTERS = new Method[ SUPPORTED_TYPES.length ];
		try {
			TO_VALUE_CONVERTERS[ 0 ] = JavaServiceHelpers.class.getMethod( "createValue", Value.class );
			for( int i = 1; i < SUPPORTED_TYPES.length; i++ ) {
				TO_VALUE_CONVERTERS[ i ] = Value.class.getMethod( "create", SUPPORTED_TYPES[ i ] );
			}
		} catch( NoSuchMethodException e ) {
			e.printStackTrace();
			assert false;
		}

		FROM_VALUE_CONVERTERS = new Method[ SUPPORTED_TYPES.length ];
		try {
			FROM_VALUE_CONVERTERS[ 0 ] = JavaServiceHelpers.class.getMethod( "createValue", Value.class );
			for( int i = 1; i < SUPPORTED_TYPES.length; i++ ) {
				FROM_VALUE_CONVERTERS[ i ] =
					JavaServiceHelpers.class.getMethod( "valueTo" + SUPPORTED_TYPES[ i ].getSimpleName(), Value.class );
			}
		} catch( NoSuchMethodException e ) {
			e.printStackTrace();
			assert false;
		}
	}

	private static Method getToValueConverter( Class< ? > param ) {
		if( param == null ) {
			return null;
		}

		if( ValueConverter.class.isAssignableFrom( param ) ) {
			try {
				return param.getMethod( "toValue", param );
			} catch( NoSuchMethodException e ) {
				return null;
			}
		}

		int i = 0;
		for( Class< ? > type : SUPPORTED_TYPES ) {
			if( param.isAssignableFrom( type ) ) {
				return TO_VALUE_CONVERTERS[ i ];
			}
			i++;
		}
		return null;
	}

	private static Method getFromValueConverter( Class< ? > param ) {
		if( param == null ) {
			return null;
		}

		if( ValueConverter.class.isAssignableFrom( param ) ) {
			try {
				return param.getMethod( "fromValue", Value.class );
			} catch( NoSuchMethodException e ) {
				return null;
			}
		}

		int i = 0;
		for( Class< ? > type : SUPPORTED_TYPES ) {
			if( param.isAssignableFrom( type ) ) {
				return FROM_VALUE_CONVERTERS[ i ];
			}
			i++;
		}
		return null;
	}

	private static CommMessage oneWayCallable( JavaService javaService, JavaOperation javaOperation,
		CommMessage message )
		throws IllegalAccessException {
		final Object[] args = getArguments( javaOperation, message );
		javaService.interpreter.execute( () -> {
			try {
				javaOperation.method.invoke( javaService, args );
			} catch( InvocationTargetException | IllegalAccessException e ) {
				// This should never happen, as we filtered this out in the constructor.
				javaService.interpreter.logSevere( e );
			}
		} );
		return CommMessage.createEmptyResponse( message );
	}

	private static CommMessage requestResponseCallable( JavaService javaService, JavaOperation javaOperation,
		CommMessage message )
		throws IllegalAccessException {
		final Object[] args = getArguments( javaOperation, message );
		try {
			final Object retObject = javaOperation.method.invoke( javaService, args );
			if( retObject == null ) {
				return CommMessage.createEmptyResponse( message );
			} else {
				return CommMessage.createResponse( message,
					(Value) javaOperation.returnValueConstructor.invoke( null, retObject ) );
			}
		} catch( InvocationTargetException e ) {
			final FaultException fault =
				(e.getCause() instanceof FaultException)
					? (FaultException) e.getCause()
					: new FaultException( e.getCause() );
			return CommMessage.createFaultResponse(
				message,
				fault );
		}
	}

	private void checkMethod( Map< String, JavaOperation > ops, Method method, Method parameterConstructor ) {
		final Class< ? > returnType;
		final Class< ? >[] exceptions;
		final Method returnValueConstructor;

		returnType = method.getReturnType();
		if( void.class.isAssignableFrom( returnType ) ) {
			final boolean isRequestResponse = method.getAnnotation( RequestResponse.class ) != null;
			exceptions = method.getExceptionTypes();
			if( isRequestResponse ) { // && ( exceptions.length == 0 || (exceptions.length == 1 &&
										// FaultException.class.isAssignableFrom( exceptions[0]) ) ) ) {
				ops.put(
					method.getName(),
					new JavaOperation(
						method,
						parameterConstructor,
						null,
						JavaService::requestResponseCallable ) );
			} else if( exceptions.length == 0 ) {
				ops.put(
					method.getName(),
					new JavaOperation(
						method,
						parameterConstructor,
						null,
						JavaService::oneWayCallable ) );
			}
		} else {
			returnValueConstructor = getToValueConverter( returnType );
			if( returnValueConstructor != null ) {
				exceptions = method.getExceptionTypes();
				if( exceptions.length == 0 ||
					(exceptions.length == 1 && FaultException.class.isAssignableFrom( exceptions[ 0 ] )) ) {
					ops.put(
						getMethodName( method ),
						new JavaOperation(
							method,
							parameterConstructor,
							returnValueConstructor,
							JavaService::requestResponseCallable ) );
				}
			}
		}
	}

	private static Object[] getArguments( final JavaOperation javaOperation, final CommMessage message )
		throws IllegalAccessException {
		if( javaOperation.parameterConstructor == null ) {
			return new Object[ 0 ];
		} else {
			try {
				return new Object[] { javaOperation.parameterConstructor.invoke( null, message.value() ) };
			} catch( InvocationTargetException e ) {
				throw new IllegalAccessException( e.getMessage() );
			}
		}
	}

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
					field.set( this, OutputPort.create( interpreter, location ) );
				} catch( IllegalArgumentException | IllegalAccessException | URISyntaxException e ) {
					throw e;
				}
			}
		}
	}

	public CommMessage callOperation( CommMessage message )
		throws InvalidIdException, IllegalAccessException {
		final JavaOperation javaOperation = operations.get( message.operationName() );
		if( javaOperation == null ) {
			throw new InvalidIdException( message.operationName() );
		}

		return javaOperation.callable.call( this, javaOperation, message );
	}

	public final void setInterpreter( Interpreter interpreter ) {
		this.interpreter = interpreter;
	}

	protected Interpreter interpreter() {
		return interpreter;
	}

	public CommChannel sendMessage( CommMessage message ) {
		LocalCommChannel c = interpreter.commCore().getLocalCommChannel();
		try {
			c.send( message );
		} catch( IOException e ) {
			e.printStackTrace();
		}
		return c;
	}

	protected Embedder getEmbedder() {
		return new Embedder( interpreter );
	}
}
