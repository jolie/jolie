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
import jolie.runtime.FaultException;

public class ScopeProcess implements Process
{
	private String id;
	private Process process;
	
	public ScopeProcess( String id, Process process )
	{
		this.id = id;
		this.process = process;
	}
	
	public void run()
		throws FaultException
	{
		ExecutionThread t = ExecutionThread.currentThread();
		t.pushScope( id );
		try {
			runScope( process );
			t.popScope();
		} catch( FaultException f ) {
			t.popScope( false );
			throw f;
		}
	}
	
	private void runScope( Process p )
		throws FaultException
	{
		try {
			p.run();
			
			if ( ExecutionThread.killed() ) {
				Process handler = ExecutionThread.getCompensation( id );
				if ( handler != null ) {
					ExecutionThread.clearKill();
					this.runScope( handler );
					ExecutionThread.setKill();
				}
			}
		} catch( FaultException f ) {
			Process handler = ExecutionThread.getFaultHandler( f.fault() );
			if ( handler != null ) {
				this.runScope( handler );
			} else
				throw f;
		}
	}
}
