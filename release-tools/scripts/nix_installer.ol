/***********************************************************************************
 *   Copyright (C) 2014 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>     *
 *   Copyright (C) 2014 by Fabrizio Montesi <famontesi@gmail.com>                  *
 *                                                                                 *
 *   This program is free software; you can redistribute it and/or modify          *
 *   it under the terms of the GNU Library General Public License as               *
 *   published by the Free Software Foundation; either version 2 of the            *
 *   License, or (at your option) any later version.                               *
 *                                                                                 *
 *   This program is distributed in the hope that it will be useful,               *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of                *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                 *
 *   GNU General Public License for more details.                                  *
 *                                                                                 *
 *   You should have received a copy of the GNU Library General Public             *
 *   License along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                               *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                     *
 *                                                                                 *
 *   For details about the authors of this software, see the AUTHORS file.         *
 ***********************************************************************************/

/**
 * Fabrizio Montesi (25 Nov 2014): Added checks for existing directory on removal, refactoring of Self
 */

include "exec.iol"
include "console.iol"
include "file.iol"
include "string_utils.iol"
include "runtime.iol"
include "inst_interface.iol"

execution{ concurrent }

constants
{
	DEFAULT_JOLIE_HOME = "/usr/lib/jolie",
	DEFAULT_LAUNCHERS_PATH = "/usr/bin",
	LAUNCHERS_PATH = "launchers/unix"
}

inputPort In {
Location: "local"
Interfaces: InstInterface
}

outputPort Self {
Interfaces: InstInterface
}

init
{
	getServiceDirectory@File()( cd );
	getLocalLocation@Runtime()( Self.location )
}

main
{
	[ normalisePath( path )( path ) {
		trim@StringUtils( path )( path );
		e = "sh";
		e.args[#e.args] = "-c";
		e.args[#e.args] = "echo " + path;
		exec@Exec( e )( result );
		path = result;
		trim@StringUtils( path )( path )
	} ] { nullProcess }

	[ getDJH()( DEFAULT_JOLIE_HOME ) { nullProcess } ] { nullProcess }

	[ getDLP()( DEFAULT_LAUNCHERS_PATH ){ nullProcess } ]{ nullProcess }

	[ installationFinished( jh )() {
		println@Console( "\n"
			+ "Please, open a new shell and execute command\n"
	 		+	"  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n"
	 		+ "  V V V V V V V V V V V V V V V V V V V V V V V V V V V V V V V\n\n"
	 		+ "  echo 'export JOLIE_HOME=\"" + jh + "\"' >> ~/.bash_profile" 
	 		+ "\n\n"
	 		+	"  ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^\n"
	 		+ "  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - "
		)()
	} ] { nullProcess }
	
	[ deleteDir( dir )() {
		e = "sh";
		e.args[#e.args] = "-c";
		e.args[#e.args] = "rm -rf " + dir;
		e.waitFor = 1;
		exec@Exec( e )( e_res )
	} ] { nullProcess }
	
	[ mkdir( dir )() {
		e = "sh";
		e.args[#e.args] = "-c";
		e.args[#e.args] = "mkdir -p " + dir;
		e.waitFor = 1;
		exec@Exec( e )( e_res )
	} ] { nullProcess }

	[ copyBins( bin_folder )(){
		// copy the content of dist/jolie
		e = "sh";
		e.args[#e.args] = "-c";
		e.args[#e.args] = lst = "cp -rp " + cd + "/" + DIST_FOLDER + "/" + 
			JOLIE_FOLDER + "/* " + bin_folder;
		e.waitFor = 1;
		exec@Exec( e )( e_res )
	}]{ nullProcess }
	
	[ copyLaunchers( l_folder )(){
		e = "sh";
		e.args[#e.args] = "-c";
		e.args[#e.args] = "cp -rp " + cd + "/" + DIST_FOLDER + "/" + LAUNCHERS_PATH + 
		"/* " + l_folder;
		e.waitFor = 1;
		exec@Exec( e )( e_res )
	}]{ nullProcess }
}
