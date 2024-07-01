package joliex.java.generate;

import joliex.java.generate.util.ClassStringBuilder;

public abstract class JavaClassBuilder {

    protected final ClassStringBuilder builder = new ClassStringBuilder();

    public abstract String className();
    public abstract void appendPackage();
    public abstract void appendDefinition();

    public String getResult() { return builder.toString(); }
}
