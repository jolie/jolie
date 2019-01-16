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

package joliex.queryengine.common;

import java.util.HashSet;
import java.util.Set;
import jolie.runtime.CompareOperators;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public final class Utils {

	public static boolean checkTreeEquality( Value v1, Value v2 ){
		if ( v1.equals( v2 ) ){ // if the root value matches
			if ( v1.children().keySet().equals( v2.children().keySet() ) ){
				for( String node : v1.children().keySet() ){
					if( !checkVectorEquality( v1.getChildren( node ),  v2.getChildren( node ) ) ){
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	public static boolean checkVectorEquality( ValueVector v1, ValueVector v2 ){
		if ( v1.size() == v2.size() ){
			for ( int i = 0; i < v1.size(); i++ ) {
				if ( !checkTreeEquality( v1.get( i ), v2.get( i ) ) ){
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	public static Value merge( Value v1, Value v2 ) throws FaultException {
		if( CompareOperators.EQUAL.test( v1, v2 ) ){
			Value returnValue = v1.clone();
			// REMOVE THIS AND JUST USE CONDITIONALS
			Set<String> keySetIntersection = new HashSet<>( v1.children().keySet() );
			keySetIntersection.retainAll( v2.children().keySet() );
			// we just need the unique set of keys for v2 since we already cloned the keys and children of V1
			Set<String> uniqueKeySetV2 = new HashSet<>( v2.children().keySet() );
			uniqueKeySetV2.removeAll( keySetIntersection );
			for ( String key : keySetIntersection ) {
				returnValue.children().put( key, merge( v1.getChildren( key ), v2.getChildren( key ) ) );
			}
			uniqueKeySetV2.forEach( ( key ) -> {
				returnValue.children().put( key, v2.getChildren( key ) );
			});
			return returnValue;
		} else {
			throw new FaultException(
				"MergeValueException",
				"Values: \n" + v1.toPrettyString() + "\n and \n" + v2.toPrettyString() + "\n cannot be merged"
			);
		}
	}
	
	public static ValueVector merge( ValueVector v1, ValueVector v2 ) throws FaultException{
		if( v1.size() >= v2.size() ){
			ValueVector returnVector = ValueVector.create();
			for ( int i = 0; i < v1.size(); i++ ) {
				if( v2.size() > i ){
					returnVector.add( merge( v1.get(i), v2.get( i ) ) );
				} else {
					returnVector.add( v1.get( i ) );
				}
			}
			return returnVector;
		} else {
			return merge( v2, v1 ); 
		}
	}
	
}