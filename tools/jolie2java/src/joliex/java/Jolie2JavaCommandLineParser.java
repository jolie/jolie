/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package joliex.java;

import java.io.IOException;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import joliex.java.formatExeption;


/**
 *
 * @author balint
 */
public class Jolie2JavaCommandLineParser extends CommandLineParser {
 public Jolie2JavaCommandLineParser(String[] args, ClassLoader parentClassLoader) throws CommandLineException, IOException
    {

            super(args,parentClassLoader);

    }
 public String GetFormat() throws formatExeption{

       if (this.getToken("--format")!=-1){
            String[] args = arguments();
         return(args[this.getToken("--format")+1]);

       }else
       {
           
           formatExeption err = new formatExeption();
           throw err;

       }

  }


}
