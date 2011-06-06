package ebdjolieconnection.types;
import java.util.List;
import java.util.LinkedList;
import jolie.runtime.Value;

public class GetERPAccountProfileRequest {
private CustomerERPReference _customer_id;
private Value v ;
private Value vReturn= Value.create() ;

public GetERPAccountProfileRequest(Value v){

this.v=v;
if (v.hasChildren("customer_id")){
_customer_id=new CustomerERPReference( v.getFirstChild("customer_id"));
}
}
public GetERPAccountProfileRequest(){

}
public CustomerERPReference getCustomer_id(){

	return _customer_id;
}
public void setCustomer_id(CustomerERPReference value ){

	_customer_id=value;
}
public Value getValue(){
if((_customer_id!=null)){
vReturn.getNewChild("customer_id").deepCopy(_customer_id.getValue());
}
return vReturn ;
}
}
