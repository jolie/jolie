/*
 * Copyright (C) 2023 Fabrizio Montesi <famontesi@gmail.com>
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

package interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import jolie.runtime.Value;

class ValueTests {
	@Test
	void addition() {
		var i1 = 222345;
		var i2 = 551231;
		var v1 = Value.create( i1 );
		var v2 = Value.create( i2 );
		v1.add( v2 );
		assertEquals( v1.intValue(), i1 + i2, "wrong addition result" );
	}

	@Test
	void equalsHashCode() {
		var i1 = 222345;
		var i2 = 551231;
		var v1 = Value.create( i1 );
		var v2 = Value.create( i2 );
		var v3 = Value.create( i1 );
		assertNotEquals( v1, v2, "wrong equals() implementation" );
		assertEquals( v1, v3, "wrong equals() implementation" );
		assertNotEquals( v1, Value.UNDEFINED_VALUE, "wrong equals() implementation" );
		assertNotEquals( v2, Value.UNDEFINED_VALUE, "wrong equals() implementation" );
		assertEquals( Value.UNDEFINED_VALUE, Value.UNDEFINED_VALUE, "wrong equals() implementation" );

		assertNotEquals( v1.hashCode(), v2.hashCode(), "wrong hashCode() implementation" );
		assertEquals( v1.hashCode(), v3.hashCode(), "wrong hashCode() implementation" );
		assertNotEquals( v1.hashCode(), Value.UNDEFINED_VALUE.hashCode(), "wrong hashCode() implementation" );
		assertNotEquals( v2.hashCode(), Value.UNDEFINED_VALUE.hashCode(), "wrong hashCode() implementation" );
		assertEquals( Value.UNDEFINED_VALUE.hashCode(), Value.UNDEFINED_VALUE.hashCode(),
			"wrong hashCode() implementation" );
	}
}
