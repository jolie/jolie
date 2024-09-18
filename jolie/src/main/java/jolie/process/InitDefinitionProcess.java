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

package jolie.process;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import jolie.Interpreter;
import jolie.net.ports.OutputPort;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InvalidIdException;
import jolie.runtime.Value;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoadingException;

public class InitDefinitionProcess extends DefinitionProcess {

	/**
	 * A `CompletableFuture` that holds the result of the execution.
	 *
	 * It is completed with either `null` if the initialization of the service is successful or an
	 * `Exception` if an error occurs.
	 */
	private final CompletableFuture< Exception > executionDone;

	public InitDefinitionProcess( Process process ) {
		super( process );
		this.executionDone = new CompletableFuture<>();
	}

	public Future< Exception > getFuture() {
		return executionDone;
	}

	/**
	 * Runs the initialization process.
	 *
	 * This method overrides the `run()` method from the parent class and performs the following steps:
	 * 1. Iterates through the communication core's protocol configurations and calls their `run()`
	 * method, to initialize input ports configurations of the service. 2. Iterates through the
	 * interpreter's output ports and calls their `configurationProcess().run()` method, to initialize
	 * all output ports configurations of the service. 3. Loads all embedded services using the
	 * `embeddedServiceLoaders`. 4. Optimizes the location of all output ports. 5. Clears the embedded
	 * service loaders. 6. If the interpreter is an internal service, copies the output port locations
	 * from the parent service. (This is marked as a hack and deprecated for future releases). 7.
	 * Attempts to complete the `executionDone` future with either `null` (success) or an `Exception`
	 * (failure). This set the completion of the initialise phase. 8. Calls the parent class's `run()`
	 * method to start the execution of init block.
	 *
	 * @throws FaultException If an error occurs during the initialization process.
	 */
	@Override
	public void run() throws FaultException {
		Interpreter interpreter = Interpreter.getInstance();
		try {
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

			try {
				this.executionDone.complete( null );
				super.run();
			} catch( ExitingException e ) {
				this.executionDone.complete( e );
			}
		} catch( EmbeddedServiceLoadingException e ) {
			interpreter.logSevere( e );
			this.executionDone.complete( e );
		}
	}
}
