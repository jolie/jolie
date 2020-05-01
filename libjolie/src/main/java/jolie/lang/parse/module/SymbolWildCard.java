/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
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
package jolie.lang.parse.module;

import java.util.Arrays;

/**
 * A class represent a Symbol defined within external execution environment. Create when consuming a
 * wildcard import statement
 */
public class SymbolWildCard extends SymbolInfoExternal
{

    public SymbolWildCard( String[] moduleTarget )
    {
        super( Arrays.toString( moduleTarget ), moduleTarget, null );
    }

}