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

import java.util.Optional;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;

public final class MatchQuery {

	private static class RequestType {

		private static final String DATA = "data";
		private static final String QUERY = "query";

		private static class QuerySubtype {

			private static final String NOT = "not";
			private static final String EQUAL = "equal";
			private static final String OR = "or";
			private static final String AND = "and";
			private static final String EXISTS = "exists";
			private static final String LEFT = "left";
			private static final String RIGHT = "right";
			private static final String PATH = "path";
			private static final String VALUE = "value";
		}
	}

	private static class ResponseType {

		private static final String RESPONSE = "response";
	}
	
	public static Value match( Value matchRequest ) throws FaultException {

		Value query = matchRequest.getFirstChild( RequestType.QUERY );
		ValueVector dataElements = matchRequest.getChildren( RequestType.DATA );
		boolean[] mask = parseMatchExpression( query )
				.orElseThrow( 
					() -> new FaultException( "MatchQuerySyntaxException", "Could not parse query expression " + query.toPrettyString() ) 
				).applyOn( dataElements );
		Value response = Value.create();
		ValueVector responseVector = ValueVector.create();
		response.children().put( ResponseType.RESPONSE, responseVector );
		for ( int i = 0; i < mask.length; i++ ) {
			if ( mask[i] ) {
				responseVector.add( dataElements.get( i ) );
			}
		}
		return response;
	}
	
	public static Optional<MatchExpression> parseMatchExpression( Value query ){
		MatchExpression e =  unsafeParseMatchExpression( query );
		return ( e != null ) ? Optional.of( e ) : Optional.empty();
	}

	//type MatchRequestType : void {
	//.data*                : undefined
	//.query                : void {
	//    .not                  : MatchExp
	//    | .or                 : void { .left: MatchExp, .right: MatchExp }
	//    | .and                : void { .left: MatchExp, .right: MatchExp }
	//    | .equal              : void { .path: Path, .value[1,*]: undefined }
	//    | .exists             : Path
	//    | bool
		
		private static MatchExpression unsafeParseMatchExpression( Value query ) throws IllegalArgumentException {
		if ( query.children().size() > 1 ) {
			throw new IllegalArgumentException( query.toPrettyString() );
		} else {
			if ( query.isBool() ) {
				return new BooleanExpression( query.boolValue() );
			} else if ( query.hasChildren( RequestType.QuerySubtype.EQUAL ) ) {
				return new EqualExpression(
						Path.parsePath( query.getFirstChild( RequestType.QuerySubtype.EQUAL ).getFirstChild( RequestType.QuerySubtype.PATH ).strValue() ),
						query.getFirstChild( RequestType.QuerySubtype.EQUAL ).getChildren( RequestType.QuerySubtype.VALUE )
				);
			} else if ( query.hasChildren( RequestType.QuerySubtype.EXISTS ) ) {
				return new ExistsExpression(
						Path.parsePath( query.getFirstChild( RequestType.QuerySubtype.EXISTS ).strValue() )
				);
			} else if ( query.hasChildren( RequestType.QuerySubtype.OR ) ) {
				return BinaryExpression.OrExpression(
						parseMatchExpression( query.getFirstChild( RequestType.QuerySubtype.OR ).getFirstChild( RequestType.QuerySubtype.LEFT ) )
							.orElseThrow( 
								() -> new IllegalArgumentException( "Could not parse left hand of " + query.toPrettyString() )
						),
						parseMatchExpression( query.getFirstChild( RequestType.QuerySubtype.OR ).getFirstChild( RequestType.QuerySubtype.RIGHT ) )
							.orElseThrow(
								() -> new IllegalArgumentException( "Could not parse right hand of " + query.toPrettyString() )
						)
				);
			} else if ( query.hasChildren( RequestType.QuerySubtype.AND ) ) {
				return BinaryExpression.AndExpression(
						parseMatchExpression( query.getFirstChild( RequestType.QuerySubtype.AND ).getFirstChild( RequestType.QuerySubtype.LEFT ) )
							.orElseThrow(
								() -> new IllegalArgumentException( "Could not parse left hand of " + query.toPrettyString() )
						),
						parseMatchExpression( query.getFirstChild( RequestType.QuerySubtype.AND ).getFirstChild( RequestType.QuerySubtype.RIGHT ) )
							.orElseThrow(
								() -> new IllegalArgumentException( "Could not parse right hand of " + query.toPrettyString() )
						)
				);
			} else if ( query.hasChildren( RequestType.QuerySubtype.NOT ) ){
					return new NotExpression( parseMatchExpression( query.getFirstChild( RequestType.QuerySubtype.NOT ) )
						.orElseThrow(
							() -> new IllegalArgumentException( "Could not parse " + query.toPrettyString() )
						)
					);
				}
			}
		return null;
	}
	
}
