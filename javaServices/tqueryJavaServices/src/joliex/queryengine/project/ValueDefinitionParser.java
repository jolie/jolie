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

package joliex.queryengine.project;

import java.util.ArrayList;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.match.MatchExpression;
import joliex.queryengine.match.MatchQuery;

class ValueDefinitionParser {
	
	private static final class ValueDefinitionType {
		private static final String PATH = "path";
		private static final String MATCH = "match";
		private static final String TERNARY = "elvis";
		private static final class TernaryDefinitionSubtype {
			private static final String CONDITION = "exp";
			private static final String IF_TRUE = "ifTrue";
			private static final String IF_FALSE = "ifFalse";
		}
	}
	
	public static ValueDefinition parseValues( ValueVector values ) throws FaultException {
		if ( values.size() == 1 ){
			return parseValue( values.get( 0 ) );
		} else {
			ArrayList<ValueDefinition> valueDefinitions = new ArrayList<>();
			for ( Value value : values ) {
				valueDefinitions.add( parseValue( value ) );
			}
			return new ListValueDefinition( valueDefinitions );
		}
	}
	
	public static ValueDefinition parseValue( Value value ) throws FaultException {
		if ( value.hasChildren( ValueDefinitionType.PATH ) ) {
			return new PathValueDefinition( value.getFirstChild( ValueDefinitionType.PATH ).strValue() );
		} else if ( value.hasChildren( ValueDefinitionType.MATCH ) ) {
			return new MatchValueDefinition( 
					parseMatchExpression( value.getFirstChild( ValueDefinitionType.MATCH ) ) );
		} else if ( value.hasChildren( ValueDefinitionType.TERNARY ) ){
			Value subNodes = value.getFirstChild( ValueDefinitionType.TERNARY );
			MatchExpression condition = parseMatchExpression( 
					subNodes.getFirstChild( ValueDefinitionType.TernaryDefinitionSubtype.CONDITION 
			) );
			ValueDefinition ifTrue = parseValues( 
					subNodes.getChildren( ValueDefinitionType.TernaryDefinitionSubtype.IF_TRUE )
			);
			ValueDefinition ifFalse = parseValues( subNodes.getChildren( 
					ValueDefinitionType.TernaryDefinitionSubtype.IF_FALSE )
			);
			return new TernaryValueDefinition( condition, ifTrue, ifFalse );
		} else if ( value.isDefined() ) {
			return new ConstantValueDefinition( value );
		} else {
			throw new IllegalArgumentException( "Could not parse project ValueDefinition " + value.toPrettyString() );
		}
	}
	
	private static MatchExpression parseMatchExpression( Value value  ) throws FaultException{
		return MatchQuery.parseMatchExpression( value ).orElseThrow( 
					() -> new FaultException( 
							"MatchQuerySyntaxException", 
							"Could not parse query expression " + value.getFirstChild( ValueDefinitionType.MATCH ).toPrettyString() ) );
	}
	
}
