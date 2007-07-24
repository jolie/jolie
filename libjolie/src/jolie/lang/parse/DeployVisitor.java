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

package jolie.lang.parse;

import jolie.lang.parse.ast.deploy.*;

public interface DeployVisitor
{
	public void visit( DeployInfo n );
	public void visit( LocationDeployInfo n );
	public void visit( WSDLInfo n );
	public void visit( OneWayOperationDeployInfo n );
	public void visit( NotificationOperationDeployInfo n );
	public void visit( RequestResponseOperationDeployInfo n );
	public void visit( SolicitResponseOperationDeployInfo n );
	public void visit( InputPortTypeInfo n );
	public void visit( OutputPortTypeInfo n );
	public void visit( PartnerLinkTypeInfo n );
	public void visit( PortBindingInfo n );
	public void visit( ServiceInfo n );
	public void visit( CorrelationSetInfo n );
	public void visit( StateInfo n );
	public void visit( ExecutionInfo n );
}
