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
import org.junit.jupiter.api.Test;

import jolie.net.CommMessage;
import jolie.runtime.Value;
import jolie.util.metadata.MetadataKey;

class CommMessageTests {
	@Test
	void testMetadata() {
		CommMessage message = CommMessage.createRequest( "op", "/", Value.create() );
		MetadataKey< String > key = MetadataKey.of( "testKey", String.class );

		String expectedValue = "testValue";
		message.metadata().put( key, expectedValue );

		String actualValue = message.metadata().get( key );

		assertEquals( expectedValue, actualValue );
	}
}
