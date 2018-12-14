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

package joliex.queryengine.match;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public final class MatchQuery {
	
	private static class RequestType {
		
		private static final String DATA = "data";
		private static final String QUERY = "query";
		private static class QuerySubtype {
			private static final String NOT = "not";
			private static final String EQUAL = "equal";
			private static final String GREATERTHAN = "greaterThan";
			private static final String LOWERTHAN = "lowerThan";
			private static final String OR = "or";
			private static final String AND = "and";
			private static final String EXISTS = "exists";
			private static final String LEFT = "left";
			private static final String RIGHT = "right";
			private static final String PATH = "path";
			private static final String VALUE = "value";
			private static String[] subNodes = { NOT, EQUAL, GREATERTHAN, LOWERTHAN, OR, AND, EXISTS };
		}
	}
		
	//type MatchRequestType : void {
	//.data*                : undefined
	//.query                : void {
	//    .not                  : MatchExp
	//    | .equal              : void { .path: Path, .value[1,*]: undefined }
	//    | .or                 : void { .left: MatchExp, .right: MatchExp }
	//    | .and                : void { .left: MatchExp, .right: MatchExp }
	//    | .exists             : Path
	//    | bool
		
	public static Value match( Value matchRequest ) {

		Value query = matchRequest.getChildren( RequestType.QUERY ).first();
		ValueVector dataElements = matchRequest.getChildren( RequestType.DATA );

		return parseMatchExpression( query ).applyOn( dataElements );

	}
	
	public static MatchExpression parseMatchExpression( Value query ) {
		
		// NOT -> MatchExpression
		
		
		// OR -> BinaryExpression
		
		
		// AND -> BinaryExpression
		
		
		// EXISTS -> Path
		
		
		// EQUAL -> Path, ValueVector
		
		
		//.query                : void {
		//    .not                  : MatchExp
		//    | .equal              : void { .path: Path, .value[1,*]: undefined }
		//    | .or                 : void { .left: MatchExp, .right: MatchExp }
		//    | .and                : void { .left: MatchExp, .right: MatchExp }
		//    | .exists             : Path
		//    | bool
		
//		System.out.println( "Query: " + query.toPrettyString() );
//		HashMap< String, BiFunction<Value,String,MatchExpression> > hashMap = new HashMap();
//		hashMap.put(
//				RequestType.QuerySubtype.AND,
//				( Value q, String s ) -> new AndExpression( createBinaryExpression( q.getChildren( s ) ) ) 
//		);
//		hashMap.put(
//				RequestType.QuerySubtype.OR,
//				( Value q, String s ) -> new OrExpression( createBinaryExpression( q.getChildren( s ) ) ) 
//		);
//		hashMap.put(
//				RequestType.QuerySubtype.NOT,
//				( Value q, String s ) -> new NotExpression( parseMatchExpression( q.getFirstChild( s ) ) ) 
//		);
//		hashMap.put(
//				RequestType.QuerySubtype.EXISTS,
//				( Value q, String s ) -> new (createBinaryExpression( q.getChildren( s ) ) ) 
//		);
//		if ( query.hasChildren() ){
//			String node = ( String ) query.children().keySet().toArray()[0];
//			System.out.println( "Found: " + node );
//		}
//		return null;
//		try {
//				if( query.hasChildren( RequestType.MatchExpressionType.AND ) ) {
//				return new AndExpression( createBinaryExpression( query.getChildren( RequestType.MatchExpressionType.AND ) ) );
//			}
//			
//			}
//
//			if (query.hasChildren(or)) {
//				ValueVector children = query.getChildren(or);
//
//				return new OrExp(createBinaryExpression(children));
//			}
//
//			if (query.hasChildren(not)) {
//				ValueVector children = query.getChildren(not);
//
//				assert (children.size() == 1);
//
//				return new NotExp(parseMatchExpression(children.first()));
//			}
//
//			if (query.hasChildren(equal)) {
//				ValueVector children = query.getChildren(equal);
//
//				return new EqualExp(createCompareExpression(children));
//			}
//
//			if (query.hasChildren(greaterThen)) {
//				ValueVector children = query.getChildren(greaterThen);
//
//				assert (children.size() == 1);
//
//				return new GreaterThenExp(createCompareExpression(children));
//			}
//
//			if (query.hasChildren(lowerThen)) {
//				ValueVector children = query.getChildren(lowerThen);
//
//				assert (children.size() == 1);
//
//				return new LowerThenExp(createCompareExpression(children));
//			}
//
//			if (query.hasChildren(exists)) {
//				ValueVector children = query.getChildren(exists);
//
//				assert (children.size() == 1);
//
//				return new ExistsExp(children.first().strValue());
//			}
//		} catch (Exception e) {
//			System.out.println(e.getMessage());
//		}
//
//		throw new IllegalArgumentException("Unsupported structure of a match request");
	}
	
//	private static BinaryExpression createBinaryExpression(ValueVector children) {
//		HashMap<String, ValueVector> leftRight = new HashMap<>();
//
//		children.forEach(
//				value -> {
//					if (value.hasChildren( RequestType.QuerySubtype.LEFT ) ) {
//						leftRight.put(
//								RequestType.QuerySubtype.LEFT, 
//								value.getChildren( RequestType.QuerySubtype.LEFT ) );
//					}
//
//					if (value.hasChildren( RequestType.QuerySubtype.RIGHT ) ) {
//						leftRight.put( RequestType.QuerySubtype.RIGHT, value.getChildren( RequestType.QuerySubtype.RIGHT ) );
//					}
//				});
//		assert (leftRight.containsKey( RequestType.QuerySubtype.LEFT ) && leftRight.containsKey( RequestType.QuerySubtype.RIGHT ) );
//
//		return new BinaryExpression(parseMatchExpression(leftRight.get( RequestType.QuerySubtype.LEFT ).first()),
//			parseMatchExpression(leftRight.get( RequestType.QuerySubtype.RIGHT ).first()));
//	}
//
//	private static CompareExp createCompareExpression(ValueVector children) {
//		HashMap<String, ValueVector> pathValue = new HashMap<>();
//
//		children.forEach(
//				value -> {
//					if ( value.hasChildren( RequestType.QuerySubtype.PATH ) ) {
//						pathValue.put( RequestType.QuerySubtype.PATH, value.getChildren( RequestType.QuerySubtype.PATH ));
//					}
//
//					if (value.hasChildren( RequestType.QuerySubtype.VALUE )) {
//						pathValue.put( RequestType.QuerySubtype.VALUE, value.getChildren( RequestType.QuerySubtype.VALUE ));
//					}
//				});
//		assert ( pathValue.containsKey( RequestType.QuerySubtype.PATH ) && pathValue.containsKey( RequestType.QuerySubtype.VALUE ) );
//		assert ( ( pathValue.get( RequestType.QuerySubtype.PATH ).size() == 1) && (pathValue.get( RequestType.QuerySubtype.VALUE ).size() == 1 ) );
//
//		return new CompareExp( pathValue.get( RequestType.QuerySubtype.PATH ).first().strValue(), pathValue.get( RequestType.QuerySubtype.VALUE ).first().strValue() );
//	}
}
