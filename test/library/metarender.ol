include "../AbstractTestUnit.iol"
include "metarender.iol"
include "metajolie.iol"
include "file.iol"
include "runtime.iol"

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
  mkdir@File( TMPDIR )(  )
  testservice = surface + "\n" + testservice
  f.filename = "library/private/tmp/metarendertest.ol"
  f.content = testservice
  writeFile@File( f )( )
  loadEmbeddedService@Runtime( { .filepath = f.filename, .type = "Jolie" } )( )
  tmp@Test()()
  //delete@File( f.filename )(  )
  //deleteDir@File( TMPDIR )(  )
  
}