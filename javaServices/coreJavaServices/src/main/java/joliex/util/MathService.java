/***************************************************************************
 *   Copyright (C) 2009-2016 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package joliex.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

/**
 * JavaService offering various math related functionalities.
 *
 * @author Fabrizio Montesi
 */
public class MathService extends JavaService {
	public Double pi() {
		return Math.PI;
	}

	public Double random() {
		return Math.random();
	}

	public Integer abs( Integer i ) {
		return Math.abs( i );
	}

	public Double pow( Value request ) {
		return Math.pow( request.getFirstChild( "base" ).doubleValue(),
			request.getFirstChild( "exponent" ).doubleValue() );
	}

	public Integer summation( Value request ) {
		int from = request.getFirstChild( "from" ).intValue();
		int to = request.getFirstChild( "to" ).intValue();
		int result = 0;
		while( from <= to ) {
			result += from;
			from++;
		}
		return result;
	}

	/**
	 * @author Claudio Guidi
	 * @author Fabrizio Montesi
	 * @param v
	 * @return
	 */
	public Double round( Value v ) {
		int decimals = 0;
		if( v.hasChildren( "decimals" ) ) {
			decimals = v.getFirstChild( "decimals" ).intValue();
		}
		double orig = v.doubleValue();
		double power = Math.pow( 10, decimals );
		double ret = orig * power;
		if( ret == Double.POSITIVE_INFINITY ) {
			BigDecimal b = BigDecimal.valueOf( orig );
			b = b.setScale( decimals, RoundingMode.HALF_UP );
			ret = b.doubleValue();
		} else {
			ret = Math.round( ret ) / power;
		}

		return ret;
	}
}
