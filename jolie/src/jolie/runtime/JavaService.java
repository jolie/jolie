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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.LocalCommChannel;
import jolie.runtime.embedding.JavaServiceHelpers;
import jolie.runtime.embedding.RequestResponse;

/**
 *
 * @author Fabrizio Montesi
 */
public abstract class JavaService
{
	@FunctionalInterface
	private interface JavaOperationCallable {
		public CommMessage call( JavaService service, JavaOperation javaOperation, CommMessage message )
			throws IllegalAccessException;
	}
	
	public interface ValueConverter {}

	private static class JavaOperation {
		private final Method method;
		private final Method parameterConstructor;
		private final Method returnValueConstructor;
		private final JavaOperationCallable callable;

		private JavaOperation(
				Method method,
				Method parameterConstructor,
				Method returnValueConstructor,
				JavaOperationCallable callable
		) {
			this.method = method;
			this.parameterConstructor = parameterConstructor;
			this.returnValueConstructor = returnValueConstructor;
			this.callable = callable;
		}
	}

	protected static class Embedder
	{
		private final Interpreter interpreter;
		
		private Embedder( Interpreter interpreter )
		{
			this.interpreter = interpreter;
		}

		public void callOneWay( CommMessage message )
		{
			LocalCommChannel c = interpreter.commCore().getLocalCommChannel();
			try {
				c.send( message );
			} catch( IOException e ) {
				// This should never happen
				e.printStackTrace();
			}
		}

		public Value callRequestResponse( CommMessage request )
			throws FaultException
		{
			LocalCommChannel c = interpreter.commCore().getLocalCommChannel();
			try {
				c.send( request );
				CommMessage response = c.recvResponseFor( request );
				if ( response.isFault() ) {
					throw response.fault();
				}
				return response.value();
			} catch( IOException e ) {
				throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
			}
		}
	}

	private Interpreter interpreter;
	private final Map< String, JavaOperation > operations;

	public JavaService()
	{
		Map< String, JavaOperation > ops  = new HashMap<>();
		
		Class<?>[] params;
		for( Method method : this.getClass().getDeclaredMethods() ) {
			if ( Modifier.isPublic( method.getModifiers() ) ) {
				params = method.getParameterTypes();
				if ( params.length == 1 ) {
					checkMethod( ops, method, getFromValueConverter( params[0] ) );
				} else if ( params.length == 0 ) {
					checkMethod( ops, method, null );
				}
			}
		}
		this.operations = Collections.unmodifiableMap( ops );
	}

	private static String getMethodName( Method method )
	{
		Identifier identifier = method.getAnnotation( Identifier.class );
		if ( identifier == null ) {
			return method.getName();
		}

		return identifier.value();
	}

	// Warning: this MUST NOT contain void.
	/* When you add something here, be sure that you define the appropriate
	 * "create..." static method in JavaServiceHelpers.
	 */
	private static final Class<?>[] supportedTypes = new Class[] {
		Value.class, String.class, Integer.class, Double.class, Boolean.class,
		Long.class, ByteArray.class
	};
	private static final Method[] toValueConverters;
	private static final Method[] fromValueConverters;

	static {
		toValueConverters = new Method[ supportedTypes.length ];
		try {
			toValueConverters[0] = JavaServiceHelpers.class.getMethod( "createValue", Value.class );
			for( int i = 1; i < supportedTypes.length; i++ ) {
				toValueConverters[ i ] = Value.class.getMethod( "create", supportedTypes[ i ] );
			}
		} catch( NoSuchMethodException e ) {
			e.printStackTrace();
			assert false;
		}

		fromValueConverters = new Method[ supportedTypes.length ];
		try {
			fromValueConverters[0] = JavaServiceHelpers.class.getMethod( "createValue", Value.class );
			for( int i = 1; i < supportedTypes.length; i++ ) {
				fromValueConverters[ i ] = JavaServiceHelpers.class.getMethod( "valueTo" + supportedTypes[i].getSimpleName(), Value.class );
			}
		} catch( NoSuchMethodException e ) {
			e.printStackTrace();
			assert false;
		}
	}

	private static Method getToValueConverter( Class<?> param )
	{
		if ( param == null ) {
			return null;
		}

		if ( ValueConverter.class.isAssignableFrom( param ) ) {
			try {
				return param.getMethod( "toValue", param.getClass() );
			} catch( NoSuchMethodException e ) {
				return null;
			}
		}

		int i = 0;
		for( Class<?> type : supportedTypes ) {
			if ( param.isAssignableFrom( type ) ) {
				return toValueConverters[i];
			}
			i++;
		}
		return null;
	}

