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
import java.util.concurrent.locks.ReentrantLock;

/**
 * A convenience class with some helper functions for cleaner coding.
 * @author Fabrizio Montesi
 */
public class Helpers
{
	public final static long serialVersionUID = jolie.lang.Constants.serialVersionUID();
	
	public static <B extends Comparable<Boolean>, T extends Throwable> void condThrow( B condition, T throwable )
		throws T
	{
		if ( condition.compareTo( true ) == 0 ) {
			throw throwable;
		}
	}
	
	public static <B extends Comparable<Boolean>> void condThrowIOException( B condition, String message )
		throws IOException
	{
		if ( condition.compareTo( true ) == 0 ) {
			throw new IOException( message );
		}
	}
	
	public static enum OSType {	Windows, MacOS, Linux, Other }
	
	private static final OSType detectedOS;
	
	static {
		final String os = System.getProperty( "os.name", "other" ).toLowerCase();
		if ( os.contains("mac") || os.contains("darwin") ) {
			detectedOS = OSType.MacOS;
		} else if ( os.contains("win") ) {
			detectedOS = OSType.Windows;
		} else if ( os.contains("nux") ) {
			detectedOS = OSType.Linux;
		} else {
			detectedOS = OSType.Other;
		}
	}
	
	public static OSType getOperatingSystemType()
	{
		return detectedOS;
	}
	
	/**
	 * Acquires lock if the current thread does not hold it already, executes code and returns.
	 * The passed lambda may throw an exception, which is then thrown by this method.
	 * 
	 * @param <T>
	 * @param lock
	 * @param code
	 * @throws T
	 */
	public static <T extends Throwable> void lockAndThen( ReentrantLock lock, ExceptionalRunnable<T> code )
		throws T
	{
		if ( lock.isHeldByCurrentThread() ) {
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
	 * Acquires lock if the current thread does not hold it already, executes code and returns.
	 * The passed lambda may throw an exception, which is then thrown by this method.
	 * @param <R>
	 * @param <T>
	 * @param lock
	 * @param code
	 * @return 
	 * @throws T 
	 */
	public static <R, T extends Throwable> R lockAndThen( ReentrantLock lock, ExceptionalCallable<R, T> code )
		throws T
	{
		final R ret;
		if ( lock.isHeldByCurrentThread() ) {
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
}
