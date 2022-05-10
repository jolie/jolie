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

		private final String protocol;

		Protocol( String protocol ) {
			this.protocol = protocol;
		}

		public String getProtocol() {
			return protocol;
		}
	}

	public static enum Field {
		PROTOCOL( "protocol" ), MESSAGE( "message" ), HEADER( "header" ), BODY( "body" ), PROCESSID(
			"processId" ), INTERNALMESSAGEID( "internalMessageId" );

		private final String id;

		Field( String name ) {
			this.id = name;
		}

		public String id() {
			return this.id;
		}
	}

	public static String protocol( Value value ) {
		return value.getFirstChild( Field.PROTOCOL.id() ).strValue();
	}

	public static Value message( Value value ) {
		return value.getFirstChild( Field.MESSAGE.id() );
	}

	public static String header( Value value ) {
		return value.getFirstChild( Field.MESSAGE.id() ).getFirstChild( Field.HEADER.id() )
			.strValue();
	}

	public static String body( Value value ) {
		return value.getFirstChild( Field.MESSAGE.id() ).getFirstChild( Field.BODY.id() )
			.strValue();
	}

	public static String internalMessageId( Value value ) {
		return value.getFirstChild( Field.INTERNALMESSAGEID.id() ).strValue();
	}

	public ProtocolMessageEvent( String body, String header, String processId, String internalMessageId,
		ProtocolMessageEvent.Protocol protocol ) {
		super( "ProtocolMessage-".concat( protocol.getProtocol() ), Value.create() );

		data().getFirstChild( Field.PROCESSID.id() ).setValue( processId );

		data().getFirstChild( Field.PROTOCOL.id() ).setValue( protocol.getProtocol() );

		data().getFirstChild( Field.MESSAGE.id() ).getFirstChild( Field.HEADER.id() )
			.setValue( header );
		data().getFirstChild( Field.MESSAGE.id() ).getFirstChild( Field.BODY.id() )
			.setValue( body );

		data().getFirstChild( Field.INTERNALMESSAGEID.id() ).setValue( internalMessageId );
	}
}
