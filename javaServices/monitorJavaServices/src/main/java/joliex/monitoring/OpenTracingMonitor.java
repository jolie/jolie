package joliex.monitoring;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Span;
import jolie.js.JsUtils;
import jolie.monitoring.MonitoringEvent;
import jolie.monitoring.events.*;
import jolie.runtime.AndJarDeps;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

@AndJarDeps( { "jaeger-client.jar", "jaeger-core.jar", "opentracing-api.jar", "opentracing-util.jar",
	"opentracing-thrift.jar", "libthrift-0.11.0.jar",
	"slf4j-api-1.6.6.jar", "slf4j-simple.jar", "jolie-js.jar", "json_simple.jar" } )

public class OpenTracingMonitor extends AbstractMonitorJavaService {



	private static JaegerTracer tracer = null;
	private final HashMap< String, ArrayList< MonitoringEvent > > activeSessions = new HashMap<>();
	private static String serviceName = "service";
	private static boolean tracerWithLogSpans = false;
	private static long sessionEndedTimeoutBeforeElaboration = 10000;

	private void createTracer() {
		Configuration.SamplerConfiguration samplerConfig =
			Configuration.SamplerConfiguration.fromEnv().withType( "const" ).withParam( 1 );
		Configuration.ReporterConfiguration reporterConfig =
			Configuration.ReporterConfiguration.fromEnv().withLogSpans( tracerWithLogSpans );
		Configuration config =
			new Configuration( serviceName ).withSampler( samplerConfig ).withReporter( reporterConfig );
		tracer = config.getTracer();
	}

	private static void setMonitoringEventTags( MonitoringEvent e, Span s ) {
		s.setTag( MonitoringEvent.FieldNames.CELLID.getName(), e.cellId() );
		s.setTag( MonitoringEvent.FieldNames.SCOPE.getName(), e.scope() );
		s.setTag( MonitoringEvent.FieldNames.SERVICE.getName(), e.service() );
		s.setTag( MonitoringEvent.FieldNames.MEMORY.getName(), e.memory() );
		s.setTag( MonitoringEvent.FieldNames.SERIAL_EVENT_ID.getName(), e.serialEventId() );
		if( e.parsingContext() != null ) {
			s.setTag( MonitoringEvent.FieldNames.CONTEXT_FILENAME.getName(), e.parsingContext().sourceName() );
			s.setTag( MonitoringEvent.FieldNames.CONTEXT_LINE.getName(), e.parsingContext().line() );
		}
	}

	private static void setMonitorSessionStaredEventTag( MonitoringEvent e, Span s ) {
		s.setTag( SessionStartedEvent.FieldNames.OPERATION_NAME.getName(),
			SessionStartedEvent.operationName( e.data() ) );
	}

	private static void setRequestOrResponseTag( MonitoringEvent e, Span s, boolean isRequest ) {
		String tagPrefix = "request-";
		if( !isRequest ) {
			tagPrefix = "response-";
		}
		try {
			s.setTag( tagPrefix + OperationStartedEvent.FieldNames.VALUE.getName(),
				getJsonString( OperationStartedEvent.value( e.data() ) ) );
		} catch( IOException ex ) {
			s.setTag( tagPrefix, "errors occure during message extraction" );
		}
		Format format = new SimpleDateFormat( "yyyy MM dd HH:mm:ss.SSS" );
		s.setTag( tagPrefix + MonitoringEvent.FieldNames.TIMESTAMP.getName(),
			format.format( new Date( e.timestamp() ) ) );
		s.setTag( tagPrefix + OperationCallEvent.FieldNames.STATUS.getName(),
			mapMessageStatus( OperationCallEvent.status( e.data() ) ) );
	}

	private static String mapMessageStatus( int status ) {
		switch( status ) {
		case OperationReplyEvent.SUCCESS:
			return "SUCCESS";
		case OperationReplyEvent.FAULT:
			return "FAULT";
		case OperationReplyEvent.ERROR:
			return "ERROR";
		}
		return "NO-STATUS";
	}

