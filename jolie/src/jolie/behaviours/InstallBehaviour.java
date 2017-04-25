/***************************************************************************
 *   Copyright (C) 2007-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.util.List;
import jolie.StatefulContext;
import jolie.runtime.HandlerInstallationReason;
import jolie.util.Pair;


public class InstallBehaviour implements Behaviour
{
	// The compensation is identified by an empty string
	private final List< Pair< String, Behaviour > > pairs;

	public InstallBehaviour( List< Pair< String, Behaviour > > pairs )
	{
		this.pairs = pairs;
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new InstallBehaviour( pairs );
	}

	@Override
	public void run( StatefulContext ctx )
	{
		for( Pair< String, Behaviour > pair : pairs ) {
			final Behaviour handler = pair.value().clone( new HandlerInstallationReason( pair.key() ) );
			if ( pair.key() == null ) {
				ctx.installCompensation( handler );
			} else {
				ctx.installFaultHandler( pair.key(), handler );
			}
		}
	}
	
	@Override
	public boolean isKillable()
	{
		return false;
	}
}
