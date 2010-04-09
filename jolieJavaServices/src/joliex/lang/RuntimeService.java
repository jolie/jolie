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

package joliex.lang;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import jolie.lang.Constants;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants.EmbeddedServiceType;
import jolie.net.CommListener;
import jolie.net.LocalCommChannel;
import jolie.net.OutputPort;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoaderCreationException;
import jolie.runtime.embedding.EmbeddedServiceLoadingException;
import jolie.runtime.FaultException;
import jolie.runtime.InvalidIdException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;
import jolie.runtime.embedding.RequestResponse;

public class RuntimeService extends JavaService
{
	private final Interpreter interpreter;
	
	public RuntimeService()
	{
		this.interpreter = Interpreter.getInstance();
	}
	
	public Value getLocalLocation()
	{
		Value v = Value.create();
		v.setValue( interpreter.commCore().getLocalCommChannel() );
		return v;
	}

	@RequestResponse
	public void setOutputPort( Value request )
	{
		String name = request.getFirstChild( "name" ).strValue();
		Value locationValue = request.getFirstChild( "location" );
		Value protocolValue = request.getFirstChild( "protocol" );
		OutputPort port =
			new OutputPort(
					interpreter(),
					name
				);
		Value l;
		Value r = interpreter.mainThread().state().root();
		l =	r.getFirstChild( name ).getFirstChild( Constants.LOCATION_NODE_NAME );
		if ( locationValue.isChannel() ) {
			l.setValue( locationValue.channelValue() );
		} else {
			l.setValue( locationValue.strValue() );
		}
		r.getFirstChild( name ).getFirstChild( Constants.PROTOCOL_NODE_NAME ).refCopy( protocolValue );

		r = ExecutionThread.currentThread().state().root();
		l =	r.getFirstChild( name ).getFirstChild( Constants.LOCATION_NODE_NAME );
		if ( locationValue.isChannel() ) {
			l.setValue( locationValue.channelValue() );
		} else {
			l.setValue( locationValue.strValue() );
		}
		r.getFirstChild( name ).getFirstChild( Constants.PROTOCOL_NODE_NAME ).deepCopy( protocolValue );

		interpreter.register( name, port );
	}

	@RequestResponse
	public void removeOutputPort( String outputPortName )
	{
		interpreter.removeOutputPort( outputPortName );
	}

	@RequestResponse
	public void setRedirection( Value request )
		throws FaultException
	{
		String serviceName = request.getChildren( "inputPortName" ).first().strValue();
		CommListener listener =
			interpreter.commCore().getListenerByInputPortName( serviceName );
		if ( listener == null )
			throw new FaultException( "RuntimeException", "Unknown inputPort: " + serviceName );
		
		String resourceName = request.getChildren( "resourceName" ).first().strValue();
		String opName = request.getChildren( "outputPortName" ).first().strValue();
		try {
			OutputPort port = interpreter.getOutputPort( opName );
			listener.redirectionMap().put( resourceName, port );
		} catch( InvalidIdException e ) {
			throw new FaultException( "RuntimeException", e );
		}
	}

	@RequestResponse
	public void removeRedirection( Value request )
		throws FaultException
	{
		String serviceName = request.getChildren( "inputPortName" ).first().strValue();
		CommListener listener =
			interpreter.commCore().getListenerByInputPortName( serviceName );
		if ( listener == null )
			throw new FaultException( "RuntimeException", "Unknown inputPort: " + serviceName );
		
		String resourceName = request.getChildren( "resourceName" ).first().strValue();
		listener.redirectionMap().remove( resourceName );
	}

	public Value getRedirection( Value request )
		throws FaultException
	{
		Value ret = null;
		String inputPortName = request.getChildren( "inputPortName" ).first().strValue();
		CommListener listener =
			interpreter.commCore().getListenerByInputPortName( inputPortName );
		if ( listener == null ) {
			throw new FaultException( "RuntimeException", Value.create( "Invalid input port: " + inputPortName ) );
		}
		
		String resourceName = request.getChildren( "resourceName" ).first().strValue();
		OutputPort p = listener.redirectionMap().get( resourceName );
		if ( p == null ) {
			ret = Value.create();
		} else {
			ret = Value.create( p.id() );
		}
		return ret;
	}
	
	public Value loadEmbeddedService( Value request )
		throws FaultException
	{
		try {
			Value channel = Value.create();
			String filePath = request.getFirstChild( "filepath" ).strValue();
			String typeStr = request.getFirstChild( "type" ).strValue();
			EmbeddedServiceType type =
				jolie.lang.Constants.stringToEmbeddedServiceType( typeStr );
			EmbeddedServiceLoader loader =
				EmbeddedServiceLoader.create( interpreter(), type, filePath, channel );
			loader.load();

			return channel;
		} catch( EmbeddedServiceLoaderCreationException e ) {
			e.printStackTrace();
			throw new FaultException( "RuntimeException", e );
		} catch( EmbeddedServiceLoadingException e ) {
			e.printStackTrace();
			throw new FaultException( "RuntimeException", e );
		}
	}

	@RequestResponse
	public void callExit( Value request )
	{
		Object o = request.valueObject();
		if ( o instanceof LocalCommChannel ) {
			((LocalCommChannel)o).interpreter().exit();
		}
	}

	public String dumpState()
	{
		Writer writer = new StringWriter();
		ValuePrettyPrinter printer = new ValuePrettyPrinter( Value.createDeepCopy( interpreter.globalValue() ), writer, "Global state" );
		try {
			printer.run();
			printer = new ValuePrettyPrinter( Value.createDeepCopy( ExecutionThread.currentThread().state().root() ), writer, "Session state" );
			printer.run();
		} catch( IOException e ) {} // Should never happen
		return writer.toString();
	}
}
