/***************************************************************************
 *   Copyright (C) 2022 by Claudio Guidi <cguidi@italianasoftware.com>     *
 *   																	   *
 * 	 Original class modified by Mauro Sgarzi								   *                                                                   *
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

import jolie.monitoring.MonitoringEvent;
import jolie.runtime.Value;

public class ProtocolMessageEvent extends MonitoringEvent {

	public enum Protocol {
		SOAP( "soap" ), HTTP( "http" );

		private String protocol;

		Protocol( String protocol ) {
			this.protocol = protocol;
		}

		public String getProtocol() {
			return protocol;
		}
	}

	public static enum FieldNames {
		PROTOCOL( "protocol" ), MESSAGE( "message" ), HEADER( "header" ), BODY( "body" ), PROCESSID(
			"processId" ), RAWID( "rawId" );

		private String fieldName;

		FieldNames( String name ) {
			this.fieldName = name;
		}

		public String getName() {
			return this.fieldName;
		}
	}

	public static String protocol( Value value ) {
		return value.getFirstChild( FieldNames.PROTOCOL.getName() ).strValue();
	}

	public static Value message( Value value ) {
		return value.getFirstChild( FieldNames.MESSAGE.getName() );
	}

	public static String header( Value value ) {
		return value.getFirstChild( FieldNames.MESSAGE.getName() ).getFirstChild( FieldNames.HEADER.getName() )
			.strValue();
	}

	public static String body( Value value ) {
		return value.getFirstChild( FieldNames.MESSAGE.getName() ).getFirstChild( FieldNames.BODY.getName() )
			.strValue();
	}

	public static String rawId( Value value ) {
		return value.getFirstChild( FieldNames.RAWID.getName() ).strValue();
	}

	public ProtocolMessageEvent( String body, String header, String processId, String rawId,
		ProtocolMessageEvent.Protocol protocol ) {
		super( "ProtocolMessage-".concat( protocol.getProtocol() ), Value.create() );

		data().getFirstChild( FieldNames.PROCESSID.getName() ).setValue( processId );

		data().getFirstChild( FieldNames.PROTOCOL.getName() ).setValue( protocol.getProtocol() );

		data().getFirstChild( FieldNames.MESSAGE.getName() ).getFirstChild( FieldNames.HEADER.getName() )
			.setValue( header );
		data().getFirstChild( FieldNames.MESSAGE.getName() ).getFirstChild( FieldNames.BODY.getName() )
			.setValue( body );

		data().getFirstChild( FieldNames.RAWID.getName() ).setValue( rawId );
	}
}
