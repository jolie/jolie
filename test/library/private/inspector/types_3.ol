/// Generic type documentation
type SetRedirectionRequest:void 
{
  /// forward replaced node documentation
  .inputPortName[1,5]:string //< Backward node documentation
  /** forward block node documentation */
  .resourceName?:string
  .outputPortName*:string /*< Backward block
                          node documentation */
} //< backward comment