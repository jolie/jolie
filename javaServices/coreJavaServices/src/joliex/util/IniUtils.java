/***************************************************************************
 *   Copyright (C) 2008-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package joliex.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map.Entry;
import jolie.Interpreter;
import jolie.jap.JapURLConnection;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import org.ini4j.Ini;

/**
 *
 * @author Fabrizio Montesi
 */
@AndJarDeps({"ini4j.jar"})
public class IniUtils extends JavaService
{
	public Value parseIniFile( Value request )
		throws FaultException
	{
		String filename = request.strValue();
		File file = new File( filename );
		InputStream istream = null;
		String charset = null;
		if ( request.hasChildren( "charset" ) ) {
			charset = request.getFirstChild( "charset" ).strValue();
		}

		try {
			if (file.exists()) {
				istream = new FileInputStream(file);
			} else {
				URL fileURL = interpreter().getClassLoader().findResource( filename );
				if (fileURL != null && fileURL.getProtocol().equals("jap")) {
					URLConnection conn = fileURL.openConnection();
					if (conn instanceof JapURLConnection) {
						JapURLConnection jarConn = (JapURLConnection) conn;
						istream = jarConn.getInputStream();
					} else {
						throw new FileNotFoundException( filename );
					}
				} else {
					throw new FileNotFoundException( filename );
				}
			}
			Reader reader;
			if ( charset != null ) {
				reader = new InputStreamReader( istream, charset );
			} else {
				reader = new InputStreamReader( istream );
			}
			reader = new BufferedReader( reader );
			Ini ini = new Ini( reader );
			Value response = Value.create();
			Value sectionValue;
			for( Entry< String, Ini.Section > sectionEntry : ini.entrySet() ) {
				sectionValue = response.getFirstChild( sectionEntry.getKey() );
				for( Entry< String, String > entry : sectionEntry.getValue().entrySet() ) {
					sectionValue.getFirstChild( entry.getKey() ).setValue( entry.getValue() );
				}
			}
			return response;
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		} finally {
			try {
				if ( istream != null ) {
					istream.close();
				}
			} catch( IOException e ) {
				Interpreter.getInstance().logWarning( e );
			}
		}
	}
}
