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
import jolie.ExecutionThread;
import jolie.net.CommCore;

public abstract class AsyncCommProtocol extends CommProtocol {

  public AsyncCommProtocol( VariablePath configurationPath ) {
    super( configurationPath );
  }

  abstract public void setupPipeline( ChannelPipeline pipeline );

  public void setupWrappablePipeline( ChannelPipeline pipeline ) {
    setupPipeline( pipeline );
  }
	
	protected void setExecutionThread( ExecutionThread t ){
		 (( CommCore.ExecutionContextThread ) Thread.currentThread() )
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

}
