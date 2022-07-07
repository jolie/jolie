/***************************************************************************
 *   Copyright 2014 (C) by Fabrizio Montesi <famontesi@gmail.com>          *
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

include "exec.iol"
include "file.iol"
include "console.iol"

constants {
	ReleaseDir = "release"
}

// Example usage: jolie release.ol ../branches/jolie_1_0

define exec
{
	exec@Exec( e )()
}

define compile
{
	e = "mvn";
	e.args[0] = "install";
	e.workingDirectory = args[0];
	e.waitFor = 1;
	e.stdOutConsoleEnable = true;
	exec;
	undef( e )
}

define recreateReleaseDir
{
	e = "rm";
	e_arg = "-rf";
	e_arg = ReleaseDir;
	e.waitFor = 1;
	e.stdOutConsoleEnable = true;
	exec@Exec( e )();

	undef( e.args );
	e = "mkdir";
	e_arg = ReleaseDir;
	exec;


	undef( e )
}

define reset_args
{
	undef( e.args )
}

main
{
	e_arg -> e.args[#e.args];
//	compile;

	recreateReleaseDir;

	e = "cp";
	e_arg = "-rp";
	e_arg = args[0] + "/dist";
	e_arg = ReleaseDir + "/dist";
	e.waitFor = 1;
	exec;


	if( #args < 1 ) {
		println@Console( "Usage `jolie release.ol /path/to/jolie/dist/directory`\n" +
			"INFO: The release assembler tool has been launched without\n" +
			"specifying the source folder for the Jolie distributable binaries.\n" +
			"Using the default one: "  + e.args[1]
  	)()
	} else {
		println@Console( "INFO: the release assembler tool has been launched on \n" +
			"source folder for the Jolie distributable binaries: " + e.args[1]
		)()
	};
	// println@Console( "Press any key to proceed..." )();
	// registerForInput@Console()();
	// in();

	reset_args;
	e_arg = "installer/dist/jolie-installer.jar";
    if ( #args == 2 ) {
        e_arg = ReleaseDir + "/" + args[1] + ".jar"
    } else {
        e_arg = ReleaseDir + "/jolie-installer.jar"
    };
    // e_arg = ReleaseDir + "/jolie_installer.jar";
	exec;

	reset_args;
	e = "zip";
	e.workingDirectory = "release";
	e_arg = "-r";
	e_arg = "dist.zip";
	e_arg = "dist";
	exec;
	undef( e.workingDirectory );

	lq.directory = "scripts";
	lq.regex = ".+\\.i?ol";
	list@File( lq )( lr );
	reset_args;
	e = "zip";
	e.workingDirectory = "release";
	e_arg = "-j";
	e_arg = "installer.zip";
	for( i = 0, i < #lr.result, i++ ) {
		e_arg = "../" + lq.directory + "/" + lr.result[ i ]
	};
	exec;
	undef( e.workingDirectory );

	reset_args;
	e = "jar";
	e_arg = "uvf";
	// e_arg = ReleaseDir + "/jolie_installer.jar";
	if ( #args == 2 ) {
        e_arg = ReleaseDir + "/" + args[1] + ".jar"
    } else {
        e_arg = ReleaseDir + "/jolie-installer.jar"
    };
	// e_arg = "scripts/MANIFEST.MF";
	e_arg = "-C";
	e_arg = ReleaseDir;
	e_arg = "dist.zip";
	e_arg = "-C";
	e_arg = ReleaseDir;
	e_arg = "installer.zip";
	e_arg = "-C";
	e_arg = ReleaseDir + "/dist/jolie";
	e_arg = "jolie.jar";
	e_arg = "-C";
	e_arg = ReleaseDir + "/dist/jolie";
	e_arg = "jolie-cli.jar";
	e_arg = "-C";
	e_arg = ReleaseDir + "/dist/jolie";
	e_arg = "lib/libjolie.jar";
	e_arg = "-C";
	e_arg = ReleaseDir + "/dist/jolie";
	e_arg = "lib/automaton.jar";
	e_arg = "-C";
	e_arg = ReleaseDir + "/dist/jolie";
	e_arg = "lib/jolie-js.jar";
	e_arg = "-C";
	e_arg = ReleaseDir + "/dist/jolie";
	e_arg = "lib/json-simple.jar";
	exec
}
