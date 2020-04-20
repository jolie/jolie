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
    private final List< String > lookedPaths;

    protected Finder( String[] target )
    {
        this.target = target;
        this.lookedPaths = new ArrayList<>();
    }

    public String[] lookupedPath()
    {
        return lookedPaths.toArray( new String[0] );
    }

    protected int moduleIndex()
    {
        return target.length - 1;
    }

    /**
     * Find a module target, return a File object points to first found path.
     * 
     * @return instance of Source object
     * @throws ModuleException if there is finder cannot locate any file.
     */
    public abstract Source find() throws ModuleException;

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
     * @return a new path of directory, throw exception if file is not found
     */
    protected Path directoryLookup( Path basePath, String dirName ) throws FileNotFoundException
    {
        Path dirPath = basePath.resolve( dirName );
        lookedPaths.add( dirPath.toString() );
        if ( Files.exists( dirPath ) && Files.isDirectory( dirPath ) ) {
            return dirPath;
        }
        throw new FileNotFoundException( dirPath.toString() );
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
        throw new FileNotFoundException( japPath.toString() );
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
        throw new FileNotFoundException( olPath.toString() );
    }

    protected Path locatePackage( Path basePath ) throws FileNotFoundException
    {
        Path ret = basePath;
        for (String pathString : this.packagesToken()) {
            ret = this.directoryLookup( basePath, pathString );
            if ( ret != null ) {
                return ret;
            }
        }
        return ret;
    }



    protected Source locateModule( Path basePath ) throws ModuleException
    {
        Optional< Source > source = Optional.empty();
        try {
            final Path packagePath = this.locatePackage( basePath );
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
        } catch (FileNotFoundException e) {
            throw new ModuleException( "unable to locate package", e );
        }

        if ( source.isEmpty() ) {
            throw new ModuleException( "unable to locate module" );
        }
        return source.get();
    }

}


class RelativePathFinder extends Finder
{

    private final URI source;
    private int packagesTokenStartIndex = 0;

    protected RelativePathFinder( String[] target, URI source )
    {
        super( target );
        assert source != null;
        this.source = source;
    }


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
    public Source find() throws ModuleException
    {
        Path basePath = resolveDotPrefix();
        Source moduleFile = super.locateModule( basePath );
        return moduleFile;
    }

    @Override
    protected String[] packagesToken()
    {
        return Arrays.copyOfRange( this.target, packagesTokenStartIndex, this.target.length - 1 );
    }

}


class AbsolutePathFinder extends Finder
{
    private final String[] includePathStrings;
    private final URI source;

    protected AbsolutePathFinder( String[] target, URI source, String[] includePathStrings )
    {
        super( target );
        this.includePathStrings = includePathStrings;
        this.source = source;
    }

    @Override
    public Source find() throws ModuleException
    {
        Path sourcePath = Paths.get( source );
        Path basePath;

        // try lookup at package directory
        try {
            if ( !sourcePath.toFile().isDirectory() ) {
                basePath = Paths.get( sourcePath.getParent().toString(), Constants.PACKAGES_DIR );
            } else {
                basePath = Paths.get( sourcePath.toString(), Constants.PACKAGES_DIR );
            }
            Source module = super.locateModule( basePath );
            return module;
        } catch (ModuleException e) {
        }

        // try lookup at includePaths
        try {
            for (String baseDir : this.includePathStrings) {
                basePath = Paths.get( baseDir, Constants.PACKAGES_DIR );
                Source module = super.locateModule( basePath );
                return module;
            }
        } catch (ModuleException e) {
        }

        // throw if module not found
        throw new ModuleException( "unable to locate module" );
    }

    @Override
    protected String[] packagesToken()
    {
        return Arrays.copyOfRange( this.target, 0, this.target.length - 1 );
    }

}

