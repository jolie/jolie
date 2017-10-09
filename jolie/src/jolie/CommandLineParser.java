/**
 * *************************************************************************
 * Copyright (C) 2008-2010 by Fabrizio Montesi <famontesi@gmail.com> * * This
 * program is free software; you can redistribute it and/or modify * it under
 * the terms of the GNU Library General Public License as * published by the
 * Free Software Foundation; either version 2 of the * License, or (at your
 * option) any later version. * * This program is distributed in the hope that
 * it will be useful, * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 * GNU General Public License for more details. * * You should have received a
 * copy of the GNU Library General Public * License along with this program; if
 * not, write to the * Free Software Foundation, Inc., * 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 **************************************************************************
 */
package jolie;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.regex.Pattern;
import jolie.jap.JapURLConnection;
import jolie.lang.Constants;
import jolie.lang.parse.Scanner;
import jolie.runtime.correlation.CorrelationEngine;
import jolie.util.Helpers;

/**
 * A parser for JOLIE's command line arguments, providing methods for accessing
 * them.
 *
 * @author Fabrizio Montesi
 */
public class CommandLineParser implements Closeable {

    private final static Pattern PATH_SEPARATOR_PATTERN = Pattern.compile(jolie.lang.Constants.pathSeparator);
    private final static Pattern OPTION_SEPARATOR_PATTERN = Pattern.compile(" ");

    private final int connectionsLimit;
    private final int connectionsCache;
    private final CorrelationEngine.Type correlationAlgorithmType;
    private final String[] includePaths;
    private final String[] optionArgs;
    private final URL[] libURLs;
    private final InputStream programStream;
    private String charset = null;
    private final File programFilepath;
    private final String[] arguments;
    private final Map< String, Scanner.Token> constants = new HashMap< String, Scanner.Token>();
    private final JolieClassLoader jolieClassLoader;
    private final boolean isProgramCompiled;
    private final boolean typeCheck;
    private final boolean tracer;
    private final boolean jTracer;
    private final boolean check;
    private final Level logLevel;
    private File programDirectory = null;

    /**
     * Returns the arguments passed to the JOLIE program.
     *
     * @return the arguments passed to the JOLIE program.
     */
    public String[] arguments() {
        return arguments;
    }

    /**
     * Returns the {@link Level} of the logger of this interpreter.
     *
     * @return the {@link Level} of the logger of this interpreter.
     */
    public Level logLevel() {
        return logLevel;
    }

    /**
     * Returns <code>true</code> if the tracer option has been specified, false
     * otherwise.
     *
     * @return <code>true</code> if the verbose option has been specified, false
     * otherwise
     */
    public boolean tracer() {
        return tracer;
    }

    /**
     * Returns <code>true</code> if the tracer:json option has been specified,
     * false otherwise.
     *
     * @return <code>true</code> if the verbose option has been specified, false
     * otherwise
     */
    public boolean jTracer() {
        return jTracer;
    }

    /**
     * Returns <code>true</code> if the check option has been specified, false
     * otherwise.
     *
     * @return <code>true</code> if the verbose option has been specified, false
     * otherwise
     */
    public boolean check() {
        return check;
    }

    /**
     * Returns {@code true} if the program is compiled, {@code false} otherwise.
     *
     * @return {@code true} if the program is compiled, {@code false} otherwise.
     */
    public boolean isProgramCompiled() {
        return isProgramCompiled;
    }

    /**
     * Returns the file path of the JOLIE program to execute.
     *
     * @return the file path of the JOLIE program to execute
     */
    public File programFilepath() {
        return programFilepath;
    }

    /**
     * Returns an InputStream for the program code to execute.
     *
     * @return an InputStream for the program code to execute
     */
    public InputStream programStream() {
        return programStream;
    }

    /**
     * Returns the program's character encoding
     *
     * @return the program's character encoding
     */
    public String charset() {
        return charset;
    }

    /**
     * Closes the underlying {@link InputStream} to the target Jolie program.
     */
    public void close()
            throws IOException {
        programStream.close();
    }

    /**
     * Returns the library URLs passed by command line with the -l option.
     *
     * @return the library URLs passed by command line
     */
    public URL[] libURLs() {
        return libURLs;
    }

