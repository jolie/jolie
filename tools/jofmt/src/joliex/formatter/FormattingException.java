/*
 * Copyright (C) 2018 Fabrizio Montesi <famontesi@gmail.com>
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

package joliex.formatter;

import java.util.ArrayList;
import java.util.List;
import jolie.lang.Constants;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;

public class FormattingException extends Exception
{
	private static final long serialVersionUID = Constants.serialVersionUID();
	
	private final List< FormattingError > errorList = new ArrayList<>();

	public static class FormattingError
	{
		private final ParsingContext context;
		private final String mesg;

		public FormattingError( ParsingContext context, String mesg )
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

	public FormattingException() {}

	public void addError( OLSyntaxNode node, String message )
	{
		if ( node != null ) {
			errorList.add( new FormattingError( node.context(), message ) );
		} else {
			errorList.add( new FormattingError( URIParsingContext.DEFAULT, message ) );
		}

	}

	public List< FormattingError > getErrorList()
	{
		return errorList;
	}

	public String getErrorMessages()
	{
		StringBuilder message = new StringBuilder();
		for( FormattingError error : errorList ) {
			message.append( error.getMessage() ).append( '\n' );
		}
		return message.toString();
	}
}
