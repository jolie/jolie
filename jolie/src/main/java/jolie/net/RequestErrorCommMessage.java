/*
 * Copyright (C) 2022 Balint Maschio <bmaschio@italianasoftware.com>
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

package jolie.net;

import jolie.runtime.FaultException;
import jolie.runtime.Value;

public class RequestErrorCommMessage extends CommMessage {

	private final String template;

	/**
	 * Constructor
	 *
	 * @param requestId the identifier for the request
	 * @param operationName the operation name for this message
	 * @param value the message data to equip the message with
	 * @param fault the fault to equip the message with
	 */
	public RequestErrorCommMessage( long requestId, String operationName, String resourcePath, Value value,
		FaultException fault, String template ) {
		super( requestId, operationName, resourcePath, value, fault );
		this.template = template;
	}

	public String getTemplate() {
		return this.template;
	}
}
