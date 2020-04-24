package jolie.lang.parse.module;

import java.util.Arrays;

public class SymbolWildCard extends SymbolInfoExternal
{

    public SymbolWildCard( String[] moduleTarget )
    {
        super( Arrays.toString( moduleTarget ), moduleTarget, null );
    }

}
