include "../AbstractTestUnit.iol"
include "services/jester/JesterConfiguratorInterface.iol"
include "json_utils.iol"
include "console.iol"

outputPort JesterConfig {
    Interfaces: JesterConfiguratorInterface
}

embedded {
    Jolie:
        "--trace services/jester/jester_configurator.ol" in JesterConfig
}

define doTest {
    service_filename = "./services/private/testservice.ol"
    service_input_port = "DEMO"
    router_host = "localhost:8000"
    wkdir = "./services/private"
    easy_interface = false

    template_json = "{ \"getOrders\":\"method=get,\ntemplate=/orders/{userId}?maxItems={maxItems}\",\n\"getOrdersByIItem\":\"method=post\",\n\"putOrder\":\"method=put\",\n\"deleteOrder\":\"method=delete\"}"
    getJsonValue@JsonUtils( template_json )( template )
    
    with( jester ) {
        .filename = service_filename;
        .host = router_host;
        .inputPort = service_input_port;
        .easyInterface = easy_interface;
        if ( !easy_interface ) {
            .template -> template
        }
    }

    getJesterConfig@JesterConfig( jester )( jester_config )

}