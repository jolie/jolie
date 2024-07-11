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

package jolie.runtime.correlation;

import java.util.List;
import java.util.Set;

import jolie.runtime.VariablePath;
import jolie.util.MultiMap;

/**
 *
 * @author Fabrizio Montesi
 */
public class CorrelationSet {
	public static class CorrelationPair {
		private final VariablePath sessionPath;
		private final VariablePath messagePath;

		public CorrelationPair( VariablePath sessionPath, VariablePath messagePath ) {
			this.sessionPath = sessionPath;
			this.messagePath = messagePath;
		}

		public VariablePath sessionPath() {
			return sessionPath;
		}

		public VariablePath messagePath() {
			return messagePath;
		}
	}

	// Maps operation names to their correlation pairs.
	private final MultiMap< String, CorrelationPair > correlationMap;
	private final List< VariablePath > correlationVariablePaths;

	public CorrelationSet( List< VariablePath > correlationVariablePaths,
		MultiMap< String, CorrelationPair > correlationMap ) {
		this.correlationMap = correlationMap;
		this.correlationVariablePaths = correlationVariablePaths;
	}

	/**
	 * Returns the list of {@link CorrelationPair} defined for the operation.
	 *
	 * @param operationName the operation name the list is defined for.
	 * @return the list of {@link CorrelationPair} defined for the operation, or {@code null} if no such
	 *         list is defined.
	 */
	public List< CorrelationPair > getOperationCorrelationPairs( String operationName ) {
		return (List< CorrelationPair >) correlationMap.get( operationName );
	}

	public List< VariablePath > correlationVariablePaths() {
		return correlationVariablePaths;
	}

	public Set< String > correlatingOperations() {
		return correlationMap.keySet();
	}
}
