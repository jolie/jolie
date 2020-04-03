include "../AbstractTestUnit.iol"
include "time.iol"
include "runtime.iol"

outputPort MySelf {
    OneWay: timeout
}

inputPort TestLocal {
    Location: "local"
    OneWay: timeout
}

define doTest
{
    getLocalLocation@Runtime()( MySelf.location )
	setNextTimeout@Time( 1000 );
    timeout()
    setNextTimeout@Time( 2000 );
    
    {
        {
            stopNextTimeout@Time()
            sleep@Time( 3000 )()
            timeout@MySelf("OK")
        }   
        |
        {
            timeout( req ) 
            if ( req != "OK" ) {
                throw( TestFailed, "Expected Ok in timeout operation" )
            } 
        }
    }

    


}