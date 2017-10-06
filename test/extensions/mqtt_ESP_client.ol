outputPort ESP {
    Location: "socket://localhost:1883"
    Protocol: mqtt {
        .osc.setOn.alias = "sonoff/cmnd/POWER";
        .osc.setOn.QoS = 0;
        .debug = true
    }
    OneWay: setOn( string ) 
}

main
{
  setOn@ESP("OFF")
}