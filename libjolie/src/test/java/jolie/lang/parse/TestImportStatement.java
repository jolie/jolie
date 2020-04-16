package jolie.lang.parse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestImportStatement {

    InputStream is;
    static SemanticVerifier.Configuration configuration = new SemanticVerifier.Configuration();

    @ParameterizedTest
    @MethodSource("importStatementTestProvider")
    void testImportStatementParsing(String code) {
        this.is = new ByteArrayInputStream(code.getBytes());
        InstanceCreator oc = new InstanceCreator(new String[] {});
        assertDoesNotThrow(() -> {
            OLParser olParser = oc.createOLParser(is);
            olParser.parse();
        });
    }

    private static Stream<Arguments> importStatementTestProvider() {
        return Stream.of(
                Arguments.of("from A import AA"),
                Arguments.of("from A import A as B"),
                Arguments.of("from A import A as B, C"),
                Arguments.of("from A import A as B, C as D"),
                Arguments.of("from A import *"),
                Arguments.of("from .A import AA"), 
                Arguments.of("from ..A import AA "),
                Arguments.of("from A.B import AA "),
                Arguments.of("from .A.B import AA ")
            );
    }

    @Disabled("not yet ready.")
    @ParameterizedTest
    @MethodSource("importStatementExceptionTestProvider")
    void testImportStatementExceptions(String code, String errorMessage)
            throws RuntimeException, IOException, URISyntaxException {
        this.is = new ByteArrayInputStream(code.getBytes());
        InstanceCreator oc = new InstanceCreator(new String[] {});
        OLParser olParser = oc.createOLParser(is);

        Exception exception = assertThrows(ParserException.class, () -> olParser.parse(),
                "Expected parse() to throw, with " + errorMessage + " but it didn't");
        assertTrue(exception.getMessage().contains(errorMessage));
    }

    private static Stream<Arguments> importStatementExceptionTestProvider() {
        return Stream.of(
                Arguments.of("from jolie2/import/simple-import/importstatement-test.ol import AA",
                        "unable to find AA in"),
                Arguments.of("from somewhere import AA", "unable to locate"),
                Arguments.of("from somewhere AA ", "expected import for an import statement"));
    }

    @AfterEach
    void closeSteam() throws IOException {
        this.is.close();
    }

}
