from interface import twiceIface

inputPort IP {
    interfaces : twiceIface
    location: "socket://localhost:3000"
    protocol: sodep
}

outputPort OP {
    interfaces : twiceIface
    location: "socket://localhost:3000"
    protocol: sodep
}