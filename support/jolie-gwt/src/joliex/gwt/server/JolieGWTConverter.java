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

package joliex.gwt.server;

import java.util.Map.Entry;

public class JolieGWTConverter
{
	public static void gwtToJolieValue( joliex.gwt.client.Value value, jolie.runtime.Value retValue )
	{
		if ( value.isString() ) {
			retValue.setValue( value.strValue() );
		} else if ( value.isInt() ) {
			retValue.setValue( value.intValue() );
		} else if ( value.isDouble() ) {
			retValue.setValue( value.doubleValue() );
		} else if ( value.isLong() ) {
			retValue.setValue( value.longValue() );
		} else if ( value.isBool() ) {
			retValue.setValue( value.boolValue() );
		} else if ( value.isByteArray() ) {
			retValue.setValue( new jolie.runtime.ByteArray( value.byteArrayValue().getBytes() ) );
		}

		jolie.runtime.ValueVector vec;
		jolie.runtime.Value valueToAdd;
		for( Entry< String, joliex.gwt.client.ValueVector > entry : value.children().entrySet() ) {
			vec = retValue.getChildren( entry.getKey() );
			for( joliex.gwt.client.Value v : entry.getValue() ) {
				valueToAdd = jolie.runtime.Value.create();
				vec.add( valueToAdd );
				gwtToJolieValue( v, valueToAdd );
			}
		}
	}
	
	public static joliex.gwt.client.FaultException jolieToGwtFault( jolie.runtime.FaultException fault )
	{
		joliex.gwt.client.Value v = new joliex.gwt.client.Value();
		if ( fault.value() != null ) {
			jolieToGwtValue( fault.value(), v );
		}
		return new joliex.gwt.client.FaultException( fault.faultName(), v );
	}
	
	public static void jolieToGwtValue( jolie.runtime.Value value, joliex.gwt.client.Value retValue )
	{	
		if ( value.isString() ) {
			retValue.setValue( value.strValue() );
		} else if ( value.isInt() ) {
			retValue.setValue( value.intValue() );
		} else if ( value.isDouble() ) {
			retValue.setValue( value.doubleValue() );
		} else if ( value.isLong() ) {
			retValue.setValue( value.longValue() );
		} else if ( value.isBool() ) {
			retValue.setValue( value.boolValue() );
		} else if ( value.valueObject() != null ) {
			retValue.setValue( value.valueObject().toString() );
		}
		
		joliex.gwt.client.ValueVector vec;
		joliex.gwt.client.Value valueToAdd;
		for( Entry< String, jolie.runtime.ValueVector > entry : value.children().entrySet() ) {
			vec = retValue.getChildren( entry.getKey() );
			for( jolie.runtime.Value v : entry.getValue() ) {
				valueToAdd = new joliex.gwt.client.Value();
				vec.add( valueToAdd );
				jolieToGwtValue( v, valueToAdd );
			}
		}
	}
}
