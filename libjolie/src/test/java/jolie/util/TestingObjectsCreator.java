package jolie.util;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A utility class for create objects for testing
 */
public class TestingObjectsCreator
{

    /**
     * create a Map of interface name and list of operateion string
     * 
     * @param name               interface name
     * @param expectedOperations array of operation names
     */
    public static Map.Entry< String, Set< String > > createInterfaceStub( String name,
            String... expectedOperations )
    {
        Map.Entry< String, Set< String > > iface =
                new AbstractMap.SimpleEntry< String, Set< String > >( name,
                        new HashSet<>( Arrays.asList( expectedOperations ) ) );
        return iface;
    }

    /**
     * create a Map of Port name and list of port declaration detail
     * 
     * @param ports array of port stub
     */
    public static Map< String, PortStub > createExpectedPortMap( PortStub... ports )
    {
        Map< String, PortStub > expectedPorts = new HashMap<>();
        for (PortStub ps : ports) {
            expectedPorts.put( ps.name, ps );
        }
        return expectedPorts;
    }

    /**
     * create an entry of URI source and set of expected symbol names
     * 
     * @param source  URI of file source
     * @param symbols array of symbols name
     */
    public static Entry< URI, Set< String > > createURISymbolsMap( URI source, String... symbols )
    {
        Entry< URI, Set< String > > entry = new AbstractMap.SimpleEntry< URI, Set< String > >(
                source, new HashSet<>( Arrays.asList( symbols ) ) );
        return entry;
    }
}
