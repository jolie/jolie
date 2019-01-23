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

package joliex.queryengine.group;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import static jolie.js.JsUtils.parseJsonIntoValue;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;
import joliex.queryengine.project.ProjectExpressionChain;
import joliex.queryengine.project.ValueToPathProjectExpression;
import joliex.queryengine.project.valuedefinition.ConstantValueDefinition;

public class Utils {
	
	public class GroupPair {
		private final Path s, d;

		public GroupPair( Path sourcePath, Path destinationPath ) {
			this.s = sourcePath;
			this.d = destinationPath;
		}
		
		public Path getDstPath() { return d; } public Path getSrcPath() { return s; }
		
	}
	
	public GroupPair getGroupPair( Path sourcePath, Path destinationPath ){
		return new GroupPair( sourcePath, destinationPath );
	}

	public static void main( String[] args ) throws IOException {
		String jsonString = "{ \"data\": [\n" +
			"  { \"a\": [ { \"d\" : [ \"1.a.d.1\" , \"1.a.d.2\" , \"1.a.d.3\" ] }, { \"e\": [ \"1.a.e.1\", \"1.a.e.2\" ] } ], \"b\": [ \"1.b.1\", \"1.b.2\" ], \"c\": { \"f\": [ \"1.c.f.1\", \"1.c.f.2\" ] } },\n" +
			"  { \"a\": { \"e\": [ \"2.a.e.1\", \"2.a.e.2\" ] }, \"b\": [ \"2.b.1\" ], \"c\": { \"f\": [ \"2.c.f.1\" ] } }, \n" +
			"  { \"a\": { \"d\": [ \"3.a.d.1\", \"3.a.d.2\", \"3.a.d.3\", \"3.a.d.4\", \"3.a.d.5\", \"3.a.d.6\" ] }, \"c\": { \"f\": [  \"3.c.f.1\" ] } }\n" +
			"]}";
//		String jsonString = "{ \"data\": [\n" +
//			"  { \"a\": [ { \"d\" : [ 5, 3, 1 ] }, { \"e\": [ 2, 4 ] } ], \"b\": [ 1, 3 ], \"c\": { \"f\": [ 1, 6 ] } },\n" +
//			"  { \"a\": { \"e\": [ 2, 5 ] }, \"b\": [ 9 ], \"c\": { \"f\": [ 6 ] } }, \n" +
//			"  { \"a\": { \"d\": [ 1, 8, 7 ] }, \"c\": { \"f\": [  1 ] } }\n" +
//			"]}";
		Value v = Value.create();
		parseJsonIntoValue( new StringReader( jsonString ), v, false );
		List<GroupPair> groupingList = new ArrayList<>();
		Utils u = new Utils();
		groupingList.add( u.getGroupPair( Path.parsePath( "a.d" ), Path.parsePath( "nodeA" ) ) );
		groupingList.add( u.getGroupPair( Path.parsePath( "b" ), Path.parsePath( "nodeB" ) ) );
		groupingList.add( u.getGroupPair( Path.parsePath( "c" ), Path.parsePath( "nodeC" ) ) );
		ValueVector dataElements = v.children().get( "data" );
		for ( Integer[] H : getPowerSet( groupingList ) ) {
			Map< Integer, ArrayList< Value > > values = getValues( H, groupingList, dataElements );
			ArrayList< ArrayList< Value > > combinations = getCombinations( values );
			for ( ArrayList<Value> combination : combinations ) {
				try {
					System.out.println( getValueToPathProjection( H, combination, groupingList ).applyOn( Value.create() ).toPrettyString() );
				} catch (FaultException ex) {
					Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
//			System.out.println( "- - - - - - - - - H is: " + Arrays.toString( H ) + " - - - - - - - - " );
//			for (ArrayList<Value> combination : combinations) {
//				System.out.print("[ ");
//				int i = 0;
//				for (Value value : combination) {
//					System.out.print("pos: " + i++ + value.toPrettyString().trim() + ",");
//				}
//				System.out.println("]\n- - - - ");
//			}
		}
	}

	public static ProjectExpressionChain getValueToPathProjection(
			Integer[] H,
			ArrayList< Value > values,
			List< GroupPair > groupingList
	) throws FaultException {
		int valuesIndex = 0;
		ProjectExpressionChain returnProjectionChain = new ProjectExpressionChain();
		for ( Integer h : H ) {
			returnProjectionChain.addExpression(
					new ValueToPathProjectExpression(
							groupingList.get( h ).getDstPath(),
							new ConstantValueDefinition( values.get( valuesIndex++ ) )
			));
		}
		return returnProjectionChain;
	}
	
	public static Map< Integer, ArrayList< Value> > getValues(
			Integer[] H,
			List<GroupPair> groupingList,
			ValueVector elements) {
		Map< Integer, ArrayList< Value > > returnValues = new HashMap<>();
		for ( int h : H ) {
			for ( Value element : elements ) {
				groupingList.get( h )
						.getSrcPath() // s_h
						.apply( element ) // [[ s_h ]]^t
						.ifPresent( ( vector ) -> { // [[ s_h ]]^t \neq \alpha
					for ( Value dataValue : vector ) {
						returnValues.putIfAbsent( h, new ArrayList<>() );
						if( !returnValues.get( h )
								.stream()
								.anyMatch( ( present ) -> 
										joliex.queryengine.common.Utils.checkTreeEquality( present,  dataValue ) ) // {{ a }}, unique values
						){
							returnValues.get( h ).add( dataValue );
						}
					}
				});
			}
		}
		return returnValues;
	}
	
	public static ArrayList<Integer[]> getPowerSet( List<GroupPair> groupList ) {
		ArrayList<Integer[]> returnList = new ArrayList();
		int powerSetSize =  (int) Math.pow( groupList.size(), 2 )-1;
		for ( int i = 0; i < powerSetSize; i++ ) {
			returnList.add( getIndexes( i, groupList.size() ) );
		}
		return returnList;
	}
	
	private static Integer[] getIndexes( int i, int size ){
		ArrayList<Integer> returnArray = new ArrayList();
		for ( int j = 0; j < size; j++ ) {
			if( i > 0 ){
				if( i%2 == 1 ) 
					returnArray.add( j );
				i = i/2;
			} else {
				break;
			}
		}
		return returnArray.toArray( new Integer[ returnArray.size() ] );
	}
	
	public static ArrayList< ArrayList < Value > > getCombinations( Map< Integer, ArrayList< Value > > values ){
		Optional<Integer> index = values.keySet().stream().max( Integer::max );
		if( index.isPresent() ){
			ArrayList< Value > currentValues = values.remove( index.get() );
			ArrayList< ArrayList<Value> > combinedList = getCombinations( values );
			ArrayList< ArrayList< Value > > returnList = new ArrayList();
				for ( ArrayList<Value> arrayList : combinedList ) {
					for ( Value currentValue : currentValues ) { // TODO select only unique values
						ArrayList< Value > thisList = new ArrayList();
						thisList.add( currentValue );
						for ( Value value : arrayList ) {
							thisList.add( value );
						}
						returnList.add( thisList );
					}
				}
				return returnList;
		} else {
			ArrayList< ArrayList < Value > > returnList = new ArrayList();
			returnList.add( new ArrayList() );
			return returnList;
		}
	}
	
}
