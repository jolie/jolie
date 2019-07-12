/// forward comment for SetOutputPortRequest
type SetOutputPortRequest:void {
  /// The name of the output port
  .name:string //< bwd comment
  {
    /// fwd The location of the output port
    .location:any
    {
      /// The protocol configuration of the output port
      .protocol?:string //< The name of the protocol (e.g., sodep, http)
      { ? }
    }
  }
}