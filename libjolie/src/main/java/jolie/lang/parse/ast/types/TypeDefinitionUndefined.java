/*
 * Copyright (C) 2009-2020 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang.parse.ast.types;

import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.context.ParsingContext;

/**
 *
 * @author Fabrizio Montesi
 */
public class TypeDefinitionUndefined extends TypeInlineDefinition {
	private static final long serialVersionUID = Constants.serialVersionUID();

	public static final String UNDEFINED_KEYWORD = "undefined";

	private static class LazyHolder {
		private LazyHolder() {}

		private final static TypeDefinitionUndefined INSTANCE = new TypeDefinitionUndefined();
	}

	private TypeDefinitionUndefined() {
		super( ParsingContext.DEFAULT, UNDEFINED_KEYWORD, BasicTypeDefinition.of( NativeType.ANY ),
			Constants.RANGE_ONE_TO_ONE );
		super.setUntypedSubTypes( true );
	}

	public static TypeDefinitionUndefined getInstance() {
		return LazyHolder.INSTANCE;
	}
}
