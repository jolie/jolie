/*******************************************************************************
 *   Copyright (C) 2018 by Larisa Safina <safina@imada.sdu.dk>                 *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         * 
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package joliex.queryengine.unwind;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;

public class UnwindQuery {

	public static Value unwind( Value request ) throws FaultException {

		System.out.println( request.toPrettyString() );

		String queryParameterString = UnwindQuery.RequestType.QUERY;
		Value queryValue = request.getFirstChild( queryParameterString );
		String queryString = queryValue.strValue();
		Path queryPath = Path.parsePath( queryString );
		
		String dataParameterString = UnwindQuery.RequestType.DATA;
		ValueVector dataElements = request.getChildren( dataParameterString );
		
		return unwindOperator( queryPath, dataElements );
	}

	private static Value unwindOperator( Path p, ValueVector data ) {
		
		Value response = Value.create();
		return response;
	}

	private static ValueVector unwindExpansionOperator( ValueVector a, Path p, Value t ) {
		
		return a;
	}

	private static class RequestType {
		
		private static String QUERY = "query";
		private static String DATA = "data";
	}
	
}
