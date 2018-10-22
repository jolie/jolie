/*******************************************************************************
 *   Copyright (C) 2014-2016 Fabrizio Montesi <famontesi@gmail.com>            *
 *   Copyright (C) 2016 by Claudio Guidi <guidiclaudio@gmail.com>              *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package jolie.net.auto;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import jolie.util.Helpers;
import org.ini4j.Ini;

/**
 *
 * @author claudio
 */
public class AutoHelper {

    public static IOException buildIOException(String message) {
        return new IOException("autoconf: " + message);
    }

    public static void throwIOException(String message)
            throws IOException {
        throw buildIOException(message);
    }

    public static void assertIOException(boolean condition, String message)
            throws IOException {
        Helpers.condThrow(condition, buildIOException(message));
    }

    public static String getLocationFromIni(String iniLocation)
            throws IOException {
        // Format: "/Section/Key:URL_to_ini"
        String[] ss = iniLocation.split(":", 2);
        assertIOException(ss.length < 2, "invalid ini location; the format is /Section/Key:URL_to_ini");

        String[] iniPath = ss[0].split("/", 3);
        assertIOException(iniPath.length < 3, "path to ini content is not well-formed; the format is /Section/Key");

        URL iniURL = new URL(ss[1]);
        Ini ini = new Ini(new InputStreamReader(iniURL.openStream()));

        Ini.Section section = ini.get(iniPath[1]);
        assertIOException(section == null, "could not find section " + iniPath[1] + " in ini");

        String retLocation = section.get(iniPath[2]);
        assertIOException(retLocation == null, "could not find key " + iniPath[2] + " in section " + iniPath[1] + " in ini");

        return retLocation;
    }
}
