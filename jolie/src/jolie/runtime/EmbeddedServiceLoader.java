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

import jolie.Constants;

public abstract class EmbeddedServiceLoader
{
	public static EmbeddedServiceLoader create(
				Constants.EmbeddedServiceType type,
				String servicePath,
				VariablePath channelVariablePath
			)
		throws EmbeddedServiceLoaderCreationException
	{
		EmbeddedServiceLoader ret = null;
		try {
			if ( type == Constants.EmbeddedServiceType.JAVA ) {
				ret = new JavaServiceLoader( servicePath, channelVariablePath );
			} else if ( type == Constants.EmbeddedServiceType.JOLIE ) {
				ret = new JolieServiceLoader( servicePath, channelVariablePath );
			}
		} catch( Exception e ) {
			throw new EmbeddedServiceLoaderCreationException( e );
		}
		
		if ( ret == null )
			throw new EmbeddedServiceLoaderCreationException( "Invalid embedded service type specified" );

		return ret;
	}
	
	abstract public void load()
		throws EmbeddedServiceLoadingException;
	
	//abstract public void shutdown();
}