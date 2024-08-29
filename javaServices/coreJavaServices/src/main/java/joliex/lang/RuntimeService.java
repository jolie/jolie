/***************************************************************************
 *   Copyright (C) 2008-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package joliex.lang;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Optional;
import java.util.function.Function;

import com.sun.management.UnixOperatingSystemMXBean;

import jolie.ExecutionThread;
import jolie.lang.Constants;
import jolie.net.CommListener;
import jolie.net.LocalCommChannel;
import jolie.net.ports.OutputPort;
import jolie.runtime.FaultException;
import jolie.runtime.InvalidIdException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoaderCreationException;
import jolie.runtime.embedding.EmbeddedServiceLoadingException;
import jolie.runtime.embedding.RequestResponse;

public class RuntimeService extends JavaService {
	public Value getLocalLocation() {
		Value v = Value.create();
		v.setValue( interpreter().commCore().getLocalCommChannel() );
		return v;
	}

	@RequestResponse
	public void setMonitor( final Value request ) {
		final VariablePath locationPath = new VariablePathBuilder( true )
			.add( Constants.MONITOR_OUTPUTPORT_NAME, 0 )
			.add( Constants.LOCATION_NODE_NAME, 0 ).toVariablePath();
		locationPath.setValue( request.getFirstChild( Constants.LOCATION_NODE_NAME ) );

		final VariablePath protocolPath = new VariablePathBuilder( true )
			.add( Constants.MONITOR_OUTPUTPORT_NAME, 0 )
			.add( Constants.PROTOCOL_NODE_NAME, 0 ).toVariablePath();
		protocolPath.setValue( request.getFirstChild( Constants.PROTOCOL_NODE_NAME ) );

		OutputPort port = new OutputPort(
			interpreter(),
			Constants.MONITOR_OUTPUTPORT_NAME,
			locationPath,
			protocolPath,
			null,
			true );
		port.optimizeLocation();

		interpreter().setMonitor( port );
	}

	@RequestResponse
	public void setOutputPort( Value request ) {
		String name = request.getFirstChild( "name" ).strValue();
		Value locationValue = request.getFirstChild( "location" );
		Value protocolValue = request.getFirstChild( "protocol" );
		OutputPort port =
			new OutputPort(
				interpreter(),
				name );
		Value l;
		Value r = interpreter().initThread().state().root();
		l = r.getFirstChild( name ).getFirstChild( Constants.LOCATION_NODE_NAME );
		if( locationValue.isChannel() ) {
			l.setValue( locationValue.channelValue() );
		} else {
			l.setValue( locationValue.strValue() );
		}
		r.getFirstChild( name ).getFirstChild( Constants.PROTOCOL_NODE_NAME ).refCopy( protocolValue );

		r = ExecutionThread.currentThread().state().root();
		l = r.getFirstChild( name ).getFirstChild( Constants.LOCATION_NODE_NAME );
		if( locationValue.isChannel() ) {
			l.setValue( locationValue.channelValue() );
		} else {
			l.setValue( locationValue.strValue() );
		}
		r.getFirstChild( name ).getFirstChild( Constants.PROTOCOL_NODE_NAME ).deepCopy( protocolValue );

		interpreter().register( name, port );
	}

	@RequestResponse
	public Value getOutputPort( Value v ) throws FaultException {
		OutputPort foundOp = null;
		Value ret = Value.create();
		for( OutputPort o : interpreter().outputPorts() ) {
			if( o.id().equals( v.getFirstChild( "name" ).strValue() ) ) {
				foundOp = o;
			}
		}
		if( foundOp == null ) {
			throw new FaultException( "OutputPortDoesNotExist" );
		} else {
			ret.getFirstChild( "name" ).setValue( foundOp.id() );
			try {
				ret.getFirstChild( "protocol" ).setValue( foundOp.getProtocol().name() );
			} catch( Exception e ) {
				ret.getFirstChild( "protocol" ).setValue( "" );
			}
			ret.getFirstChild( "location" ).setValue( foundOp.locationVariablePath().getValue().strValue() );
		}
		return ret;
	}

	@RequestResponse
	public Value getOutputPorts() {
		Value ret = Value.create();
		int counter = 0;
		for( OutputPort o : interpreter().outputPorts() ) {
			ret.getChildren( "port" ).get( counter ).getFirstChild( "name" ).setValue( o.id() );
			try {
				ret.getChildren( "port" ).get( counter ).getFirstChild( "protocol" ).setValue( o.getProtocol().name() );
			} catch( Exception e ) {
				ret.getChildren( "port" ).get( counter ).getFirstChild( "protocol" ).setValue( "" );
			}
			ret.getChildren( "port" ).get( counter ).getFirstChild( "location" )
				.setValue( o.locationVariablePath().getValue().strValue() );
			counter++;
		}
		return ret;
	}

	@RequestResponse
	public Value getProcessId() {
		Value response = Value.create();
		response.setValue( ExecutionThread.currentThread().getSessionId() );
		return response;
	}

	@RequestResponse
	public void removeOutputPort( String outputPortName ) {
		interpreter().removeOutputPort( outputPortName );
	}

	@RequestResponse
	public void setRedirection( Value request )
		throws FaultException {
		String serviceName = request.getChildren( "inputPortName" ).first().strValue();
		CommListener listener =
			interpreter().commCore().getListenerByInputPortName( serviceName );
		if( listener == null ) {
			throw new FaultException( "RuntimeException", "Unknown inputPort: " + serviceName );
		}

		String resourceName = request.getChildren( "resourceName" ).first().strValue();
		String opName = request.getChildren( "outputPortName" ).first().strValue();
		try {
			OutputPort port = interpreter().getOutputPort( opName );
			listener.inputPort().redirectionMap().put( resourceName, port );
		} catch( InvalidIdException e ) {
			throw new FaultException( "RuntimeException", e );
		}
	}

	@RequestResponse
	public void removeRedirection( Value request )
		throws FaultException {
		String serviceName = request.getChildren( "inputPortName" ).first().strValue();
		CommListener listener =
			interpreter().commCore().getListenerByInputPortName( serviceName );
		if( listener == null ) {
			throw new FaultException( "RuntimeException", "Unknown inputPort: " + serviceName );
		}

		String resourceName = request.getChildren( "resourceName" ).first().strValue();
		listener.inputPort().redirectionMap().remove( resourceName );
	}

	public Value getRedirection( Value request )
		throws FaultException {
		Value ret;
		String inputPortName = request.getChildren( "inputPortName" ).first().strValue();
		CommListener listener =
			interpreter().commCore().getListenerByInputPortName( inputPortName );
		if( listener == null ) {
			throw new FaultException( "RuntimeException", Value.create( "Invalid input port: " + inputPortName ) );
		}

		String resourceName = request.getChildren( "resourceName" ).first().strValue();
		OutputPort p = listener.inputPort().redirectionMap().get( resourceName );
		if( p == null ) {
			ret = Value.create();
		} else {
			ret = Value.create( p.id() );
		}
		return ret;
	}

	public String getVersion() {
		return Constants.VERSION;
	}

	public Value getIncludePaths() {
		Value ret = Value.create();
		String[] includePaths = interpreter().includePaths();
		for( String path : includePaths ) {
			ret.getNewChild( "path" ).setValue( path );
		}
		return ret;
	}

	@RequestResponse
	public Value loadEmbeddedService( Value request )
		throws FaultException {
		try {
			Value channel = Value.create();
			final EmbeddedServiceLoader.EmbeddedServiceConfiguration configuration;
			if( request.hasChildren( "filepath" ) ) {
				configuration = new EmbeddedServiceLoader.ExternalEmbeddedServiceConfiguration(
					jolie.lang.Constants.stringToEmbeddedServiceType( request.firstChildOrDefault( "type",
						Value::strValue, Constants.EmbeddedServiceType.JOLIE.toString() ) ),
					request.getFirstChild( "filepath" ).strValue(),
					Optional.ofNullable( request.firstChildOrDefault( "service", Value::strValue, null ) ),
					Optional.ofNullable( request.firstChildOrDefault( "params", Function.identity(), null ) ) );
			} else {
				configuration = new EmbeddedServiceLoader.ExternalEmbeddedNativeCodeConfiguration(
					request.getFirstChild( "code" ).strValue() );
			}
			EmbeddedServiceLoader loader = EmbeddedServiceLoader.create( interpreter(), configuration, channel );
			interpreter().addEmbeddedServiceLoader( loader );
			loader.load();
			return channel;
		} catch( EmbeddedServiceLoaderCreationException | EmbeddedServiceLoadingException e ) {
			throw new FaultException( "RuntimeException", e );
		}
	}

	public Value getenv( String name ) {
		final Value retVal = Value.create();
		final String env = System.getenv( name );
		if( env != null ) {
			retVal.setValue( env );
		}
		return retVal;
	}

	@RequestResponse
	public void loadLibrary( String libraryPath )
		throws FaultException {
		try {
			interpreter().getClassLoader().addJarResource( libraryPath );
		} catch( IOException | IllegalArgumentException e ) {
			throw new FaultException( "IOException", e );
		}
	}

	@RequestResponse
	public void callExit( Value request ) {
		Object o = request.valueObject();
		if( o instanceof LocalCommChannel ) {
			((LocalCommChannel) o).interpreter().exit();
		}
	}

	public String dumpState() {
		Writer writer = new StringWriter();
		ValuePrettyPrinter printer =
			new ValuePrettyPrinter( Value.createDeepCopy( interpreter().globalValue() ), writer, "Global state" );
		try {
			printer.run();
			printer = new ValuePrettyPrinter( Value.createDeepCopy( ExecutionThread.currentThread().state().root() ),
				writer, "Process state" );
			printer.run();
		} catch( IOException e ) {
		} // Should never happen
		return writer.toString();
	}

	@RequestResponse
	public void halt( Value request ) {
		Runtime.getRuntime().halt(
			request.firstChildOrDefault( "status", Value::intValue, 0 ) );
	}

	public Value stats() {
		final Value stats = Value.create();
		stats_files( stats.getFirstChild( "files" ) );
		stats_os( stats.getFirstChild( "os" ) );
		stats_memory( stats.getFirstChild( "memory" ) );
		return stats;
	}

	private void stats_os( Value stats ) {
		OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		stats.setFirstChild( "arch", osBean.getArch() );
		stats.setFirstChild( "availableProcessors", osBean.getAvailableProcessors() );
		stats.setFirstChild( "name", osBean.getName() );
		stats.setFirstChild( "systemLoadAverage", osBean.getSystemLoadAverage() );
		stats.setFirstChild( "version", osBean.getVersion() );
	}

	private void stats_files( Value stats ) {
		OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if( osBean instanceof UnixOperatingSystemMXBean ) {
			UnixOperatingSystemMXBean unixBean = (UnixOperatingSystemMXBean) osBean;
			stats.setFirstChild( "openCount", unixBean.getOpenFileDescriptorCount() );
			stats.setFirstChild( "maxCount", unixBean.getMaxFileDescriptorCount() );
		}
	}

	private void stats_memory( Value stats ) {
		Runtime runtime = Runtime.getRuntime();
		stats.setFirstChild( "free", runtime.freeMemory() );
		stats.setFirstChild( "total", runtime.totalMemory() );
		stats.setFirstChild( "used", runtime.totalMemory() - runtime.freeMemory() );
	}
}
