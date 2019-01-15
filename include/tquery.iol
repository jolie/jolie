type Path               : string

type MatchRequestType   : void {
  .data*                : undefined
  .query                : MatchExp
}

type MatchExp           : UnaryExp | ORExp | ANDExp | NOTExp

type UnaryExp           : EQUALExp | EXISTSExp | bool

type EQUALExp           : void {
  .equal                : void {
    .path               : Path
    .value[1,*]         : undefined
  }
}

type EXISTSExp          : void {
  .exists               : Path
}

type ORExp              : void {
  .or                   : BinaryExp
}

type ANDExp             : void {
  .and                  : BinaryExp
}

type NOTExp             : void {
  .not                  : MatchExp
}

type BinaryExp          : void {
  .left                 : MatchExp
  .right                : MatchExp
}

type UnwindRequest      : void {
  .data*                : undefined
  .query                : Path
}

type ProjectRequest     : void {
  .data*                : undefined
  .query[1,*]           : ProjectionExp
}

type ProjectionExp      : Path | ValuesToPathExp

type ValuesToPathExp    : void {
  .dstPath              : Path
  .value[1,*]           : Value
}

type Value              : bool | Path | MatchExp | TernaryExp

type TernaryExp         : void {
  .exp                  : MatchExp
  .ifTrue               : Value
  .ifFalse              : Value
}

type GroupRequest       : void {
  .data*                : undefined
  .query                : GroupExp
}

type GroupExp           : void {
  .aggregate[1,*]       : GroupDefinition
  .groupBy[1,*]         : GroupDefinition
}

type GroupDefinition    : void {
  .dstPath              : Path
  .srcPath              : Path
}

type LookupRequest      : void {
  .leftData*            : undefined
  .leftPath             : Path
  .rightData*           : undefined
  .rightPath            : Path
  .dstPath              : Path
}

type ResponseType       : void {
  .result*               : undefined
}

interface TQueryInterface {
  RequestResponse :
  match   ( MatchRequestType  )( ResponseType ) throws MalformedQueryExpression( string ),
  unwind  ( UnwindRequest     )( ResponseType ) throws MalformedQueryExpression( string ),
  project ( ProjectRequest    )( ResponseType ) throws MalformedQueryExpression( string ),
  group   ( GroupRequest      )( ResponseType ) throws MalformedQueryExpression( string ),
  lookup  ( LookupRequest     )( ResponseType ) throws MalformedQueryExpression( string )
}

outputPort TQuery {
  Interfaces: TQueryInterface
}

embedded {
  Java: "joliex.queryengine.TQueryService" in TQuery
}
