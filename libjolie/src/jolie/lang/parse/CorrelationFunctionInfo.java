/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.lang.parse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.util.ArrayListMultiMap;
import jolie.util.MultiMap;

/**
 *
 * @author Fabrizio Montesi
 */
public class CorrelationFunctionInfo
{
	public static class CorrelationPairInfo
	{
		private final VariablePathNode sessionPath;
		private final VariablePathNode messagePath;

		public CorrelationPairInfo( VariablePathNode sessionPath, VariablePathNode messagePath )
		{
			this.sessionPath = sessionPath;
			this.messagePath = messagePath;
		}

		public VariablePathNode sessionPath()
		{
			return sessionPath;
		}

		public VariablePathNode messagePath()
		{
			return messagePath;
		}
	}

	private final List< CorrelationSetInfo > correlationSets = new ArrayList<>();
	private final Map< String, CorrelationSetInfo > operationCorrelationSetMap = new HashMap<>();
	private final MultiMap< String, CorrelationPairInfo > correlationPairs = new ArrayListMultiMap<>();
	private final MultiMap< CorrelationSetInfo, String > correlationSetOperations = new ArrayListMultiMap<>();

	public List< CorrelationSetInfo > correlationSets()
	{
		return correlationSets;
	}

	public Collection< CorrelationPairInfo > getOperationCorrelationPairs( String operationName )
	{
		return correlationPairs.get( operationName );
	}

	public void putCorrelationPair( String operationName, CorrelationPairInfo pair )
	{
		correlationPairs.put( operationName, pair );
	}

	public Map< String, CorrelationSetInfo > operationCorrelationSetMap()
	{
		return operationCorrelationSetMap;
	}

	public MultiMap< CorrelationSetInfo, String > correlationSetOperations()
	{
		return correlationSetOperations;
	}
}
