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

import java.util.List;

import jolie.ExecutionThread;
import jolie.util.Pair;


public class InstallProcess implements Process
{
	// The compensation is identified by an empty string
	private List< Pair< String, Process > > pairs;

	public InstallProcess( List< Pair< String, Process > > pairs )
	{
		this.pairs = pairs;
	}

	/**
	 * @todo perhaps the currentThread() thing can be stored in a local var. Check if this causes concurrency problems.
	 */
	public void run()
	{
		for( Pair< String, Process > pair : pairs ) {
			if ( pair.key() == null )
				ExecutionThread.currentThread().installCompensation( pair.value() );
			else
				ExecutionThread.currentThread().installFaultHandler( pair.key(), pair.value() );
		}
	}
}
