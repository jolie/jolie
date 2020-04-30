/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang.parse.module;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import jolie.lang.Constants;
import jolie.util.Helpers;

/**
 * Finder find a module target, it is divided into two class, a finder for relative path or absolute
 * path
 * 
 * @return
 * @throws ModuleException if there is finder cannot locate any file.
 */
public abstract class Finder
{
    protected final String[] target;
    private final List< String > remainingToken;
    private final List< String > lookedPaths;
    private boolean isJapPackage = false;

    protected Finder( String[] target )
    {
        this.target = target;
        this.lookedPaths = new ArrayList<>();
        this.remainingToken = new ArrayList<>( Arrays.asList( target ) );
    }

    /**
     * @return list of looked path during finding process
     */
    public String[] lookupedPath()
    {
        return lookedPaths.toArray( new String[0] );
    }

    /**
     * @return an index of module string token of the importstatement module target
     */
    protected int moduleIndex()
    {
        return target.length - 1;
    }

    /**
     * Find a module target, return a File object points to first found path.
     * 
     * @return Source object
     * @throws FileNotFoundException if there is finder cannot locate any file.
     */
    public abstract Source find() throws FileNotFoundException;

    /**
     * returns a Finder object corresponding to target either it is a relative import(starts with .)
     * or absolute one
     */
    static Finder getFinderForTarget( URI source, String[] includePathStrings, String[] target )
    {
        final boolean isRelativeImport = target[0].isEmpty() ? true : false;

        if ( isRelativeImport ) {
            return new RelativePathFinder( target, source );
        } else {
            return new AbsolutePathFinder( target, source, includePathStrings );
        }
    }

    /**
     * @return an array of tokens except last one, which denote the module name
     */
    protected abstract String[] packagesToken();

    /**
     * @return an importing module name
     */
    protected String moduleName()
    {
        return this.target[this.target.length - 1];
    }

    /**
     * Perform a lookup to a directory name from basePath
     * 
     * @param basePath base path for lookup
     * @param dirName  directory name
     * @return a new path of directory,
     * @throws FileNotFoundException file is not found
     */
    protected Path directoryLookup( Path basePath, String dirName ) throws FileNotFoundException
    {
        Path dirPath = basePath.resolve( dirName );
        lookedPaths.add( dirPath.toString() );
        if ( Files.exists( dirPath ) && Files.isDirectory( dirPath ) ) {
            return dirPath;
        }
        throw new FileNotFoundException();
    }

    /**
     * Perform a lookup to a jap filename from basePath
     * 
     * @param basePath base path for lookup
     * @param filename a filename
     * @return a new File of jap file, null if file is not found
     */
    private File japLookup( Path basePath, String filename ) throws FileNotFoundException
    {
        Path japPath = basePath.resolve( filename + ".jap" );
        lookedPaths.add( japPath.toString() );
        if ( Files.exists( japPath ) ) {
            return japPath.toFile();
        }
        throw new FileNotFoundException();
    }

    /**
     * Perform a lookup to a ol filename from basePath
     * 
     * @param basePath base path for lookup
     * @param filename a filename
     * @return a new path of ol file, null if file is not found
     */
    private File olLookup( Path basePath, String filename ) throws FileNotFoundException
    {
        Path olPath = basePath.resolve( filename + ".ol" );
        lookedPaths.add( olPath.toString() );
        if ( Files.exists( olPath ) ) {
            return olPath.toFile();
        }
        throw new FileNotFoundException();
    }

    /**
     * perform lookup to the target module's package path
     * 
     * @param basePath base path for lookup
     * @return a path corresponding to the package
     * @throws FileNotFoundException package directory is not found.
     */
    protected Path locatePackage( Path basePath ) throws FileNotFoundException
    {
        String[] packagesToken = this.packagesToken();
        for (int i = 0; i < packagesToken.length; i++) {
            if ( isJapPackage ) {
                break;
            }
            String pathString = packagesToken[i];
            // try directory lookup
            try {
                basePath = this.directoryLookup( basePath, pathString );
                remainingToken.remove( i );
                continue;
            } catch (FileNotFoundException e) {
            }

            // try jap lookup
            try {
                basePath = this.japLookup( basePath, pathString ).toPath();
                isJapPackage = true;
                remainingToken.remove( i );
            } catch (FileNotFoundException e) {
                throw e;
            }
        }
        return basePath;
    }