	private static void setMonitorOperationStartedEndedEventTag( MonitoringEvent e, Span s ) {
		s.setTag( OperationStartedEvent.FieldNames.OPERATION_NAME.getName(),
			OperationStartedEvent.operationName( e.data() ) );
		s.setTag( OperationStartedEvent.FieldNames.MESSAGE_ID.getName(), OperationStartedEvent.messageId( e.data() ) );
	}

	private static void setMonitorThrowEventTag( MonitoringEvent e, Span s ) {
		s.setTag( ThrowEvent.FieldNames.FAULTNAME.getName(),
			ThrowEvent.faultname( e.data() ) );
	}

	private static void setMonitorFaultHandlerEventTag( MonitoringEvent e, Span s ) {
		s.setTag( ThrowEvent.FieldNames.FAULTNAME.getName(),
			ThrowEvent.faultname( e.data() ) );
	}



	private static void setMonitorProtocolEvent( MonitoringEvent e, Span s ) {
		s.setTag( ProtocolMessageEvent.FieldNames.HEADER.getName(), ProtocolMessageEvent.header( e.data() ) );
		s.setTag( ProtocolMessageEvent.FieldNames.PROTOCOL.getName(), ProtocolMessageEvent.protocol( e.data() ) );
		s.setTag( ProtocolMessageEvent.FieldNames.MESSAGE.getName(), ProtocolMessageEvent.message( e.data() ) );
	}

	private static void setMonitorOperationCallReplyEventTag( MonitoringEvent e, Span s ) {
		s.setTag( OperationCallEvent.FieldNames.OPERATION_NAME.getName(),
			OperationCallEvent.operationName( e.data() ) );
		s.setTag( OperationCallEvent.FieldNames.MESSAGE_ID.getName(), OperationCallEvent.messageId( e.data() ) );
		s.setTag( OperationCallEvent.FieldNames.OUTPUT_PORT.getName(), OperationCallEvent.outputPort( e.data() ) );
		s.setTag( OperationCallEvent.FieldNames.DETAILS.getName(), OperationCallEvent.details( e.data() ) );
	}

	private static String getSessionId( String service, String processId ) {
		return service + "-" + processId;
	}

	private static String getOperationSpanKey( String sessionId, String operationName ) {
		return sessionId + "-" + operationName;
	}

	private static String getOperationOutputPortSpanKey( String sessionId, String operationName, String outputPort ) {
		return sessionId + "-" + operationName + "@" + outputPort;
	}

	private static long getMicroSecondsTimestamp( long ts ) {
		return ts * 1000;
	}


	private static String getJsonString( Value request ) throws IOException {

		StringBuilder stringBuilder = new StringBuilder();
		JsUtils.valueToJsonString( request, true, Type.UNDEFINED, stringBuilder );
		return stringBuilder.toString();
	}


	private synchronized void insertOrAddNewEventIntoActiveSessionList( MonitoringEvent e, String sessionId ) {
		if( e.type().equals( MonitoringEvent.EventTypes.MONITOR_ATTACHED.getType() ) ) {
			tracer.buildSpan( MonitoringEvent.EventTypes.MONITOR_ATTACHED.getType() )
				.withStartTimestamp( getMicroSecondsTimestamp( e.timestamp() ) )
				.ignoreActiveSpan().start().finish( getMicroSecondsTimestamp( e.timestamp() ) );

		} else {
			// discard log@Runtime because log already appears as LogEvent
			if( activeSessions.containsKey( sessionId ) ) {
				activeSessions.get( sessionId ).add( e );
			} else {
				ArrayList< MonitoringEvent > arrayList = new ArrayList<>();
				arrayList.add( e );
				activeSessions.put( sessionId, arrayList );
			}
		}

	}


