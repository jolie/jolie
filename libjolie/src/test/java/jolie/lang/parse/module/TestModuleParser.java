package jolie.lang.parse.module;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.module.ModuleCrawler.ModuleCrawlerResult;
import jolie.util.CheckUtility;
import jolie.util.PortStub;
import jolie.util.TestingObjectsCreator;
import jolie.util.jap.JolieURLStreamHandlerFactory;

public class TestModuleParser {

	static {
		JolieURLStreamHandlerFactory.registerInVM();
	}

	private static String BASE_DIR = "imports/";
	private static URL baseDir = TestModuleParser.class.getClassLoader().getResource( BASE_DIR );

	private static String[] includePaths = new String[ 0 ];

	@Test
	void testImportNestedModules() throws FileNotFoundException, URISyntaxException {
		ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
			this.getClass().getClassLoader() );
		ModuleCrawler crawler = new ModuleCrawler( Paths.get( baseDir.toURI() ), includePaths, parser );

		URI target = Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri();

		Map.Entry< URI, Set< String > > aOLSymbols = TestingObjectsCreator.createURISymbolsMap(
			Paths.get( baseDir.toURI() ).resolve( "A.ol" ).toUri(), "A", "from_b" );

		Map.Entry< URI, Set< String > > packageBDotBSymbols =
			TestingObjectsCreator.createURISymbolsMap(
				Paths.get( baseDir.toURI() ).resolve( "A" ).resolve( "B.ol" ).toUri(),
				"C_type", "b_type", "b_type" );

		Map.Entry< URI, Set< String > > packageCSymbols = TestingObjectsCreator
			.createURISymbolsMap( Paths.get( baseDir.toURI() ).resolve( "A" )
				.resolve( "packages" ).resolve( "C.ol" ).toUri(), "b_type", "c" );

		Map< URI, Set< String > > expectedSourceSymbols =
			Stream.of( aOLSymbols, packageBDotBSymbols, packageCSymbols ).collect(
				Collectors.toMap( elem -> (URI) elem.getKey(), elem -> elem.getValue() ) );

