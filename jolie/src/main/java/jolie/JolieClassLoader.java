/***************************************************************************
 *   Copyright 2008-2015 (C) by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.regex.Pattern;

import jolie.lang.Constants;
import jolie.net.CommCore;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.runtime.AndJarDeps;
import jolie.runtime.CanUseJars;
import jolie.runtime.JavaService;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoaderFactory;

/**
 * JolieClassLoader is used to resolve the loading of JOLIE extensions and external libraries.
 * 
 * @author Fabrizio Montesi
 */
public final class JolieClassLoader extends URLClassLoader {
	private final static char EXTENSION_SPLIT_CHAR = ':';

	private final Map< String, String > channelExtensionClassNames = new HashMap<>();
	private final Map< String, String > listenerExtensionClassNames = new HashMap<>();
	private final Map< String, String > protocolExtensionClassNames = new HashMap<>();
	private final Map< String, String > embeddingExtensionClassNames = new HashMap<>();

	private void init( URL[] urls )
		throws IOException {
		for( URL url : urls ) {
			if( "jar".equals( url.getProtocol() ) ) {
				try {
					checkJarForJolieExtensions( (JarURLConnection) url.openConnection() );
				} catch( IOException e ) {
					throw new IOException( "Loading failed for jolie extension jar " + url.toString(), e );
				}
			}
		}
	}

	/**
	 * Constructor
	 * 
	 * @param urls the urls to use for the lookup of libraries
	 * @param parent the parent class loader to use for lookup fallback
	 * @throws java.io.IOException if the initialization fails, e.g. if a required dependency in some
	 *         specified file can not be satisfied
	 */
	public JolieClassLoader( URL[] urls, ClassLoader parent )
		throws IOException {
		super( urls, parent );
		init( urls );
	}

	@Override
	protected Class< ? > findClass( String className )
		throws ClassNotFoundException {
		final Class< ? > c = super.findClass( className );
		if( JavaService.class.isAssignableFrom( c ) ) {
			checkForJolieAnnotations( c );
		}
		return c;
	}

	private final static Pattern DELEGATED_PACKAGES = Pattern.compile(
		"(java\\."
			+ "|jolie\\.jap\\."
			+ "|jolie\\.lang\\."
			+ "|jolie\\.runtime\\."
			+ "|jolie\\.process\\."
			+ "|jolie\\.util\\."
			+ ").*" );

	@Override
	public Class< ? > loadClass( final String className )
		throws ClassNotFoundException {
		if( DELEGATED_PACKAGES.matcher( className ).matches() ) {
			return getParent().loadClass( className );
		}

		try {
			final Class< ? > c = findLoadedClass( className );
			return (c == null) ? findClass( className ) : c;
		} catch( ClassNotFoundException e ) {
			return getParent().loadClass( className );
		}
	}

