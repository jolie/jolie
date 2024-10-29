type __RangeInt: void {
  min: int 
  max: int
} | void {
  min: int 
  infinite: bool
}

type __RangeDouble: void {
  min: double 
  max*: double
} | void {
  min: double 
  infinite: bool
}


type __RangeLong: void {
  min: long 
  max*: long
} | void {
  min: long 
  infinite: bool
}

type __StringRefinedType: void {
  length: __RangeInt
} | void {
  enum[1,*]: string {
    isLast: bool
  }
} | void {
  regex: string
}

type __IntRefinedType: void {
  ranges[1,*] {
    rangeInt: __RangeInt
    isLast: bool
  } 
}

type __DoubleRefinedType: void {
  ranges[1,*] {
    rangeDouble: __RangeDouble
    isLast: bool
  }
}

type __LongRefinedType: void {
  ranges[1,*] {
    rangeLong: __RangeLong
    isLast: bool
  }
}

type __NativeType: void {
  string_type: bool {
    refined_type*: __StringRefinedType
  }
} | void {
  int_type: bool {
    refined_type*: __IntRefinedType
  }
} | void {
  double_type: bool {
    refined_type*: __DoubleRefinedType
  }
} | void {
  any_type: bool
} | void {
  void_type: bool
} | void {
  raw_type: bool
} | void {
  bool_type: bool
} | void {
  long_type: bool {
    refined_type?: __LongRefinedType
  }
} 


type __Cardinality: void {
  min: int
  max?: int
  infinite?: int
}

type __SubType: void {
  name: string
  cardinality: __Cardinality
  type: __Type
  documentation?: string
}

type __TypeInLine: void {
  root_type: __NativeType
  sub_type* {
    isFirst: bool
    isLast: bool
    sb: __SubType
  }
}

type __TypeLink: void {
  link_name: string
}

type __TypeChoice: void {
  choice: void {
      left_type: __TypeInLine | __TypeLink 
      right_type: __Type
  }
}

type __TypeUndefined: void {
  undefined: bool
}

type __Type: __TypeInLine | __TypeLink | __TypeChoice | __TypeUndefined 

type __TypeDefinition: void {
  name: string
  type: __Type
  documentation?: string
}


type __Fault: void {
  name: string
  type: __NativeType | __TypeUndefined | __TypeLink
}

type __Operation: void {
  operation_name: string
  documentation?: string
  input: string
  output?: string
  fault*: __Fault
}

type __Interface: void {
  name: string
  types*: __TypeDefinition
  operations*: __Operation
  documentation?: string
}

type __Port: void {
  name: string
  protocol?: string
  location: any
  interfaces*: __Interface
}

type __Service: void {
  name: string
  input*: string
  output*: string
}
