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
import java.util.Arrays;
import jolie.lang.Constants;

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

    protected Finder( String[] target )
    {
        this.target = target;
    }

    /**
     * Find a module target, return a File object points to first found path.
     * 
     * @return Source object
     * @throws FileNotFoundException if there is finder cannot locate any file.
     */
    public abstract Source find() throws FileNotFoundException;

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
     * Perform a lookup to a jap filename from basePath
     * 
     * @param basePath base path for lookup
     * @param filename a filename
     * @return a new File of jap file, null if file is not found
     */
    protected File japLookup( Path basePath, String filename ) throws FileNotFoundException
    {
        Path japPath = basePath.resolve( filename + ".jap" );
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
    protected File olLookup( Path basePath, String filename ) throws FileNotFoundException
    {
        Path olPath = basePath.resolve( filename + ".ol" );
        if ( Files.exists( olPath ) ) {
            return olPath.toFile();
        }
        throw new FileNotFoundException( olPath.toString() );
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
        Path packagePath = Paths.get( basePath.toString(), this.packagesToken() );
        File olTargetFile = this.olLookup( packagePath, this.moduleName() );
        if ( !olTargetFile.exists() ) {
            throw new FileNotFoundException( olTargetFile.toString() );
        }
        return new FileSource( olTargetFile );
    }

}


/**
 * A class represent the finder for relative ImportStatement
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
        for (; i < this.target.length - 1; i++) {
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
        Source moduleFile = super.locateModule( basePath );
        return moduleFile;
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

    private final Path[] packagesDirectories;
    private final Path workingDirectory;

    public AbsolutePathFinder( String[] target, Path workingDirectory, Path[] packagesDirectories )
    {
        super( target );
        this.packagesDirectories = packagesDirectories;
        this.workingDirectory = workingDirectory;
    }


    @Override
    public Source find() throws FileNotFoundException
    {
        /**
         * 1. Try to resolve P directly from WDIR.
         * 2. Check if FIRST.jap is in WDIR/lib. If so, resolve REST inside of this jap.
         * 3. Try to resolve P from the list of packages directories.
         */
        FileNotFoundException err = null;
        try {
            // 1. resolve from Working directory
            Source moduleFile = super.locateModule( workingDirectory );
            return moduleFile;
        } catch (FileNotFoundException e) {
            err = e;
        }

        try {
            // 2. WDIR/lib/FIRST.jap
            File japFile = this.japLookup( workingDirectory.resolve( "lib" ), this.target[0] );
            return new JapSource( japFile, String.join( Constants.fileSeparator,
                    Arrays.copyOfRange( this.target, 1, this.target.length ) ) );
        } catch (IOException e) {
            err.addSuppressed( e );
        }

        try {
            // * 3. Try to resolve P from the list of packages directories.
            for (Path packagePath : packagesDirectories) {
                Source moduleFile = super.locateModule( packagePath );
                return moduleFile;
            }
        } catch (FileNotFoundException e) {
            err.addSuppressed( e );
        }

        // throw if module not found
        throw err;
    }

    @Override
    protected String[] packagesToken()
    {
        return Arrays.copyOfRange( this.target, 0, this.target.length - 1 );
    }

}

