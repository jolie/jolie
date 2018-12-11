type Path             : string

type MatchRequestType : void {
.data*                : undefined
.query                : MatchExp
}

type MatchExp         : UnaryExp | ORExp | ANDExp | NOTExp

type UnaryExp         : EQUALExp | GREATERTHENExp | LOWERTHENExp | EXISTSExp | bool

type EQUALExp         : void {
.equal                : CompareExp           
}

type GREATERTHENExp   : void {
.greaterThen          : CompareExp           
}

type LOWERTHENExp     : void {
.lowerThen            : CompareExp           
}

type CompareExp       : void {
.path                 : Path
.value[1,*]           : undefined
}

type EXISTSExp        : void {
.exists               : Path
}

type ORExp            : void {
.or                   : BinaryExp
}

type ANDExp           : void {
.and                  : BinaryExp
}

type NOTExp           : void {
.not                  : MatchExp
}

type BinaryExp        : void {
.left                 : MatchExp
.right                : MatchExp
}

type UnwindRequest    : void {
.data*                : undefined
.query                : Path
}

type ProjectRequest   : void {
.data*                : undefined
.query[1,*]           : ProjectionExp
}

type ProjectionExp    : Path | ValuesToPathExp

type ValuesToPathExp  : void {
.dstPath              : Path
.value[1,*]           : Value
}

type Value            : bool | Path | MatchExp | TernaryExp

type TernaryExp       : void {
.exp                  : MatchExp
.ifTrue               : Value
.ifFalse              : Value
}

type GroupRequest     : void {
.data*                : undefined
.query                : GroupExp
}

type GroupExp         : void {
.aggregate[1,*]       : GroupDefinition
.groupBy[1,*]         : GroupDefinition
}

type GroupDefinition  : void {
.dstPath              : Path
.srcPath              : Path
}

type LookupRequest    : void {
.leftData*            : undefined
.leftPath             : Path
.rightData*           : undefined
.rightPath            : Path
.dstPath              : Path
}

interface TQueryInterface {
  RequestResponse :
    match   ( MatchRequestType  )( undefined ) throws MalformedQueryExpression( string ),
    unwind  ( UnwindRequest     )( undefined ) throws MalformedQueryExpression( string ),
    project ( ProjectRequest    )( undefined ) throws MalformedQueryExpression( string ),
    group   ( GroupRequest      )( undefined ) throws MalformedQueryExpression( string ),
    lookup  ( LookupRequest     )( undefined ) throws MalformedQueryExpression( string )
}

outputPort TQuery {
  Interfaces: TQueryInterface
}

embedded {
  Java: "joliex.queryengine.TQueryService" in TQuery
}