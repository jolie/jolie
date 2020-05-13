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

import jolie.lang.Constants;
import jolie.lang.parse.context.ParsingContext;

public class ModuleException extends Exception
{

    private static final long serialVersionUID = Constants.serialVersionUID();
    private ParsingContext context = null;

    public ModuleException( ParsingContext context, String message )
    {
        super( message );
        this.context = context;
    }

    public ModuleException( String message )
    {
        super( message );
    }

    public ModuleException( ParsingContext context, Throwable arg1 )
    {
        super( arg1 );
        this.context = context;
    }

    public ModuleException( String arg0, Throwable arg1 )
    {
        super( arg0, arg1 );
    }

    public ModuleException( Throwable arg0 )
    {
        super( arg0 );
    }

    public void setContext( ParsingContext context )
    {
        this.context = context;
    }

    @Override
    public String getMessage()
    {
        if ( context != null ) {
            return new StringBuilder().append( context.sourceName() ).append( ':' )
                    .append( context.line() ).append( ": error: " ).append( super.getMessage() )
                    .toString();
        }
        return super.getMessage();
    }
}
