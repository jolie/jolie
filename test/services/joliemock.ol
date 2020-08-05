include "../AbstractTestUnit.iol"
include "services/joliemock/public/interfaces/JolieMockInterface.iol"
include "metajolie.iol"
include "console.iol"

outputPort JolieMock {
    Interfaces: JolieMockInterface
}


embedded {
    Jolie:
        "services/joliemock/joliemock.ol" in JolieMock
}


define doTest {
    getInputPortMetaData@MetaJolie( { .filename = "./services/private/testservice.ol" } )( ipts )
    getMock@JolieMock( ipts )( mock )
    println@Console( mock )()

}