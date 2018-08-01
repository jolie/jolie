include "../AbstractTestUnit.iol"
include "zip_utils.iol"
include "converter.iol"

define doTest
{
    entrylist.( "test1/test1/test1" ) = "test";
    entrylist.( "test2/test2" ) = "test";
    entrylist.( "test3" ) = "test";

    foreach( e : entrylist ) {
        stringToRaw@Converter( e )( rawcontent );
        zip_req.( e ) = rawcontent
    };
    zip@ZipUtils( zip_req )( zip_archive );

    listEntries@ZipUtils( { .archive = zip_archive } )( entries );
    if ( #entries.entry != 3 ) {
        throw( TestFailed, "listEntries: wrong number of entries, expected 3 found " +#entries.entry )
    };
    for( e = 0, e < #entries.entry, e++ ) {
        if ( entrylist.( entries.entry[ e ] ) != "test" ) {
            throw( TestFailed, "listEntries: wrong entry " + entries.entry[ e ] )
        }
    }
    ;
    for( e = 0, e < #entries.entry, e++ ) {
        with( read_req ) {
            .archive -> zip_archive;
            .entry = entries.entry[ e ]
        };
        readEntry@ZipUtils( read_req )( entry_raw );
        rawToString@Converter( entry_raw )( read_string );
        if ( read_string != entries.entry[ e ] ) {
            throw( TestFailed, "readEntry: wrong entry content, expected " + entries.entry[ e ] + ", found " +read_string )
        }
    }
}
