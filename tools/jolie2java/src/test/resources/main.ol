include "TestInterface.iol"

service Test {
    inputPort ip {
        location: "local"
        interfaces: TestInterface
    }
    foreign java {
        class: "com.test.Test"
    }
}