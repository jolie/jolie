/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi									   *
 *   Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk>                  *                   *
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

import java.util.List;

import jolie.lang.Constants;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.CodeCheckException;
import jolie.lang.CodeCheckMessage;

public class ParserException extends CodeCheckException {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final ParsingContext context;

	public ParserException( CodeCheckMessage mesg ) {
		super( List.of( mesg ) );
		this.context = mesg.context().isPresent() ? mesg.context().get() : null;
	}

	// Since no ParserException is made without context, it will not be a problem to return context,
	// cause it will never be null,
	// but this is probably a bad idea for the future, so should de fixed.
	// Also, the MetaJolie uses this to set the context, but the message that MetaJolie gets first
	// already contains this information.
	// Should maybe not be duplicated?
	public ParsingContext context() {
		return context;
	}
}
