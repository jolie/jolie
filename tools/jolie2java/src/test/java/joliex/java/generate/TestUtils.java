/**
 * *************************************************************************
 * Copyright (C) 2019 Claudio Guidi	<cguidi@italianasoftware.com>
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * Library General Public License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA. For details about the authors of this software, see the
 * AUTHORS file.
 * *************************************************************************
 */
package joliex.java.generate;

import jolie.lang.Constants;

import java.io.File;
import java.util.Arrays;

public class TestUtils {

	public static void delete( File file ) {
		if ( file.isDirectory() )
			deleteContents( file );

		file.delete();
	}

	public static void deleteContents( File folder ) {
		Arrays.stream( folder.list() ).forEach( f -> {
			final File tmpFile = new File( folder.getPath() + Constants.FILE_SEPARATOR + f );
			
			if( tmpFile.isDirectory() )
				deleteContents( tmpFile );

			tmpFile.delete();
		} );
	}
}
