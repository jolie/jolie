/***************************************************************************
 *   Copyright (C) 2014-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A convenience class with some helper functions for cleaner coding.
 * 
 * @author Fabrizio Montesi
 */
public class Helpers {
	public final static long serialVersionUID = jolie.lang.Constants.serialVersionUID();

	public static String parentFromURL( URL url )
		throws URISyntaxException {
		String parent = null;
		URI uri = url.toURI();
		switch( uri.getScheme() ) {
		case "jap":
		case "file":
		case "jar":
			parent = url.toURI().toString();
			parent = parent.substring( 0, parent.lastIndexOf( '/' ) );
			break;
		}
		return parent;
	}

	@SafeVarargs
	public static < T > Optional< T > firstNonNull( Supplier< T >... suppliers ) {
		T result;
		for( Supplier< T > supplier : suppliers ) {
			result = supplier.get();
			if( result != null ) {
				return Optional.of( result );
			}
		}
		return Optional.empty();
	}

	public static < B extends Comparable< Boolean >, T extends Throwable > void condThrow( B condition, T throwable )
		throws T {
		if( condition.compareTo( true ) == 0 ) {
			throw throwable;
		}
	}

	public static < B extends Comparable< Boolean > > void condThrowIOException( B condition, String message )
		throws IOException {
		if( condition.compareTo( true ) == 0 ) {
			throw new IOException( message );
		}
	}

	public enum OSType {
		WINDOWS, MACOS, LINUX, OTHER
	}

	private static final OSType DETECTED_OS;

	static {
		final String os = System.getProperty( "os.name", "other" ).toLowerCase();
		if( os.contains( "mac" ) || os.contains( "darwin" ) ) {
			DETECTED_OS = OSType.MACOS;
		} else if( os.contains( "win" ) ) {
			DETECTED_OS = OSType.WINDOWS;
		} else if( os.contains( "nux" ) ) {
			DETECTED_OS = OSType.LINUX;
		} else {
			DETECTED_OS = OSType.OTHER;
		}
	}

	public static OSType getOperatingSystemType() {
		return DETECTED_OS;
	}

	public static < T > T ifWindowsOrElse( Supplier< T > supplierWindows, Supplier< T > supplierNotWindows ) {
		if( DETECTED_OS == OSType.WINDOWS ) {
			return supplierWindows.get();
		} else {
			return supplierNotWindows.get();
		}
	}

	/**
	 * Acquires lock if the current thread does not hold it already, executes code and returns. The
	 * passed lambda may throw an exception, which is then thrown by this method.
	 */
	public static < T extends Throwable > void lockAndThen( ReentrantLock lock, ExceptionalRunnable< T > code )
		throws T {
		if( lock.isHeldByCurrentThread() ) {
			code.run();
		} else {
			lock.lock();
			try {
				code.run();
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Acquires lock if the current thread does not hold it already, executes code and returns. The
	 * passed lambda may throw an exception, which is then thrown by this method.
	 */
	public static < R, T extends Throwable > R lockAndThen( ReentrantLock lock, ExceptionalCallable< R, T > code )
		throws T {
		final R ret;
		if( lock.isHeldByCurrentThread() ) {
			ret = code.call();
		} else {
			lock.lock();
			try {
				ret = code.call();
			} finally {
				lock.unlock();
			}
		}
		return ret;
	}

	/**
	 * Tries to acquires lock if the current thread does not hold it already, executes code and returns.
	 * The passed lambda may throw an exception, which is then thrown by this method. If the lock cannot
	 * be acquired, the lambda is not run.
	 */
	public static < T extends Throwable > void tryLockAndThen( ReentrantLock lock, ExceptionalRunnable< T > code )
		throws T {
		if( lock.isHeldByCurrentThread() ) {
			code.run();
		} else if( lock.tryLock() ) {
			try {
				code.run();
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Tries to acquires lock if the current thread does not hold it already, executes code and returns.
	 * The passed lambda may throw an exception, which is then thrown by this method. If the lock cannot
	 * be acquired, the lambda is not run.
	 */
	public static < T1 extends Throwable, T2 extends Throwable > void tryLockOrElse( ReentrantLock lock,
		ExceptionalRunnable< T1 > code, ExceptionalRunnable< T2 > elseCode )
		throws T1, T2 {
		if( lock.isHeldByCurrentThread() ) {
			code.run();
		} else if( lock.tryLock() ) {
			try {
				code.run();
			} finally {
				lock.unlock();
			}
		} else {
			elseCode.run();
		}
	}

	/**
	 * Tries to acquires lock if the current thread does not hold it already, executes code and returns.
	 * The passed lambda may throw an exception, which is then thrown by this method. If the lock cannot
	 * be acquired, the lambda is not run.
	 */
	public static < R, T1 extends Throwable, T2 extends Throwable > R tryLockOrElse( ReentrantLock lock,
		ExceptionalCallable< R, T1 > code, ExceptionalCallable< R, T2 > elseCode )
		throws T1, T2 {
		final R ret;
		if( lock.isHeldByCurrentThread() ) {
			ret = code.call();
		} else if( lock.tryLock() ) {
			try {
				ret = code.call();
			} finally {
				lock.unlock();
			}
		} else {
			ret = elseCode.call();
		}
		return ret;
	}
}
