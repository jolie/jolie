from ..test-unit import TestUnitInterface
from trees import Trees

service Main {

    embed Trees as Trees

    inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

    main {
        test()() {  
            for( x = 0, x < 100, x++ ) {
                a.c.d.f[ x ] << {
                    testString = "testString"
                    mykey = "key" + x
                    x = x
                }
            }
            getHashMap@Trees( {
                vector -> a.c.d.f 
                key = "mykey"
            })( hashmap )

            count = 0
            foreach( y : hashmap ) {
                count++
                if ( y != hashmap.( y ).mykey ) {
                    throw( TestFailed, "Key not correspond, expected " + y + " found " + hashmap.( y ).mykey )
                }
            }

            if ( count != 100 ) {
                throw( TestFailed, "expected 100 keys, found " + count )
            }
        }
    }
}