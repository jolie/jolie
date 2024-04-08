/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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
 * @author Fabrizio Montesi
 */
public class ProvideUntilProcess implements Process {
	private final NDChoiceProcess provide, until;
	private final Map< String, InputOperation > inputOperationsMap;

	public ProvideUntilProcess( NDChoiceProcess provide, NDChoiceProcess until ) {
		this.provide = provide;
		this.until = until;
		Map< String, InputOperation > mutOperationsMap = new HashMap<>();
		mutOperationsMap.putAll( provide.inputOperations() );
		mutOperationsMap.putAll( until.inputOperations() );
		this.inputOperationsMap = Collections.unmodifiableMap( mutOperationsMap );
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new ProvideUntilProcess( (NDChoiceProcess) provide.copy( reason ),
			(NDChoiceProcess) until.copy( reason ) );
	}

	@Override
	public void run()
		throws FaultException, ExitingException {
		ExecutionThread ethread = ExecutionThread.currentThread();
		if( ethread.isKilled() ) {
			return;
		}

		boolean keepRun = true;

		try {
			while( keepRun ) {
				Future< SessionMessage > f = ethread.requestMessage( inputOperationsMap, ethread );

				SessionMessage m = f.get();
				Pair< InputOperationProcess, Process > branch = provide.branches().get( m.message().operationName() );
				if( branch == null ) {
					// It is an until branch
					branch = until.branches().get( m.message().operationName() );
					keepRun = false;
				}
				branch.key().receiveMessage( m, ethread.state() ).run();
				branch.value().run();
			}
		} catch( CancellationException | InterruptedException | ExecutionException e ) {
			Interpreter.getInstance().logSevere( e );
		}
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
