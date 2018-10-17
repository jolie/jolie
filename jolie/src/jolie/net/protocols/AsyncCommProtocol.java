/********************************************************************************
 *   Copyright (C) 2017 by Martin Møller Andersen <maan511@student.sdu.dk>     *
 *   Copyright (C) 2017 by Fabrizio Montesi <famontesi@gmail.com>              *
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/
package jolie.net.protocols;

import io.netty.channel.ChannelPipeline;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.ports.InputPort;
import jolie.runtime.VariablePath;

public abstract class AsyncCommProtocol extends CommProtocol
{
	private ExecutionThread initExecutionThread = null;

	public AsyncCommProtocol( VariablePath configurationPath )
	{
		super( configurationPath );
	}

	abstract public void setupPipeline( ChannelPipeline pipeline );

	public void setupWrappablePipeline( ChannelPipeline pipeline )
	{
		setupPipeline( pipeline );
	}
	
	protected void setSendExecutionThread( Long k ){
		//if we send a response
		if ( channel().parentPort() instanceof InputPort ){
			ExecutionThread t = Interpreter.getInstance().commCore().getExecutionThread( channel() );
			setExecutionThread_internal( t != null ? t : initExecutionThread );
		}
		// if we send a request
		else {
			// this is a long
			setExecutionThread_internal( Interpreter.getInstance().commCore().pickExecutionThread( k ) );
			// once we set the ExecutionThread, we can check if the channel is threadSafe (we need the Values in the protocol)
			// If the protocol is not threadSafe, we will use the CommChannel to retrieve the ExecutionThread in the response
			// and we can remove the mapping to that ExecutionThread associated to the message ID
			if ( !isThreadSafe() )
				Interpreter.getInstance().commCore().getExecutionThread( k );
		}
	}
	
	protected <K> void setReceiveExecutionThread( K k ){
		if ( channel().parentPort() instanceof InputPort ){
			setExecutionThread_internal( initExecutionThread );
		} else {
			setExecutionThread_internal( Interpreter.getInstance().commCore().getExecutionThread( k ) );
		}
	}
	
	protected CommMessage retrieveSynchonousRequest( CommChannel c ){
		return Interpreter.getInstance().commCore().retrieveSynchronousRequest( c );
	}
	
	private void setExecutionThread_internal( ExecutionThread t )
	{
		( (CommCore.ExecutionContextThread) Thread.currentThread() ).executionThread( t );
	}

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream ) throws IOException
	{
		throw new UnsupportedOperationException( "Should not be called." );
	}

	@Override
	public CommMessage recv( InputStream istream, OutputStream ostream ) throws IOException
	{
		throw new UnsupportedOperationException( "Should not be called." );
	}

	public void setInitExecutionThread( ExecutionThread t )
	{
		initExecutionThread = t;
	}

}
