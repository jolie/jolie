package jolie.tracer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.function.Supplier;

import jolie.Interpreter;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;

public class FileTracer implements Tracer {

	private static final int MAX_LINE_COUNT = 2000;
	private int actionCounter = 0;
	private Writer fileWriter;
	private final Interpreter interpreter;
	private final TracerUtils.TracerLevels tracerLevels;
	private int lineCount = 0;

	public FileTracer( Interpreter interpreter, TracerUtils.TracerLevels tLevel ) {
		this.tracerLevels = tLevel;
		this.interpreter = interpreter;
		createNewLogFile();
	}

	private synchronized void fileWriterFlush( StringBuilder stBuilder ) {
		try {
			fileWriter.write( stBuilder.toString() );
			fileWriter.flush();
			lineCount++;
			if( lineCount >= MAX_LINE_COUNT ) {
				fileWriter.close();
				createNewLogFile();
				lineCount = 0;
			}
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}

	private void createNewLogFile() {
		String format = "ddMMyyyyHHmmssSSS";
		SimpleDateFormat sdf = new SimpleDateFormat( format );
		final Date now = new Date();
		String filename = sdf.format( now );
		File logFile = new File( filename + ".jolie.log.json" );
		try {
			fileWriter = new FileWriter( logFile, true );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}

	@Override
	public void trace( Supplier< ? extends TraceAction > supplier ) {
		final TraceAction action = supplier.get();
		actionCounter++;
		if( action instanceof MessageTraceAction ) {
			trace( (MessageTraceAction) action );
		} else if( action instanceof EmbeddingTraceAction ) {
			trace( (EmbeddingTraceAction) action );
		} else if( action instanceof AssignmentTraceAction ) {
			trace( (AssignmentTraceAction) action );
		} else if( action instanceof ProtocolTraceAction ) {
			trace( (ProtocolTraceAction) action );
		}
	}

	private String getCurrentTimeStamp() {
		String format = "dd/MM/yyyy HH:mm:ss.SSS";
		SimpleDateFormat sdf = new SimpleDateFormat( format );
		final Date now = new Date();
		return sdf.format( now );
	}

	private void trace( EmbeddingTraceAction action ) {
		if( tracerLevels.equals( TracerUtils.TracerLevels.ALL ) ) {
			StringBuilder stBuilder = new StringBuilder();
			stBuilder.append( "{" )
				.append( "\"" ).append( actionCounter ).append( "\":[" )
				.append( "\"" ).append( getCurrentTimeStamp() ).append( "\"," );
			if( action.context() == null ) {
				stBuilder.append( "\"" ).append( interpreter.programDirectory() )
					.append( interpreter.programFilename() )
					.append( "\",\"" ).append( interpreter.programFilename() ).append( "\",\"\"," );
			} else {
				stBuilder.append( "\"" ).append( action.context().source() ).append( "\",\"" )
					.append( action.context().sourceName() ).append( "\",\"" )
					.append( action.context().startLine() + 1 )
					.append( "\"," );
			}


			switch( action.type() ) {
			case SERVICE_LOAD:
				stBuilder.append( "\"" ).append( "emb" ).append( "\"," );
				break;
			default:
				break;
			}
			stBuilder.append( "\"" ).append( action.name() ).append( "\"," )
				.append( "\"" ).append( action.description() ).append( "\"]}\n" );
			fileWriterFlush( stBuilder );
		}


	}

	private void trace( MessageTraceAction action ) {
		if( tracerLevels.equals( TracerUtils.TracerLevels.ALL )
			|| (tracerLevels.equals( TracerUtils.TracerLevels.COMM )) ) {
			StringBuilder stBuilder = new StringBuilder()
				.append( "{" )
				.append( "\"" ).append( actionCounter ).append( "\":[" )
				.append( "\"" ).append( getCurrentTimeStamp() ).append( "\"," );
			if( action.context() == null ) {
				stBuilder.append( "\"" ).append( interpreter.programDirectory() )
					.append( interpreter.programFilename() )
					.append( "\",\"" ).append( interpreter.programFilename() ).append( "\",\"\"," );
			} else {
				stBuilder.append( "\"" ).append( action.context().source() ).append( "\",\"" )
					.append( action.context().sourceName() ).append( "\",\"" )
					.append( action.context().startLine() + 1 )
					.append( "\"," );
			}
			switch( action.type() ) {
			case SOLICIT_RESPONSE:
				stBuilder.append( "\"" ).append( "sr" ).append( "\"," );
				break;
			case NOTIFICATION:
				stBuilder.append( "\"" ).append( "n" ).append( "\"," );
				break;
			case ONE_WAY:
				stBuilder.append( "\"" ).append( "ow" ).append( "\"," );
				break;
			case REQUEST_RESPONSE:
				stBuilder.append( "\"" ).append( "rr" ).append( "\"," );
				break;
			case COURIER_NOTIFICATION:
				stBuilder.append( "\"" ).append( "cn" ).append( "\"," );
				break;
			case COURIER_SOLICIT_RESPONSE:
				stBuilder.append( "\"" ).append( "csr" ).append( "\"," );
				break;
			default:
				break;
			}
			stBuilder.append( "\"" ).append( action.description() ).append( "\"," )
				.append( "\"" ).append( action.name() ).append( "\"" );
			if( action.message() != null ) {
				stBuilder.append( ",\"" ).append( action.message().requestId() ).append( "\"," );

				Writer writer = new StringWriter();
				Value messageValue = action.message().value().clone();
				if( action.message().isFault() ) {
					messageValue = action.message().fault().value().clone();
					messageValue.getFirstChild( "__faultname" ).setValue( action.message().fault().faultName() );
				}
				ValuePrettyPrinter printer = new ValuePrettyPrinter(
					messageValue,
					writer,
					"Value:" );
				printer.setByteTruncation( 50 );
				printer.setIndentationOffset( 0 );
				try {
					printer.run();
				} catch( IOException e ) {
				} // Should never happen

				String encodedString = Base64.getEncoder().encodeToString( writer.toString().trim().getBytes() );
				stBuilder.append( "\"" ).append( encodedString ).append( "\"" );
			}
			stBuilder.append( "]}\n" );
			fileWriterFlush( stBuilder );
		}
	}

	private void trace( AssignmentTraceAction action ) {
		if( tracerLevels.equals( TracerUtils.TracerLevels.ALL )
			|| (tracerLevels.equals( TracerUtils.TracerLevels.COMP )) ) {

			StringBuilder stBuilder = new StringBuilder();
			stBuilder.append( "{" )
				.append( "\"" ).append( actionCounter ).append( "\":[" )
				.append( "\"" ).append( getCurrentTimeStamp() ).append( "\"," );
			if( action.context() == null ) {
				stBuilder.append( "\"" ).append( interpreter.programDirectory() )
					.append( interpreter.programFilename() )
					.append( "\",\"" ).append( interpreter.programFilename() ).append( "\",\"\"," );
			} else {
				stBuilder.append( "\"" ).append( action.context().source() ).append( "\",\"" )
					.append( action.context().sourceName() ).append( "\",\"" )
					.append( action.context().startLine() + 1 )
					.append( "\"," );
			}
			switch( action.type() ) {
			case ASSIGNMENT:
				stBuilder.append( "\"" ).append( "comp" ).append( "\"," );
				break;
			case POINTER:
				stBuilder.append( "\"" ).append( "alias" ).append( "\"," );
				break;
			case DEEPCOPY:
				stBuilder.append( "\"" ).append( "dcopy" ).append( "\"," );
				break;
			default:
				break;
			}
			stBuilder.append( "\"" ).append( action.description() ).append( "\"," )
				.append( "\"" ).append( action.name() ).append( "\"" );
			if( action.value() != null ) {
				stBuilder.append( ",\"\"," );

				Writer writer = new StringWriter();
				Value value = action.value().clone();

				ValuePrettyPrinter printer = new ValuePrettyPrinter(
					value,
					writer,
					"Value:" );
				printer.setByteTruncation( 50 );
				printer.setIndentationOffset( 6 );
				try {
					printer.run();
				} catch( IOException e ) {
				} // Should never happen

				String encodedString = Base64.getEncoder().encodeToString( writer.toString().trim().getBytes() );
				stBuilder.append( "\"" ).append( encodedString ).append( "\"" );
			}
			stBuilder.append( "]}\n" );
			fileWriterFlush( stBuilder );
		}
	}

	private void trace( ProtocolTraceAction action ) {
		if( tracerLevels.equals( TracerUtils.TracerLevels.ALL )
			|| (tracerLevels.equals( TracerUtils.TracerLevels.COMM )) ) {
			StringBuilder stBuilder = new StringBuilder();
			stBuilder.append( "{" )
				.append( "\"" ).append( actionCounter ).append( "\":[" )
				.append( "\"" ).append( getCurrentTimeStamp() ).append( "\"," );
			if( action.context() == null ) {
				stBuilder.append( "\"" ).append( interpreter.programDirectory() )
					.append( interpreter.programFilename() )
					.append( "\",\"" ).append( interpreter.programFilename() ).append( "\",\"\"," );
			} else {
				stBuilder.append( "\"" ).append( action.context().source() ).append( "\",\"" )
					.append( action.context().sourceName() ).append( "\",\"" )
					.append( action.context().startLine() + 1 )
					.append( "\"," );
			}
			switch( action.type() ) {
			case HTTP:
				stBuilder.append( "\"" ).append( "http" ).append( "\"," );
				break;
			case SOAP:
				stBuilder.append( "\"" ).append( "soap" ).append( "\"," );
				break;
			default:
				break;
			}
			stBuilder.append( "\"" ).append( action.description() ).append( "\"," )
				.append( "\"" ).append( action.name() ).append( "\"" );
			if( action.message() != null ) {
				stBuilder.append( ",\"\"," );


				String encodedString = Base64.getEncoder().encodeToString( action.message().getBytes() );
				stBuilder.append( "\"" ).append( encodedString ).append( "\"" );
			}
			stBuilder.append( "]}\n" );
			fileWriterFlush( stBuilder );
		}
	}


}
