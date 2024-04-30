package joliex.util;

import jolie.runtime.JavaService;
import java.math.BigDecimal;
import interfaces.*;
import types.*;

/**
 * JavaService offering various math related functionalities.
 * 
 * @author Fabrizio Montesi
 */
public final class NewMathService extends JavaService implements MathInterface {

	public Double pi() {
		return Math.PI;
	}

	public Double random() {
		return Math.random();
	}

	public Integer abs( Integer i ) {
		return Math.abs( i );
	}

	public Double pow( PowRequest request ) {
		return Math.pow( request.base(), request.exponent() );
	}

	public Integer summation( SummationRequest request ) {
		int from = request.from();
		int to = request.to();
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
	public Double round( RoundRequestType r ) {
		int decimals = r.decimals().orElse( 0 );
		double orig = r.content().value();
		double power = Math.pow( 10, decimals );
		double ret = orig * power;
		if( ret == Double.POSITIVE_INFINITY ) {
			BigDecimal b = BigDecimal.valueOf( orig );
			b = b.setScale( decimals, BigDecimal.ROUND_HALF_UP );
			ret = b.doubleValue();
		} else {
			ret = Math.round( ret ) / power;
		}

		return ret;
	}
}
