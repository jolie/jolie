/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
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
package jolie.net.coap.options;

import java.math.BigInteger;
import java.util.Arrays;

public class UintOptionValue extends OptionValue<Long> {

    public static final long UNDEFINED = -1;

    public UintOptionValue(int optionNumber, byte[] value)
	    throws IllegalArgumentException {
	this(optionNumber, shortenValue(value), false);
    }

    public UintOptionValue(int optionNumber, byte[] value, boolean allowDefault)
	    throws IllegalArgumentException {
	super(optionNumber, shortenValue(value), allowDefault);
    }

    @Override
    public Long getDecodedValue() {
	return new BigInteger(1, value).longValue();
    }

    @Override
    public int hashCode() {
	return getDecodedValue().hashCode();
    }

    @Override
    public boolean equals(Object object) {
	if (!(object instanceof UintOptionValue)) {
	    return false;
	}

	UintOptionValue other = (UintOptionValue) object;
	return Arrays.equals(this.getValue(), other.getValue());
    }

    public static byte[] shortenValue(byte[] value) {
	int index = 0;
	while (index < value.length - 1 && value[index] == 0) {
	    index++;
	}

	return Arrays.copyOfRange(value, index, value.length);
    }
}
