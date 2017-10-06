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
package jolie.net.coap;

import java.util.Arrays;

public class OpaqueOptionValue extends OptionValue<byte[]> {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public OpaqueOptionValue(int optionNumber, byte[] value)
	    throws IllegalArgumentException {
	super(optionNumber, value, false);
    }

    @Override
    public byte[] getDecodedValue() {
	return this.value;
    }

    @Override
    public int hashCode() {
	return Arrays.hashCode(getDecodedValue());
    }

    @Override
    public boolean equals(Object object) {
	if (!(object instanceof OpaqueOptionValue)) {
	    return false;
	}

	OpaqueOptionValue other = (OpaqueOptionValue) object;
	return Arrays.equals(this.getValue(), other.getValue());
    }

    @Override
    public String toString() {
	return toHexString(this.value);
    }

    public static String toHexString(byte[] bytes) {
	if (bytes.length == 0) {
	    return "<empty>";
	} else {
	    return "0x" + bytesToHex(bytes);
	}
    }

    private static String bytesToHex(byte[] bytes) {
	char[] hexChars = new char[bytes.length * 2];
	for (int j = 0; j < bytes.length; j++) {
	    int v = bytes[j] & 0xFF;
	    hexChars[j * 2] = hexArray[v >>> 4];
	    hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	}
	return new String(hexChars);
    }
}