	private void createSpans( String sessionId ) {
		Deque< Span > spanStack = new ArrayDeque<>();
		HashMap< String, Span > synchronousCallOpenedSpans = new HashMap<>();

		ArrayList< MonitoringEvent > arrayList = activeSessions.get( sessionId );
		arrayList.sort( new Comparator< MonitoringEvent >() {
			@Override
			public int compare( MonitoringEvent o1, MonitoringEvent o2 ) {
				// first rule recognizing session started and session ended
				if( o1.type().equals( MonitoringEvent.EventTypes.SESSION_STARTED.getType() )
					|| o2.type().equals( MonitoringEvent.EventTypes.SESSION_ENDED.getType() ) ) {
					return -1;
				}
				if( o2.type().equals( MonitoringEvent.EventTypes.SESSION_STARTED.getType() )
					|| o1.type().equals( MonitoringEvent.EventTypes.SESSION_ENDED.getType() ) ) {
					return 1;
				}
				// second rule, checking the timestamp
				if( o1.timestamp() > o2.timestamp() ) {
					return 1;
				}
				if( o1.timestamp() < o2.timestamp() ) {
					return -1;
				}
				if( o1.timestamp() == o2.timestamp() ) {
					if( o1.serialEventId() > o2.serialEventId() ) {
						return 1;
					} else {
						return -1;
					}
				}
				return 0;
			}
		} );


		arrayList.stream().forEach( ( MonitoringEvent e ) -> {
			if( e.type().equals( MonitoringEvent.EventTypes.SESSION_STARTED.getType() ) ) {
				Span span = tracer.buildSpan( sessionId )
					.withStartTimestamp( getMicroSecondsTimestamp( e.timestamp() ) ).ignoreActiveSpan().start();
				setMonitoringEventTags( e, span );
				setMonitorSessionStaredEventTag( e, span );
				spanStack.push( span );
			}

			if( e.type().equals( (MonitoringEvent.EventTypes.SESSION_ENDED.getType()) ) ) {
				spanStack.pop().finish( getMicroSecondsTimestamp( e.timestamp() ) );
			}

			if( e.type().equals( MonitoringEvent.EventTypes.OPERATION_CALL_ASYNC.getType() ) ) {
				Span parentSpan = spanStack.getFirst();
				Span span = tracer
					.buildSpan( getOperationOutputPortSpanKey( sessionId, OperationCallEvent.operationName( e.data() ),
						OperationCallEvent.outputPort( e.data() ) ) )
					.asChildOf( parentSpan ).withStartTimestamp( getMicroSecondsTimestamp( e.timestamp() ) )
					.ignoreActiveSpan().start();
				setMonitoringEventTags( e, span );
				setMonitorOperationCallReplyEventTag( e, span );
				setRequestOrResponseTag( e, span, true );
				span.finish( getMicroSecondsTimestamp( e.timestamp() ) );
			}

			if( e.type().equals( MonitoringEvent.EventTypes.OPERATION_RECEIVED_ASYNC.getType() ) ) {
				Span parentSpan = spanStack.getFirst();
				Span span = tracer
					.buildSpan( getOperationSpanKey( sessionId, OperationStartedEvent.operationName( e.data() ) ) )
					.asChildOf( parentSpan ).withStartTimestamp( getMicroSecondsTimestamp( e.timestamp() ) )
					.ignoreActiveSpan().start();
				setMonitoringEventTags( e, span );
				setMonitorOperationStartedEndedEventTag( e, span );
				setRequestOrResponseTag( e, span, true );
				span.finish( getMicroSecondsTimestamp( e.timestamp() ) );
			}

			if( e.type().equals( MonitoringEvent.EventTypes.OPERATION_STARTED.getType() ) ) {
				Span parentSpan = spanStack.getFirst();
				Span span = tracer
					.buildSpan( getOperationSpanKey( sessionId, OperationStartedEvent.operationName( e.data() ) ) )
					.asChildOf( parentSpan ).withStartTimestamp( getMicroSecondsTimestamp( e.timestamp() ) )
					.ignoreActiveSpan().start();
				setMonitoringEventTags( e, span );
				setMonitorOperationStartedEndedEventTag( e, span );
				setRequestOrResponseTag( e, span, true );
				spanStack.push( span );
			}

			if( e.type().equals( MonitoringEvent.EventTypes.OPERATION_ENDED.getType() ) ) {
				Span span = spanStack.pop();
				setRequestOrResponseTag( e, span, false );
				span.finish( getMicroSecondsTimestamp( e.timestamp() ) );
			}

			if( e.type().equals( MonitoringEvent.EventTypes.OPERATION_CALL.getType() ) ) {
				Span parentSpan = spanStack.getFirst();
				String spanKey = getOperationOutputPortSpanKey( sessionId, OperationCallEvent.operationName( e.data() ),
					OperationCallEvent.outputPort( e.data() ) );
				Span span = tracer
					.buildSpan( spanKey )
					.asChildOf( parentSpan ).withStartTimestamp( getMicroSecondsTimestamp( e.timestamp() ) )
					.ignoreActiveSpan().start();
				setMonitoringEventTags( e, span );
				setMonitorOperationCallReplyEventTag( e, span );
				setRequestOrResponseTag( e, span, true );
				synchronousCallOpenedSpans.put( spanKey, span );
			}

			if( e.type().equals( MonitoringEvent.EventTypes.OPERATION_REPLY.getType() ) ) {
				String spanKey = getOperationOutputPortSpanKey( sessionId, OperationCallEvent.operationName( e.data() ),
					OperationCallEvent.outputPort( e.data() ) );
				Span span = synchronousCallOpenedSpans.get( spanKey );
				setRequestOrResponseTag( e, span, false );
				span.finish( getMicroSecondsTimestamp( e.timestamp() ) );
				synchronousCallOpenedSpans.remove( spanKey );
			}

			if( e.type().equals( MonitoringEvent.EventTypes.LOG.getType() ) ) {
				Span span = spanStack.getFirst();
				span.log( LogEvent.message( e.data() ) );
			}

			if( e.type().equals( MonitoringEvent.EventTypes.THROW.getType() ) ) {
				Span parentSpan = spanStack.getFirst();
				Span span = tracer
					.buildSpan( "throw-" + ThrowEvent.faultname( e.data() ) )
					.asChildOf( parentSpan ).withStartTimestamp( getMicroSecondsTimestamp( e.timestamp() ) )
					.ignoreActiveSpan().start();
				setMonitoringEventTags( e, span );
				setMonitorThrowEventTag( e, span );
				span.finish( getMicroSecondsTimestamp( e.timestamp() ) );
			}
			if( e.type().equals( MonitoringEvent.EventTypes.FAULT_HANDLER_START.getType() ) ) {
				Span parentSpan = spanStack.getFirst();
				Span span = tracer
					.buildSpan( "faultHandlerStart-" + ThrowEvent.faultname( e.data() ) )
					.asChildOf( parentSpan ).withStartTimestamp( getMicroSecondsTimestamp( e.timestamp() ) )
					.ignoreActiveSpan().start();
				setMonitoringEventTags( e, span );
				setMonitorFaultHandlerEventTag( e, span );
				spanStack.push( span );
			}
			if( e.type().equals( MonitoringEvent.EventTypes.FAULT_HANDLER_END.getType() ) ) {
				Span span = spanStack.pop();
				span.finish( getMicroSecondsTimestamp( e.timestamp() ) );
			}


			if( e.type().equals( MonitoringEvent.EventTypes.PROTOCOL_MESSAGE.getType() ) ) {
				Span span = spanStack.getFirst();
				setMonitorProtocolEvent( e, span );
			}
		} );

	}


