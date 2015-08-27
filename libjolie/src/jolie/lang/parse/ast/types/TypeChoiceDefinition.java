package jolie.lang.parse.ast.types;

import jolie.lang.NativeType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Range;

import java.util.Map;
import java.util.Set;

public class TypeChoiceDefinition extends TypeDefinition {
    private final TypeDefinition T1;
    private TypeDefinition T2;
    //private Map< String, TypeDefinition > subTypes = null;
    //private boolean untypedSubTypes = false;
    private Range t2Cardinality;

    public TypeChoiceDefinition(ParsingContext context, String id, Range cardinality, TypeDefinition T1)
    {
        super(context, id, cardinality );
        this.T1 = T1;
    }

    public TypeChoiceDefinition setT2(TypeDefinition T2, Range cardinality) {
        this.T2 = T2;
        this.t2Cardinality = cardinality;
        return this;
    }

    public Range getT2Cardinality(){
        return T2.cardinality();
    }

    @Override
    public void accept(OLVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public TypeDefinition getSubType(String id) {
        return T1.getSubType(id);
    }


    @Override
    public Set<Map.Entry<String, TypeDefinition>> subTypes() {
        return T1.subTypes();
    }

    public Set<Map.Entry<String, TypeDefinition>> t2SubTypes() {
        return T2.subTypes();
    }

    @Override
    public boolean hasSubTypes() {
        if (T1!=null) {
            return T1.hasSubTypes();
        }
        else return false;
    }

    @Override
    public boolean untypedSubTypes() {
        return T1.untypedSubTypes();
    }

    public boolean t2UntypedSubTypes() {
        return T2.untypedSubTypes();
    }

    @Override
    public NativeType nativeType() {
        return  null;
    }

    public NativeType t1NativeType() {
        return T1.nativeType();
    }

    public NativeType t2NativeType() {
        return T2.nativeType();
    }

    @Override
    public boolean hasSubType(String id) {
        return T1.hasSubType(id);
    }

    public TypeDefinition getT1(){
        return T1;
    }

    public TypeDefinition getT2(){
        return T2;
    }
}
