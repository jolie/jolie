/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

package jolie.runtime;

import jolie.ExecutionThread;
import jolie.State;
import jolie.process.TransformationReason;
import jolie.runtime.expression.Expression;
import jolie.util.Pair;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents a variable path, e.g. a.b[3], offering mechanisms for referring to the object pointed
 * by it.
 *
 * @author Fabrizio Montesi
 */
public class VariablePath implements Expression {
	public static class EmptyPathLazyHolder {
		private EmptyPathLazyHolder() {}

		@SuppressWarnings( "unchecked" )
		public static final Pair< Expression, Expression >[] EMPTY_PATH = new Pair[ 0 ];
	}

	private final Pair< Expression, Expression >[] path; // Right Expression may be null

	public final Pair< Expression, Expression >[] path() {
		return path;
	}

	public boolean isGlobal() {
		return false;
	}

	protected static Pair< Expression, Expression >[] cloneExpressionHelper( Pair< Expression, Expression >[] path,
		TransformationReason reason ) {
		@SuppressWarnings( "unchecked" )
		Pair< Expression, Expression >[] clonedPath = new Pair[ path.length ];
		for( int i = 0; i < path.length; i++ ) {
			clonedPath[ i ] = new Pair<>(
				path[ i ].key().cloneExpression( reason ),
				(path[ i ].value() == null) ? null : path[ i ].value().cloneExpression( reason ) );
		}
		return clonedPath;
	}

	public VariablePath copy() {
		return new VariablePath( Arrays.copyOf( path, path.length ) );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		Pair< Expression, Expression >[] clonedPath = cloneExpressionHelper( path, reason );
		return new VariablePath( clonedPath );
	}

	public final VariablePath containedSubPath( VariablePath otherVarPath ) {
		if( getRootValue() != otherVarPath.getRootValue() )
			return null;

		// If the other path is shorter than this, it's not a subpath.
		if( otherVarPath.path.length < path.length )
			return null;

		int i, myIndex, otherIndex;
		Pair< Expression, Expression > pair, otherPair;
		Expression expr, otherExpr;
		for( i = 0; i < path.length; i++ ) {
			pair = path[ i ];
			otherPair = otherVarPath.path[ i ];

			// *.element_name is not a subpath of *.other_name
			if( !pair.key().evaluate().strValue().equals( otherPair.key().evaluate().strValue() ) )
				return null;

			// If element name is equal, check for the same index
			expr = pair.value();
			otherExpr = otherPair.value();

			myIndex = (expr == null) ? 0 : expr.evaluate().intValue();
			otherIndex = (otherExpr == null) ? 0 : otherExpr.evaluate().intValue();
			if( myIndex != otherIndex )
				return null;
		}


		// Now i represents the beginning of the subpath, we can just copy it from there
		@SuppressWarnings( "unchecked" )
		Pair< Expression, Expression >[] subPath = new Pair[ otherVarPath.path.length - i ];
		System.arraycopy( otherVarPath.path, i, subPath, 0, otherVarPath.path.length - i );
		/*
		 * for( int k = 0; i < otherVarPath.path.length; i++ ) { subPath[k] = otherVarPath.path[i]; k++; }
		 */

		return _createVariablePath( subPath );
	}

	protected VariablePath _createVariablePath( Pair< Expression, Expression >[] path ) {
		return new VariablePath( path );
	}

	public VariablePath( Pair< Expression, Expression >[] path ) {
		this.path = path;
	}

	protected Value getRootValue() {
		ExecutionThread executionThread = ExecutionThread.currentThread();
		if( executionThread != null ) {
			return executionThread.state().root();
		}
		// fallback value
		return new State().root();
	}

