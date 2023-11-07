/*
 * Copyright (C) 2015 Fabrizio Montesi <famontesi@gmail.com>
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
package jolie;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class for managing shared state across different Jolie extensions.
 *
 * @author Fabrizio Montesi
 */
public final class StaticUtils {
	private static final Map< String, Object > SHARED_STATE = new ConcurrentHashMap<>();

	public static void create( Class< ? > holder, Callable< Object > task ) {
		SHARED_STATE.computeIfAbsent( holder.getName(),
			k -> {
				try {
					return task.call();
				} catch( Exception e ) {
					throw new RuntimeException( e );
				}
			} );
	}

	public static < T > T retrieve( Class< ? > holder, Class< T > type ) {
		return type.cast( SHARED_STATE.get( holder.getName() ) );
	}
}
