include "../AbstractTestUnit.iol"

include "time.iol"
include "console.iol"

define doTest {

   /* changeDataFormat */
   with( request ) {
     .date = "05/06/2007";
     .fromFormat = "dd/MM/yyyy";
     .toFormat = "ddMMyyyy"
   };
   changeDateFormat@Time( request )( response );
   expected = "05062007";
   if ( response.date != expected ) {
       println@Console( "Expected " + expected + ", found " + response.date )()
   }
}