	public final void undef() {
		Pair< Expression, Expression > pair;
		ValueVector currVector;
		Value currValue = getRootValue();
		int index;
		String keyStr;
		for( int i = 0; i < path.length; i++ ) {
			pair = path[ i ];
			keyStr = pair.key().evaluate().strValue();
			currVector = currValue.children().get( keyStr );
			if( currVector == null ) {
				return;
			} else if( currVector.size() < 1 ) {
				currValue.children().remove( keyStr );
				return;
			}
			if( pair.value() == null ) {
				if( (i + 1) < path.length ) {
					currValue = currVector.get( 0 );
				} else { // We're finished
					currValue.children().remove( keyStr );
				}
			} else {
				index = pair.value().evaluate().intValue();
				if( (i + 1) < path.length ) {
					if( currVector.size() <= index ) {
						return;
					}
					currValue = currVector.get( index );
				} else {
					if( currVector.size() > index ) {
						currVector.remove( index );
					}
				}
			}
		}
	}

	public final Value getValue() {
		return getValue( getRootValue() );
	}

	public final Value getValue( ValueLink l ) {
		final State state = ExecutionThread.currentThread().state();

		if( state.hasAlias( this, l ) ) {
			throw buildAliasAccessException().toRuntimeFaultException();
		} else {
			state.putAlias( this, l );
		}

		Value v = getValue();
		state.removeAlias( this, l );
		return v;
	}

	public final Value getValue( Value currValue ) {
		for( Pair< Expression, Expression > pair : path ) {
			final String keyStr = pair.key().evaluate().strValue();
			currValue =
				pair.value() == null
					? currValue.getFirstChild( keyStr )
					: currValue.getChildren( keyStr ).get( pair.value().evaluate().intValue() );
		}


		return currValue;
	}

	public final void setValue( Value value ) {
		Pair< Expression, Expression > pair;
		ValueVector currVector;
		Value currValue = getRootValue();
		int index;
		String keyStr;

		if( path.length == 0 ) {
			currValue.refCopy( value );
		} else {
			for( int i = 0; i < path.length; i++ ) {
				pair = path[ i ];
				keyStr = pair.key().evaluate().strValue();
				currVector = currValue.getChildren( keyStr );
				if( pair.value() == null ) {
					if( (i + 1) < path.length ) {
						currValue = currVector.get( 0 );
					} else { // We're finished
						if( currVector.get( 0 ).isUsedInCorrelation() ) {
							currVector.get( 0 ).refCopy( value );
						} else {
							currVector.set( 0, value );
						}
					}
				} else {
					index = pair.value().evaluate().intValue();
					if( (i + 1) < path.length ) {
						currValue = currVector.get( index );
					} else {
						if( currVector.get( index ).isUsedInCorrelation() ) {
							currVector.get( index ).refCopy( value );
						} else {
							currVector.set( index, value );
						}
					}
				}
			}
		}

	}

	public final Value getValueOrNull() {
		return getValueOrNull( getRootValue() );
	}

	public final Optional< Value > getValueOpt( Value v ) {
		return Optional.ofNullable( getValueOrNull( v ) );
	}

	public final Value getValueOrNull( Value currValue ) {
		for( int i = 0; i < path.length; i++ ) {
			final Pair< Expression, Expression > pair = path[ i ];
			final ValueVector currVector = currValue.children().get( pair.key().evaluate().strValue() );
			if( currVector == null ) {
				return null;
			}
			if( pair.value() == null ) {
				if( (i + 1) < path.length ) {
					if( currVector.isEmpty() ) {
						return null;
					}
					currValue = currVector.get( 0 );
				} else { // We're finished
					if( currVector.isEmpty() ) {
						return null;
					} else {
						return currVector.get( 0 );
					}
				}
			} else {
				final int index = pair.value().evaluate().intValue();
				if( currVector.size() <= index ) {
					return null;
				}
				currValue = currVector.get( index );
				if( (i + 1) >= path.length ) {
					return currValue;
				}
			}
		}

		return currValue;
	}

	public final ValueVector getValueVector( ValueVectorLink l ) {
		final State state = ExecutionThread.currentThread().state();
		if( state.hasAlias( this, l ) ) {
			throw buildAliasAccessException().toRuntimeFaultException();
		} else {
			state.putAlias( this, l );
		}
		ValueVector v = getValueVector();
		if( state.hasAlias( this, v ) ) {
			throw buildAliasAccessException().toRuntimeFaultException();
		}
		state.removeAlias( this, l );
		return v;
	}

