
package jolie.runtime;

/**
 * Instructs the JolieClassLoader to load the specified
 * jar library files before instantiating a JavaService.
 *
 * @author Fabrizio Montesi
 */
public @interface NeedsJars
{
	String[] value();
}
