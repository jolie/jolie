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

package jolie.lang.parse.ast;

import java.util.ArrayList;
import java.util.List;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

public class OutputPortInfo extends PortInfo {

	private OLSyntaxNode protocol = null;
	private OLSyntaxNode location = null;
	private List<InterfaceExtenderDefinition> interfaceExtenders = new ArrayList<>();

	public OutputPortInfo( ParsingContext context, String id ) {
		super( context, id );
	}

	public void setProtocol( OLSyntaxNode protocol ) {
		this.protocol = protocol;
	}

	public void setLocation( OLSyntaxNode location ) {
		this.location = location;
	}

	/**
	 * @param interfaceExtender the interfaceExtender to set
	 */
	public void addInterfaceExtender( InterfaceExtenderDefinition interfaceExtender ) {
		this.interfaceExtenders.add(interfaceExtender);
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}

	public OLSyntaxNode protocol() {
		return protocol;
	}

	public String protocolId() {
		return PortInfo.extractProtocolId( this.protocol );
	}

	public OLSyntaxNode location() {
		return location;
	}

	/**
	 * @return the interfaceExtender
	 */
	public InterfaceExtenderDefinition[] interfaceExtenders() {
		return interfaceExtenders.toArray(new InterfaceExtenderDefinition[0]);
	}
}
