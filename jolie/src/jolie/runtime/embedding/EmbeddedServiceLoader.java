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


package jolie.runtime.embedding;

import jolie.runtime.*;
import jolie.lang.Constants;
import jolie.Interpreter;
import jolie.net.CommChannel;

public abstract class EmbeddedServiceLoader
{
	private static EmbeddedServiceLoader createLoader(
				Interpreter interpreter,
				Constants.EmbeddedServiceType type,
				String servicePath
			)
		throws EmbeddedServiceLoaderCreationException
	{
		EmbeddedServiceLoader ret = null;
		try {
			if ( type == Constants.EmbeddedServiceType.JAVA ) {
				ret = new JavaServiceLoader( servicePath );
			} else if ( type == Constants.EmbeddedServiceType.JOLIE ) {
				ret = new JolieServiceLoader( interpreter, servicePath );
			} else if ( type == Constants.EmbeddedServiceType.JAVASCRIPT ) {
				ret = new JavaScriptServiceLoader( servicePath );
			}
		} catch( Exception e ) {
			throw new EmbeddedServiceLoaderCreationException( e );
		}
		
		if ( ret == null ) {
			throw new EmbeddedServiceLoaderCreationException( "Invalid embedded service type specified" );
		}

		return ret;
	}
	
	public static EmbeddedServiceLoader create(
				Interpreter interpreter,
				Constants.EmbeddedServiceType type,
				String servicePath,
				Value channelValue
			)
		throws EmbeddedServiceLoaderCreationException
	{
		EmbeddedServiceLoader ret = createLoader( interpreter, type, servicePath );
		ret.channelDest = channelValue;
		return ret;
	}
	
	public static EmbeddedServiceLoader create(
				Interpreter interpreter,
				Constants.EmbeddedServiceType type,
				String servicePath,
				VariablePath channelPath
			)
		throws EmbeddedServiceLoaderCreationException
	{
		EmbeddedServiceLoader ret = createLoader( interpreter, type, servicePath );
		ret.channelDest = channelPath;
		return ret;
	}
	
	private Expression channelDest = null;
	
	protected void setChannel( CommChannel channel )
	{
		if ( channelDest != null ) {
			if ( channelDest instanceof VariablePath ) {
				((VariablePath)channelDest).getValue().setValue( channel );
			} else if ( channelDest instanceof Value ) {
				((Value)channelDest).setValue( channel );
			}
		}
	}
	
	abstract public void load()
		throws EmbeddedServiceLoadingException;
}