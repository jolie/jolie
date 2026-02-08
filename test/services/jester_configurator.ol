include "../AbstractTestUnit.iol"
include "services/jester/JesterConfiguratorInterface.iol"
include "json_utils.iol"
include "console.iol"
include "string_utils.iol"

outputPort JesterConfig {
    Interfaces: JesterConfiguratorInterface
}

embedded {
    Jolie:
        "services/jester/jester_configurator.ol" in JesterConfig
}

define doTest {
    service_filename = "./services/private/testservice.ol"
    service_input_port = "DEMO"
    router_host = "localhost:8000"
    wkdir = "./services/private"
    easy_interface = false

    template_json = "{ 
        \"getUsers\":{
            \"method\":\"post\", 
            \"template\":\"/users/{country}\"
        }, 
        \"getOrders\": {
            \"method\":\"get\", 
            \"template\":\"/orders/{userId}?maxItems={maxItems}\"
        },
        \"getOrdersByItem\": {
            \"method\":\"post\"
        },
        \"putOrder\": {
            \"method\":\"put\"
        },
        \"deleteOrder\": {
            \"method\":\"delete\"
        }}"
    getJsonValue@JsonUtils( template_json )( template )
    
    with( jester ) {
        ..filename = service_filename;
        ..host = router_host;
        ..inputPort = service_input_port;
        ..easyInterface = easy_interface;
        if ( !easy_interface ) {
            ..template -> template
        }
    }

    getJesterConfig@JesterConfig( jester )( jester_config )
    if ( #jester_config.routes != 5 ) {
        println@Console("Expected 5 routes, found " + #jester_config.routes)()
        throw( TestFailed )
    }
    found_interesting_template = false
    interesting_template = "/orders/{userId}?maxItems={maxItems}"
    for( i = 0, i < #jester_config.routes, i++ ) {
        if ( jester_config.routes[ i ].template == interesting_template ) {
            found_interesting_template = true
            if ( !is_defined( jester_config.routes[ i ].cast ) ) {
                println@Console("Cast is missing in template " + interesting_template )()
                throw( TestFailed )
            }
        }
    }
    if ( !found_interesting_template ) {
        println@Console("Missing template " + interesting_template )()
        throw( TestFailed )
    }

}