		assertDoesNotThrow( () -> {
			// parse a program
			ModuleRecord mainRecord = parser.parse( target, includePaths );

			// crawl dependencies
			ModuleCrawlerResult crawlResult = crawler.crawl( mainRecord );

			// check symbols
			Set< URI > visitedURI = new HashSet<>();
			for( ModuleRecord mr : crawlResult.toMap().values() ) {
				if( expectedSourceSymbols.containsKey( mr.source() ) ) {
					Set< String > expectedSymbols = expectedSourceSymbols.get( mr.source() );
					CheckUtility.checkSymbols( mr.symbolTable(), expectedSymbols );
					visitedURI.add( mr.source() );
				} else {
					throw new Exception( "unexpected source " + mr.source().toString() );
				}
			}

			if( !visitedURI.removeAll( expectedSourceSymbols.keySet() ) ) {
				throw new Exception( "source " + Arrays.toString( visitedURI.toArray() )
					+ " not found in crawl result" );
			}

			GlobalSymbolReferenceResolver symbolResolver =
				new GlobalSymbolReferenceResolver( crawlResult );

			// resolve symbols
			symbolResolver.resolveExternalSymbols();

			// check if all external symbol is linked
			for( ModuleRecord mr : crawlResult.toMap().values() ) {
				CheckUtility.checkSymbolNodeLinked( mr.symbolTable() );
			}

		} );

	}

	@Test
	void testImportWildCard() throws FileNotFoundException, URISyntaxException {
		URI target = Paths.get( baseDir.toURI() ).resolve( "test_wildcard.ol" ).toUri();
		ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
			this.getClass().getClassLoader() );
		ModuleCrawler crawler = new ModuleCrawler( Paths.get( baseDir.toURI() ), includePaths, parser );

		Map.Entry< URI, Set< String > > expectedSymbolsRoot =
			TestingObjectsCreator.createURISymbolsMap( target, "date", "number", "foo", "bar",
				"baz", "dateFoo", "fooIface" );
		Map.Entry< URI, Set< String > > expectedSymbolsExt =
			TestingObjectsCreator.createURISymbolsMap(
				Paths.get( baseDir.toURI() ).resolve( "packages" ).resolve( "type.ol" )
					.toFile().toURI(),
				"date", "number", "foo", "bar", "baz", "dateFoo" );

		Map< URI, Set< String > > expectedSourceSymbols =
			Stream.of( expectedSymbolsRoot, expectedSymbolsExt ).collect(
				Collectors.toMap( elem -> elem.getKey(), elem -> elem.getValue() ) );

		Map.Entry< String, Set< String > > ifaceEntry =
			TestingObjectsCreator.createInterfaceStub( "fooIface", "fooOp" );

		PortStub opPort = new PortStub( "OP",
			Stream.of( ifaceEntry ).collect(
				Collectors.toMap( elem -> elem.getKey(), elem -> elem.getValue() ) ),
			"fooOp" );

		Map.Entry< String, Set< String > > ifaceEntry2 =
			TestingObjectsCreator.createInterfaceStub( "fooIface", "fooOp" );

		PortStub opPort2 = new PortStub( "OP2",
			Stream.of( ifaceEntry2 ).collect(
				Collectors.toMap( elem -> elem.getKey(), elem -> elem.getValue() ) ),
			"fooOp" );

		Map< String, PortStub > expectedOutputPorts =
			TestingObjectsCreator.createExpectedPortMap( opPort, opPort2 );

		assertDoesNotThrow( () -> {

			// parse a program
			ModuleRecord mainRecord = parser.parse( target, includePaths );

			ModuleCrawlerResult crawlResult = crawler.crawl( mainRecord );
			GlobalSymbolReferenceResolver symbolResolver =
				new GlobalSymbolReferenceResolver( crawlResult );
			symbolResolver.resolveExternalSymbols();

			// check symbols
			Set< URI > visitedURI = new HashSet<>();
			for( ModuleRecord mr : crawlResult.toMap().values() ) {
				if( expectedSourceSymbols.containsKey( mr.source() ) ) {
					Set< String > expectedSymbols = expectedSourceSymbols.get( mr.source() );
					CheckUtility.checkSymbols( mr.symbolTable(), expectedSymbols );
					visitedURI.add( mr.source() );
				}
			}

			if( !visitedURI.removeAll( expectedSourceSymbols.keySet() ) ) {
				throw new Exception( "source " + Arrays.toString( visitedURI.toArray() )
					+ " not found in crawl result" );
			}

			symbolResolver.resolveLinkedType();
			// check types
			CheckUtility.checkOutputPorts( mainRecord.program(), expectedOutputPorts );
			// check semantic, all linked type should be set
			CheckUtility.checkSemantic( mainRecord.program(), symbolResolver.symbolTables(),
				false );

		} );
	}


	@Test
	void testImportCyclicDependency() throws FileNotFoundException, URISyntaxException {
		URI target = Paths.get( baseDir.toURI() ).resolve( "cyclic" ).resolve( "A.ol" ).toUri();
		ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
			this.getClass().getClassLoader() );
		ModuleCrawler crawler = new ModuleCrawler( Paths.get( baseDir.toURI() ), includePaths, parser );

		Map.Entry< URI, Set< String > > expectedSymbolsRoot =
			TestingObjectsCreator.createURISymbolsMap( target, "foo", "bar" );
		Map.Entry< URI, Set< String > > expectedSymbolsExt =
			TestingObjectsCreator.createURISymbolsMap( target.resolve( "B.ol" ), "foo", "bar" );

		Map< URI, Set< String > > expectedSourceSymbols =
			Stream.of( expectedSymbolsRoot, expectedSymbolsExt ).collect(
				Collectors.toMap( elem -> elem.getKey(), elem -> elem.getValue() ) );

		Set< String > expectedType = new HashSet<>( Arrays.asList( "foo" ) );

		assertDoesNotThrow( () -> {

			// parse a program
			ModuleRecord mainRecord = parser.parse( target, includePaths );

			ModuleCrawlerResult crawlResult = crawler.crawl( mainRecord );
			GlobalSymbolReferenceResolver symbolResolver =
				new GlobalSymbolReferenceResolver( crawlResult );
			symbolResolver.resolveExternalSymbols();

			// check symbols
			Set< URI > visitedURI = new HashSet<>();
			for( ModuleRecord mr : crawlResult.toMap().values() ) {
				if( expectedSourceSymbols.containsKey( mr.source() ) ) {
					Set< String > expectedSymbols = expectedSourceSymbols.get( mr.source() );
					CheckUtility.checkSymbols( mr.symbolTable(), expectedSymbols );
					visitedURI.add( mr.source() );
				} else {
					throw new Exception( "unexpected source " + mr.source().toString() );
				}
			}

			if( !visitedURI.removeAll( expectedSourceSymbols.keySet() ) ) {
				throw new Exception( "source " + Arrays.toString( visitedURI.toArray() )
					+ " not found in crawl result" );
			}

			symbolResolver.resolveLinkedType();
			// check types
			CheckUtility.checkTypes( mainRecord.program(), expectedType );
			// check semantic, all linked type should be set
			CheckUtility.checkSemantic( mainRecord.program(), symbolResolver.symbolTables(),
				false );

		} );
	}


	@Test
	void testImportJap() throws URISyntaxException, FileNotFoundException {
		URI target = Paths.get( baseDir.toURI() ).resolve( "test_jap.ol" ).toUri();
		ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
			this.getClass().getClassLoader() );
		ModuleCrawler crawler = new ModuleCrawler( Paths.get( baseDir.toURI() ), includePaths, parser );

		Map.Entry< String, Set< String > > ifaceEntry =
			TestingObjectsCreator.createInterfaceStub( "TwiceAPI", "twice" );

		PortStub opPort = new PortStub( "OP",
			Stream.of( ifaceEntry ).collect(
				Collectors.toMap( elem -> elem.getKey(), elem -> elem.getValue() ) ),
			"twice" );

		Map< String, PortStub > expectedOutputPorts =
			TestingObjectsCreator.createExpectedPortMap( opPort );

		assertDoesNotThrow( () -> {

			// parse a program
			ModuleRecord mainRecord = parser.parse( target, includePaths );

			ModuleCrawlerResult crawlResult = crawler.crawl( mainRecord );
			GlobalSymbolReferenceResolver symbolResolver =
				new GlobalSymbolReferenceResolver( crawlResult );
			symbolResolver.resolveExternalSymbols();

			symbolResolver.resolveLinkedType();

			// check interface in outputPort
			CheckUtility.checkOutputPorts( mainRecord.program(), expectedOutputPorts );

			// check semantic, all linked type should be set
			CheckUtility.checkSemantic( mainRecord.program(), symbolResolver.symbolTables(),
				false );

		} );
	}


	@Test
	void testImportInterface()
		throws FileNotFoundException, URISyntaxException, FileNotFoundException {
		URI target = Paths.get( baseDir.toURI() ).resolve( "test_iface.ol" ).toUri();
		ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), includePaths,
			this.getClass().getClassLoader() );
		ModuleCrawler crawler = new ModuleCrawler( Paths.get( baseDir.toURI() ), includePaths, parser );

		Map.Entry< String, Set< String > > ifaceEntry =
			TestingObjectsCreator.createInterfaceStub( "twiceIface", "twice" );

		PortStub ipPort = new PortStub( "IP",
			Stream.of( ifaceEntry ).collect(
				Collectors.toMap( elem -> elem.getKey(), elem -> elem.getValue() ) ),
			"twice" );
		PortStub opPort = new PortStub( "OP",
			Stream.of( ifaceEntry ).collect(
				Collectors.toMap( elem -> elem.getKey(), elem -> elem.getValue() ) ),
			"twice" );

		Map< String, PortStub > expectedInputPorts =
			TestingObjectsCreator.createExpectedPortMap( ipPort );
		Map< String, PortStub > expectedOutputPorts =
			TestingObjectsCreator.createExpectedPortMap( opPort );

		assertDoesNotThrow( () -> {

			// parse a program
			ModuleRecord mainRecord = parser.parse( target, includePaths );

			ModuleCrawlerResult crawlResult = crawler.crawl( mainRecord );
			GlobalSymbolReferenceResolver symbolResolver =
				new GlobalSymbolReferenceResolver( crawlResult );
			symbolResolver.resolveExternalSymbols();

			symbolResolver.resolveLinkedType();
			CheckUtility.checkInputPorts( mainRecord.program(), expectedInputPorts );
			CheckUtility.checkOutputPorts( mainRecord.program(), expectedOutputPorts );

			// check semantic, all linked type should be set
			CheckUtility.checkSemantic( mainRecord.program(), symbolResolver.symbolTables(),
				false );

		} );
	}

	@Test
	void testImportType() throws FileNotFoundException, IOException, ParserException,
		ModuleException, URISyntaxException {
		String code = "from packages.type import date, number, foo, bar, baz, dateFoo";
		InputStream is = new ByteArrayInputStream( code.getBytes() );

		ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[ 0 ],
			this.getClass().getClassLoader() );
		ModuleCrawler crawler = new ModuleCrawler( Paths.get( baseDir.toURI() ), includePaths, parser );
		Scanner s = new Scanner( is, baseDir.toURI(), null );

		Map.Entry< URI, Set< String > > expectedSymbolsRoot = TestingObjectsCreator
			.createURISymbolsMap( Paths.get( baseDir.toURI() ).toFile().toURI(), "date",
				"number", "foo", "bar", "baz", "dateFoo" );
		Map.Entry< URI, Set< String > > expectedSymbolsExt =
			TestingObjectsCreator.createURISymbolsMap(
				Paths.get( baseDir.toURI() ).resolve( "packages" ).resolve( "type.ol" )
					.toFile().toURI(),
				"date", "number", "foo", "bar", "baz", "dateFoo" );

		Map< URI, Set< String > > expectedSourceSymbols =
			Stream.of( expectedSymbolsRoot, expectedSymbolsExt ).collect(
				Collectors.toMap( elem -> elem.getKey(), elem -> elem.getValue() ) );

		assertDoesNotThrow( () -> {
			// parse a program
			ModuleRecord mainRecord = parser.parse( s );

			// crawl dependencies
			ModuleCrawlerResult crawlResult = crawler.crawl( mainRecord );

			// check symbols
			Set< URI > visitedURI = new HashSet<>();
			for( ModuleRecord mr : crawlResult.toMap().values() ) {
				if( expectedSourceSymbols.containsKey( mr.source() ) ) {
					Set< String > symbols = expectedSourceSymbols.get( mr.source() );
					CheckUtility.checkSymbols( mr.symbolTable(), symbols );
					visitedURI.add( mr.source() );
				} else {
					throw new Exception( "unexpected source " + mr.source().toString()
						+ ", expected " + expectedSourceSymbols.keySet().toString() );
				}
			}

			if( !visitedURI.removeAll( expectedSourceSymbols.keySet() ) ) {
				throw new Exception( "source " + Arrays.toString( visitedURI.toArray() )
					+ " not found in crawl result" );
			}

			GlobalSymbolReferenceResolver symbolResolver =
				new GlobalSymbolReferenceResolver( crawlResult );

			// resolve symbols
			symbolResolver.resolveExternalSymbols();

			// check if all external symbol is linked
			for( ModuleRecord mr : crawlResult.toMap().values() ) {
				CheckUtility.checkSymbolNodeLinked( mr.symbolTable() );
			}

		} );
		is.close();
	}

	@ParameterizedTest
	@MethodSource( "moduleSystemExceptionTestProvider" )
	void testModuleSystemException( String code, String exception, String errorMessage ) {
		InputStream is = new ByteArrayInputStream( code.getBytes() );
		ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[ 0 ],
			this.getClass().getClassLoader() );

		Throwable ex = assertThrows( Exception.class, () -> {
			Scanner s = new Scanner( is, baseDir.toURI(), null );
			ModuleRecord mr = parser.parse( s );
			ModuleCrawler crawler = new ModuleCrawler( Paths.get( baseDir.toURI() ), includePaths, parser );
			ModuleCrawlerResult crawlResult = crawler.crawl( mr );
			GlobalSymbolReferenceResolver symbolResolver =
				new GlobalSymbolReferenceResolver( crawlResult );
			symbolResolver.resolve();
		} );

		String expectedMessage = errorMessage;
		String actualMessage = ex.getMessage();
		assertTrue( actualMessage.contains( exception ), "exception class mismatch expected " + exception );
		assertTrue( actualMessage.contains( expectedMessage ),
			"expected exception message to contain " + expectedMessage + " but found "
				+ actualMessage );
	}


	private static Stream< Arguments > moduleSystemExceptionTestProvider() {
		return Stream.of(
			Arguments.of( "from .some.where import A", "ModuleNotFoundException",
				"Module \"where\" not found from lookup path" ),
			Arguments.of( "from A import A\n from B import A", "DuplicateSymbolException",
				"detected duplicate declaration of symbol A" ),
			Arguments.of( "from A import someSymbol", "SymbolNotFoundException",
				"someSymbol is not defined" ),
			Arguments.of( "from twice.some.where import someSymbol", "ModuleNotFoundException",
				"some/where in" ),
			Arguments.of( "main{ someProc }", "SymbolNotFoundException",
				"someProc is not defined in symbolTable" ),
			Arguments.of( "outputPort OP { interfaces: iface }", "SymbolNotFoundException",
				"iface is not defined in symbolTable" ),
			Arguments.of( "inputPort IP { interfaces: iface location:\"local\" }",
				"SymbolNotFoundException", "iface is not defined in symbolTable" ),
			Arguments.of( "main { t = 2 instanceof customType }", "SymbolNotFoundException",
				"customType is not defined in symbolTable" ),
			Arguments.of( "interface iface {oneWay: test(customType)}", "SymbolNotFoundException",
				"customType is not defined in symbolTable" ),
			Arguments.of( "interface iface {requestResponse: test(customType)(string)}",
				"SymbolNotFoundException", "customType is not defined in symbolTable" ),
			Arguments.of( "interface iface {requestResponse: test(void)(customType)}",
				"SymbolNotFoundException", "customType is not defined in symbolTable" ),
			Arguments.of(
				"interface iface {requestResponse: test(void)(string) throws NumberException( NumberExceptionType )}",
				"SymbolNotFoundException", "NumberExceptionType is not defined in symbolTable" ),
			Arguments.of( "from A import privateType", "IllegalAccessSymbolException",
				"Illegal access to symbol privateType" ),
			Arguments.of(
				"from packages.interface import twiceIface main{ t = 2 instanceof twiceIface}",
				"SymbolTypeMismatchException",
				"twiceIface is not defined as a TypeDefinition" ),
			Arguments.of( "from packages.type import foo outputPort op{interfaces: foo}",
				"SymbolTypeMismatchException",
				"foo is not defined as a InterfaceDefinition" ) );
	}


	@ParameterizedTest
	@MethodSource( "importStatementExceptionTestProvider" )
	void testImportStatementExceptions( String code, String errorMessage )
		throws IOException, URISyntaxException {
		InputStream is = new ByteArrayInputStream( code.getBytes() );
		ModuleParser parser = new ModuleParser( StandardCharsets.UTF_8.name(), new String[ 0 ],
			this.getClass().getClassLoader() );

		Scanner s = new Scanner( is, baseDir.toURI(), null );

		Exception exception = assertThrows( ParserException.class, () -> {
			parser.parse( s );
		} );

		String expectedMessage = errorMessage;
		String actualMessage = exception.getMessage();

		assertTrue( actualMessage.contains( expectedMessage ) );

	}

	private static Stream< ? extends Arguments > importStatementExceptionTestProvider() {
		return Stream.of(
			Arguments.of( "from .A import AA as", "error: expected Identifier after as" ),
			Arguments.of( "from A import ", "error: expected Identifier or * after import" ),
			Arguments.of( "from \"somewhere\" import AA ",
				"error: expected Identifier, dot or import for an import statement. Found token type STRING" ) );
	}
}
