# JIoT - Jolie for the Internet of Things - Version 1.0 #

JIoT aims to bring the power of an high level Service Oriented 
Language into the Internet of Things.

```Jolie
interface TemperatureInterface { 
    OneWay: receiveTemperature( string )
}

inputPort  HttpCP {
  Location: "socket://localhost:8000"
  Protocol: http {
    .method = "get"
  }
  Interfaces: TemperatureInterface
}

inputPort  MqttCP {
  Location: "socket://localhost:8050"
  Protocol: mqtt {
    .broker = "socket://iot.eclipse.org:1883"
    .osc.receiveTemperature {
        .alias = "jolie/request/temperature"
    }
  }
  Interfaces: TemperatureInterface
}

inputPort CoapCP {
  Location: "socket://localhost:8805"
  Protocol: CoAP
  Interfaces: TemperatureInterface
}

outputPort Broker {
  Location: "socket://iot.eclipse.org:1883"
  Protocol: mqtt {
    .osc.setTmp {
      .alias = "jolie/request/temperature"
    }
  }
  OneWay: setTmp( string )
}

execution { concurrent }

main {
  setTmp@Broker( "22.5" );
  receiveTemperature( data );
  println@Console( data )
}
``` 

### Contact ###

[stefanopio dot zingaro at unibo dot it]