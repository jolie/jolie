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
import jolie.runtime.VariablePath;
import jolie.runtime.Value;

public class ForEachProcess implements Process
{
	private VariablePath keyPath, valuePath, targetPath;
	private Process process;

	public ForEachProcess(
			VariablePath keyPath,
			VariablePath valuePath,
			VariablePath targetPath,
			Process process )
	{
		this.keyPath = keyPath;
		this.valuePath = valuePath;
		this.targetPath = targetPath;
		this.process = process;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new ForEachProcess( keyPath, valuePath, targetPath, process.clone( reason ) );
	}
	
	public void run()
		throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;
		
		Value target = targetPath.getValue();
		VariablePath currPath;
		for( String id : target.children().keySet() ) {
			keyPath.getValue().setStrValue( id );
			currPath = targetPath.clone();
			currPath.addPathNode( id, null );
			valuePath.makePointer( currPath );
			
			process.run();
		}
	}
}
