package jolie.lang.parse.module;

import java.util.Map;
import jolie.lang.parse.Scanner;

public class ModuleParsingConfiguration {

	/**
	 * an array of string for lookup path of include statement in Module
	 */
	private final String[] includePaths;

	private final String[] packagePaths;

	private final String charset;
	private final ClassLoader classLoader;
	private final boolean includeDocumentation;

	private final Map< String, Scanner.Token > constantsMap;


	public ModuleParsingConfiguration( String charset, String[] includePaths, String[] packagePaths, ClassLoader classLoader,
		Map< String, Scanner.Token > constantsMap, boolean includeDocumentation ) {
		this.charset = charset;
		this.includePaths = includePaths;
		this.packagePaths = packagePaths;
		this.classLoader = classLoader;
		this.constantsMap = constantsMap;
		this.includeDocumentation = includeDocumentation;
	}

	public String[] includePaths() {
		return includePaths;
	}

	public String[] packagePaths() {
		return packagePaths;
	}

	public String charset() {
		return charset;
	}

	public ClassLoader classLoader() {
		return classLoader;
	}

	public boolean includeDocumentation() {
		return includeDocumentation;
	}

	public Map< String, Scanner.Token > constantsMap() {
		return constantsMap;
	}
}
