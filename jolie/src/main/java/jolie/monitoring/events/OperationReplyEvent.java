/***************************************************************************
 *   Copyright (C) 2014 by Claudio Guidi <cguidi@italianasoftware.com>          *
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
 * @author claudio
 */
public class OperationReplyEvent extends MonitoringEvent {
	public static final int SUCCESS = 0;
	public static final int FAULT = 1;
	public static final int ERROR = 2;

	public static enum FieldNames {
		OPERATION_NAME( "operationName" ), MESSAGE_ID( "messageId" ), STATUS( "status" ), DETAILS(
			"details" ), OUTPUT_PORT( "outputPort" ), VALUE( "value" );

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

	public static Integer status( Value value ) {
		return value.getFirstChild( FieldNames.STATUS.getName() ).intValue();
	}

	public static String details( Value value ) {
		return value.getFirstChild( FieldNames.DETAILS.getName() ).strValue();
	}

	public static String outputPort( Value value ) {
		return value.getFirstChild( FieldNames.OUTPUT_PORT.getName() ).strValue();
	}

	public static Value value( Value value ) {
		return value.getFirstChild( FieldNames.VALUE.getName() );
	}

	public OperationReplyEvent( String operationName, String processId, String messageId, int status, String details,
		String outputPort, String service, String scope, ParsingContext context, Value value ) {
		super( EventTypes.OPERATION_REPLY, service, scope, processId, context, Value.create() );

		data().getFirstChild( FieldNames.OPERATION_NAME.getName() ).setValue( operationName );
		data().getFirstChild( FieldNames.MESSAGE_ID.getName() ).setValue( messageId );
		data().getFirstChild( FieldNames.STATUS.getName() ).setValue( status );
		data().getFirstChild( FieldNames.DETAILS.getName() ).setValue( details );
		data().getFirstChild( FieldNames.OUTPUT_PORT.getName() ).setValue( outputPort );
		data().getFirstChild( FieldNames.VALUE.getName() ).deepCopy( value );
	}
}
