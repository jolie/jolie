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


package jolie.runtime;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import jolie.lang.Constants;
import jolie.Interpreter;
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
	public interface ValueConverter {}

	private static class JavaOperation {
		private final Constants.OperationType operationType;
		private final Method method;
		private final Method parameterConstructor;
		private final Method returnValueConstructor;

		private JavaOperation(
				Constants.OperationType operationType,
				Method method,
				Method parameterConstructor,
				Method returnValueConstructor
		) {
			this.operationType = operationType;
			this.method = method;
			this.parameterConstructor = parameterConstructor;
			this.returnValueConstructor = returnValueConstructor;
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
	private final Map< String, JavaOperation > operations =
					new HashMap< String, JavaOperation >();

	public JavaService()
	{
		Class<?>[] params;
		for( Method method : this.getClass().getDeclaredMethods() ) {
			if ( Modifier.isPublic( method.getModifiers() ) ) {
				params = method.getParameterTypes();
				if ( params.length == 1 ) {
					checkMethod( method, getFromValueConverter( params[0] ) );
				} else if ( params.length == 0 ) {
					checkMethod( method, null );
				}
			}
		}
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
		ByteArray.class
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

	private void checkMethod( Method method, Method parameterConstructor )
	{
		Class<?> returnType;
		Class<?>[] exceptions;
		Method returnValueConstructor;

		returnType = method.getReturnType();
		if ( void.class.isAssignableFrom( returnType ) ) {
			boolean isRequestResponse;
			if ( method.getAnnotation( RequestResponse.class ) == null ) {
				isRequestResponse = false;
			} else {
				isRequestResponse = true;
			}
			exceptions = method.getExceptionTypes();
			if ( isRequestResponse ) { // && ( exceptions.length == 0 || (exceptions.length == 1 && FaultException.class.isAssignableFrom( exceptions[0]) ) ) ) {
				operations.put(
					method.getName(),
					new JavaOperation( Constants.OperationType.REQUEST_RESPONSE, method, parameterConstructor, null )
				);
			} else if ( exceptions.length == 0 ) {
				operations.put(
					method.getName(),
					new JavaOperation( Constants.OperationType.ONE_WAY, method, parameterConstructor, null )
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
					operations.put(
						getMethodName( method ),
						new JavaOperation( Constants.OperationType.REQUEST_RESPONSE, method, parameterConstructor, returnValueConstructor )
					);
				}
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
		CommMessage ret = null;
		Object retObject = null;
		final Object[] args;
		if ( javaOperation.parameterConstructor == null ) {
			args = new Object[0];
		} else {
			args = new Object[1];
			try {
				args[0] = javaOperation.parameterConstructor.invoke( null, message.value() );
			} catch( InvocationTargetException e ) {
				throw new IllegalAccessException( e.getMessage() );
			}
		}
		if ( javaOperation.operationType == Constants.OperationType.ONE_WAY ) {
			final JavaService javaService = this;
			interpreter.execute( new Runnable() {
				public void run()
				{
					try {
						javaOperation.method.invoke( javaService, args );
					} catch( InvocationTargetException e ) {
						// This should never happen, as we filtered this out in the constructor.
						interpreter.logSevere( e );
					} catch( IllegalAccessException e ) {
						interpreter.logSevere( e );
					}
				}
			} );
			ret = CommMessage.createEmptyResponse( message );
		} else { // Request-Response
			try {
				retObject = javaOperation.method.invoke( this, args );
				if ( retObject == null ) {
					ret = CommMessage.createEmptyResponse( message );
				} else {
					ret = CommMessage.createResponse( message, (Value)javaOperation.returnValueConstructor.invoke( null, retObject ) );
				}
			} catch( InvocationTargetException e ) {
				FaultException fault;
				if ( e.getCause() instanceof FaultException ) {
					fault = (FaultException)e.getCause();
				} else {
					fault = new FaultException( e.getCause() );
				}
				ret = CommMessage.createFaultResponse(
					message,
					fault
				);
			}
		}
		return ret;
	}
	
	public final void setInterpreter( Interpreter interpreter )
	{
		this.interpreter = interpreter;
	}
	
	protected Interpreter interpreter()
	{
		return interpreter;
	}

	/*protected Embedder getEmbedder()
	{

	}*/
	
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