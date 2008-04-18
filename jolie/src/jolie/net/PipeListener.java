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

package jolie.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import jolie.Interpreter;

public class PipeListener extends CommListener
{
	static class ByteArrayCommChannel extends CommChannel
	{
		protected CommProtocol protocol;
		protected ByteArrayOutputStream istream;
		protected ByteArrayOutputStream ostream;

		public ByteArrayCommChannel(
				CommProtocol protocol,
				ByteArrayOutputStream istream,
				ByteArrayOutputStream ostream )
		{
			this.protocol = protocol;
			this.istream = istream;
			this.ostream = ostream;
			protocol.setChannel( this );
		}

		protected void closeImpl()
			throws IOException
		{
			synchronized( ostream ) {
				ostream.close();
			}
		}

		public CommMessage recv()
			throws IOException
		{
			InputStream stream;
			CommMessage ret;
			synchronized( istream ) {
				while( (stream = new ByteArrayInputStream( istream.toByteArray() )).available() <= 0 ) {
					try {
						istream.wait();
					} catch( InterruptedException ie ) {}
				}
				ret = protocol.recv( stream );
				if ( stream.available() <= 0 )
					istream.reset();
			}
			return ret;
		}

		public void send( CommMessage message )
			throws IOException
		{
			synchronized( ostream ) {
				protocol.send( ostream, message );
				ostream.notifyAll();
			}
		}
	}
	
	static class PipeCommChannel extends ByteArrayCommChannel {
		private PipeListener listener;
		public PipeCommChannel( PipeListener listener, CommProtocol protocol )
		{
			super( protocol, new ByteArrayOutputStream(), new ByteArrayOutputStream() );
			this.toBeClosed = false;
			this.listener = listener;
		}
		public void send( CommMessage message )
			throws IOException
		{
			super.send( message );
			synchronized( listener ) {
				listener.currentChannel = this;
				if ( listener.waiting )
					listener.notify();
			}
		}
		/*protected void disposeForInputImpl()
		{
			synchronized( istream ) {
				
			}
		}*/
	}
	
	protected PipeCommChannel currentChannel = null;
	protected boolean waiting = false;
	
	public PipeListener(
			Interpreter interpreter,
			CommProtocol protocol,
			Collection< InputPort > inputPorts,
			Map< String, OutputPort > redirectionMap
			)
		throws IOException
	{
		super( interpreter, protocol, inputPorts, redirectionMap );
	}
	
	public CommChannel createPipeCommChannel()
		throws IOException
	{
		return new PipeCommChannel( this, createProtocol() );
	}
	
	public void run()
	{
		CommChannel channel;
		try {
			while( isInterrupted() == false ) {
				synchronized( this ) {
					if ( currentChannel == null ) {
						waiting = true;
						wait();
					}
					channel =
						new ByteArrayCommChannel(
								currentChannel.protocol.clone(),
								currentChannel.ostream,
								currentChannel.istream );
					channel.setParentListener( this );
					interpreter().commCore().scheduleReceive( channel, this );	
					currentChannel = null;
				}
			}
		} catch( InterruptedException ie ) {}
	}
}
