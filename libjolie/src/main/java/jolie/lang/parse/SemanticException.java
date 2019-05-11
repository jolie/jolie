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
import jolie.lang.Constants;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;

/**
 *
 * @author claudio
 * Sep 2014, Fabrizio Montesi: use ParsingContext instead of custom fields
 */
public class SemanticException extends Exception
{
	private static final long serialVersionUID = Constants.serialVersionUID();
	
	private final List< SemanticError > errorList = new ArrayList<>();

	public static class SemanticError
	{
		private final ParsingContext context;
		private final String mesg;

		public SemanticError( ParsingContext context, String mesg )
		{
			this.context = context;
			this.mesg = mesg;
		}

		public String getMessage()
		{
			return new StringBuilder()
					.append( context.sourceName() )
					.append( ':' )
					.append( context.line() )
					.append( ": error: " )
					.append( mesg )
					.toString();
		}

		public ParsingContext context()
		{
			return context;
		}
	}

	public SemanticException() {}

	public void addSemanticError( OLSyntaxNode node, String message )
	{
		if ( node != null ) {
			errorList.add( new SemanticError( node.context(), message ) );
		} else {
			errorList.add( new SemanticError( URIParsingContext.DEFAULT, message ) );
		}

	}

	public List< SemanticError > getErrorList()
	{
		return errorList;
	}

	public String getErrorMessages()
	{
		StringBuilder message = new StringBuilder();
		for( SemanticError error : errorList ) {
			message.append( error.getMessage() ).append( '\n' );
		}
		return message.toString();
	}
}
