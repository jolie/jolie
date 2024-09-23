/*
 * Copyright (C) 2024 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.process;

import java.util.concurrent.CompletableFuture;
import jolie.Interpreter;
import jolie.net.ports.OutputPort;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InvalidIdException;
import jolie.runtime.Value;
import jolie.runtime.embedding.EmbeddedServiceLoader;

public class InitProcess implements Process {
	private final CompletableFuture< Exception > initFuture;

	public InitProcess( CompletableFuture< Exception > initFuture ) {
		this.initFuture = initFuture;
	}

	@Override
	public void run()
		throws FaultException {
		Interpreter interpreter = Interpreter.getInstance();
		try {
			// Configure input ports
			for( Process p : interpreter.commCore().protocolConfigurations() ) {
				try {
					p.run();
				} catch( ExitingException e ) {
					interpreter.logSevere( e );
					assert false;
				}
			}

			for( OutputPort outputPort : interpreter.outputPorts() ) {
				try {
					outputPort.configurationProcess().run();
				} catch( FaultException | FaultException.RuntimeFaultException fe ) {
					// If this happens, it's been caused by a bug in the SemanticVerifier
					assert (false);
				} catch( ExitingException e ) {
					interpreter.logSevere( e );
					assert false;
				}
			}

			for( EmbeddedServiceLoader loader : interpreter.embeddedServiceLoaders() ) {
				loader.load();
			}

			for( OutputPort outputPort : interpreter.outputPorts() ) {
				outputPort.optimizeLocation();
			}

			interpreter.embeddedServiceLoaders().clear(); // Clean up for GC

			// If an internal service, copy over the output port locations from the parent service
			if( interpreter.parentInterpreter() != null ) {
				Value parentInitRoot = interpreter.parentInterpreter().initThread().state().root();
				for( OutputPort parentPort : interpreter.parentInterpreter().outputPorts() ) {
					try {
						OutputPort port = interpreter.getOutputPort( parentPort.id() );

						// TODO: This is a hack. Deprecate internal services in future releases.
						port.locationVariablePath()
							.setValue( parentPort.locationVariablePath().getValue( parentInitRoot ) );

						parentPort.protocolConfigurationPath().getValueOpt( parentInitRoot )
							.ifPresent( port.protocolConfigurationPath()::setValue );
						port.optimizeLocation();
					} catch( InvalidIdException e ) {
					}
				}
			}

			initFuture.complete( null );
		} catch( Exception e ) {
			interpreter.logSevere( e );
			initFuture.complete( e );
		}
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return this;
	}

	@Override
	public boolean isKillable() {
		return false;
	}
}
