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

package jolie.deploy.wsdl;

import java.util.Vector;

public class OperationWSDLInfo implements Cloneable
{
	private Vector< String > inVarNames;
	private Vector< String > outVarNames;
	private String boundName;
	private PortType portType;
	
	public OperationWSDLInfo clone()
	{
		OperationWSDLInfo retval = new OperationWSDLInfo();
		retval.setInVarNames( inVarNames );
		retval.setOutVarNames( outVarNames );
		retval.setBoundName( boundName );
		retval.setPortType( portType );
		return retval;
	}
	
	public void setPortType( PortType portType )
	{
		this.portType = portType;
	}
	
	public PortType portType()
	{
		return portType;
	}
	
	public OperationWSDLInfo()
	{
		inVarNames = null;
		outVarNames = null;
		boundName = null;
		portType = null;
	}
	
	public String boundName()
	{
		return boundName;
	}
	
	public Vector< String > inVarNames()
	{
		return inVarNames;
	}
	
	public Vector< String > outVarNames()
	{
		return outVarNames;
	}
	
	public void setBoundName( String boundName )
	{
		this.boundName = boundName;
	}
	
	public void setOutVarNames( Vector< String > outVarNames )
	{
		this.outVarNames = outVarNames;
	}
	
	public void setInVarNames( Vector< String > inVarNames )
	{
		this.inVarNames = inVarNames;
	} 
}
