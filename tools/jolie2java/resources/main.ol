include "./resources/TestInterface.iol"

inputPort MyPort {
  Location: "socket://localhost:9000"
  Protocol: sodep
  Interfaces: TestInterface
}

main {
  nullProcess
}
