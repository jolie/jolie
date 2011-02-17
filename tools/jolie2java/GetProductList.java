package testjolie2java.types;
import jolie.runtime.Value;

public class GetProductList {
private String  filter;
private Value v ;
private Value vReturn= Value.create() ;

public GetProductList(Value v){

this.v=v;
filter=v.getFirstChild("filter").strValue();
}
public GetProductList(){

}
public String getFilter(){

	return filter;
}
public void setFilterValue( String value ){

		filter=value;
}
public Value getValue(){
vReturn.getNewChild("filter").setValue(filter);
return vReturn ;
}
 }
