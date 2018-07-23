include "math.iol"

type EuclideanDistanceFaultType: void {
  .exceptionMessage: string
}

type Point: void {
  .coordinates*: double
}

type EuclideanDistanceRequest: void {
  .firstPoint: Point
  .secondPoint: Point
}

type EuclideanDistanceResponse: double

interface GeometryInterface {
  RequestResponse:
    euclideanDistance( EuclideanDistanceRequest )( EuclideanDistanceResponse ) throws 
      CoordinatesDimensionInconsistency( EuclideanDistanceFaultType )
} 

service Geometry 
{
  Interfaces: GeometryInterface
  main 
  {
    euclideanDistance( EuclideanDistanceRequest )( EuclideanDistanceResponse ) {
      
      install( CoordinatesDimensionInconsistency => nullProcess /* let the caller handle this */ ) ;
      if( #EuclideanDistanceRequest.firstPoint.coordinates != #EuclideanDistanceRequest.secondPoint.coordinates ){
        faultError.exceptionMessage = "Coordinates have different dimensions" ;
        throw( CoordinatesDimensionInconsistency, faultError )
      } ;
      
      summation = 0 ;
      for ( i=0, i<#EuclideanDistanceRequest.firstPoint.coordinates, i++ ) {
        relativeDistance = EuclideanDistanceRequest.firstPoint.coordinates[i] - EuclideanDistanceRequest.secondPoint.coordinates[i] ;
        powRequest.base = relativeDistance ; powRequest.exponent = 2.0 ;
        pow@Math( powRequest )( powResponse ) ; summation = summation + powResponse
      } ;
      powRequest.base = summation ; powRequest.exponent = 0.5 ;
      pow@Math( powRequest )( powResponse ) ; 
      EuclideanDistanceResponse = powResponse
    }
  }
}