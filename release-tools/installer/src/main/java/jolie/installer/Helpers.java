/***************************************************************************
 *   Copyright (C) 2014 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.installer;

/**
 * A convenience class with some helper functions for cleaner coding.
 * @author Fabrizio Montesi
 */
public class Helpers
{
	
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
}
