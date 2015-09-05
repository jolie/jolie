package jolie.lang.parse.ast.types;

import jolie.lang.NativeType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Range;

import java.util.*;

public class TypeChoiceDefinition extends TypeDefinition {
    private final TypeDefinition left;
    private final TypeDefinition right;

    public TypeChoiceDefinition(ParsingContext context, String id, Range cardinality, TypeDefinition left, TypeDefinition right)
    {
        super(context, id, cardinality );
        this.left = left;
        this.right = right;
    }

    /*public TypeChoiceDefinition setRight(TypeDefinition right, Range cardinality) {
        this.right = right;
        Range t2Cardinality = cardinality;
        return this;
    }*/

    @Override
    public void accept(OLVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public TypeDefinition getSubType(String id) {
        return left.getSubType(id);
    }

    @Override
    public Set<Map.Entry<String, TypeDefinition>> subTypes() {
        return left.subTypes();
    }

    @Override
    public boolean hasSubTypes() {
        if (left !=null) {
            return left.hasSubTypes();
        }
        else return false;
    }

    @Override
    public boolean untypedSubTypes() {
        return left.untypedSubTypes();
    }

    @Override
    public NativeType nativeType() {
        return  null;
    }

    @Override
    public boolean hasSubType(String id) {
        return left.hasSubType(id);
    }

    public TypeDefinition left(){
        return left;
    }

    public TypeDefinition right(){
        return right;
    }

    public ArrayList<TypeDefinition> both(){
        return new ArrayList<TypeDefinition>() {{add(left); add(right);}};
    }
}
