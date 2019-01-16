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

include "private/match.ol"
include "private/unwind.ol"
include "private/project.ol"
include "private/group.ol"
include "private/lookup.ol"

define doTest
{
  install( default => valueToPrettyString@StringUtils( main )( t ); println@Console( t )() );
  /* read from a directory containing json files that populate our database */
  getServiceDirectory@File()( serviceDirectory );
  getFileSeparator@File()( sep );
  datafilesDirectory = serviceDirectory + sep + "private";
  list@File( {
    .regex = ".*\\.json",
    .directory = datafilesDirectory
  } )( listResponse );
  for ( file in listResponse.result ) {
    readFile@File( {
      .filename = datafilesDirectory + sep + file,
      .format = "json"
    } )( readFileResponse );
    bios[ readFileResponse._id ] << readFileResponse
  };
  scope( doMatch )
  {
   install(
     IllegalArgumentException =>
      valueToPrettyString@StringUtils( doMatch )( t );
      println@Console( "IllegalArgumentException: " + t )()
     );
    match;
    match@TQuery( matchRequest )( matchResponse );
    undef( matchRequest );
    match_bool;
    match@TQuery( matchRequest )( matchResponse );
    undef( matchRequest );
    match_bool_bis;
    match@TQuery( matchRequest )( matchResponse );
    undef( matchRequest );
    match_equal_value;
    match@TQuery( matchRequest )( matchResponse );
    undef( matchRequest );
    match_equal_value_bis;
    match@TQuery( matchRequest )( matchResponse )

  };

  scope( doUnwind )
  {
    install(
      TypeMismatch =>
        valueToPrettyString@StringUtils( doUnwind )( t );
        println@Console( "TypeMismatch: " + t )()
    );
    unwind_bios;
    unwind@TQuery( unwindRequest )( unwindResponse )
  }
  ;

  scope( doProject )
  {
    install(
      TypeMismatch =>
        valueToPrettyString@StringUtils( doProject )( t );
        println@Console( "TypeMismatch: " + t )()
    );
    projectRequest.data << bios;
    resetQuery;
    project_path;
    project@TQuery( projectRequest )( projectResponse );
    valueToPrettyString@StringUtils( projectResponse )( t );
    println@Console( t )();
    resetQuery;
    project_value;
    project@TQuery( projectRequest )( projectResponse );
    valueToPrettyString@StringUtils( projectResponse )( t );
    println@Console( t )();
    resetQuery;
    project_value_path;
    project@TQuery( projectRequest )( projectResponse );
    valueToPrettyString@StringUtils( projectResponse )( t );
    println@Console( t )();
    resetQuery;
    project_value_match;
    project@TQuery( projectRequest )( projectResponse );
    valueToPrettyString@StringUtils( projectResponse )( t );
    println@Console( t )();
    resetQuery;
    project_value_ternary;
    project@TQuery( projectRequest )( projectResponse );
    valueToPrettyString@StringUtils( projectResponse )( t );
    println@Console( t )();
    resetQuery;
    scope( failing ){
      install( MergeValueException => 
      valueToPrettyString@StringUtils( failing )( t );
        println@Console( "MergeValueException: " + t )()
      );
      failing_project_value_chain;
      project@TQuery( projectRequest )( projectResponse );
      valueToPrettyString@StringUtils( projectResponse )( t );
      println@Console( t )()  
    };
    resetQuery;
    successful_project_value_chain;
    project@TQuery( projectRequest )( projectResponse );
    valueToPrettyString@StringUtils( projectResponse )( t );
    println@Console( t )()  
  }
  // ;
  //
  // scope( doGroup )
  // {
  //   install(
  //     TypeMismatch =>
  //       valueToPrettyString@StringUtils( doGroup )( t );
  //       println@Console( "TypeMismatch: " + t )()
  //   );
  //   group;
  //   group@TQuery( groupRequest )( groupResponse )
  // };
  //
  // scope( doLookup )
  // {
  //   install(
  //     TypeMismatch =>
  //       valueToPrettyString@StringUtils( doLookup )( t );
  //       println@Console( "TypeMismatch: " + t )()
  //   );
  //   lookup;
  //   lookup@TQuery( lookupRequest )( lookupResponse )
  // }
}
