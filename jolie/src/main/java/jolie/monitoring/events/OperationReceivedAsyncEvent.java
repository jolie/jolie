/***************************************************************************
 *   Copyright (C) 2012 by Claudio Guidi <cguidi@italianasoftware.com>          *
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

package jolie.monitoring.events;

import jolie.lang.parse.context.ParsingContext;
import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

/**
 *
 * @author claudio guidi 27/01/2012
 */
public class OperationReceivedAsyncEvent extends MonitoringEvent {

	public static enum FieldNames {
		OPERATION_NAME( "operationName" ), MESSAGE_ID( "messageId" ), VALUE( "value" );

		private String fieldName;

		FieldNames( String name ) {
			this.fieldName = name;
		}

		public String getName() {
			return this.fieldName;
		}
	}

	public static String operationName( Value value ) {
		return value.getFirstChild( FieldNames.OPERATION_NAME.getName() ).strValue();
	}

	public static String messageId( Value value ) {
		return value.getFirstChild( FieldNames.MESSAGE_ID.getName() ).strValue();
	}

	public static Value value( Value value ) {
		return value.getFirstChild( FieldNames.VALUE.getName() );
	}

	public OperationReceivedAsyncEvent( String operationName, String processId, String messageId, String service,
		String scope, ParsingContext context,
		Value value ) {

		super( EventTypes.OPERATION_RECEIVED_ASYNC, service, scope, processId, context, Value.create() );

		data().getFirstChild( FieldNames.OPERATION_NAME.getName() ).setValue( operationName );
		data().getFirstChild( FieldNames.MESSAGE_ID.getName() ).setValue( messageId );
		data().getFirstChild( FieldNames.VALUE.getName() ).deepCopy( value );

	}

}
