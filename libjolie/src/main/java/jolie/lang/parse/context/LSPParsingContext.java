package jolie.lang.parse.context;

import java.util.List;

/**
 *
 * @param textLocation
 * @param enclosingCode
 */
public record LSPParsingContext(Location textLocation, List< String > enclosingCode) {
}
