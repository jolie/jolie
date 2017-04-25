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

import jolie.StatefulContext;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;

public interface Behaviour
{	
	public void run( StatefulContext ctx )
		throws FaultException, ExitingException;
	
	public default Behaviour clone( TransformationReason reason )
	{
		throw new UnsupportedOperationException( "Called clone on a behaviour that was not supposed to be cloned. (This is a bug in the Jolie interpreter: please report it!)" );
	}
	
	public boolean isKillable();
	
	public static interface BehaviourRunnable {
		public void run( StatefulContext ctx )
			throws FaultException, ExitingException;
	}
	
	public static Behaviour killable( BehaviourRunnable runnable )
	{
		return (KillableBehaviour) runnable::run;
	}
	
	public static Behaviour unkillable( BehaviourRunnable runnable )
	{
		return (UnkillableBehaviour) runnable::run;
	}
	
	public static Behaviour unkillableLater( BehaviourRunnable runnable )
	{
		return (UnkillableBehaviour) ( ctx ) -> {
			if ( !ctx.isKilled() ) runnable.run( ctx );
		};
	}
}
