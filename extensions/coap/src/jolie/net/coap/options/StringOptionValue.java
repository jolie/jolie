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

import io.netty.util.CharsetUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import java.util.Arrays;
import java.util.Locale;

public class StringOptionValue extends OptionValue<String> {

    private static final Charset charset = CharsetUtil.UTF_8;

    public StringOptionValue(int optionNumber, byte[] value)
	    throws IllegalArgumentException {
	this(optionNumber, value, false);
    }

    public StringOptionValue(int optionNumber, byte[] value,
	    boolean allowDefault) throws IllegalArgumentException {
	super(optionNumber, value, allowDefault);
    }

    public StringOptionValue(int optionNumber, String value)
	    throws IllegalArgumentException {

	this(optionNumber, optionNumber == Option.URI_HOST
		? convertToByteArrayWithoutPercentEncoding(
			value.toLowerCase(Locale.ENGLISH))
		: ((optionNumber == Option.URI_PATH
		|| optionNumber == Option.URI_QUERY)
			? convertToByteArrayWithoutPercentEncoding(value)
			: value.getBytes(charset)));
    }

    @Override
    public String getDecodedValue() {
	return new String(value, charset);
    }

    @Override
    public int hashCode() {
	return getDecodedValue().hashCode();
    }

    @Override
    public boolean equals(Object object) {
	if (!(object instanceof StringOptionValue)) {
	    return false;
	}

	StringOptionValue other = (StringOptionValue) object;
	return Arrays.equals(this.getValue(), other.getValue());
    }

    public static byte[] convertToByteArrayWithoutPercentEncoding(String s)
	    throws IllegalArgumentException {

	ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes(charset));
	ByteArrayOutputStream out = new ByteArrayOutputStream();

	int i;

	do {
	    i = in.read();
	    if (i == -1) {
		break;
	    }
	    if (i == 0x25) {
		int d1 = Character.digit(in.read(), 16);
		int d2 = Character.digit(in.read(), 16);

		if (d1 == -1 || d2 == -1) {
		    throw new IllegalArgumentException("Invalid percent "
			    + "encoding in: " + s);
		}

		out.write((d1 << 4) | d2);
	    } else {
		out.write(i);
	    }

	} while (true);

	byte[] result = out.toByteArray();

	return result;
    }
}
