from ..test-unit import TestUnitInterface
from metajolie import MetaJolie
from file import File 
from runtime import Runtime 
from string_utils import StringUtils
from console import Console 
from message_digest import MessageDigest
from metarender-code import MetaRenderCode

interface TmpInterface {
  RequestResponse:
    tmp
}

constants {
    TMPDIR = "library/private/tmp"
}

service Main {

    embed MetaJolie as MetaJolie
    embed StringUtils as StringUtils
    embed Runtime as Runtime
    embed File as File 
    embed Console as Console 
    embed MessageDigest as MessageDigest
    embed MetaRenderCode as MetaRenderCode


    outputPort Test {
      location: "socket://localhost:9000"
      protocol: sodep
      interfaces: TmpInterface
    }

    inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

    main {
        test()() {
          
         
          getInputPortMetaData@MetaJolie({ filename = "private/sample_service.ol" })( meta_description )
          getSurfaceWithoutOutputPort@MetaRenderCode( meta_description.input )( surface  )
          readFile@File({ filename = "library/private/sample_service.ol" })( testservice )
          replaceAll@StringUtils( testservice { regex = "from .SampleInterface import TmpInterface", replacement = "\n" } )( testservice_final )
          replaceAll@StringUtils( testservice_final { regex = "TmpInterface", replacement = "TPortInterface" } )( testservice_final )


          mkdir@File( TMPDIR )()
          filename = "library/private/tmp/metarendertest.ol"
          writeFile@File( {
              filename = filename
              content = surface + "\n" + testservice_final
          } )( )
          del = true
          scope( s ) {
              //install( default => deleteDir@File( TMPDIR )(); del = false; throw( TestFailed, "error with the rendered test service" ) )
              loadEmbeddedService@Runtime( { filepath = filename, type = "Jolie" } )( )
              tmp@Test()()
          }
          if ( del ) {
            deleteDir@File( TMPDIR )()
          }  

          getInputPort@MetaRenderCode( meta_description.input )( ip )
          md5@MessageDigest( ip )( md5ip )
          check_ip = "7792921fad78d69b174b4e25684fc9ce"
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
          getOutputPort@MetaRenderCode( meta_description.output[ found_index ] )( op )
          md5@MessageDigest( op )( md5op )
          check_op = "f24d6a5392c18ce1ddf90e8b336d4135"
          if ( md5op != check_op ) {
            throw( TestFailed, "wrong generation of OutputPort, expected\n\n" + check_op + "\n\nfound\n\n" + md5op + ", found plain text:" + op )
          }

          // refinedTypes
          undef( rq )
          rq.filename = "private/sample_service_refined_types.ol"
          getInputPortMetaData@MetaJolie( rq )( meta_description )
          
        
          
          getInterface@MetaRenderCode( meta_description.input.interfaces )( interface_string )

          

          md5@MessageDigest( interface_string )( md5intf )
          check_op = "67af1f0213d6daf96383d335b412a57b"
          if ( md5intf != check_op ) {
            throw( TestFailed, "wrong generation of interface with refined types, expected\n\n" + check_op + "\n\nfound\n\n" + md5intf + ", found plain text:" + interface_string )
          }
       }
    }
}

