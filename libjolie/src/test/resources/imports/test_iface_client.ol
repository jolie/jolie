from interface import twiceIface

include "console.iol"

outputPort OP{
    interfaces : twiceIface
    location: "socket://localhost:3000"
    protocol: sodep
}


main {
    twice@OP(2)(req)
    println@Console(req)()
}