/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.net.auto;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.OutputPort;
import jolie.runtime.AndJarDeps;

/**
 *
 * @author Claudio Guidi
 */
@AndJarDeps({"ini4j.jar"})
public class AutoChannelFactory extends CommChannelFactory {
    
    CommCore commCore;
    HashMap<URI,String> locationMap = new HashMap<URI,String>();

    public AutoChannelFactory(CommCore commCore) {
        super(commCore);
        this.commCore = commCore;
    }

    @Override
    public CommChannel createChannel(URI locationURI, OutputPort port) throws IOException {

                String location = null;
                if ( !locationMap.containsKey(locationURI) ) {
		   String[] ss = locationURI.getSchemeSpecificPart().split( ":", 2 );
		
		   if ( "ini".equals( ss[0] ) ) {
			  location = AutoHelper.getLocationFromIni( ss[1] );
                          locationMap.put(locationURI, location);
		   } else {
		          AutoHelper.throwIOException( "unsupported scheme: " + locationURI.getScheme() );
		   }
                } else {
                    location = locationMap.get(locationURI);
                }

		AutoHelper.assertIOException( location == null, "internal error: location is null" );
		AutoHelper.assertIOException( location.equals( Constants.LOCAL_LOCATION_KEYWORD ), "autoconf does not support local locations" );
		
		try {
			URI uri = new URI( location );
                        return commCore.createCommChannel(uri, port);
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		}
    }

}
