from ..test-unit import TestUnitInterface
from file import File
from console import Console
from string-utils import StringUtils
from joliedoc-lib import JolieDocLib
from metajolie import MetaJolie

service Main {

    embed File as File
    embed StringUtils as StringUtils
    embed JolieDocLib as JolieDocLib
    embed MetaJolie as MetaJolie
    embed Console as Console

    inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }


    main {
        test()() {
            getInputPortMetaData@MetaJolie( { filename = "private/sample_service_joliedoclib.ol" } )( meta_description )

            //valueToPrettyString@StringUtils( meta_description.input )( s ); println@Console( s )()
            _getPort@JolieDocLib( { documentation_cr_replacement = "<br>", port << meta_description.input } )( _port )
            
        }
    }

}