    /**
     * Returns the include paths passed by command line with the -i option.
     *
     * @return the include paths passed by command line
     */
    public String[] includePaths() {
        return includePaths;
    }

    /**
     * Returns the connection limit parameter passed by command line with the -c
     * option.
     *
     * @return the connection limit parameter passed by command line
     */
    public int connectionsLimit() {
        return connectionsLimit;
    }

    /**
     * Returns the connection cache parameter passed by command line with the
     * --conncache option.
     *
     * @return the connection cache parameter passed by command line
     */
    public int connectionsCache() {
        return connectionsCache;
    }

    private static String getOptionString(String option, String description) {
        return ('\t' + option + "\t\t" + description + '\n');
    }

    private String getVersionString() {
        return (Constants.VERSION + "  " + Constants.COPYRIGHT);
    }

    /**
     * Returns a map containing the constants defined by command line.
     *
     * @return a map containing the constants defined by command line
     */
    public Map< String, Scanner.Token> definedConstants() {
        return constants;
    }

    /**
     * Returns the usage help message of Jolie.
     *
     * @return the usage help message of Jolie.
     */
    protected String getHelpString() {
        StringBuilder helpBuilder = new StringBuilder();
        helpBuilder.append(getVersionString());
        helpBuilder.append("\n\nUsage: jolie [options] behaviour_file [program arguments]\n\n");
        helpBuilder.append("Available options:\n");
        helpBuilder.append(
                getOptionString("-h, --help", "Display this help information"));
        //TODO include doc for -l and -i
        helpBuilder.append(getOptionString("-C ConstantIdentifier=ConstantValue", "Sets constant ConstantIdentifier to ConstantValue before starting execution \n"
                + "-C ConstantIdentifier=ConstantValue".replaceAll("(.)", " ") + "\t\t\t"
                + "(under Windows use quotes or double-quotes, e.g., -C \"ConstantIdentifier=ConstantValue\" )"));
        helpBuilder.append(
                getOptionString("--connlimit [number]", "Set the maximum number of active connection threads"));
        helpBuilder.append(
                getOptionString("--conncache [number]", "Set the maximum number of cached persistent output connections"));
        helpBuilder.append(
                getOptionString("--correlationAlgorithm [simple|hash]", "Set the algorithm to use for message correlation"));
        helpBuilder.append(
                getOptionString("--log [severe|warning|info|fine]", "Set the logging level (default: info)"));
        helpBuilder.append(
                getOptionString("--typecheck [true|false]", "Check for correlation and other data related typing errors (default: false)"));
        helpBuilder.append(
                getOptionString("--check", "Check for syntactic and semantic errors."));
        helpBuilder.append(
                getOptionString("--trace", "Activate tracer"));
        helpBuilder.append(
                getOptionString("--charset [character encoding, eg. UTF-8]", "Character encoding of the source *.ol/*.iol (default: system-dependent, on GNU/Linux UTF-8)"));
        helpBuilder.append(
                getOptionString("--version", "Display this program version information"));
        return helpBuilder.toString();
    }

