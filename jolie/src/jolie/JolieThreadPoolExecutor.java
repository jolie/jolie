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

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Fabrizio Montesi
 */
public final class JolieThreadPoolExecutor extends ThreadPoolExecutor
{
	public JolieThreadPoolExecutor( final ThreadFactory factory )
	{
		super(
			0,
			Integer.MAX_VALUE - 8,
			60L,
			TimeUnit.SECONDS,
			new SynchronousQueue<>(),
			factory,
			new ThreadPoolExecutor.AbortPolicy()
		);
	}
}
