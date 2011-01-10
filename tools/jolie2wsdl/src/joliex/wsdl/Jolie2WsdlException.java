package joliex.wsdl;



/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author claudio
 */
public class Jolie2WsdlException  extends Exception {
    
    private String message;

    public Jolie2WsdlException( String message ) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    
}
