/***************************************************************************
 *   Copyright (C) 2012 by Fabrizio Montesi <famontesi@gmail.com>          *
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
 * {@link MonitoringEvent} for a session start.
 * 
 * @author Fabrizio Montesi
 */
public class SessionStartedEvent extends MonitoringEvent {

	public static enum FieldNames {
		OPERATION_NAME( "operationName" );

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


	public SessionStartedEvent( String operationName, String processId, String service, String scope,
		ParsingContext context ) {
		super( EventTypes.SESSION_STARTED, service, scope, processId, context, Value.create() );

		data().getFirstChild( FieldNames.OPERATION_NAME.getName() ).setValue( operationName );

	}
}
