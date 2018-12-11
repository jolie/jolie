include "../AbstractTestUnit.iol"
include "file.iol"
include "console.iol"
include "string_utils.iol"
include "tquery.iol"

init
{
  /* read from a directory containing json files that populate our database */
  list@File( { .regex = ".*\\.json", .directory = "private" } )( listResponse );
  for ( file in listResponse.result ) {
    readFileRequest << {
      .filename = listRequest.directory + file ,
      .format = "json"
    };
    readFile@File( readFileRequest )( readFileResponse );
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
  scope( doMatch )
  {
  	install( IllegalArgumentException
  		=> 
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
  
  }
  
/*
  unwind;
  // valueToPrettyString@StringUtils( unwindRequest )( ps );
  // println@Console( ps )();
  unwind@TQ( unwindRequest )( unwindResponse );

  // project_path;
  // project@TQ( projectRequest )( projectResponse );
  // undef( projectRequest );
  // project_value;
  // project@TQ( projectRequest )( projectResponse );
  
  // group;
  // group@TQ( groupRequest )( groupResponse );
  
  // lookup;
  // lookup@TQ( lookupRequest )( lookupResponse )
*/
}