include "../AbstractTestUnit.iol"
include "zip_utils.iol"
include "converter.iol"

define doTest
{
    NUM_ENTRIES = 10;
    for( i = 0, i < NUM_ENTRIES, i++ ) {
        content = "this is a test " + i;
        stringToRaw@Converter( content )( raw_content );
        entry_name = "entry" + i;
        entries.( entry_name ) = content;
        rq.( entry_name ) = raw_content
    };
    zip@ZipUtils( rq )( rz.archive );
    listEntries@ZipUtils( rz )( list );
    if ( #list.entry != NUM_ENTRIES ) {
        throw( TestFailed, "The number of entries does not correspond" )
    };
    for( i = 0, i < #list.entry, i++ ) {
        if ( !is_defined( entries.( list.entry[ i ] ) ) ) {
            throw( TestFailed, "Entry " + list.entry[ i ] + " does not exits" )
        };
        rz.entry = list.entry[ i ];
        readEntry@ZipUtils( rz )( entry );
        if ( entry != entries.( list.entry[ i ] ) ) {
            throw( TestFailed, "Content of entry " + i + " does not correspond" )
        }
    }
}
