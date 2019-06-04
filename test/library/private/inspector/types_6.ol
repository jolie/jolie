/// MyType documentation
type MyType : void {
  .a : string //< a nice subnode
}

/// interface documentation
interface MyInterface {
  OneWay: 
  owOperation1( MyType ) /*< one-way op1 documentation */,
  /// one-way op2 documentation
  owOperation2( void ) //< a backward comment for the request
  RequestResponse: 
  /// request-response op1 documentation
  rrOperation1( void )( void ) throws MyFault( void ) MyOtherFault( string ), 
  rrOperation2( void )( void ) //< request-response op2 documentation
} //< backward interface documentation

/// interface 2 documentation
interface MyInterface2 {
  RequestResponse:
  /// request-response op1 MyInterface2 documentation
  rrOperation1( void )( void ),
  rrOperation2( void )( void ) //< request-response op2 MyInterface2 documentation
}

/// port documentation
inputPort MyInput {
  Location: "local"
  Interfaces: MyInterface
} //< bwc port documentation

/** block comment */
outputPort MyOutput {
  Interfaces: MyInterface2
} /*< bw block comment */