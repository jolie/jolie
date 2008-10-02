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

package jolie;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.jar.Attributes;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ext.Identifier;
import jolie.runtime.AndJarDeps;
import jolie.runtime.CanUseJars;

/**
 * JolieClassLoader is used to resolve the loading of JOLIE extensions and external libraries.
 * @author Fabrizio Montesi
 */
public class JolieClassLoader extends URLClassLoader
{
	final private Interpreter interpreter;
	public JolieClassLoader( URL[] urls, Interpreter interpreter )
		throws IOException
	{
		super( urls );
		this.interpreter = interpreter;
		for( URL url : urls ) {
			if ( "jar".equals( url.getProtocol() ) ) {
				checkJarJolieExtensions( (JarURLConnection)url.openConnection() );
			}
		}
	}
	
	@Override
	public Class<?> loadClass( String className )
		throws ClassNotFoundException
	{
		Class<?> c = super.loadClass( className );
		AndJarDeps needsJars = c.getAnnotation( AndJarDeps.class );
		if ( needsJars != null ) {
			for( String filename : needsJars.value() ) {
				/*
				 * TODO jar unloading when service is unloaded?
				 * Consider other services needing the same jars in that.
				 */
				try {
					addJarResource( filename );
				} catch( MalformedURLException e ) {
				} catch( IOException e ) {
				}
			}
		}
		CanUseJars canUseJars = c.getAnnotation( CanUseJars.class );
		if ( canUseJars != null ) {
			for( String filename : canUseJars.value() ) {
				/*
				 * TODO jar unloading when service is unloaded?
				 * Consider other services needing the same jars in that.
				 */
				try {
					addJarResource( filename );
				} catch( Exception e ) {}
			}
		}
		
		return c;
	}
	
	private void checkForChannelExtension( Attributes attrs )
		throws IOException
	{
		String className = attrs.getValue( Constants.Manifest.ChannelExtension );
		if ( className != null ) {
			try {
				Class<?> c = loadClass( className );
				if ( CommChannelFactory.class.isAssignableFrom( c ) ) {
					Class< ? extends CommChannelFactory > fClass = (Class< ? extends CommChannelFactory >)c;
					Identifier pId = c.getAnnotation( Identifier.class );
					if ( pId == null ) {
						throw new IOException( "Class " + fClass.getName() + " does not specify a protocol identifier." );
					}
					CommChannelFactory factory = fClass.newInstance();
					factory.setCommCore( interpreter.commCore() );
					interpreter.commCore().setCommChannelFactory( pId.value(), factory );
				}
			} catch( ClassNotFoundException e ) {
				throw new IOException( e );
			} catch( InstantiationException e ) {
				throw new IOException( e );
			} catch( IllegalAccessException e ) {
				throw new IOException( e );
			}
		}
	}
	
	private void checkForListenerExtension( Attributes attrs )
		throws IOException
	{
		String className = attrs.getValue( Constants.Manifest.ListenerExtension );
		if ( className != null ) {
			try {
				Class<?> c = loadClass( className );
				if ( CommListenerFactory.class.isAssignableFrom( c ) ) {
					Class< ? extends CommListenerFactory > fClass = (Class< ? extends CommListenerFactory >)c;
					Identifier pId = c.getAnnotation( Identifier.class );
					if ( pId == null ) {
						throw new IOException( "Class " + fClass.getName() + " does not specify a protocol identifier." );
					}
					CommListenerFactory factory = fClass.newInstance();
					factory.setCommCore( interpreter.commCore() );
					interpreter.commCore().setCommListenerFactory( pId.value(), factory );
				}
			} catch( ClassNotFoundException e ) {
				throw new IOException( e );
			} catch( InstantiationException e ) {
				throw new IOException( e );
			} catch( IllegalAccessException e ) {
				throw new IOException( e );
			}
		}
	}
	
	private void checkForProtocolExtension( Attributes attrs )
		throws IOException
	{
		String className = attrs.getValue( Constants.Manifest.ProtocolExtension );
		if ( className != null ) {
			try {
				Class<?> c = loadClass( className );
				if ( CommProtocolFactory.class.isAssignableFrom( c ) ) {
					Class< ? extends CommProtocolFactory > fClass = (Class< ? extends CommProtocolFactory >)c;
					Identifier pId = c.getAnnotation( Identifier.class );
					if ( pId == null ) {
						throw new IOException( "Class " + fClass.getName() + " does not specify a protocol identifier." );
					}
					CommProtocolFactory factory = fClass.newInstance();
					factory.setCommCore( interpreter.commCore() );
					interpreter.commCore().setCommProtocolFactory( pId.value(), factory );
				}
			} catch( ClassNotFoundException e ) {
				throw new IOException( e );
			} catch( InstantiationException e ) {
				throw new IOException( e );
			} catch( IllegalAccessException e ) {
				throw new IOException( e );
			}
		}
	}
	
	private void checkJarJolieExtensions( JarURLConnection jarConnection )
		throws IOException
	{
		Attributes attrs = jarConnection.getManifest().getMainAttributes();
		checkForChannelExtension( attrs );
		checkForListenerExtension( attrs );
		checkForProtocolExtension( attrs );
	}
	
	/**
	 * Adds a Jar file to the pool of resource to look into for extensions.
	 * @param jarName the Jar filename
	 * @throws java.net.MalformedURLException
	 * @throws java.io.IOException if the Jar file could not be found or if jarName does not refer to a Jar file
	 */
	public void addJarResource( String jarName )
		throws MalformedURLException, IOException
	{
		URL url = findResource( jarName );
		if ( url == null ) {
			throw new IOException( "Resource not found: " + jarName );
		}
		addURL( new URL( "jar:" + url + "!/" ) );
		URLConnection urlConn = url.openConnection();
		if ( urlConn instanceof JarURLConnection ) {				
			checkJarJolieExtensions( (JarURLConnection)urlConn );
		} else {
			throw new IOException( "Jar file not found: " + jarName );
		}
	}
}