    private void parseCommandLineConstant(String input)
            throws IOException {
        try {
            // for command line options use the system's default charset (null)
            Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()), new URI("urn:CommandLine"), null);
            Scanner.Token token = scanner.getToken();
            if (token.is(Scanner.TokenType.ID)) {
                String id = token.content();
                token = scanner.getToken();
                if (token.isNot(Scanner.TokenType.ASSIGN)) {
                    throw new IOException("expected = after constant identifier " + id + ", found token type " + token.type());
                }
                token = scanner.getToken();
                if (token.isValidConstant() == false) {
                    throw new IOException("expected constant value for constant identifier " + id + ", found token type " + token.type());
                }
                constants.put(id, token);
            } else {
                throw new IOException("expected constant identifier, found token type " + token.type());
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns <code>true</code> if the verbose option has been specified, false
     * otherwise.
     *
     * @return <code>true</code> if the verbose option has been specified, false
     * otherwise
     */
    /*	public boolean verbose()
     {
     return verbose;
     }
     */
    /**
     * Returns the type of correlation algorithm that has been specified.
     *
     * @return the type of correlation algorithm that has been specified.
     * @see CorrelationEngine
     */
    public CorrelationEngine.Type correlationAlgorithmType() {
        return correlationAlgorithmType;
    }

    /**
     * Constructor
     *
     * @param args the command line arguments
     * @param parentClassLoader the ClassLoader to use for finding resources
     * @throws jolie.CommandLineException if the command line is not valid or
     * asks for simple information. (like --help and --version)
     * @throws java.io.IOException
     */
    public CommandLineParser(String[] args, ClassLoader parentClassLoader)
            throws CommandLineException, IOException {
        this(args, parentClassLoader, ArgumentHandler.DEFAULT_ARGUMENT_HANDLER);
    }

    /**
     * Constructor
     *
     * @param args the command line arguments
     * @param parentClassLoader the ClassLoader to use for finding resources
     * @param argHandler
     * @throws CommandLineException
     * @throws IOException
     */
    public CommandLineParser(String[] args, ClassLoader parentClassLoader, ArgumentHandler argHandler)
            throws CommandLineException, IOException {
        this(args, parentClassLoader, argHandler, false);
    }

    /**
     * Constructor
     *
     * @param args the command line arguments
     * @param parentClassLoader the ClassLoader to use for finding resources
     * @param ignoreFile do not open file that is given as parameter (used for
     * internal services)
     * @throws CommandLineException
     * @throws IOException
     */
    public CommandLineParser(String[] args, ClassLoader parentClassLoader, boolean ignoreFile)
            throws CommandLineException, IOException {
        this(args, parentClassLoader, ArgumentHandler.DEFAULT_ARGUMENT_HANDLER, ignoreFile);
    }

    /**
     * Constructor
     *
     * @param args the command line arguments
     * @param parentClassLoader the ClassLoader to use for finding resources
     * @param argHandler
     * @param ignoreFile do not open file that is given as parameter (used for
     * internal services)
     * @throws CommandLineException
     * @throws IOException
     */
    public CommandLineParser(String[] args, ClassLoader parentClassLoader, ArgumentHandler argHandler, boolean ignoreFile)
            throws CommandLineException, IOException {
        List< String> argsList = new ArrayList<>(args.length);
        Collections.addAll(argsList, args);

        String csetAlgorithmName = "simple";
        List< String> optionsList = new ArrayList<>();
        boolean bTracer = false;
        boolean bCheck = false;
        boolean bTypeCheck = false; // Default for typecheck

        boolean bJTracer = false;

        Level lLogLevel = Level.INFO;
        List< String> programArgumentsList = new ArrayList<>();
        LinkedList< String> includeList = new LinkedList<>();
        List< String> libList = new ArrayList<>();
        int cLimit = -1;
        int cCache = 100;
        String pwd = new File("").getCanonicalPath();
        includeList.add(pwd);
        includeList.add("include");
        libList.add(pwd);
        libList.add("ext");
        libList.add("lib");
        String olFilepath = null;
        String japUrl = null;
        int i = 0;
        // First parse Jolie arguments with the Jolie program argument
        for (; i < argsList.size() && olFilepath == null; i++) {
            if ("--help".equals(argsList.get(i)) || "-h".equals(argsList.get(i))) {
                throw new CommandLineException(getHelpString());
            } else if ("-C".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                i++;
                try {
                    parseCommandLineConstant(argsList.get(i));
                } catch (IOException e) {
                    throw new CommandLineException("Invalid constant definition, reason: " + e.getMessage());
                }
                optionsList.add(argsList.get(i));
            } else if ("-i".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                i++;
                if (japUrl != null) {
                    argsList.set(i, argsList.get(i).replace("$JAP$", japUrl));
                }
                String[] tmp = PATH_SEPARATOR_PATTERN.split(argsList.get(i));
                Collections.addAll(includeList, tmp);
                optionsList.add(argsList.get(i));
            } else if ("-l".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                i++;
                if (japUrl != null) {
                    argsList.set(i, argsList.get(i).replace("$JAP$", japUrl));
                }
                String[] tmp = PATH_SEPARATOR_PATTERN.split(argsList.get(i));
                Collections.addAll(libList, tmp);
                optionsList.add(argsList.get(i));
            } else if ("--connlimit".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                i++;
                cLimit = Integer.parseInt(argsList.get(i));
                optionsList.add(argsList.get(i));
            } else if ("--conncache".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                i++;
                cCache = Integer.parseInt(argsList.get(i));
                optionsList.add(argsList.get(i));
            } else if ("--correlationAlgorithm".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                i++;
                csetAlgorithmName = argsList.get(i);
                optionsList.add(argsList.get(i));
            } else if ("--typecheck".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                i++;
                String typeCheckStr = argsList.get(i);
                optionsList.add(argsList.get(i));
                if ("false".equals(typeCheckStr)) {
                    bTypeCheck = false;
                } else if ("true".equals(typeCheckStr)) {
                    bTypeCheck = true;
                }
            } else if ("--check".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                bCheck = true;
            } else if ("--trace".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                bTracer = true;
            } else if ("--trace:json".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                bJTracer = true;

            } else if ("--log".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                i++;
                String level = argsList.get(i);
                switch (level) {
                    case "severe":
                        lLogLevel = Level.SEVERE;
                        break;
                    case "warning":
                        lLogLevel = Level.WARNING;
                        break;
                    case "fine":
                        lLogLevel = Level.FINE;
                        break;
                    case "info":
                        lLogLevel = Level.INFO;
                        break;
                }
                optionsList.add(argsList.get(i));
            } else if ("--charset".equals(argsList.get(i))) {
                optionsList.add(argsList.get(i));
                i++;
                charset = argsList.get(i);
                optionsList.add(argsList.get(i));
            } else if ("--version".equals(argsList.get(i))) {
                throw new CommandLineException(getVersionString());
            } else if (argsList.get(i).endsWith(".ol")
                    || argsList.get(i).endsWith(".iol")
                    || argsList.get(i).endsWith(".olc")) {
                if (olFilepath == null) {
                    olFilepath = argsList.get(i);
                } else {
                    programArgumentsList.add(argsList.get(i));
                }
            } else if (argsList.get(i).endsWith(".jap")) {
                if (olFilepath == null) {
                    String japFilename = new File(argsList.get(i)).getCanonicalPath();
                    JarFile japFile = new JarFile(japFilename);
                    Manifest manifest = japFile.getManifest();
                    olFilepath = parseJapManifestForMainProgram(manifest, japFile);
                    if (Helpers.getOperatingSystemType() == Helpers.OSType.Windows) {
                        olFilepath = olFilepath.replace("\\", "/");
                    }
                    libList.add(japFilename);
                    Collection< String> japOptions = parseJapManifestForOptions(manifest);
                    argsList.addAll(i + 1, japOptions);
                    japUrl = japFilename + "!";
                    programDirectory = new File(japFilename).getParentFile();
                } else {
                    programArgumentsList.add(argsList.get(i));
                }
            } else {
                // It's an unrecognized argument
                int newIndex = argHandler.onUnrecognizedArgument(argsList, i);
                if (newIndex == i) {
					// The handler didn't change the index.
                    // We abort so to avoid infinite looping.
                    throw new CommandLineException("Unrecognized command line option: " + argsList.get(i));
                }
                i = newIndex;
            }
        }
        // Now parse the command line arguments for the Jolie program
        for (; i < argsList.size() && olFilepath != null; i++) {
            programArgumentsList.add(argsList.get(i));
        }

        typeCheck = bTypeCheck;
        logLevel = lLogLevel;

        correlationAlgorithmType = CorrelationEngine.Type.fromString(csetAlgorithmName);
        if (correlationAlgorithmType == null) {
            throw new CommandLineException("Unrecognized correlation algorithm: " + csetAlgorithmName);
        }
        optionArgs = optionsList.toArray(new String[optionsList.size()]);
        arguments = programArgumentsList.toArray(new String[programArgumentsList.size()]);
		// whitepages = whitepageList.toArray( new String[ whitepageList.size() ] );

        if (olFilepath == null && !ignoreFile) {
            throw new CommandLineException("Input file not specified.");
        }

        connectionsLimit = cLimit;
        connectionsCache = cCache;

        List< URL> urls = new ArrayList<>();
        for (String path : libList) {
            if (path.contains("!/") && !path.startsWith("jap:") && !path.startsWith("jar:")) {
                path = "jap:file:" + path;
            }
            if (path.endsWith(".jar") || path.endsWith(".jap")) {
                if (path.startsWith("jap:")) {
                    urls.add(new URL(path + "!/"));
                } else {
                    urls.add(new URL("jap:file:" + path + "!/"));
                }
            } else if (new File(path).isDirectory()) {
                urls.add(new URL("file:" + path + "/"));
            } else if (path.endsWith(Constants.fileSeparator + "*")) {
                File dir = new File(path.substring(0, path.length() - 2));
                String jars[] = dir.list((File directory, String filename) -> filename.endsWith(".jar"));
                if (jars != null) {
                    for (String jarPath : jars) {
                        urls.add(new URL("jar:file:" + dir.getCanonicalPath() + '/' + jarPath + "!/"));
                    }
                }
            } else {
                try {
                    urls.add(new URL(path));
                } catch (MalformedURLException e) {
//					e.printStackTrace();
                }
            }
        }
        urls.add(new URL("file:/"));
        libURLs = urls.toArray(new URL[]{});
        jolieClassLoader = new JolieClassLoader(libURLs, parentClassLoader);

        GetOLStreamResult olResult = getOLStream(olFilepath, includeList, jolieClassLoader);

        if (olResult.stream == null) {
            if (ignoreFile) {
                olResult.source = olFilepath;
                olResult.stream = new ByteArrayInputStream(new byte[]{});
            } else if (olFilepath.endsWith(".ol")) {
                // try to read the compiled version of the ol file
                olFilepath += "c";
                olResult = getOLStream(olFilepath, includeList, jolieClassLoader);
                if (olResult.stream == null) {
                    throw new FileNotFoundException(olFilepath);
                }
            } else {
                throw new FileNotFoundException(olFilepath);
            }
        }

        isProgramCompiled = olFilepath.endsWith(".olc");
        tracer = bTracer && !isProgramCompiled;
        jTracer = bJTracer && !isProgramCompiled;
        check = bCheck && !isProgramCompiled;
        programFilepath = new File(olResult.source);
        programStream = olResult.stream;

        includePaths = includeList.toArray(new String[]{});
    }

    /**
     * Adds the standard include and library subdirectories of the program to
     * the classloader paths.
     */
    /* private void addProgramDirectories( List< String > includeList, List< String > libList, String olFilepath )
     {
     File olFile = new File( olFilepath );
     if ( olFile.exists() ) {
     File parent = olFile.getParentFile();
     if ( parent != null && parent.isDirectory() ) {
     String parentPath = parent.getAbsolutePath();
     includeList.add( parentPath );
     includeList.add( parentPath + "/include" );
     libList.add( parentPath );
     libList.add( parentPath + "/lib" );
     }
     }
     } */
    /**
     * Returns the directory in which the main program is located.
     *
     * @return the directory in which the main program is located.
     */
    public File programDirectory() {
        return programDirectory;
    }

    /**
     * Returns the value of the --typecheck option.
     *
     * @return the value of the --typecheck option.
     */
    public boolean typeCheck() {
        return typeCheck;
    }

    /**
     * Returns the classloader to use for the program.
     *
     * @return the classloader to use for the program.
     */
    public JolieClassLoader jolieClassLoader() {
        return jolieClassLoader;
    }

    /**
     * Returns the command line options passed to this command line parser. This
     * does not include the name of the program.
     *
     * @return the command line options passed to this command line parser.
     */
    public String[] optionArgs() {
        return optionArgs;
    }

    private String parseJapManifestForMainProgram(Manifest manifest, JarFile japFile) {
        String filepath = null;
        if (manifest != null) { // See if a main program is defined through a Manifest attribute
            Attributes attrs = manifest.getMainAttributes();
            filepath = attrs.getValue(Constants.Manifest.MAIN_PROGRAM);
        }

        if (filepath == null) { // Main program not defined, we make <japName>.ol and <japName>.olc guesses
            String name = new File(japFile.getName()).getName();
            filepath = new StringBuilder()
                    .append(name.subSequence(0, name.lastIndexOf(".jap")))
                    .append(".ol")
                    .toString();
            if (japFile.getEntry(filepath) == null) {
                filepath = null;
                filepath = filepath + 'c';
                if (japFile.getEntry(filepath) == null) {
                    filepath = null;
                }
            }
        }

        if (filepath != null) {
            filepath = new StringBuilder()
                    .append("jap:file:")
                    .append(japFile.getName())
                    .append("!/")
                    .append(filepath)
                    .toString();
        }
        return filepath;
    }

    private Collection< String> parseJapManifestForOptions(Manifest manifest)
            throws IOException {
        Collection< String> optionList = new ArrayList();
        if (manifest != null) {
            Attributes attrs = manifest.getMainAttributes();
            String options = attrs.getValue(Constants.Manifest.OPTIONS);
            if (options != null) {
                String[] tmp = OPTION_SEPARATOR_PATTERN.split(options);
                Collections.addAll(optionList, tmp);
            }
        }
        return optionList;
    }

    private static class GetOLStreamResult {

        private String source;
        private InputStream stream;
    }

    private GetOLStreamResult getOLStream(String olFilepath, LinkedList< String> includePaths, ClassLoader classLoader)
            throws FileNotFoundException, IOException {
        GetOLStreamResult result = new GetOLStreamResult();

        URL olURL = null;
        File f = new File(olFilepath).getAbsoluteFile();
        if (f.exists()) {
            result.stream = new FileInputStream(f);
            result.source = f.toURI().getSchemeSpecificPart();
            programDirectory = f.getParentFile();
        } else {
            for (int i = 0; i < includePaths.size() && result.stream == null; i++) {
                f = new File(
                        includePaths.get(i)
                        + jolie.lang.Constants.fileSeparator
                        + olFilepath
                );
                if (f.exists()) {
                    f = f.getAbsoluteFile();
                    result.stream = new FileInputStream(f);
                    result.source = f.toURI().getSchemeSpecificPart();
                    programDirectory = f.getParentFile();
                }
            }
            if (result.stream == null) {
                try {
                    olURL = new URL(olFilepath);
                    result.stream = olURL.openStream();
                    result.source = olFilepath;
                    if (result.stream == null) {
                        throw new MalformedURLException();
                    }
                } catch (MalformedURLException e) {
                    olURL = classLoader.getResource(olFilepath);
                    if (olURL != null) {
                        result.stream = olURL.openStream();
                        result.source = olFilepath;
                    }
                }
                if (programDirectory == null && olURL != null && olURL.getPath() != null) {
                    // Try to extract the parent directory of the JAP/JAR library file
                    try {
                        File urlFile = new File(JapURLConnection.nestingSeparatorPattern.split(new URI(olURL.getPath()).getSchemeSpecificPart())[0]).getAbsoluteFile();
                        if (urlFile.exists()) {
                            programDirectory = urlFile.getParentFile();
                        }
                    } catch (URISyntaxException e) {
                    }
                }
            }
        }
        if (result.stream != null) {
            if (f.exists() && f.getParent() != null) {
                includePaths.addFirst(f.getParent());
            } else if (olURL != null) {
                String urlString = olURL.toString();
                includePaths.addFirst(urlString.substring(0, urlString.lastIndexOf('/') + 1));
            }

            result.stream = new BufferedInputStream(result.stream);
        }
        return result;
    }

    /**
     * A handler for unrecognized arguments, meant to be implemented by classes
     * that wants to extend the behaviour of {@link jolie.CommandLineParser}.
     *
     * @author Fabrizio Montesi
     */
    public interface ArgumentHandler {

        /**
         * Called when {@link CommandLineParser} cannot recognize a command line
         * argument.
         *
         * @param argumentsList the argument list.
         * @param index the index at which the unrecognized argument has been
         * found in the list.
         * @return the new index at which the {@link CommandLineParser} should
         * continue parsing the arguments.
         * @throws CommandLineException if the argument is invalid or not
         * recognized.
         */
        public int onUnrecognizedArgument(List< String> argumentsList, int index)
                throws CommandLineException;

        /**
         * Default {@link ArgumentHandler}. It just throws a
         * {@link CommandLineException} when it finds an unrecognised option.
         */
        public static ArgumentHandler DEFAULT_ARGUMENT_HANDLER
                = new ArgumentHandler() {
                    public int onUnrecognizedArgument(List< String> argumentsList, int index)
                    throws CommandLineException {
                        throw new CommandLineException("Unrecognized command line option: " + argumentsList.get(index));
                    }
                };
    }
}
