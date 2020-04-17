package jolie.lang.parse.module;

import jolie.lang.Constants;

public class ModuleException extends Exception {

    private static final long serialVersionUID = Constants.serialVersionUID();

    public ModuleException(String message) {
        super(message);
    }

    public ModuleException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public ModuleException(Throwable arg0) {
        super(arg0);
    }
}