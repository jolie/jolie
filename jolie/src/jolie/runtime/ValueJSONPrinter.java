/*
 * Copyright (C) 2017 Vins.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package jolie.runtime;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 *
 * @author Vins
 */
public class ValueJSONPrinter {
    
	private final Value root;
	private final Writer writer;
	//private int indentation = 0;
	private int byteTruncation = -1; // Default: no truncation

	public ValueJSONPrinter( Value root, Writer writer)
	{
		this.root = root;
		this.writer = writer;
	}

	public void setByteTruncation( int truncation )
	{
		this.byteTruncation = truncation;
	}

	public void run()
		throws IOException
	{
		writeNativeValue( root );
		writeChildren( root );
	}
	
	private void writeNativeValue( Value value )
		throws IOException
	{
		if ( value.isUsedInCorrelation() ) {
			writer.write( "{\"InCorrelation\" : \"true" );
		}else{
                    
			writer.write( "{\"InCorrelation\" : \"false" );
                }
		if ( value.valueObject() != null ) {
                    
			if ( value.valueObject() instanceof ByteArray && byteTruncation > -1 ) {
				String s = value.byteArrayValue().toString();
                                writer.write("\",\"VariableValue\":\"" + ( s.substring( 0, Math.min( byteTruncation, s.length() ) ) + "..." ) + "\",");
			} else {
                            
                                writer.write("\",\"VariableValue\":\"" +  value.valueObject().toString() + "\"");
			}
                        writer.write(",\"ValueType\":\"" +  value.valueObject().getClass().getName());
		}else{
                    
                        writer.write("\",\"ValueType\":\"None");
                }
	}

	private void writeChildren( Value value )
		throws IOException
	{
		Integer i;
		for( Map.Entry< String, ValueVector > entry : value.children().entrySet() ) {
			if ( entry.getValue().isEmpty() ) {
                            writer.write("\",\"VariableNameEmpty\" : \""+ entry.getKey() +"\",");
			} else {
				i = 0;
				for( Value child : entry.getValue() ) {
                                    
                            writer.write("\",\"VariableName\" : \""+ entry.getKey() +" \", \"VariableArrayPosition\": \""+  i.toString()+"\"},");
					writeNativeValue( child );
					writeChildren( child );
					i++;
				}
			}
		}
	}
}
