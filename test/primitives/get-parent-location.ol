from ..test-unit import TestUnitInterface
from .private.json-schema-get-parent-location import JsonSchema
from runtime import Runtime


service Main {

    embed JsonSchema as JsonSchema
    embed Runtime as Runtime
    

	inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

    main {
        test()() {
            loadEmbeddedService@Runtime( {
                filepath = "./primitives/private/json-schema-get-parent-location.ol"
                service = "JsonSchema"
            })( loc )
        }
    }
}