	private static Method getFromValueConverter( Class<?> param )
	{
		if ( param == null ) {
			return null;
		}

		if ( ValueConverter.class.isAssignableFrom( param ) ) {
			try {
				return param.getMethod( "fromValue", Value.class );
			} catch( NoSuchMethodException e ) {
				return null;
			}
		}

		int i = 0;
		for( Class<?> type : supportedTypes ) {
			if ( param.isAssignableFrom( type ) ) {
				return fromValueConverters[i];
			}
			i++;
		}
		return null;
	}
	
	private static CommMessage oneWayCallable( JavaService javaService, JavaOperation javaOperation, CommMessage message )
		throws IllegalAccessException
	{
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
	
	private static CommMessage requestResponseCallable( JavaService javaService, JavaOperation javaOperation, CommMessage message )
		throws IllegalAccessException
	{
		final Object[] args = getArguments( javaOperation, message );
		try {
			final Object retObject = javaOperation.method.invoke( javaService, args );
			if ( retObject == null ) {
				return CommMessage.createEmptyResponse( message );
			} else {
				return CommMessage.createResponse( message, (Value)javaOperation.returnValueConstructor.invoke( null, retObject ) );
			}
		} catch( InvocationTargetException e ) {
			final FaultException fault =
				( e.getCause() instanceof FaultException )
				? (FaultException)e.getCause()
				: new FaultException( e.getCause() );
			return CommMessage.createFaultResponse(
				message,
				fault
			);
		}
	}

	private void checkMethod( Map< String, JavaOperation > ops, Method method, Method parameterConstructor )
	{
		final Class<?> returnType;
		final Class<?>[] exceptions;
		final Method returnValueConstructor;

		returnType = method.getReturnType();
		if ( void.class.isAssignableFrom( returnType ) ) {
			final boolean isRequestResponse = method.getAnnotation( RequestResponse.class ) != null;
			exceptions = method.getExceptionTypes();
			if ( isRequestResponse ) { // && ( exceptions.length == 0 || (exceptions.length == 1 && FaultException.class.isAssignableFrom( exceptions[0]) ) ) ) {
				ops.put(
					method.getName(),
					new JavaOperation(
						method,
						parameterConstructor,
						null,
						JavaService::requestResponseCallable
					)
				);
			} else if ( exceptions.length == 0 ) {
				ops.put(
					method.getName(),
					new JavaOperation(
						method,
						parameterConstructor,
						null,
						JavaService::oneWayCallable
					)
				);
			}
		} else {
			returnValueConstructor = getToValueConverter( returnType );
			if ( returnValueConstructor != null ) {
				exceptions = method.getExceptionTypes();
				if ( exceptions.length == 0 ||
					( exceptions.length == 1 && FaultException.class.isAssignableFrom( exceptions[0] ) )
					)
				{
					ops.put(
						getMethodName( method ),
						new JavaOperation(
							method,
							parameterConstructor,
							returnValueConstructor,
							JavaService::requestResponseCallable
						)
					);
				}
			}
		}
	}

	private static Object[] getArguments( final JavaOperation javaOperation, final CommMessage message )
		throws IllegalAccessException
	{
		if ( javaOperation.parameterConstructor == null ) {
			return new Object[0];
		} else {
			try {
				return new Object[] { javaOperation.parameterConstructor.invoke( null, message.value() ) };
			} catch( InvocationTargetException e ) {
				throw new IllegalAccessException( e.getMessage() );
			}
		}
	}
	
	public CommMessage callOperation( CommMessage message )
		throws InvalidIdException, IllegalAccessException
	{
		final JavaOperation javaOperation = operations.get( message.operationName() );
		if ( javaOperation == null ) {
			throw new InvalidIdException( message.operationName() );
		}

		return javaOperation.callable.call( this, javaOperation, message );
	}
	
	public final void setInterpreter( Interpreter interpreter )
	{
		this.interpreter = interpreter;
	}
	
	protected Interpreter interpreter()
	{
		return interpreter;
	}
	
	public CommChannel sendMessage( CommMessage message )
	{
		LocalCommChannel c = interpreter.commCore().getLocalCommChannel();
		try {
			c.send( message );
		} catch( IOException e ) {
			e.printStackTrace();
		}
		return c;
	}
}