	private FaultException buildAliasAccessException() {
		String alias = "";
		boolean isRoot = true;
		for( Pair< Expression, Expression > p : path ) {
			if( isRoot ) {
				alias += p.key().evaluate().strValue();
				isRoot = false;
			} else {
				alias += "." + p.key().evaluate().strValue();
			}
		}


		return new FaultException( "AliasAccessException",
			"Found a loop when accessing an alias pointing to path: " + alias );
	}


	public final ValueVector getValueVector( Value currValue ) {
		ValueVector currVector = null;
		for( int i = 0; i < path.length; i++ ) {
			final Pair< Expression, Expression > pair = path[ i ];
			currVector = currValue.getChildren( pair.key().evaluate().strValue() );
			if( (i + 1) < path.length ) {
				if( pair.value() == null ) {
					currValue = currVector.get( 0 );
				} else {
					currValue = currVector.get( pair.value().evaluate().intValue() );
				}
			}
		}

		return currVector;
	}

	public final ValueVector getValueVectorOrNull( Value currValue ) {
		ValueVector currVector = null;
		for( int i = 0; i < path.length; i++ ) {
			final Pair< Expression, Expression > pair = path[ i ];
			currVector = currValue.children().get( pair.key().evaluate().strValue() );
			if( currVector == null ) {
				return null;
			}
			if( (i + 1) < path.length ) {
				if( pair.value() == null ) {
					if( currVector.isEmpty() ) {
						return null;
					}
					currValue = currVector.get( 0 );
				} else {
					final int index = pair.value().evaluate().intValue();
					if( currVector.size() <= index ) {
						return null;
					}
					currValue = currVector.get( index );
				}
			}
		}

		return currVector;
	}

	public final ValueVector getValueVectorOrNull() {
		return getValueVectorOrNull( getRootValue() );
	}

	public final ValueVector getValueVector() {
		return getValueVector( getRootValue() );
	}

	public final void makePointer( VariablePath rightPath ) {
		makePointer( getRootValue(), rightPath );
	}

	public final void makePointer( Value currValue, VariablePath rightPath ) {
		Pair< Expression, Expression > pair;
		ValueVector currVector;
		int index;
		String keyStr;
		for( int i = 0; i < path.length; i++ ) {
			pair = path[ i ];
			keyStr = pair.key().evaluate().strValue();
			currVector = currValue.getChildren( keyStr );
			if( pair.value() == null ) {
				if( (i + 1) < path.length ) {
					currValue = currVector.get( 0 );
				} else { // We're finished
					currValue.children().put( keyStr, ValueVector.createLink( rightPath ) );
				}
			} else {
				index = pair.value().evaluate().intValue();
				if( (i + 1) < path.length ) {
					currValue = currVector.get( index );
				} else {
					currVector.set( index, Value.createLink( rightPath ) );
				}
			}
		}

	}

	public Object getValueOrValueVector() {
		Pair< Expression, Expression > pair;
		ValueVector currVector;
		Value currValue = getRootValue();
		int index;
		for( int i = 0; i < path.length; i++ ) {
			pair = path[ i ];
			currVector = currValue.getChildren( pair.key().evaluate().strValue() );
			if( pair.value() == null ) {
				if( (i + 1) < path.length ) {
					currValue = currVector.get( 0 );
				} else { // We're finished
					return currVector;
				}
			} else {
				index = pair.value().evaluate().intValue();
				if( (i + 1) < path.length ) {
					currValue = currVector.get( index );
				} else {
					return currVector.get( index );
				}
			}
		}


		return currValue;
	}

	public final void deepCopy( VariablePath rightPath ) {
		Object myObj = getValueOrValueVector();
		if( myObj instanceof Value ) {
			((Value) myObj).deepCopy( rightPath.getValue() );
		} else {
			ValueVector myVec = (ValueVector) myObj;
			ValueVector rightVec = rightPath.getValueVector();
			for( int i = 0; i < rightVec.size(); i++ ) {
				myVec.get( i ).deepCopy( rightVec.get( i ) );
			}
		}
	}

	@Override
	public final Value evaluate() {
		final Value v = getValueOrNull();
		return (v == null) ? Value.UNDEFINED_VALUE : v;
	}
}
