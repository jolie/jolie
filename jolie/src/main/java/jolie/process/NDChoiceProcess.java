/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.net.SessionMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.util.Pair;

/**
 * Implements a non-deterministic choice. An NDChoiceProcess instance collects pairs which couple an
 * InputOperationProcess object with a Process object. When the ChoiceProcess object is run, it
 * waits for the receiving of a communication on one of its InputProcess objects. When a
 * communication is received, the following happens: \li the communication is resolved by the
 * corresponding InputProcess instance. \li the paired Process object is executed.
 * 
 * After that, the ChoiceProcess terminates, so the other pairs are ignored.
 * 
 * @author Fabrizio Montesi
 */
public class NDChoiceProcess implements Process {
	private final Map< String, Pair< InputOperationProcess, Process > > branches;
	private final Map< String, InputOperation > inputOperationsMap;

	/**
	 * Constructor
	 * 
	 * @param branches
	 */
	public NDChoiceProcess( Pair< InputOperationProcess, Process >[] branches ) {
		Map< String, Pair< InputOperationProcess, Process > > mutBranches = new HashMap<>();
		Map< String, InputOperation > mutOperationsMap = new HashMap<>();
		for( Pair< InputOperationProcess, Process > pair : branches ) {
			mutBranches.put( pair.key().inputOperation().id(), pair );
			mutOperationsMap.put( pair.key().inputOperation().id(), pair.key().inputOperation() );
		}
		this.branches = Collections.unmodifiableMap( mutBranches );
		this.inputOperationsMap = Collections.unmodifiableMap( mutOperationsMap );
	}

	protected Map< String, Pair< InputOperationProcess, Process > > branches() {
		return branches;
	}

	protected Map< String, InputOperation > inputOperations() {
		return inputOperationsMap;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		@SuppressWarnings( "unchecked" )
		Pair< InputOperationProcess, Process >[] b = new Pair[ branches.values().size() ];
		int i = 0;
		for( Pair< InputOperationProcess, Process > pair : branches.values() ) {
			b[ i++ ] = new Pair<>( pair.key(), pair.value().copy( reason ) );
		}
		return new NDChoiceProcess( b );
	}

	/**
	 * Runs the non-deterministic choice behaviour.
	 * 
	 * @throws jolie.runtime.FaultException
	 * @throws jolie.runtime.ExitingException
	 */
	@Override
	public void run()
		throws FaultException, ExitingException {
		ExecutionThread ethread = ExecutionThread.currentThread();
		if( ethread.isKilled() ) {
			return;
		}

		Future< SessionMessage > f = ethread.requestMessage( inputOperationsMap, ethread );
		try {
			SessionMessage m = f.get();
			Pair< InputOperationProcess, Process > branch = branches.get( m.message().operationName() );
			branch.key().receiveMessage( m, ethread.state() ).run();
			branch.value().run();
		} catch( CancellationException | ExecutionException | InterruptedException e ) {
			Interpreter.getInstance().logSevere( e );
		}
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
