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
import jolie.runtime.HandlerInstallationReason;


public class CurrentHandlerProcess implements Process {
	private CurrentHandlerProcess() {}

	private static class LazyHolder {
		private LazyHolder() {}

		private static final CurrentHandlerProcess INSTANCE = new CurrentHandlerProcess();
	}

	public static CurrentHandlerProcess getInstance() {
		return CurrentHandlerProcess.LazyHolder.INSTANCE;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		Process ret = getInstance();
		if( reason instanceof HandlerInstallationReason ) {
			HandlerInstallationReason r = (HandlerInstallationReason) reason;
			if( r.handlerId() == null )
				ret = ExecutionThread.currentThread().getCurrentScopeCompensation();
			else
				ret = ExecutionThread.currentThread().getFaultHandler( r.handlerId(), false );

			if( ret == null )
				ret = NullProcess.getInstance();
		}

		return ret;
	}

	@Override
	public void run() {
		// We should never execute this process node.
		assert (false);
	}

	@Override
	public boolean isKillable() {
		// TODO: check this
		return true;
	}
}
