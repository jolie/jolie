from ...tools.jolie2openapi.jolie2openapi import Jolie2OpenApi
from ..test-unit import TestUnitInterface
from runtime import Runtime
from time import Time
from file import File

service Main {

    embed Runtime as Runtime
    embed Time as Time
    embed File as File
	
    inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	

	main {
		[ test()() {
            loadEmbeddedService@Runtime( {
                type = "Jolie"
                service = "Jolie2OpenApi"
                filepath = "../tools/jolie2openapi/jolie2openapi.ol --filename=./tools/private/test_service.ol "
                    + "--ip=PeopleAddressService --router=localhost:8000 --output=./tools/private/tmp.json "
                    + "--template=./tools/private/rest-template.json"
            } )( TestUnit.location ) 
            sleep@Time( 1000 )()
            readFile@File( {
                filename = "./tools/private/tmp.json"
            } )( tmp )
    
            readFile@File( {
                filename = "./tools/private/test_service_openapi3.json"
            } )( test_service_openapi )
            if ( tmp != test_service_openapi ) {
                throw( TestFailed, "Wrong openapi generation")
            } 

            // openapi 2.0 test
            loadEmbeddedService@Runtime( {
                type = "Jolie"
                service = "Jolie2OpenApi"
                filepath = "../tools/jolie2openapi/jolie2openapi.ol --filename=./tools/private/test_service.ol "
                    + "--ip=PeopleAddressService --router=localhost:8000 --output=./tools/private/tmp.json "
                    + "--template=./tools/private/rest-template.json --openapi=2.0"
            } )( TestUnit.location ) 
            sleep@Time( 1000 )()
            readFile@File( {
                filename = "./tools/private/tmp.json"
            } )( tmp )
    
            readFile@File( {
                filename = "./tools/private/test_service_openapi2.json"
            } )( test_service_openapi )
            if ( tmp != test_service_openapi ) {
                throw( TestFailed, "Wrong openapi generation, --openapi=2.0")
            } 

            // level0 test openapi 3.0
            loadEmbeddedService@Runtime( {
                type = "Jolie"
                service = "Jolie2OpenApi"
                filepath = "../tools/jolie2openapi/jolie2openapi.ol --filename=./tools/private/test_service.ol "
                    + "--ip=PeopleAddressService --router=localhost:8000 --output=./tools/private/tmp.json "
                    + "--level0"
            } )( TestUnit.location ) 
            sleep@Time( 1000 )()

            readFile@File( {
                filename = "./tools/private/tmp.json"
            } )( tmp )
    
            readFile@File( {
                filename = "./tools/private/test_service_level0_openapi3.json"
            } )( test_service_openapi )
            if ( tmp != test_service_openapi ) {
                throw( TestFailed, "Wrong openapi generation, --level0")
            } 

            // level0 test openapi 2.0
            loadEmbeddedService@Runtime( {
                type = "Jolie"
                service = "Jolie2OpenApi"
                filepath = "../tools/jolie2openapi/jolie2openapi.ol --filename=./tools/private/test_service.ol "
                    + "--ip=PeopleAddressService --router=localhost:8000 --output=./tools/private/tmp.json "
                    + "--level0 --openapi=2.0"
            } )( TestUnit.location ) 
            sleep@Time( 1000 )()

            readFile@File( {
                filename = "./tools/private/tmp.json"
            } )( tmp )
    
            readFile@File( {
                filename = "./tools/private/test_service_level0_openapi2.json"
            } )( test_service_openapi )
            if ( tmp != test_service_openapi ) {
                throw( TestFailed, "Wrong openapi generation, --level0 --openapi=2.0")
            } 

            delete@File( "./tools/private/tmp.json")()

            
        }]
    }   
}

