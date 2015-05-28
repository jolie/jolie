include "console.iol"

interface EmbeddedIface {
RequestResponse:
  twice(int)(int)
}

inputPort Embedded {
Location: "local://A"
Interfaces: EmbeddedIface
}

main
{
  twice( x )( x * 2 )
}
