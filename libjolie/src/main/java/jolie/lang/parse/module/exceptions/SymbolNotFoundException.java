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

 package jolie.lang.parse.module.exceptions;

import jolie.lang.Constants;

public class SymbolNotFoundException extends Exception
{

    private static final long serialVersionUID = Constants.serialVersionUID();
    private final String symbolName;
    private final String[] moduleTargets;

    public SymbolNotFoundException( String symbolName )
    {
        super( symbolName );
        this.symbolName = symbolName;
        this.moduleTargets = null;
    }

    public SymbolNotFoundException( String symbolName, String[] moduleTargets )
    {
        super( symbolName );
        this.symbolName = symbolName;
        this.moduleTargets = moduleTargets;
    }

    @Override
    public String getMessage()
    {
        if (this.moduleTargets == null)
            return this.symbolName + " is not defined in symbolTable";
        else
            return this.symbolName + " is not defined in " + String.join(".", this.moduleTargets); 
    }
}
