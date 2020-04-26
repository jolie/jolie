package jolie.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Stub class for testing declaration of a port
 */
public class PortStub
{
    /**
     * Port name
     */
    public String name;

    /**
     * A set of interface name and it's operations name
     */
    public Map< String, Set< String > > ifaces;

    /**
     * A set of operations name defined in Port
     */
    public Set< String > ops;

    public PortStub( String name, Map< String, Set< String > > ifaces, String... ops )
    {
        this.name = name;
        this.ifaces = ifaces;
        this.ops = new HashSet<>( Arrays.asList( ops ) );
    }
}
