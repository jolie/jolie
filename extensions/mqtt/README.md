# JIoT - Jolie for the Internet of Things - Version 1.0 #

JIoT aims to bring the power of an high level Service Oriented Language into the Internet of Things.

```Jolie
inputPort IN_MQTT {
  Location: "socket://localhost:8000"
  Protocol: MQTT {
    .broker = hostname:port;
    .clientId = clientId;
    .osc.getMessage.alias = “planets/earth”
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
}
```

### To Do ###

* Implement Jolie Module for encoding MQTT
* Add datagram support in Jolie (UDP)
* Implement Jolie Module for encoding CoAP

# Maven Project jiot-mqtt README #

This is the readme file, improvements to come.

### Some Basic Knowledge ###
1. Example taken from https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_8.0.0/com.ibm.mq.dev.doc/q028910_.htm 
2. Javadoc at https://www.eclipse.org/paho/files/javadoc/index.html
3. Broker URL from https://iot.eclipse.org/getting-started#tutorials

### Dependencies ###

* Get Maven @ https://maven.apache.org
* Add Eclipse Paho dependencies for Maven as stated @ https://eclipse.org/paho/clients/java/
```
<project ...>
<repositories>
    <repository>
        <id>Eclipse Paho Repo</id>
        <url>%REPOURL%</url>
    </repository>
</repositories>
...
<dependencies>
    <dependency>
        <groupId>org.eclipse.paho</groupId>
        <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
        <version>%VERSION%</version>
    </dependency>
</dependencies>
</project>
```
* Import Project into Intellij IDEA CE - Download @ https://www.jetbrains.com/idea/
* Run Tests - Expected output
```
Everything that has a beginning has an end.
```

### Application summary ###

  1. Create an instance of MQTT client
  2. Configure options for the client connection:
    * MQTT version
    * Connection Timeout in seconds
    * Keep Alive Interval in seconds
    * Clean Session (server remember state across restarts and reconnects)
    * Servers URI - String[] list of possible URI where to reconnect (not set)
    * User Name
  3. Connect to broker with the connection options
  4. Subscribe to a topic
  5. Publish message to a topic
  6. Receive notification of a new message
  7. Unsubscribe to a topic
  8. Disconnect to broker

### Principles of topic name syntax and semantics ###

  1. A topic must be at least one character long.
  2. Topic names are case sensitive.
  3. Topic names can include the space character.
  4. A leading "/" creates a distinct topic.
  5. Do not include the null character (Unicode \x0000) in any topic.
  6. The length is limited to 64k.
  7. There can be any number of root nodes.

### Contact ###

[stefanopio dot zingaro at unibo dot it]