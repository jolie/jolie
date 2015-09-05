/***************************************************************************
 *   Copyright 2006-2011 (C) by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.io.Serializable;
import java.util.List;
import jolie.lang.Constants;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

public class CorrelationSetInfo extends OLSyntaxNode
{
	public static class CorrelationAliasInfo implements Serializable {
		private static final long serialVersionUID = Constants.serialVersionUID();
		
		private final String guardName;
		private final VariablePathNode variablePath;

		public CorrelationAliasInfo( String guardName, VariablePathNode variablePath )
		{
			this.guardName = guardName;
			this.variablePath = variablePath;
		}

		public String guardName()
		{
			return guardName;
		}

		public VariablePathNode variablePath()
		{
			return variablePath;
		}
	}

	public static class CorrelationVariableInfo implements Serializable {
		private static final long serialVersionUID = Constants.serialVersionUID();
		
		private final VariablePathNode correlationVariablePath;
		private final List< CorrelationAliasInfo > aliases;

		public CorrelationVariableInfo( VariablePathNode correlationVariablePath, List< CorrelationAliasInfo > aliases )
		{
			this.correlationVariablePath = correlationVariablePath;
			this.aliases = aliases;
		}

		public List< CorrelationAliasInfo > aliases()
		{
			return aliases;
		}

		public VariablePathNode correlationVariablePath()
		{
			return correlationVariablePath;
		}
	}

	private final List< CorrelationVariableInfo > variables;
	
	public CorrelationSetInfo( ParsingContext context, List< CorrelationVariableInfo > variables )
	{
		super( context );
		this.variables = variables;
	}
	
	public List< CorrelationVariableInfo > variables()
	{
		return variables;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
