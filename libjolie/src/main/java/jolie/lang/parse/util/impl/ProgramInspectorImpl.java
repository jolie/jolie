/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.lang.parse.util.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.util.ProgramInspector;

/**
 * Implementation of {@link jolie.lang.parse.util.ProgramInspector}.
 *
 * @author Fabrizio Montesi
 */
public class ProgramInspectorImpl implements ProgramInspector {
	private final URI[] sources;
	private final Map< URI, List< TypeDefinition > > types;
	private final Map< URI, List< InterfaceDefinition > > interfaces;
	private final Map< URI, List< InputPortInfo > > inputPorts;
	private final Map< URI, List< OutputPortInfo > > outputPorts;
	private final Map< URI, List< EmbeddedServiceNode > > embeddedServices;
	private final Map< URI, Map< OLSyntaxNode, List< OLSyntaxNode > > > behaviouralDependencies;
	private final Map< URI, List< ServiceNode > > serviceNodes;

	public ProgramInspectorImpl(
		URI[] sources,
		Map< URI, List< TypeDefinition > > types,
		Map< URI, List< InterfaceDefinition > > interfaces,
		Map< URI, List< InputPortInfo > > inputPorts,
		Map< URI, List< OutputPortInfo > > outputPorts,
		Map< URI, List< EmbeddedServiceNode > > embeddedServices,
		Map< URI, Map< OLSyntaxNode, List< OLSyntaxNode > > > behaviouralDependencies,
		Map< URI, List< ServiceNode > > serviceNodes ) {
		this.sources = sources;
		this.interfaces = interfaces;
		this.inputPorts = inputPorts;
		this.types = types;
		this.outputPorts = outputPorts;
		this.embeddedServices = embeddedServices;
		this.behaviouralDependencies = behaviouralDependencies;
		this.serviceNodes = serviceNodes;
	}

	@Override
	public URI[] getSources() {
		return sources;
	}

	@Override
	public TypeDefinition[] getTypes() {
		List< TypeDefinition > result = new ArrayList<>();
		List< TypeDefinition > list;
		for( URI source : sources ) {
			list = types.get( source );
			if( list != null ) {
				result.addAll( list );
			}
		}
		return result.toArray( new TypeDefinition[ 0 ] );
	}

	@Override
	public TypeDefinition[] getTypes( URI source ) {
		List< TypeDefinition > list = types.get( source );
		if( list == null ) {
			return new TypeDefinition[ 0 ];
		}
		return list.toArray( new TypeDefinition[ 0 ] );
	}

	@Override
	public InterfaceDefinition[] getInterfaces() {
		List< InterfaceDefinition > result = new ArrayList<>();
		List< InterfaceDefinition > list;
		for( URI source : sources ) {
			list = interfaces.get( source );
			if( list != null ) {
				result.addAll( list );
			}
		}
		return result.toArray( new InterfaceDefinition[ 0 ] );
	}

	@Override
	public InterfaceDefinition[] getInterfaces( URI source ) {
		List< InterfaceDefinition > list = interfaces.get( source );
		if( list == null ) {
			return new InterfaceDefinition[ 0 ];
		}
		return list.toArray( new InterfaceDefinition[ 0 ] );
	}

	@Override
	public InputPortInfo[] getInputPorts() {
		List< InputPortInfo > result = new ArrayList<>();
		List< InputPortInfo > list;
		for( URI source : sources ) {
			list = inputPorts.get( source );
			if( list != null ) {
				result.addAll( list );
			}
		}
		return result.toArray( new InputPortInfo[ 0 ] );
	}

	@Override
	public InputPortInfo[] getInputPorts( URI source ) {
		List< InputPortInfo > list = inputPorts.get( source );
		if( list == null ) {
			return new InputPortInfo[ 0 ];
		}
		return list.toArray( new InputPortInfo[ 0 ] );
	}

	@Override
	public OutputPortInfo[] getOutputPorts() {
		List< OutputPortInfo > result = new ArrayList<>();
		List< OutputPortInfo > list;
		for( URI source : sources ) {
			list = outputPorts.get( source );
			if( list != null ) {
				result.addAll( list );
			}
		}
		return result.toArray( new OutputPortInfo[ 0 ] );
	}

	@Override
	public OutputPortInfo[] getOutputPorts( URI source ) {
		List< OutputPortInfo > list = outputPorts.get( source );
		if( list == null ) {
			return new OutputPortInfo[ 0 ];
		}
		return list.toArray( new OutputPortInfo[ 0 ] );
	}

	@Override
	public EmbeddedServiceNode[] getEmbeddedServices() {
		List< EmbeddedServiceNode > result = new ArrayList<>();
		List< EmbeddedServiceNode > list;
		for( URI source : sources ) {
			list = embeddedServices.get( source );
			if( list != null ) {
				result.addAll( list );
			}
		}
		return result.toArray( new EmbeddedServiceNode[ 0 ] );
	}

	@Override
	public Map< OLSyntaxNode, List< OLSyntaxNode > > getBehaviouralDependencies() {
		Map< OLSyntaxNode, List< OLSyntaxNode > > result = new HashMap<>();
		Map< OLSyntaxNode, List< OLSyntaxNode > > list;
		for( URI source : sources ) {
			list = behaviouralDependencies.get( source );
			if( list != null ) {
				result.putAll( list );
			}
		}
		return result;
	}

	@Override
	public Map< OLSyntaxNode, List< OLSyntaxNode > > getBehaviouralDependencies( URI source ) {
		Map< OLSyntaxNode, List< OLSyntaxNode > > list = behaviouralDependencies.get( source );
		if( list == null ) {
			return new HashMap<>();
		}
		return list;
	}

	@Override
	public EmbeddedServiceNode[] getEmbeddedServices( URI source ) {
		List< EmbeddedServiceNode > list = embeddedServices.get( source );
		if( list == null ) {
			return new EmbeddedServiceNode[ 0 ];
		}
		return list.toArray( new EmbeddedServiceNode[ 0 ] );
	}

	@Override
	public ServiceNode[] getServiceNodes() {
		List< ServiceNode > result = new ArrayList<>();
		List< ServiceNode > list;
		for( URI source : sources ) {
			list = serviceNodes.get( source );
			if( list != null ) {
				result.addAll( list );
			}
		}
		return result.toArray( new ServiceNode[ 0 ] );
	}

	@Override
	public ServiceNode[] getServiceNodes( URI source ) {
		List< ServiceNode > list = serviceNodes.get( source );
		if( list == null ) {
			return new ServiceNode[ 0 ];
		}
		return list.toArray( new ServiceNode[ 0 ] );
	}
}
