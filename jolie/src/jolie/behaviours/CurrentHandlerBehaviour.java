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

import jolie.ExecutionThread;
import jolie.StatefulContext;
import jolie.runtime.HandlerInstallationReason;


public class CurrentHandlerBehaviour implements Behaviour
{
	private CurrentHandlerBehaviour(){}
	
	private static class LazyHolder {
		private LazyHolder() {}
		static final CurrentHandlerBehaviour instance = new CurrentHandlerBehaviour();
	}
	
	static public CurrentHandlerBehaviour getInstance()
	{
		return CurrentHandlerBehaviour.LazyHolder.instance;
	}
	
	public Behaviour clone( TransformationReason reason )
	{
		Behaviour ret = getInstance();
		if ( reason instanceof HandlerInstallationReason ) {
			HandlerInstallationReason r = (HandlerInstallationReason)reason;
			if ( r.handlerId() == null )
				ret = ExecutionThread.currentThread().getCurrentScopeCompensation();
			else
				ret = ExecutionThread.currentThread().getFaultHandler( r.handlerId(), false );
			
			if ( ret == null )
				ret = NullBehaviour.getInstance();
		}
		
		return ret;
	}
	
	public void run(StatefulContext ctx)
	{
		// We should never execute this process node.
		assert( false );
	}
	
	public boolean isKillable()
	{
		// TODO: check this
		return true;
	}
}