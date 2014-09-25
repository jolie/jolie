
/***************************************************************************
 *   Copyright (C) 2014 by Claudio Guidi <guidiclaudio@gmail.com>          *
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

import java.util.ArrayList;
import java.util.List;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;

/**
 *
 * @author claudio
 */
public class SemanticException extends Exception {
	ArrayList<SemanticError> errorList = new ArrayList();
	
	public static class SemanticError {
			private final String sourceName;
			private final int line;
			private final String mesg;		
			
			public SemanticError( String sourceName, int line, String mesg ) {
				this.sourceName = sourceName;
				this.line = line;
				this.mesg = mesg;
			}
			
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

	public SemanticException( ) {}
	
	public void addSemanticError( OLSyntaxNode node, String message ) {
			if ( node != null ) {
			ParsingContext context = node.context();
			errorList.add(  new SemanticError( context.sourceName(), context.line(), message ));			
		} else {
			errorList.add(  new SemanticError( "", 0, message));
		}
			
	}
	
	public List<SemanticError> getErrorList() {
			return errorList;
	}
	
	public String getErrorMessages() {
		StringBuilder message = new StringBuilder();
		for( int i = 0; i < errorList.size(); i++ ) {
			message.append( errorList.get( i).getMessage() ).append( "\n");
		}
		return message.toString();
	}
	
	
}
