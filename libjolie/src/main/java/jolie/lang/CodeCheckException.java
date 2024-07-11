/*
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk>
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

package jolie.lang;

import java.util.Iterator;
import java.util.List;

public class CodeCheckException extends Exception {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final List< CodeCheckMessage > messageList;

	/**
	 * Contructs a CodeCheckException from a list of CodeCheckMessages
	 *
	 * @param messageList
	 */
	public CodeCheckException( List< CodeCheckMessage > messageList ) {
		this.messageList = messageList;
	}

	/**
	 * Returns the list of CodeCheckMessages from the CodeCheckException
	 *
	 * @return
	 */
	public List< CodeCheckMessage > messages() {
		return messageList;
	}

	/**
	 * Returns a string containing all CodeCheckMessages from the CodeCheckException
	 */
	@Override
	public String getMessage() {
		Iterator< CodeCheckMessage > iterator = messageList.iterator();
		StringBuilder messageString = new StringBuilder();
		while( iterator.hasNext() ) {
			CodeCheckMessage currentMessage = iterator.next();
			messageString
				.append( currentMessage.toString() );
		}
		return messageString.toString();
	}


}
