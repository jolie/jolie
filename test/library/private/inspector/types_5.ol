/// forward comment choice
type myChoice : 
  void {
    /// first choice, fwd
    .a: int
    .b: void {
      .c: int { ? } //< first choice, nested, bwd
    } //< first choice, bwd
  }
  | string 
  | int {
    // subtype type choice
    .d: void
      | int 
      | double { .e: string /*< very, nested, bwd comment */ }
  } 
//< backward comment choice