/*****************************************************************************
 * Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                           *
 *                                                                           *
 *   This program is free software; you can redistribute it and/or modify    *
 *   it under the terms of the GNU Library General Public License as         *  
 *   published by the Free Software Foundation; either version 2 of the      *
 *   License, or (at your option) any later version.                         *
 *                                                                           *
 *   This program is distributed in the hope that it will be useful,         *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 *   GNU General Public License for more details.                            *
 *                                                                           *
 *   You should have received a copy of the GNU Library General Public       *
 *   License along with this program; if not, write to the                   *
 *   Free Software Foundation, Inc.,                                         *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.               *
 *                                                                           *
 *   For details about the authors of this software, see the AUTHORS file.   *
 *****************************************************************************/
package jolie.net;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class PubSubListener extends CommListener {

	CommChannel channel;
	
	public PubSubListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort
	)
		throws IOException {
		super( interpreter, protocolFactory, inputPort );
	}

	@Override
	public void shutdown() {
		System.out.println( "Shutting down the listener" );
//		try {
//			channel.closeImpl();
//		} catch ( IOException ex ) {
//			Logger.getLogger( PubSubListener.class.getName() ).log( Level.SEVERE, null, ex );
//		}
	}

	@Override
	public void run() {
		// wait for the init to load the configurationParameters
		String protocol = "";
		String broker = "";
		do {
			protocol = inputPort().protocolConfigurationPath().getValue().strValue();
			broker = inputPort().protocolConfigurationPath().evaluate().getFirstChild( "broker" ).strValue();
		} while( protocol.equals( "" ) || broker.equals( "") );
		try {
			URI location = URI.create( broker );
			channel = interpreter().commCore().createInputCommChannel( location, inputPort() );
		} catch ( IOException ex ) {
			Logger.getLogger( PubSubListener.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}
}
