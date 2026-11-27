/*
 * Copyright (C) 2026 The Jolie developers
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

package jolie.tracer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class PrintingTracerSanitizeTest {
	@Test
	public void dropsEscapeSequences() {
		assertEquals( "a[31mred[0m", PrintingTracer.sanitizeForConsole( "a\u001b[31mred\u001b[0m" ) );
	}

	@Test
	public void dropsCarriageReturnAndNul() {
		assertEquals( "line1line2", PrintingTracer.sanitizeForConsole( "line1\rline2" ) );
		assertEquals( "ab", PrintingTracer.sanitizeForConsole( "a\u0000b" ) );
	}

	@Test
	public void keepsNewlineAndTab() {
		assertEquals( "a\n\tb", PrintingTracer.sanitizeForConsole( "a\n\tb" ) );
	}

	@Test
	public void replacesUnpairedSurrogates() {
		assertEquals( "a\uFFFDb", PrintingTracer.sanitizeForConsole( "a\uD800b" ) );
		assertEquals( "a\uFFFDb", PrintingTracer.sanitizeForConsole( "a\uDC00b" ) );
	}

	@Test
	public void keepsPairedSurrogates() {
		assertEquals( "a\uD83D\uDE00b", PrintingTracer.sanitizeForConsole( "a\uD83D\uDE00b" ) );
	}
}
