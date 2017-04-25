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
import jolie.runtime.FaultException;
import jolie.runtime.ParallelExecution;

public class ParallelBehaviour implements Behaviour
{
	final private Behaviour[] children;

	public ParallelBehaviour( Behaviour[] children )
	{
		this.children = children;
	}

	@Override
	public void run(StatefulContext ctx)
		throws FaultException
	{
		(new ParallelExecution( children, ctx )).run();
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new ParallelBehaviour( children );
	}
	
	@Override
	public boolean isKillable()
	{
		for( Behaviour child : children ) {
			if ( child.isKillable() == false ) {
				return false;
			}
		}
		
		return true;
	}
}