    /**
     * perform lookup to the target module's
     * 
     * @param basePath base path for lookup
     * @return Source object corresponding to the module.
     * @throws ModuleException when a module is not found
     */
    protected Source locateModule( Path basePath ) throws FileNotFoundException
    {
        Optional< Source > source = Optional.empty();
        final Path packagePath = this.locatePackage( basePath );
        if ( isJapPackage ) {
            try {
                return new JapSource( packagePath.toFile(),
                        String.join( Constants.fileSeparator, remainingToken ) );
            } catch (IOException e) {
                throw new FileNotFoundException();
            }
        }
        source = Helpers.firstNonNull( () -> {
            try {
                File olTargetFile = this.olLookup( packagePath, this.moduleName() );
                return new FileSource( olTargetFile );
            } catch (FileNotFoundException e) {
                return null;
            }
        }, () -> {
            try {
                File japTargetFile = this.japLookup( packagePath, this.moduleName() );
                return new JapSource( japTargetFile );
            } catch (IOException e) {
                return null;
            }
        } );

        if ( !source.isPresent() ) {
            throw new FileNotFoundException();
        }
        return source.get();
    }

}


/**
 * A class represent the finder for relative path importstatement
 */
class RelativePathFinder extends Finder
{

    /**
     * an URI source of the caller
     */
    private final URI source;
    private int packagesTokenStartIndex = 0;

    protected RelativePathFinder( String[] target, URI source )
    {
        super( target );
        assert source != null;
        this.source = source;
    }


    /**
     * resolve path from source, each dot prefix means 1 level higer from the caller path directory
     */
    private Path resolveDotPrefix()
    {
        Path sourcePath = Paths.get( source );
        Path basePath;
        if ( !sourcePath.toFile().isDirectory() ) {
            basePath = sourcePath.getParent();
        } else {
            basePath = sourcePath;
        }
        int i = 1;
        for (; i < super.moduleIndex(); i++) {
            if ( target[i].isEmpty() ) {
                basePath = basePath.getParent();
            } else {
                break;
            }
        }
        packagesTokenStartIndex = i;
        return basePath;
    }

    @Override
    public Source find() throws FileNotFoundException
    {
        Path basePath = resolveDotPrefix();
        try {
            Source moduleFile = super.locateModule( basePath );
            return moduleFile;
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException( "module " + Arrays.toString( target )
                    + " not found, lookup path: " + Arrays.toString( this.lookupedPath() ) );
        }
    }

    @Override
    protected String[] packagesToken()
    {
        return Arrays.copyOfRange( this.target, packagesTokenStartIndex, this.target.length - 1 );
    }

}


/**
 * A class represent the finder for absolute path importstatement
 */
class AbsolutePathFinder extends Finder
{
    private final String[] includePathStrings;
    private final URI source;

    protected AbsolutePathFinder( String[] target, URI source, String[] includePathStrings )
    {
        super( target );
        assert source != null;
        this.includePathStrings = includePathStrings;
        this.source = source;
    }

    @Override
    public Source find() throws FileNotFoundException
    {
        Path sourcePath = Paths.get( source );
        Path basePath;

        // try lookup in package directory
        try {
            if ( !sourcePath.toFile().isDirectory() ) {
                basePath = Paths.get( sourcePath.getParent().toString(), Constants.PACKAGES_DIR );
            } else {
                basePath = Paths.get( sourcePath.toString(), Constants.PACKAGES_DIR );
            }
            Source module = super.locateModule( basePath );
            return module;
        } catch (FileNotFoundException e) {
        }

        // try lookup in includePaths
        for (String baseDir : this.includePathStrings) {
            basePath = Paths.get( baseDir, Constants.PACKAGES_DIR );
            try {
                Source module = super.locateModule( basePath );
                return module;
            } catch (FileNotFoundException e) {
            }
        }

        // throw if module not found
        throw new FileNotFoundException(
                "unable to locate module " + String.join( ".", this.target ) + " , lookup path: "
                        + Arrays.toString( this.lookupedPath() ) );
    }

    @Override
    protected String[] packagesToken()
    {
        return Arrays.copyOfRange( this.target, 0, this.target.length - 1 );
    }

}

