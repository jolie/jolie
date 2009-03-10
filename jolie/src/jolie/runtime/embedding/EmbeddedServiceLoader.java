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
	final private Expression channelDest;

	protected EmbeddedServiceLoader( Expression channelDest )
	{
		this.channelDest = channelDest;
	}

	private static EmbeddedServiceLoader createLoader(
				Interpreter interpreter,
				Constants.EmbeddedServiceType type,
				String servicePath,
				Expression channelDest
			)
		throws EmbeddedServiceLoaderCreationException
	{
		EmbeddedServiceLoader ret = null;
		try {
			if ( type == Constants.EmbeddedServiceType.JAVA ) {
				ret = new JavaServiceLoader( channelDest, servicePath, interpreter );
			} else if ( type == Constants.EmbeddedServiceType.JOLIE ) {
				ret = new JolieServiceLoader( channelDest, interpreter, servicePath );
			} else if ( type == Constants.EmbeddedServiceType.JAVASCRIPT ) {
				ret = new JavaScriptServiceLoader( channelDest, servicePath );
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
		return createLoader( interpreter, type, servicePath, channelValue );
	}
	
	public static EmbeddedServiceLoader create(
				Interpreter interpreter,
				Constants.EmbeddedServiceType type,
				String servicePath,
				VariablePath channelPath
			)
		throws EmbeddedServiceLoaderCreationException
	{
		return createLoader( interpreter, type, servicePath, channelPath );
	}
	
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