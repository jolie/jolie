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

package jolie.behaviours;

import java.util.ArrayList;
import jolie.Interpreter;
import jolie.StatefulContext;
import jolie.net.ports.OutputPort;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InvalidIdException;
import jolie.runtime.Value;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoadingException;

public class InitDefinitionBehaviour extends DefinitionBehaviour
{
	public InitDefinitionBehaviour( Behaviour process )
	{
		super( process );
	}
	
	@Override
	public void run(StatefulContext ctx)
		throws FaultException
	{
		ArrayList<Behaviour> behaviours = new ArrayList(64);
		Interpreter interpreter = ctx.interpreter();
		
		for( OutputPort outputPort : interpreter.outputPorts() ) {
			//outputPort.configurationProcess().run( ctx );
			behaviours.add( outputPort.configurationProcess() );
		}

		behaviours.add( new SimpleBehaviour()
		{
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				for( EmbeddedServiceLoader loader : interpreter.embeddedServiceLoaders() ) {
					try {
						loader.load();
					} catch( EmbeddedServiceLoadingException ex ) {
						interpreter.logSevere( ex );
					}
				}
			}
		});

		behaviours.add( new SimpleBehaviour()
		{
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				for( OutputPort outputPort : interpreter.outputPorts() ) {
					outputPort.optimizeLocation( ctx );
				}
			}
		});

		behaviours.add( new SimpleBehaviour()
		{
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				interpreter.embeddedServiceLoaders().clear(); // Clean up for GC
			}
		});

		for( Behaviour p : interpreter.commCore().protocolConfigurations() ) {
			behaviours.add( p );
		}

		behaviours.add( new SimpleBehaviour()
		{
			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				// If an internal service, copy over the output port locations from the parent service
				if ( interpreter.parentInterpreter() != null ) {
					Value parentInitRoot = interpreter.parentInterpreter().initContext().state().root();
					for ( OutputPort parentPort : interpreter.parentInterpreter().outputPorts() ) {
						try {
							OutputPort port = interpreter.getOutputPort( parentPort.id() );
							port.locationVariablePath().setValue( parentPort.locationVariablePath().getValue( parentInitRoot ) );
							port.protocolConfigurationPath().setValue( parentPort.protocolConfigurationPath().getValue( parentInitRoot ) );
							port.optimizeLocation( ctx );
						} catch( InvalidIdException e ) {}
					}
				}
			}
		});

		ctx.executeNext( new SequentialBehaviour(behaviours.toArray(new Behaviour[behaviours.size()])) );

		try {
			super.run( ctx );
		} catch( ExitingException e ) {}
	}
}