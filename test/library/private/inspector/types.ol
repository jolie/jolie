/// fwd 1
type myType: void

/// fwd 2
type myType2: string
//< bwd 2

/// Generic type documentation
type SetRedirectionRequest:void 
{
  /// forward replace node documentation
  .inputPortName[1,5]:string //< Backward node documentation
  /// forward node documentation
  .resourceName?:string
  .outputPortName*:string //< Another backward node documentation
} //< backward comment

/// Another generic type documentation
type RuntimeExceptionType: void

type RuntimeExceptionType2: undefined
//< Another backward generic type documentation

/// forward comment for SetOutputPortRequest
type SetOutputPortRequest:void {
  /// The name of the output port
  .name:string //< bwd comment
  {
    /// fwd The location of the output port
    .location:any //< back the location of the output port
    /// The protocol configuration of the output port
    .protocol?:string //< The name of the protocol (e.g., sodep, http)
    { ? }
  }
}