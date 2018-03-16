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

import jolie.runtime.VariablePath;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jolie.net.CommMessage;

import io.netty.channel.ChannelPipeline;
import java.util.HashMap;
import jolie.ExecutionThread;
import jolie.net.CommCore;

public abstract class AsyncCommProtocol extends CommProtocol {

	private final HashMap<Long, ExecutionThread> threadCollection;
	
  public AsyncCommProtocol( VariablePath configurationPath ) {
    super( configurationPath );
		this.threadCollection = new HashMap<>();
  }

  abstract public void setupPipeline( ChannelPipeline pipeline );

  public void setupWrappablePipeline( ChannelPipeline pipeline ) {
    setupPipeline( pipeline );
  }
	
	public void setExecutionThread( ExecutionThread t ){
		 ( ( CommCore.ExecutionContextThread ) Thread.currentThread() )
			 .executionThread( t );
	}

  @Override
  public void send( OutputStream ostream, CommMessage message, InputStream istream ) throws IOException {
    throw new UnsupportedOperationException( "Should not be called." );
  }

  @Override
  public CommMessage recv( InputStream istream, OutputStream ostream ) throws IOException {
    throw new UnsupportedOperationException( "Should not be called." );
  }
	
	public void addExecutionThread( Long id, ExecutionThread t ){
		threadCollection.put( id, t );
	}
	
	public boolean hasExecutionThread( Long id ){
		return threadCollection.containsKey( id );
	}
		
		/**
	Returns the Thread associated with the @id.
	@param id
	@return Thread
	*/
	public ExecutionThread peekExecutionThread( Long id ){
		return threadCollection.get( id );
	}
		
	/**
	Returns the Thread associated with the @id and removes it from the collection.
	@param id
	@return Thread
	*/
	public ExecutionThread getExecutionThread( Long id ){
		return threadCollection.remove( id );
	}
	
	public ExecutionThread getThreadUnsafeExecutionThread(){
		Long id = threadCollection.keySet().stream().findFirst().get();
		return getExecutionThread( id );
	}

}
