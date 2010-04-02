/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi                                *
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

package jolie.jap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 *
 * @author Fabrizio Montesi
 */
public class JapURLConnection extends URLConnection
{
	private static final int BUF_SIZE = 2048;
	private static final Pattern urlPattern = Pattern.compile( "([^!]*!/[^!]*)(?:!/)?(.*)?" );
	public static final Pattern nestingSeparatorPattern = Pattern.compile( "!/" );

	private static final Map< URL, JarFile > japCache = new HashMap< URL, JarFile >();

	private InputStream inputStream;
	private long entrySize = 0L;

	public JapURLConnection( URL url )
		throws MalformedURLException, IOException
	{
		super( url );
	}

	private synchronized static void putInCache( URL url, JarFile jar )
	{
		japCache.put( url, jar );
	}

	private synchronized static JarFile getFromCache( URL url )
	{
		return japCache.get( url );
	}

	private static JarFile retrieve( URL url, final InputStream in )
		throws IOException
	{
		JarFile jar = getFromCache( url );
		if ( jar == null ) {
			try {
				jar = AccessController.doPrivileged(
				new PrivilegedExceptionAction< JarFile >() {
					public JarFile run()
						throws IOException
					{
						File tmpFile = null;
						OutputStream out = null;
						try {
							tmpFile = File.createTempFile( "jap_cache", null );
							tmpFile.deleteOnExit();
							out = new FileOutputStream( tmpFile );
							int read = 0;
							byte[] buf = new byte[ BUF_SIZE ];
							while( (read = in.read(buf)) != -1 ) {
								out.write( buf, 0, read );
							}
							out.close();
							out = null;
							return new JarFile( tmpFile );
						} catch( IOException e ) {
							if ( tmpFile != null ) {
								tmpFile.delete();
							}
							throw e;
						} finally {
							if ( in != null ) {
								in.close();
							}
							if ( out != null ) {
								out.close();
							}
						}
					}
				} );
				putInCache( url, jar );
			} catch( PrivilegedActionException e ) {
				throw new IOException( e );
			}
		}
		return jar;
	}

	public long getEntrySize()
		throws IOException
	{
		connect();
		return entrySize;
	}

	public void connect()
		throws IOException
	{
		if ( !connected ) {
			String path = url.getPath();
			Matcher matcher = urlPattern.matcher( path );
			if ( matcher.matches() ) {
				path = matcher.group( 1 );
				String subPath = matcher.group( 2 );
				JarURLConnection jarURLConnection = (JarURLConnection) new URL( "jar:" + path ).openConnection();
				inputStream = jarURLConnection.getInputStream();
				if ( subPath.isEmpty() == false ) {
					JarFile jar = retrieve( new URL( path ), inputStream );
					String[] nodes = nestingSeparatorPattern.split( subPath );
					int i;
					for( i = 0; i < nodes.length - 1; i++ ) {
						path += "!/" + nodes[ i ];
						jar = retrieve( new URL( path ), inputStream );
					}
					ZipEntry entry = jar.getEntry( nodes[i] );
					entrySize = entry.getSize();
					inputStream = jar.getInputStream( entry );
				}
			} else {
				throw new MalformedURLException( "Invalid JAP URL path: " + path );
			}

			connected = true;
		}
	}

	@Override
	public InputStream getInputStream()
		throws IOException
	{
		connect();
		return inputStream;
    }
}
