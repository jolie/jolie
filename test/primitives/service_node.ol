
/**
    Test syntax
*/
service main2(var : string) {
    main{
        nullProcess
    }
}

/**
    Test parsing behavioural statement inside inside node
*/
service main3 {

    outputPort testOP {
        location: "local://test"
        protocol: "sodep"
        requestResponse: print( any )( any ) 
    }

    inputPort testIP {
        location: "local://test"
        protocol: "sodep"
        requestResponse: print( any )( any ) 
    }

    courier testIP {
        [ print( request )( response ) ] {
            println@Console("")();
            forward( request )( response )
        }
    }
    
    init {
        nullProcess
    }
    main{
        nullProcess
    }
}

interface TestUnitInterface {
RequestResponse:
	test(void)(void) throws TestFailed(any)
}


service main{

    execution { single }

    inputPort TestUnitInput {
        Location: "local"
        Interfaces: TestUnitInterface
    }

    define doTest {
        nullProcess
    }

    main
    {
        test()() {
            doTest
        }
    }
}