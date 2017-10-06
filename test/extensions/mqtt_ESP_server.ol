include "console.iol"

inputPort Listener {
    Location: "socket://localhost:8001"
    Protocol: mqtt {
        .broker = "socket://localhost:1883";
        .osc.read.alias = "sonoff/STATE";
        .debug = true;
        .format = "json"
    }
    OneWay: read( string ) 
}

main
{
  read( result ); 
  println@Console( result )()
}