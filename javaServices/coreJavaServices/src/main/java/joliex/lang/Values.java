/*
 *   Copyright (C) 2022 by Fabrizio Montesi <famontesi@gmail.com>
 *   Copyright (C) 2018 by Larisa Safina <safina@imada.sdu.dk>
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *   Copyright (C) 2024 by Marco Peressotti <marco.peressotti@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as
 *   published by the Free Software Foundation; either version 2 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the
 *   Free Software Foundation, Inc.,
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 *   For details about the authors of this software, see the AUTHORS file.
 */

package joliex.lang;

import java.util.SortedSet;
import java.util.TreeSet;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.TypeCastingException;

public class Values extends JavaService {
	public static class ComparisonRequest implements ValueConverter {
		private final Value fst;
		private final Value snd;

		private ComparisonRequest( Value fst, Value snd ) {
			this.fst = fst;
			this.snd = snd;
		}

		public static ComparisonRequest fromValue( Value v ) {
			return new ComparisonRequest( v.getFirstChild( "fst" ), v.getFirstChild( "snd" ) );
		}

		public static Value toValue( ComparisonRequest r ) {
			Value v = Value.create();
			v.setFirstChild( "fst", r.fst );
			v.setFirstChild( "snd", r.snd );
			return v;
		}
	}

	public Boolean equals( ComparisonRequest request ) {
		return checkTreeEquals( request.fst, request.snd );
	}

	private static boolean checkTreeEquals( Value v1, Value v2 ) {
		if( strictEquals( v1, v2 ) ) {
			if( v1.children().keySet().equals( v2.children().keySet() ) ) {
				for( String node : v1.children().keySet() ) {
					if( !checkVectorEquality( v1.getChildren( node ), v2.getChildren( node ) ) ) {
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

	private static boolean strictEquals( Value v1, Value v2 ) {
		boolean r = false;
		try {
			if( v1.isDefined() && v2.isDefined() ) {
				if( v1.isByteArray() && v2.isByteArray() ) {
					r = v1.byteArrayValueStrict().equals( v2.byteArrayValueStrict() );
				} else if( v1.isString() && v2.isString() ) {
					r = v1.strValueStrict().equals( v2.strValueStrict() );
				} else if( v1.isInt() && v2.isInt() ) {
					r = v1.intValueStrict() == v2.intValueStrict();
				} else if( v1.isDouble() && v2.isDouble() ) {
					r = v1.doubleValueStrict() == v2.doubleValueStrict();
				} else if( v1.isBool() && v2.isBool() ) {
					r = v1.boolValueStrict() == v2.boolValueStrict();
				} else if( v1.isLong() && v2.isLong() ) {
					r = v1.longValueStrict() == v2.longValueStrict();
				} else if( v1.valueObject() != null && v2.valueObject() != null ) {
					r = v1.valueObject().equals( v2.valueObject() );
				}
			} else {
				// undefined == undefined
				r = !(v1.isDefined() && v2.isDefined());
			}
		} catch( TypeCastingException ignored ) {
		}
		return r;
	}

	public static boolean checkVectorEquality( ValueVector v1, ValueVector v2 ) {
		if( v1.size() == v2.size() ) {
			for( int i = 0; i < v1.size(); i++ ) {
				if( !checkTreeEquals( v1.get( i ), v2.get( i ) ) ) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns a hash code value for the argument.
	 * 
	 * @param request
	 * @return has code
	 */
	public Integer hashCode( Value request ) {
		return Values.hash( request );
	}

	/*
	 * The implementation of equals and hash must guarantee that equals(v1,v2) implies hash(v1) ==
	 * hash(v2)
	 */
	private static int hash( Value value ) {
		int hashCode = 0;
		if( value != null ) {
			try {
				if( value.isDefined() ) {
					if( value.isByteArray() ) {
						hashCode = value.byteArrayValueStrict().hashCode();
					} else if( value.isString() ) {
						hashCode = value.strValueStrict().hashCode();
					} else if( value.isInt() ) {
						hashCode = Integer.hashCode( value.intValueStrict() );
					} else if( value.isDouble() ) {
						hashCode = Double.hashCode( value.doubleValueStrict() );
					} else if( value.isBool() ) {
						hashCode = Boolean.hashCode( value.boolValueStrict() );
					} else if( value.isLong() ) {
						hashCode = Long.hashCode( value.longValueStrict() );
					} else {
						hashCode = java.util.Objects.hashCode( value.valueObject() );
					}
				}
			} catch( TypeCastingException ignored ) {
			}
			/*
			 * Similarly to java collections, each increment is weighted to spread out codes. To guarantee
			 * correctness of the result wrt equals, the order in which children are visited must be
			 * deterministic hence, keys are sorted.
			 */
			SortedSet< String > keys = new TreeSet<>( value.children().keySet() );
			for( String key : keys ) {
				for( Value v : value.getChildren( key ) ) {
					hashCode = 31 * hashCode + hash( v );
				}
			}
		}
		return hashCode;
	}

}
