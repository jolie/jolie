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


package jolie.runtime;

import jolie.deploy.OperationDeployInfo;

/** Generic operation declaration
 * 
 * @author Fabrizio Montesi
 * @todo Change the behaviour of getByWSDLBoundName (rather ugly) through a bi-directional HashMap.
 *
 */
abstract public class Operation extends AbstractMappedGlobalObject
{
	private OperationDeployInfo deployInfo;
	
	public OperationDeployInfo deployInfo()
	{
		return deployInfo;
	}

	public Operation( String id )
	{
		super( id );
		deployInfo = new OperationDeployInfo();
	}
	
	public String value()
	{
		return id();
	}
	
	abstract public void register();
}