/*****************************************************************************
 *  Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  *
 *                                                                           *
 *  This program is free software; you can redistribute it and/or modify     *
 *  it under the terms of the GNU Library General Public License as          *
 *  published by the Free Software Foundation; either version 2 of the       *
 *  License, or (at your option) any later version.                          *
 *                                                                           *
 *  This program is distributed in the hope that it will be useful,          *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 *  GNU General Public License for more details.                             *
 *                                                                           *
 *  You should have received a copy of the GNU Library General Public        *
 *  License along with this program; if not, write to the                    *
 *  Free Software Foundation, Inc.,                                          *
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                *
 *                                                                           *
 *  For details about the authors of this software, see the AUTHORS file.    *
 *****************************************************************************/

include "../AbstractTestUnit.iol"
include "file.iol"
include "console.iol"
include "string_utils.iol"
include "tquery.iol"

init
{
  /* read from a directory containing json files that populate our database */
  getServiceDirectory@File()( serviceDirectory );
  list@File( {
    .regex = ".*\\.json",
    .directory = serviceDirectory + "/private/",
    .info = true
  } )( listResponse );
  for ( file in listResponse.result ) {
    readFile@File( {
      .filename = file.info.absolutePath,
      .format = "json"
    } )( readFileResponse );
    bios[ readFileResponse._id ] << readFileResponse
  }
}

include "private/match.ol"
include "private/unwind.ol"
include "private/project.ol"
include "private/group.ol"
include "private/lookup.ol"

define doTest
{
  // scope( doMatch )
  // {
  //  install(
  //    IllegalArgumentException =>
  //     valueToPrettyString@StringUtils( doMatch )( t );
  //     println@Console( "IllegalArgumentException: " + t )()
  //    );
   //  match;
   //  match@TQuery( matchRequest )( matchResponse );
   //  undef( matchRequest );
   //  match_bool;
   //  match@TQuery( matchRequest )( matchResponse );
   //  undef( matchRequest );
   //  match_bool_bis;
   //  match@TQuery( matchRequest )( matchResponse );
   //  undef( matchRequest );
   //  match_equal_value;
   //  match@TQuery( matchRequest )( matchResponse );
   //  undef( matchRequest );
   //  match_equal_value_bis;
   //  match@TQuery( matchRequest )( matchResponse )

  // }

  scope( doUnwind )
  {
    // install(
    //   TypeMismatch =>
    //     valueToPrettyString@StringUtils( doUnwind )( t );
    //     println@Console( "TypeMismatch: " + t )()
    // );
    unwind;
    unwind@TQuery( unwindRequest )( unwindResponse )
  }

  // project_path;
  // project@TQuery( projectRequest )( projectResponse );
  // undef( projectRequest );
  // project_value;
  // project@TQuery( projectRequest )( projectResponse );

  // group;
  // group@TQuery( groupRequest )( groupResponse );

  // lookup;
  // lookup@TQuery( lookupRequest )( lookupResponse )

}
