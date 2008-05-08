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


package jolie.net;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import jolie.Interpreter;
import jolie.runtime.Value;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.exceptions.DBusException;

public class DBusCommChannel extends CommChannel
{
	private class StringCodeJavaFileObject extends SimpleJavaFileObject
	{
		final private String sourceCode;
		public StringCodeJavaFileObject( String className, String sourceCode )
			throws URISyntaxException
		{
			super( new URI( className.replace( '.', '/' ) + ".java" ), Kind.SOURCE );
			this.sourceCode = sourceCode;
		}
		@Override
		public CharSequence getCharContent( boolean ignoreEncodingErrors )
			throws IOException
		{
			return sourceCode;
		}
	}
	
	private String buildDBusJavaInterface( String methodName, String paramClassName )
	{
		String iName;
		String[] ss = objectName.split( "[.]" );
		String p = new String();
		int l = ss.length;
		if ( l >= 2 ) {
			for( int i = ss.length - 2; i > 0; i-- )
				p = "." + ss[ i ] + p;
			p = ss[0] + p;
			iName = ss[ l - 1 ];
		} else
			iName = objectName;
		 
		StringBuilder code = new StringBuilder();
		if ( !p.isEmpty() ) {
			code.append( "package " + p + ";" );
		}		
		code
			.append( "import org.freedesktop.dbus.DBusInterface;" )
			.append( "public interface " + iName )
				.append( " extends DBusInterface" )
			.append( "{" );
		/*
		for( String operation : port.operations() ) {
			code.append( "public Object " ).append( operation ).append( "(String s);" );
		}*/
		
		code.append( "public Object " ).append( methodName )
			.append( "(" );
		if ( paramClassName.isEmpty() )
			code.append( ");" );
		else {
			code.append( paramClassName )
				.append( " p);" );
		}

		code.append( "}" );
		return code.toString();
	}
	
	private List< CommMessage > recvMessages = new Vector< CommMessage >();
	private DBusConnection connection;
	private String serviceName, path, objectName;
	
	public DBusCommChannel(
			DBusConnection connection,
			String serviceName,
			String path,
			OutputPort port
			)
		throws IOException, URISyntaxException
	{
		this.connection = connection;
		this.serviceName = serviceName;
		
		String[] ss = path.split( "/" );
		int l = ss.length;
		this.path = new String();
		if ( l > 2 ) {
			for( int i = l - 2; i > 0; i-- ) {
				this.path = "/" + ss[ i ] + this.path;
			}
		} else if ( l == 2 )
			this.path = "/";
		else
			throw new IOException( "Invalid DBus path: " + path );

		this.objectName = ss[ l - 1 ];
		toBeClosed = false; // DBusCommChannel connections are kept open by default
	}
	
	public void closeImpl()
	{
		connection.disconnect();
	}
	
	public CommMessage recv()
	{
		if ( recvMessages.isEmpty() )
			return CommMessage.createEmptyMessage();
		return recvMessages.remove( 0 );
	}

	@SuppressWarnings("unchecked")
	public void send( CommMessage message )
		throws IOException
	{
		Class< ? extends DBusInterface > objectClass;
		StringCodeJavaFileObject sourceProgram = null;
		String paramClassName = "";
		if ( message.value().valueObject() != null )
			paramClassName = message.value().valueObject().getClass().getName();
		try {
			sourceProgram =	new StringCodeJavaFileObject(
							objectName, buildDBusJavaInterface( message.operationName(), paramClassName )
						);
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		}
		
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		JavaCompiler.CompilationTask task = compiler.getTask(
					null,
					null,
					null,
					Arrays.asList( new String[] { "-d", "." } ),
					null,
					Arrays.asList( sourceProgram )
				);
		boolean result = task.call();
		if ( result == false )
			throw new IOException( "Could not create DBus interface for " + serviceName );
		try {
			objectClass =
				(Class< ? extends DBusInterface >)
					Interpreter.getInstance().getClassLoader().loadClass( objectName );
		} catch( ClassNotFoundException e ) {
			throw new IOException( e );
		}
		
		try {
			Object o = connection.getRemoteObject( serviceName, path, objectClass );
			Method m = null;
			Object ret = null;
			if ( paramClassName.isEmpty() ) {
				m = o.getClass().getDeclaredMethod( message.operationName() );
				ret = m.invoke( o );
			} else {
				m = o.getClass().getDeclaredMethod( message.operationName(), message.value().valueObject().getClass() );
				ret = m.invoke( o, message.value().valueObject() );
			}
			Value value = Value.create();
			value.setValue( ret );
			recvMessages.add( new CommMessage( message.operationName(), "/", value ) );
		} catch( DBusException e ) {
			throw new IOException( e );
		} catch( NoSuchMethodException e ) {
			throw new IOException( e );
		} catch( IllegalAccessException e ) {
			throw new IOException( e );
		} catch( InvocationTargetException e ) {
			throw new IOException( e );
		}
	}
}