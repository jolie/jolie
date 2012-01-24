/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map.Entry;
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
		try {
			Reader reader = new FileReader( request.strValue() );
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
		}
	}
}
