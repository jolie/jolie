package jolie.lang.parse.module;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class ModuleFinderImplTest {

	private final String[] packagesPath = new String[] { "src/test/resources" };

	private final ModuleFinder finder = new ModuleFinderImpl( packagesPath );

	/**
	 * Find a module with the given {@code importPath} starting from the given {@code originURI}.
	 *
	 * @param originURI the origin path to start searching for the module
	 * @param importPath the import path of the module to find
	 */
	void findExpectSuccess( URI originURI, ImportPath importPath, URI expectSourceURI ) {
		assertDoesNotThrow( () -> {
			ModuleSource source = finder.find( originURI, importPath );
			assertTrue( expectSourceURI.equals( source.uri() ),
				"expected " + expectSourceURI + " but found " + source.uri() );
			InputStream is = source.openStream();
			is.close();
		} );
	}

	@Test
	void testImport() {
		var importPath = new ImportPath( Arrays.asList( "", "d1", "d2", "twice_api" ) ); // .d1.d2.twice_api
		var originURI = Paths.get( "src/test/resources/jap/twice/main.ol" ).toUri();
		findExpectSuccess( originURI, importPath,
			Paths.get( "src/test/resources/jap/twice/d1/d2/twice_api.ol" ).toUri() );
	}

	@Test
	void testImportOneDotOmittedModule() {
		var importPath = new ImportPath( Arrays.asList( "" ) ); // from . import ... translates to [""]
		var originURI = Paths.get( "src/test/resources/service.ol" ).toUri();
		findExpectSuccess( originURI, importPath, Paths.get( "src/test/resources/main.ol" ).toUri() );
	}

	@Test
	void testImportOneDot() {
		var importPath = new ImportPath( Arrays.asList( "", "service" ) ); // from .service import ... translates to
																			// [""]
		var originURI = Paths.get( "src/test/resources/main.ol" ).toUri();
		findExpectSuccess( originURI, importPath, Paths.get( "src/test/resources/service.ol" ).toUri() );
	}

	@Test
	void testImportTwoDotsOmittedModule() {
		var importPath = new ImportPath( Arrays.asList( "", "" ) ); // from .. import ... translates to ["", ""]
		var originURI = Paths.get( "src/test/resources/tmp/service.ol" ).toUri();
		findExpectSuccess( originURI, importPath, Paths.get( "src/test/resources/main.ol" ).toUri() );
	}

	@Test
	void testImportTwoDots() {
		var importPath = new ImportPath( Arrays.asList( "", "", "service" ) ); // from ..service import ... translates
																				// to ["", ""]
		var originURI = Paths.get( "src/test/resources/tmp/main.ol" ).toUri();
		findExpectSuccess( originURI, importPath, Paths.get( "src/test/resources/service.ol" ).toUri() );
	}

	@Test
	void testImportThreeDotsOmittedModule() {
		var importPath = new ImportPath( Arrays.asList( "", "", "" ) ); // from .. import ... translates to ["", ""]
		var originURI = Paths.get( "src/test/resources/tmp/tmp/service.ol" ).toUri();
		findExpectSuccess( originURI, importPath, Paths.get( "src/test/resources/main.ol" ).toUri() );
	}

	@Test
	void testImportThreeDots() {
		var importPath = new ImportPath( Arrays.asList( "", "", "", "service" ) ); // from ..service import ...
																					// translates to ["", ""]
		var originURI = Paths.get( "src/test/resources/tmp/tmp/main.ol" ).toUri();
		findExpectSuccess( originURI, importPath, Paths.get( "src/test/resources/service.ol" ).toUri() );
	}

	@Test
	void testImportPackage() {
		var importPath = new ImportPath( Arrays.asList( "console" ) ); // from .. import ... translates to ["", ""]
		var originURI = Paths.get( "src/test/resources/main.ol" ).toUri();
		findExpectSuccess( originURI, importPath, Paths.get( "src/test/resources/packages/console.ol" ).toUri() );
	}

	@Test
	void testImportJAP() {
		// setup cwd
		var oldValue = System.getProperty( "user.dir" );
		System.setProperty( "user.dir", Paths.get( "src/test/resources" ).toAbsolutePath().toString() );
		final ModuleFinder finder = new ModuleFinderImpl( packagesPath );

		var importPath = new ImportPath( Arrays.asList( "twice" ) ); // from .. import ... translates to ["", ""]
		var originURI = Paths.get( "src/test/resources/main.ol" ).toAbsolutePath().toUri();

		assertDoesNotThrow( () -> {
			var expectSourceURI =
				new URI( "jap:" + Paths.get( "src/test/resources/lib/twice.jap" ).toAbsolutePath().toUri().toString()
					+ "!/twice/main.ol" );
			ModuleSource source = finder.find( originURI, importPath );
			assertEquals( expectSourceURI, source.uri(),
				"expected " + expectSourceURI + " but found " + source.uri() );
			InputStream is = source.openStream();
			is.close();
		} );
		System.setProperty( "user.dir", oldValue );
	}
}
