include "../AbstractTestUnit.iol"
include "metarender.iol"
include "metajolie.iol"
include "file.iol"
include "runtime.iol"
include "string_utils.iol"

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
  getSurface@MetaRender( meta_description.input )( surface  )
  f.filename = "library/private/sample_service.ol"
  readFile@File( f )( testservice )
  replace_str = testservice
  replace_str.regex = "SampleInterface.iol"
  replace_str.replacement = "../SampleInterface.iol"
  replaceAll@StringUtils( replace_str )( testserice_final )
  mkdir@File( TMPDIR )(  )
  testservice = surface + "\n" + testserice_final
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
}