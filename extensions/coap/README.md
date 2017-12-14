# CoAP extension for the [Jolie Programming Language](http://www.jolie-lang.org)

Extension for the Jolie Programming Language based on Maven implementing CoAP ([RFC 7252](https://tools.ietf.org/html/rfc7252)). 
Dependencies are [Jolie](https://github.com/jolie/jolie) and [Netty](http://netty.io) (4.1.17).

## Example of usages

First we need to define a Jolie `interface` to be used in common. Lets build a thermostat interface exposing operations for *get* and *set* temperature. Operation *core* is the operation for the CoAP Core resource list retrieval at `/.well-known/core` address.

```jolie
type TmpType: void { .id?: string } | int { .id?: string }

interface ThermostatInterface {
    OneWay: setTmp( TmpType )
    RequestResponse: 
        getTmp( TmpType )( int ),
        core( void )( string )
}
```

---

### Client Code Example

The example below shows a possible implementation of the CoAP server.

```jolie
include "console.iol"
include "thermostat.iol"

outputPort Thermostat {
    Location: "datagram://localhost:5683"
    Protocol: coap {
        .debug = false;
        .proxy = false;
        .osc.getTmp << {
            .alias = "%!{id}/getTemperature",
            .method = "GET",
            .confirmable = true
        };
        .osc.setTmp << {
            .format = "raw",
            .method = "POST",
            .alias = "%!{id}/setTemperature",
            .confirmable = true
        };
        .osc.core << {
            .messageCode = "GET",
            .alias = "/.well-known/core"
        }
    }
    Interfaces: ThermostatInterface
}

outputPort CoapServer {
    Location: "datagram://coap.me:5683"
    Protocol: coap {
        .debug = false;
        .osc.core << {
            .messageCode = "GET",
            .alias = "/.well-known/core"
        }
    }
    Interfaces: ThermostatInterface
}

main
{
    {
        println@Console( " Retrieving temperature 
        from Thermostat n.42 ... " )()
        |
        getTmp@Thermostat( { .id = "42" } )( t )
    };
    println@Console( " Thermostat n.42 forwarded temperature: " 
        + t + " C.")();
    t_confort = 21;
    if (t < t_confort) {
        println@Console( " Setting Temperature of Thermostat n.42 to " 
        + t_confort + " C ..." )()
        |
        setTmp@Thermostat( 21 { .id = "42" } );
        println@Console( " ... Thermostat set the Temperature 
        accordingly!" )()
    };
    core@Thermostat( )( resp );
    println@Console( resp )() 
}
```

### Server Code Example

Lets show the server deployment of `inputPort Thermostat` and the behaviour for each invoked `operation`. 

```jolie
include "console.iol"
include "thermostat.iol"

inputPort  Thermostat {
    Location: "datagram://localhost:5683"
    Protocol: coap {
        .debug = false;
        .proxy = false;
        .osc.getTmp << {
            .format = "raw",
            .method = "205",
            .alias = "42/getTemperature"
        };
        .osc.setTmp << {
            .alias = "42/setTemperature"
        };
        .osc.core << {
            .alias = "/.well-known/core",
            .messageCode = "205"
        }
    }
    Interfaces: ThermostatInterface
}

main 
{
    [
        getTmp( temp )( resp ) 
        {
            resp = 19;
            println@Console( " Setting Temperature of the Thermostat to " 
                + temperatura )()   
        }
    ]
    |
    [
        setTmp( temperatura )
    ] 
    {
        println@Console( " Get Temperature Request. Forwarding: " 
            + resp + " C")()
    }
    |
    [
        core(  )( response )
        {
            response = 
            "
            </obs>;
                obs;
                rt=\"observe\";
                title=\"Observable resource which changes every 5 seconds\",
            </obs-pumping>;
                obs;
                rt=\"observe\";
                title=\"Observable resource which changes every 5 seconds\"
            "
        }
    ]
}     
```

* The [paper](http://cs.unibo.it/~sgiallor/publications/hicss2018/hicss2018.pdf) - Maurizio Gabbrielli, Saverio Giallorenzo, Ivan Lanese, and Stefano Pio Zingaro: A Language-based Approach for Interoperability of IoT Platforms. Hawaii International Conference on System Sciences 2018, IEEE Computer Society 2018.
* More on the jIoT (Jolie for IoT) page project @ http://cs.unibo.it/projects/jolie/jiot.html
* More on me @ http://cs.unibo.it/~stefanopio.zingaro
