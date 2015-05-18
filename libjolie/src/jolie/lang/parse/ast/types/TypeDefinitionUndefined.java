/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
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

package jolie.lang.parse.ast.types;

import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.context.URIParsingContext;

/**
 *
 * @author Fabrizio Montesi
 */
public class TypeDefinitionUndefined extends TypeInlineDefinition
{
	public static final String UNDEFINED_KEYWORD = "undefined";

	private static class LazyHolder {
		private LazyHolder() {}
		private final static TypeDefinitionUndefined instance = new TypeDefinitionUndefined();
	}

	private TypeDefinitionUndefined()
	{
		super( URIParsingContext.DEFAULT, UNDEFINED_KEYWORD, NativeType.ANY, Constants.RANGE_ONE_TO_ONE );
		super.setUntypedSubTypes( true );
	}

	public static TypeDefinitionUndefined getInstance()
	{
		return LazyHolder.instance;
	}
}
