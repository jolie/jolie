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


import jolie.ExecutionThread;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;

public class SequentialProcess implements Process {
	final private Process[] children;

	public SequentialProcess( Process[] children ) {
		if( children.length < 1 ) {
			throw new IllegalArgumentException( "Process sequences must contain at least one child." );
		}
		this.children = children;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		Process[] p = new Process[ children.length ];
		int i = 0;
		for( Process child : children ) {
			p[ i++ ] = child.copy( reason );
		}
		return new SequentialProcess( p );
	}

	@Override
	public void run()
		throws FaultException, ExitingException {
		final ExecutionThread ethread = ExecutionThread.currentThread();
		for( Process proc : children ) {
			if( ethread.isKilled() && proc.isKillable() ) {
				return;
			}
			proc.run();
		}
	}

	@Override
	public boolean isKillable() {
		return children[ 0 ].isKillable();
	}
}
