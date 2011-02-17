package testjolie2java.types;
import testjolie2java.types.typeTwo;
import jolie.runtime.Value;

public class dummyRequestOne {
private typeTwo first;
private Value v ;
private Value vReturn ;

public dummyRequestOne(Value v){

this.v=v;
first=new typeTwo( v.getFirstChild("first"));
}
public dummyRequestOne(){

}
public typeTwo getFirst(){

	return first;
}
public void setFirst(typeTwo value ){

	first=value;
}
public Value getValue(){
vReturn.getNewChild("first").deepCopy(first.getValue());
return vReturn ;
}
 }