	private void checkForJolieAnnotations( Class< ? > c ) {
		final AndJarDeps neededJars = c.getAnnotation( AndJarDeps.class );
		if( neededJars != null ) {
			for( String filename : neededJars.value() ) {
				/*
				 * TODO jar unloading when service is unloaded? Consider other services needing the same jars in
				 * that.
				 */
				try {
					addJarResource( filename );
				} catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}
		final CanUseJars optionalJars = c.getAnnotation( CanUseJars.class );
		if( optionalJars != null ) {
			for( String filename : optionalJars.value() ) {
				/*
				 * TODO jar unloading when service is unloaded? Consider other services needing the same jars in
				 * that.
				 */
				try {
					addJarResource( filename );
				} catch( IOException e ) {
				}
			}
		}
	}

	private Class< ? > loadExtensionClass( String className )
		throws ClassNotFoundException {
		final Class< ? > c = loadClass( className );
		checkForJolieAnnotations( c );
		return c;
	}

	/**
	 * Creates and returns an {@link EmbeddedServiceLoader}, selecting it from the built-in and
	 * externally loaded Jolie extensions.
	 * 
	 * @param name
	 * @param interpreter
	 * @return
	 * @throws java.io.IOException
	 */
	public synchronized EmbeddedServiceLoaderFactory createEmbeddedServiceLoaderFactory( String name,
		Interpreter interpreter )
		throws IOException {
		String className = embeddingExtensionClassNames.get( name );
		if( className != null ) {
			try {
				final Class< ? > c = loadExtensionClass( className );
				if( EmbeddedServiceLoaderFactory.class.isAssignableFrom( c ) ) {
					@SuppressWarnings( "unchecked" )
					final Class< ? extends EmbeddedServiceLoaderFactory > fClass =
						(Class< ? extends EmbeddedServiceLoaderFactory >) c;
					return fClass.getConstructor().newInstance();
				}
			} catch( ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
				| InvocationTargetException e ) {
				throw new IOException( e );
			}
		}

		return null;
	}

	/**
	 * Creates and returns a <code>CommChannelFactory</code>, selecting it from the built-in and
	 * externally loaded JOLIE extensions.
	 * 
	 * @param name the identifier of the factory to create
	 * @param commCore the <code>CommCore</code> instance to use for constructing the factory
	 * @return the requested factory
	 * @throws java.io.IOException if the factory could not have been created
	 */
	public synchronized CommChannelFactory createCommChannelFactory( String name, CommCore commCore )
		throws IOException {
		CommChannelFactory factory = null;
		String className = channelExtensionClassNames.get( name );
		if( className != null ) {
			try {
				Class< ? > c = loadExtensionClass( className );
				if( CommChannelFactory.class.isAssignableFrom( c ) ) {
					@SuppressWarnings( "unchecked" )
					Class< ? extends CommChannelFactory > fClass = (Class< ? extends CommChannelFactory >) c;
					factory = fClass.getConstructor( CommCore.class ).newInstance( commCore );
				}
			} catch( ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
				| InvocationTargetException e ) {
				throw new IOException( e );
			}
		}

		return factory;
	}

	private void checkForChannelExtension( Attributes attrs )
		throws IOException {
		addExtensionToMap( channelExtensionClassNames, attrs.getValue( Constants.Manifest.CHANNEL_EXTENSION ) );
	}

	private void checkForEmbeddingExtension( Attributes attrs )
		throws IOException {
		addExtensionToMap( embeddingExtensionClassNames, attrs.getValue( Constants.Manifest.EMBEDDING_EXTENSION ) );
	}

	/**
	 * Creates and returns a <code>CommListenerFactory</code>, selecting it from the built-in and
	 * externally loaded JOLIE extensions.
	 * 
	 * @param name the identifier of the factory to create
	 * @param commCore the <code>CommCore</code> instance to use for constructing the factory
	 * @return the requested factory
	 * @throws java.io.IOException if the factory could not have been created
	 */
	public synchronized CommListenerFactory createCommListenerFactory( String name, CommCore commCore )
		throws IOException {
		CommListenerFactory factory = null;
		String className = listenerExtensionClassNames.get( name );
		if( className != null ) {
			try {
				Class< ? > c = loadExtensionClass( className );
				if( CommListenerFactory.class.isAssignableFrom( c ) ) {
					@SuppressWarnings( "unchecked" )
					Class< ? extends CommListenerFactory > fClass = (Class< ? extends CommListenerFactory >) c;
					factory = fClass.getConstructor( CommCore.class ).newInstance( commCore );
				}
			} catch( ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
				| InvocationTargetException e ) {
				throw new IOException( e );
			}
		}

		return factory;
	}

	private void checkForListenerExtension( Attributes attrs )
		throws IOException {
		addExtensionToMap( listenerExtensionClassNames, attrs.getValue( Constants.Manifest.LISTENER_EXTENSION ) );
	}

	/**
	 * Creates and returns a <code>CommProtocolFactory</code>, selecting it from the built-in and
	 * externally loaded JOLIE extensions.
	 * 
	 * @param name the identifier of the factory to create
	 * @param commCore the <code>CommCore</code> instance to use for constructing the factory
	 * @return the requested factory
	 * @throws java.io.IOException if the factory could not have been created
	 */
	public synchronized CommProtocolFactory createCommProtocolFactory( String name, CommCore commCore )
		throws IOException {
		CommProtocolFactory factory = null;
		String className = protocolExtensionClassNames.get( name );
		if( className != null ) {
			try {
				Class< ? > c = loadExtensionClass( className );
				if( CommProtocolFactory.class.isAssignableFrom( c ) ) {
					@SuppressWarnings( "unchecked" )
					Class< ? extends CommProtocolFactory > fClass = (Class< ? extends CommProtocolFactory >) c;
					factory = fClass.getConstructor( CommCore.class ).newInstance( commCore );
				}
			} catch( ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
				| InvocationTargetException e ) {
				throw new IOException( e );
			}
		}

		return factory;
	}

	private static void addExtensionToMap( Map< String, String > map, String extensionDescriptor )
		throws IOException {
		if( extensionDescriptor != null ) {
			int pos = extensionDescriptor.indexOf( EXTENSION_SPLIT_CHAR );
			if( pos > 0 && pos < extensionDescriptor.length() - 1 ) {
				map.put( extensionDescriptor.substring( 0, pos ), extensionDescriptor.substring( pos + 1 ) );
			} else {
				throw new IOException( "Invalid extension definition found in manifest file: " + extensionDescriptor );
			}
		}
	}

	private void checkForProtocolExtension( Attributes attrs )
		throws IOException {
		addExtensionToMap( protocolExtensionClassNames, attrs.getValue( Constants.Manifest.PROTOCOL_EXTENSION ) );
	}

	private void checkJarForJolieExtensions( JarURLConnection jarConnection )
		throws IOException {
		final Attributes attrs = jarConnection.getMainAttributes();
		if( attrs != null ) {
			checkForChannelExtension( attrs );
			checkForListenerExtension( attrs );
			checkForProtocolExtension( attrs );
			checkForEmbeddingExtension( attrs );
		}
	}

	/**
	 * Adds a Jar file to the pool of resource to look into for extensions.
	 * 
	 * @param jarName the Jar filename
	 * @throws java.net.MalformedURLException
	 * @throws java.io.IOException if the Jar file could not be found or if jarName does not refer to a
	 *         Jar file
	 */
	public void addJarResource( String jarName )
		throws MalformedURLException, IOException {
		URL url;
		try {
			url = findResource( jarName );
		} catch( IllegalArgumentException e ) {
			// On Windows, paths don't get interpreted correctly
			// due to drive letters and backslashes, hence make a
			// second parsing attempt by constructing an URI
			jarName = new File( jarName ).toURI().toString();
			url = findResource( jarName );
		}
		if( url == null ) {
			// Try again with URI method, since from
			// Java 17's URLClassLoader.findResource fixes undocumented
			// throws IllegalArgumentException on Windows
			jarName = new File( jarName ).toURI().toString();
			url = findResource( jarName );
		}
		if( url == null ) {
			throw new IOException( "Resource not found: " + jarName );
		}

		if( url.getProtocol().startsWith( "jap" ) ) {
			addURL( new URL( url + "!/" ) );
		} else {
			addURL( new URL( "jap:" + url + "!/" ) );
		}
	}
}
