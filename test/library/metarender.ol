include "../AbstractTestUnit.iol"
include "metarender.iol"
include "metajolie.iol"
include "file.iol"
include "runtime.iol"
include "string_utils.iol"
include "console.iol"
include "message_digest.iol"

outputPort Test {
    Location: "socket://localhost:9000"
    Protocol: sodep
    RequestResponse: tmp
}

constants {
    TMPDIR = "library/private/tmp"
}

define doTest
{
  with( rq ) {
      .filename = "private/sample_service.ol"
  };
  getInputPortMetaData@MetaJolie( rq )( meta_description )
  getSurfaceWithoutOutputPort@MetaRender( meta_description.input )( surface  )
  f.filename = "library/private/sample_service.ol"
  readFile@File( f )( testservice )
  replace_str = testservice
  replace_str.regex = ".SampleInterface"
  replace_str.replacement = "..SampleInterface"
  replaceAll@StringUtils( replace_str )( testservice_final )
  mkdir@File( TMPDIR )(  )
  testservice = surface + "\n" + testservice_final
  f.filename = "library/private/tmp/metarendertest.ol"
  f.content = testservice
  writeFile@File( f )( )
  del = true
  scope( s ) {
	//install( default => deleteDir@File( TMPDIR )(); del = false; throw( TestFailed, "error with the rendered test service" ) )
	loadEmbeddedService@Runtime( { .filepath = f.filename, .type = "Jolie" } )( )
	tmp@Test()()
  }
  if ( del ) {
	  deleteDir@File( TMPDIR )()
  }  

  getInputPort@MetaRender( meta_description.input )( ip )
  md5@MessageDigest( ip )( md5ip )
  check_ip = "c49711878ebc23c48e5176f40dfa4063"
  if ( md5ip != check_ip ) {
    throw( TestFailed, "wrong generation of InputPort, expected\n\n" + check_ip + "\n\nfound\n\n" + md5ip )
  }
  rq.filename = "private/sample_service2.ol"
  getOutputPortMetaData@MetaJolie( rq )( meta_description )
  found_index = 0
  for ( o = 0, o < #meta_description.output, o++ ) {
    if ( meta_description.output[ o ].name == "TPort2" ) {
       found_index = o
    }
  }
  getOutputPort@MetaRender( meta_description.output[ found_index ] )( op )
  md5@MessageDigest( op )( md5op )
  check_op = "ee20273319da26e2107490da0c51e57c"
  if ( md5op != check_op ) {
    throw( TestFailed, "wrong generation of OutputPort, expected\n\n" + check_op + "\n\nfound\n\n" + md5op + ", found plain text:" + op )
  }

  // refinedTypes
  undef( rq )
  rq.filename = "private/sample_service_refined_types.ol"
  getInputPortMetaData@MetaJolie( rq )( meta_description )
  getInterface@MetaRender( meta_description.input.interfaces )( interface_string )
  md5@MessageDigest( interface_string )( md5intf )
  check_op = "67af1f0213d6daf96383d335b412a57b"
  if ( md5intf != check_op ) {
    throw( TestFailed, "wrong generation of interface with refined types, expected\n\n" + check_op + "\n\nfound\n\n" + md5intf + ", found plain text:" + interface_string )
  }



  /* expected 
type T1:void {
  .f6[1,1]:double( ranges( [4.0,5.0],[10.0,20.0],[100.0,200.0],[300.0,*]) )
  .f7[1,1]:string( regex( ".*@.*\\..*" ) )
  .f1[1,1]:string( length( [ 0,10 ] ) )
  .f2[1,1]:string( enum(["hello","homer","simpsons" ] ) )
  .f3[1,1]:string( length( [ 0,20 ] ) )
  .f4[1,1]:int( ranges( [1,4],[10,20],[100,200],[300,*]) )
  .f5[1,1]:long( ranges( [3,4],[10,20],[100,200],[300,*]) )
}

interface TmpInterface {
RequestResponse:
  tmp( T1 )( T1 )
}

  */

}