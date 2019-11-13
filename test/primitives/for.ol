include "../AbstractTestUnit.iol"


define doTest
{
    a.b[0].c = 1;
    a.b[1] = "ciao";
    a.b[2] = 7

    count_c = 0
    for ( s in a.b ) {
        if ( is_defined( s.c ) ) {
            count_c++
        }
    }

    if ( count_c == 0) {
        throw( TestFailed )
    }
}