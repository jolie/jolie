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

package jolie.monitoring;

import jolie.Jolie;
import jolie.lang.parse.context.ParsingContext;
import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.Value;

import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A monitoring event, supporting the {@link ValueConverter} interface for automatic transformation
 * between Jolie messages and Java objects.
 * 
 * @author Fabrizio Montesi
 */
public class MonitoringEvent implements ValueConverter {

	private static final AtomicLong SERIAL_EVENT_ID_COUNTER = new AtomicLong( 1L );

	public static enum EventTypes {
		MONITOR_ATTACHED( "MonitorAttached" ), OPERATION_CALL( "OperationCall" ), OPERATION_CALL_ASYNC(
			"OperationCallAsync" ), OPERATION_ENDED( "OperationEnded" ), OPERATION_REPLY(
				"OperationReply" ), OPERATION_STARTED( "OperationStarted" ), OPERATION_RECEIVED_ASYNC(
					"OperationReceivedAsync" ), PROTOCOL_MESSAGE( "ProtocolMessage" ), SESSION_ENDED(
						"SessionEnded" ), SESSION_STARTED( "SessionStarted" ), LOG( "Log" ), THROW(
							"throw" ), FAULT_HANDLER_START(
								"FaultHandlerStart" ), FAULT_HANDLER_END( "FaultHandlerEnd" );

		private String eventType;

		EventTypes( String type ) {
			this.eventType = type;
		}

		public String getType() {
			return eventType;
		}
	}

	public static enum FieldNames {
		TYPE( "type" ), TIMESTAMP( "timestamp" ), MEMORY( "memory" ), SERVICE( "service" ), DATA( "data" ), SCOPE(
			"scope" ), PROCESSID( "processId" ), CELLID(
				"cellId" ), CONTEXT( "context" ), CONTEXT_FILENAME(
					"filename" ), CONTEXT_LINE( "line" ), SERIAL_EVENT_ID( "serialEventId" );

		private String fieldName;

		FieldNames( String name ) {
			this.fieldName = name;
		}

		public String getName() {
			return this.fieldName;
		}
	}

	private final String type;
	private final long timestamp;
	private final long memory;
	private final Value data;
	private final String service;
	private final ParsingContext parsingContext;
	private final String scope;
	private final String processId;
	private final long serialEventId;

	public MonitoringEvent( MonitoringEvent.EventTypes type, String serviceFileName, String scope,
		String processId, ParsingContext parsingContext, Value data ) {
		this(
			type.getType(),
			System.currentTimeMillis(),
			Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
			serviceFileName,
			scope,
			processId,
			parsingContext,
			data );
	}

	private MonitoringEvent( String type, long timestamp, long memory, String serviceFileName, String scope,
		String processId, ParsingContext parsingContext,
		Value data ) {
		this.type = type;
		this.timestamp = timestamp;
		this.memory = memory;
		this.data = data;
		this.service = serviceFileName;
		this.parsingContext = parsingContext;
		this.scope = scope;
		this.processId = processId;
		this.serialEventId = SERIAL_EVENT_ID_COUNTER.getAndIncrement();
	}

	public String type() {
		return type;
	}

	public static String type( Value value ) {
		return value.getFirstChild( FieldNames.TYPE.getName() ).strValue();
	}

	public long timestamp() {
		return timestamp;
	}

	public static long timestamp( Value value ) {
		return value.getFirstChild( FieldNames.TIMESTAMP.getName() ).longValue();
	}

	public long memory() {
		return memory;
	}

	public static long memory( Value value ) {
		return value.getFirstChild( FieldNames.MEMORY.getName() ).longValue();
	}

	public String service() {
		return service;
	}

	public static String service( Value value ) {
		return value.getFirstChild( FieldNames.SERVICE.getName() ).strValue();
	}

	public Value data() {
		return data;
	}

	public static Value data( Value value ) {
		return value.getFirstChild( FieldNames.DATA.getName() );
	}

	public String scope() {
		return scope;
	}

	public static String scope( Value value ) {
		return value.getFirstChild( FieldNames.SCOPE.getName() ).strValue();
	}

	public String processId() {
		return processId;
	}

	public long serialEventId() {
		return this.serialEventId;
	}

	public static String processId( Value value ) {
		return value.getFirstChild( FieldNames.PROCESSID.getName() ).strValue();
	}

	public int cellId() {
		return Jolie.cellId;
	}

	public ParsingContext parsingContext() {
		return parsingContext;
	}


	public static ParsingContext parsingContext( Value value ) {
		ParsingContext parsingContext = null;
		if( value.hasChildren( FieldNames.CONTEXT.getName() ) ) {

			parsingContext = new ParsingContext() {
				@Override
				public URI source() {
					return null;
				}

				@Override
				public String sourceName() {
					return value.getFirstChild( FieldNames.CONTEXT.getName() )
						.getFirstChild( FieldNames.CONTEXT_FILENAME.getName() ).strValue();
				}

				@Override
				public int line() {
					return value.getFirstChild( FieldNames.CONTEXT.getName() )
						.getFirstChild( FieldNames.CONTEXT_LINE.getName() ).intValue();
				}
			};
		}
		return parsingContext;
	}


	public static MonitoringEvent fromValue( Value value ) {
		return new MonitoringEvent(
			type( value ),
			timestamp( value ),
			memory( value ), service( value ),
			scope( value ),
			processId( value ),
			parsingContext( value ),
			data( value ) );
	}


	public static Value toValue( MonitoringEvent e ) {
		Value ret = Value.create();
		ret.getFirstChild( FieldNames.TYPE.getName() ).setValue( e.type() );
		ret.getFirstChild( FieldNames.TIMESTAMP.getName() ).setValue( e.timestamp() );
		ret.getFirstChild( FieldNames.MEMORY.getName() ).setValue( e.memory() );
		ret.getFirstChild( FieldNames.PROCESSID.getName() ).setValue( e.processId() );
		ret.getChildren( FieldNames.DATA.getName() ).add( e.data() );
		ret.getFirstChild( FieldNames.SERVICE.getName() ).setValue( e.service() );
		ret.getFirstChild( FieldNames.CELLID.getName() ).setValue( Jolie.cellId );
		ret.getFirstChild( FieldNames.SCOPE.getName() ).setValue( e.scope() );
		ret.getFirstChild( FieldNames.SERIAL_EVENT_ID.getName() ).setValue( e.serialEventId() );
		if( e.parsingContext() != null ) {
			ret.getFirstChild( FieldNames.CONTEXT.getName() ).getFirstChild( FieldNames.CONTEXT_FILENAME.getName() )
				.setValue( e.parsingContext().sourceName() );
			ret.getFirstChild( FieldNames.CONTEXT.getName() ).getFirstChild( FieldNames.CONTEXT_LINE.getName() )
				.setValue( e.parsingContext().line() );
		}
		return ret;
	}
}
