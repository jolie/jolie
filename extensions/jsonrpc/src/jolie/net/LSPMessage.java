/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.net;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author eferos93
 */
public class LSPMessage {
    private byte[] content = null;
    final private Map< String, String > propMap = new HashMap<>();
    
    public void setContent( byte[] content ) {
        this.content = content;
    }
    
    public void setProperty(String name, String value) {
        propMap.put( name.toLowerCase(), value );
    }
    
    public Collection< Entry< String, String > > getProperties() {
        return propMap.entrySet();
    }
    
    public String getProperty( String name ) {
        String temp = propMap.get( name.toLowerCase() );
        if(temp!=null)
            temp=temp.trim();
        return temp;
    }
    
    public int size() {
        if ( content == null )
            return 0;
        return content.length;
    }
    
    public byte[] content() {
        return content;
    }
}
