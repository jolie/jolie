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
import jolie.js.JsUtils;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;
import joliex.queryengine.match.BinaryExpression;
import joliex.queryengine.match.EqualExpression;
import joliex.queryengine.match.ExistsExpression;
import joliex.queryengine.match.MatchExpression;
import joliex.queryengine.match.NotExpression;
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
//		String jsonString = "{ \"data\": [\n" +
//			"  { \"a\": [ { \"d\" : [ \"1.a.d.1\" , \"1.a.d.2\" , \"1.a.d.3\" ] }, { \"e\": [ \"1.a.e.1\", \"1.a.e.2\" ] } ], \"b\": [ \"1.b.1\", \"1.b.2\" ], \"c\": { \"f\": [ \"1.c.f.1\", \"1.c.f.2\" ] } },\n" +
//			"  { \"a\": { \"e\": [ \"2.a.e.1\", \"2.a.e.2\" ] }, \"b\": [ \"2.b.1\" ], \"c\": { \"f\": [ \"2.c.f.1\" ] } }, \n" +
//			"  { \"a\": { \"d\": [ \"3.a.d.1\", \"3.a.d.2\", \"3.a.d.3\", \"3.a.d.4\", \"3.a.d.5\", \"3.a.d.6\" ] }, \"c\": { \"f\": [  \"3.c.f.1\" ] } }\n" +
//			"]}";
		String jsonString = "{ \"data\": [\n" +
			"  { \"a\": [ { \"d\" : [ 5, 3, 1 ] }, { \"e\": [ 2, 4 ] } ], \"b\": [ 1, 3 ], \"c\": { \"f\": [ 1, 6 ] } },\n" +
			"  { \"a\": { \"e\": [ 2, 5 ] }, \"b\": [ 9 ], \"c\": { \"f\": [ 6 ] } }, \n" +
			"  { \"a\": { \"d\": [ 1, 8, 7 ] }, \"c\": { \"f\": [  1 ] } }\n" +
			"]}";
		Value v = Value.create();
		JsUtils.parseJsonIntoValue( new StringReader( jsonString ), v, false );
		
		// grouping request parsing, i.e., s_1 > r_1, ..., s_n > r_n
		List<GroupPair> groupingList = new ArrayList<>();
		Utils u = new Utils();
		groupingList.add( u.getGroupPair( Path.parsePath( "a.d" ), Path.parsePath( "nodeA" ) ) );
		groupingList.add( u.getGroupPair( Path.parsePath( "b" ), Path.parsePath( "nodeB" ) ) );
		groupingList.add( u.getGroupPair( Path.parsePath( "c" ), Path.parsePath( "nodeC" ) ) );
		
		List<GroupPair> aggregationList = new ArrayList<>();
		aggregationList.add( u.getGroupPair( Path.parsePath( "a.e" ), Path.parsePath( "nodeE" ) ) );
		
		ValueVector dataElements = v.children().get( "data" );
		ValueVector resultElements = ValueVector.create();
		for ( Integer[] H : getPowerSet( groupingList ) ) {
			Map< Integer, ArrayList< ValueVector > > values = getValues( H, groupingList, dataElements );
			// this is a', i.e., all possible combinations of values, given a chosen set H of paths s_1, ..., s_n
			ArrayList< ArrayList< ValueVector > > combinations = getCombinations( values );
			// we concatenate the result of the projection for all the combinations (a')
			
			ValueVector returnValue = ValueVector.create();
			// we merge the projection for a given a'
			for ( ArrayList<ValueVector> combination : combinations ) {
				try {
					Value chi = getValueToPathProjection( H, combination, groupingList ).applyOn( Value.create() );
					Value tmp = Value.create();
					for ( GroupPair qi : aggregationList ) {
						// we need to merge the results for the whole aggregationList into a single tree
//						getMatchExpression( qi, H, groupingList, combination )
						// then we add it into resultValue
					}
						// for i = 0 to n
						// for t_i in match( a, q_i, ~s_j, ~<s_h, a_h>)
							// concatenate t_i [[ q_i ]]^t_i > p_i
				} catch ( FaultException ex ) {
					Logger.getLogger( Utils.class.getName() ).log( Level.SEVERE, null, ex );
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
	
	private static boolean sorted( Integer[] H ){
		for ( int i = 0; i < H.length-1; i++ ) {
			if( H[ i ] > H[ i+1 ] ){
				return false;
			}
		}
		return true;
	}
	
	private static MatchExpression getMatchExpression( GroupPair qi, Integer[] H, List<GroupPair> groupingList, ArrayList<ValueVector> combination ) {
		assert sorted( H );
		MatchExpression returnExpression = new ExistsExpression( qi.getSrcPath() );
		// we prepare the doNotExistSj expression
		// find all sj (not in H)
		Optional<MatchExpression> doNotExistSj = Optional.empty();
		Optional<MatchExpression> existsShAndEqualComb = Optional.empty();
		int j, i; j = i = 0;
		for ( ; i < groupingList.size(); i++ ) {
			if( i == H[ j ] ){
				existsShAndEqualComb = ( existsShAndEqualComb.isEmpty() ) ?
						Optional.of( BinaryExpression.AndExpression( 
							new ExistsExpression( groupingList.get( i ).getSrcPath() ), 
							new EqualExpression( groupingList.get( i ).getSrcPath(), combination.get( i ) )
						) ) :
						Optional.of( BinaryExpression.AndExpression(
							BinaryExpression.AndExpression(
								new ExistsExpression(( groupingList.get( i ).getSrcPath() ) ),
								new EqualExpression( groupingList.get( i ).getSrcPath(), combination.get( i ) )
							),
							existsShAndEqualComb.get()
						) );
				j++;
			} else {
				doNotExistSj = ( doNotExistSj.isEmpty() ) ? 
					Optional.of( new ExistsExpression( groupingList.get( i ).getSrcPath() ) ) :
					Optional.of( 
						BinaryExpression.OrExpression( 
							doNotExistSj.get(), 
							new ExistsExpression( groupingList.get( i ).getSrcPath() )
						)
					);
			}
		}
		if ( doNotExistSj.isPresent() ){ doNotExistSj = Optional.of( new NotExpression( doNotExistSj.get() ) ); }
		Optional<MatchExpression> sh = Optional.empty();
		if( doNotExistSj.isPresent() ){
			returnExpression = BinaryExpression.AndExpression( returnExpression, doNotExistSj.get() );
		}
		if( existsShAndEqualComb.isPresent() ){
			returnExpression = BinaryExpression.AndExpression(returnExpression, existsShAndEqualComb.get() );
		}
		return returnExpression;
	}

	public static ProjectExpressionChain getValueToPathProjection(
			Integer[] H,
			ArrayList< ValueVector > values,
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
	
	public static Map< Integer, ArrayList< ValueVector > > getValues(
			Integer[] H,
			List<GroupPair> groupingList,
			ValueVector elements) {
		Map< Integer, ArrayList< ValueVector > > returnValues = new HashMap<>();
		for ( int h : H ) {
			for ( Value element : elements ) {
				groupingList.get( h )
						.getSrcPath() // s_h
						.apply( element ) // [[ s_h ]]^t
						.ifPresent( ( vector ) -> { // [[ s_h ]]^t \neq \alpha
							returnValues.putIfAbsent( h, new ArrayList<>() );
							if ( !returnValues.get( h )
									.stream()
									.anyMatch( ( present ) -> 
											joliex.queryengine.common.Utils.checkVectorEquality( present, vector )
									)
							){
								returnValues.get( h ).add( vector );
							}
//					for ( Value dataValue : vector ) {
//						returnValues.putIfAbsent( h, new ArrayList<>() );
//						if( !returnValues.get( h )
//								.stream()
//								.anyMatch( ( present ) -> 
//										joliex.queryengine.common.Utils.checkTreeEquality( present,  dataValue ) ) // {{ a }}, unique values
//						){
//							returnValues.get( h ).add( dataValue );
//						}
//					}
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
	
	public static ArrayList< ArrayList < ValueVector > > getCombinations( Map< Integer, ArrayList< ValueVector > > values ){
		Optional<Integer> index = values.keySet().stream().max( Integer::max );
		if( index.isPresent() ){
			ArrayList< ValueVector > currentValues = values.remove( index.get() );
			ArrayList< ArrayList< ValueVector > > combinedList = getCombinations( values );
			ArrayList< ArrayList< ValueVector > > returnList = new ArrayList();
				for ( ArrayList<ValueVector> arrayList : combinedList ) {
					for ( ValueVector currentValue : currentValues ) { // TODO select only unique values
						ArrayList< ValueVector > thisList = new ArrayList();
						thisList.add( currentValue );
						for ( ValueVector vector : arrayList ) {
							thisList.add( vector );
						}
						returnList.add( thisList );
					}
				}
				return returnList;
		} else {
			ArrayList< ArrayList < ValueVector > > returnList = new ArrayList();
			returnList.add( new ArrayList() );
			return returnList;
		}
	}
	
}
