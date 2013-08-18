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

import jolie.lang.Constants;

public class ParserException extends Exception
{
	private static final long serialVersionUID = Constants.serialVersionUID();
	
	private final String sourceName;
	private final int line;
	private final String mesg;
	
	public ParserException( String sourceName, int line, String mesg )
	{
		this.sourceName = sourceName;
		this.line = line;
		this.mesg = mesg;
	}
	
	@Override
	public String getMessage()
	{
		return new StringBuilder()
			.append( this.sourceName )
			.append( ':' )
			.append( line )
			.append( ": error: " )
			.append( mesg )
			.toString();
	}
        
        public int getLine() {
            return line;
        }
        
        public String getSourceName() {
            return sourceName;
        }
}