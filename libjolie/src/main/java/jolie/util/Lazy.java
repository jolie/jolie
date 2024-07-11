/*
 * Copyright (C) 2024 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.util;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Utility class to store lazily-instantiated values.
 *
 * @author Fabrizio Montesi
 * @param <T> the type of the stored value
 */
public class Lazy< T > implements Supplier< T > {
	private volatile Supplier< T > contentSupplier;

	/**
	 * Constructor
	 *
	 * @param initSupplier code for initialising the value the first time it is requested
	 */
	public Lazy( Supplier< T > initSupplier ) {
		contentSupplier = new Supplier<>() {
			private final ReentrantLock lock = new ReentrantLock();

			@Override
			public T get() {
				lock.lock();
				try {
					if( contentSupplier == this ) {
						T content = initSupplier.get();
						contentSupplier = () -> content;
					}
				} finally {
					lock.unlock();
				}
				return contentSupplier.get();
			}
		};
	}

	/**
	 * Get the present value, or initialise a new one if not already present
	 */
	@Override
	public T get() {
		return contentSupplier.get();
	}
}
