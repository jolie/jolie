/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package joliex.java;

import java.io.IOException;
import java.net.URI;
import jolie.net.CommMessage;
import jolie.runtime.Value;
import jolie.util.LocationParser;

/**
 * The {@code Service} class provides methods for calling a JOLIE service.
 * This is an abstract class, which must be instantiated by using the createInstance method.
 *
 * @author Fabrizio Montesi
 */
public abstract class Service
{
	protected static final String DEFAULT_RESOURCE_PATH = "/";
	private final ServiceFactory factory;
	private final String resourcePath;

	public Service( ServiceFactory factory, URI location )
	{
		this.factory = factory;
		resourcePath = LocationParser.getResourcePath( location );
	}

	public void callRequestResponse( String operationName, Value requestValue, Callback callback )
	{
		factory.execute( createRequestResponseRunnable( CommMessage.createRequest( operationName, resourcePath, requestValue ), callback ) );
	}

	public void callOneWay( String operationName, Value requestValue, Callback callback )
	{
		factory.execute( createOneWayRunnable( CommMessage.createRequest( operationName, resourcePath, requestValue ), callback ) );
	}

	public abstract void close()
		throws IOException;

	protected abstract Runnable createOneWayRunnable( CommMessage message, Callback callback );
	protected abstract Runnable createRequestResponseRunnable( CommMessage request, Callback callback );
}
