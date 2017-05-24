# JIoT - Jolie for the Internet of Things - Version 1.0 #

JIoT aims to bring the power of an high level Service Oriented 
Language into the Internet of Things.

```Jolie
inputPort IN_MQTT {
  Location: "socket://localhost:8000"
  Protocol: MQTT {
    .broker = hostname:port;
    .clientId = clientId;
    .osc.getMessage.alias = “planets/earth”
    .subscriber = on_demand | fixed_up_to | on_start
  }
  OneWay: getMessage
}

inputPort IN_CoAP {
  Location: "socket://localhost:8805"
  Protocol: CoAP
  OneWay: getMessage
}

outputPort Broker {
  Location: hostname:port
  Protocol: MQTT {
    .clientId = clientId;
    .osc.sendMessage.alias = “planet/earth”
  }
  RequestResponse: sendMessage
}

execution { concurrent }

define messageArrived {
  println@Console( data )
}

init {
  sendMessage@Broker( “Hello World!” )
}

main {
 install( ConnectionLost => println@Console( “Connection Lost!” );
 getMessage( data );
 messageArrived
 sendMessage( "22 C" )( temperature )
}
```

### To Do ###

* Implement Jolie Module for encoding MQTT
* Add datagram support in Jolie (UDP)
* Implement Jolie Module for encoding CoAP
* Integrate kotlin and mqtt module into the ant build.xml
* 

### Contact ###

[stefanopio dot zingaro at unibo dot it]