	@Override
	public void pushEvent( MonitoringEvent e ) {

		if( tracer != null ) {
			String sessionId = getSessionId( e.service(), e.processId() );
			insertOrAddNewEventIntoActiveSessionList( e, sessionId );

			if( e.type().equals( MonitoringEvent.EventTypes.SESSION_ENDED.getType() ) ) {
				try {
					// waiting for all events of the same session. Due to asynchronous reception, we need to wait
					Thread.sleep( sessionEndedTimeoutBeforeElaboration );
					createSpans( sessionId );
					activeSessions.remove( sessionId );

				} catch( InterruptedException e1 ) {
					e1.printStackTrace();
				}
			}
		}
	}

	public void setMonitor( Value request ) {
		if( request.hasChildren( "service_name" ) ) {
			serviceName = request.getFirstChild( "service_name" ).strValue();
		}
		if( request.hasChildren( "session_ended_timeout_before_elaboration" ) ) {
			sessionEndedTimeoutBeforeElaboration =
				request.getFirstChild( "session_ended_timeout_before_elaboration" ).longValue();
		}
		if( request.hasChildren( "tracer_with_log_spans" ) ) {
			tracerWithLogSpans = request.getFirstChild( "tracer_with_log_spans" ).boolValue();
		}
		createTracer();
	}

}


