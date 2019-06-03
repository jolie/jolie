/// Generic type documentation
type SetRedirectionRequest:void 
{
  /// forward replace node documentation
  .inputPortName[1,5]:string //< Backward node documentation
  /* forward node documentation */
  .resourceName?:string
  .outputPortName*:string /*< Another backward
                          node documentation */
} //< backward comment