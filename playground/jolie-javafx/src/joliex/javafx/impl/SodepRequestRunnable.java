/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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
package joliex.javafx.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import javafx.async.RunnableFuture;
import jolie.net.CommMessage;
import jolie.net.SodepProtocol;
import jolie.runtime.ClosedVariablePath;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.util.LocationParser;

/**
 *
 * @author Fabrizio Montesi
 */
public class SodepRequestRunnable implements RunnableFuture
{
    private final String location;
    private final String operationName;
    private final Value value;
    private final SodepRequestListener listener;

    public SodepRequestRunnable(
	String location,
	String operationName,
	Value value,
	SodepRequestListener listener )
    {
	this.location = location;
	this.operationName = operationName;
	this.value = value;
	this.listener = listener;
    }

    @Override
    public void run()
    {
	try {
	    URI uri = new URI( location );
	    String resourcePath = LocationParser.getResourcePath( uri );
	    Socket socket = new Socket( uri.getHost(), uri.getPort() );
	    SodepProtocol sodep = new SodepProtocol(
		new ClosedVariablePath( VariablePath.EmptyPathLazyHolder.emptyPath, Value.create() ) );
	    CommMessage request = CommMessage.createRequest( operationName, resourcePath, value );
	    InputStream istream = socket.getInputStream();
	    OutputStream ostream = socket.getOutputStream();
	    sodep.send( ostream, request, istream );
	    CommMessage response = sodep.recv( istream, ostream );
	    if ( response.isFault() ) {
		listener._onFault( response.fault() );
	    } else {
		listener._onComplete( response.value() );
	    }
	} catch( IOException e ) {
	    listener._onError( e );
	} catch( URISyntaxException e ) {
	    listener._onError( e );
	}
    }
}
