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

import jolie.Interpreter;
import jolie.JolieClassLoader;
import jolie.net.JavaCommChannel;


public class JavaServiceLoader extends EmbeddedServiceLoader
{
	private String servicePath;
	
	public JavaServiceLoader( String servicePath )
	{
		this.servicePath = servicePath;
	}

	public void load()
		throws EmbeddedServiceLoadingException
	{
		Class<?> c;
		try {
			JolieClassLoader cl = Interpreter.getInstance().getClassLoader();
			c = cl.loadClass( servicePath );
			Object obj = c.newInstance();
			if ( !(obj instanceof JavaService) )
				throw new EmbeddedServiceLoadingException( servicePath + " is not a valid JavaService" );
			((JavaService)obj).setInterpreter( Interpreter.getInstance() );
			setChannel(	new JavaCommChannel( (JavaService)obj )	);
		} catch( InstantiationException e ) {
			throw new EmbeddedServiceLoadingException( e );
		} catch( IllegalAccessException e ) {
			throw new EmbeddedServiceLoadingException( e );
		} catch( ClassNotFoundException e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}
}