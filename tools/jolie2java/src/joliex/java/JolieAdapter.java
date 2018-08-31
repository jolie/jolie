/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
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
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

/**
 * An adapter for calling One-Way and Request-Response operations of the
 * embedder of a {@code JavaService}.
 * @see JavaService
 * @author Fabrizio Montesi
 */
public class JolieAdapter
{
	private final JavaService javaService;
	private final String resourcePath;

	public JolieAdapter( JavaService javaService, String resourcePath )
	{
		 this.javaService = javaService;
		this.resourcePath = resourcePath;
	}

	public Value callRequestResponse( String operationName, Value request )
		throws FaultException
	{
		CommMessage requestMesg = CommMessage.createRequest( operationName, resourcePath, request );
		try {
			CommMessage response = javaService.sendMessage( requestMesg ).recvResponseFor( requestMesg );
			if ( response.isFault() ) {
				throw response.fault();
			}
			return response.value();
		} catch( IOException e ) {
			return Value.create();
		}
	}

	public void callOneWay( String operationName, Value request )
	{
		CommMessage requestMesg = CommMessage.createRequest( operationName, resourcePath, request );
		javaService.sendMessage( requestMesg );
	}
}
