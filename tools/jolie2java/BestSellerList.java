package ebdjolieconnection.types;
import java.util.List;
import java.util.LinkedList;
import jolie.runtime.Value;

public class BestSellerList {
private List<String> _MSN_Number;
private Value v ;
private Value vReturn= Value.create() ;

public BestSellerList(Value v){

this.v=v;
if (v.hasChildren("MSN_Number")){
_MSN_Number= new LinkedList<String>();
}
if (v.hasChildren("MSN_Number")){
for(int counterMSN_Number=0;counterMSN_Number<v.getChildren("MSN_Number").size();counterMSN_Number++){
		String supportMSN_Number=new String(v.getChildren("MSN_Number").get(counterMSN_Number).strValue());
		_MSN_Number.add(supportMSN_Number);
}
}
}
public BestSellerList(){

_MSN_Number= new LinkedList<String>();
}
public int getMSN_NumberSize(){

	return _MSN_Number.size();
}
public String getMSN_NumberValue(int index){

	return _MSN_Number.get(index);
}
public void addMSN_NumberValue( String value ){
		_MSN_Number.add(value);
}
public void removeMSN_NumberValue( int index ){
		_MSN_Number.remove(index);
}
public Value getValue(){
if(!(_MSN_Number.isEmpty()) && (_MSN_Number!=null)){
	for(int counterMSN_Number=0;counterMSN_Number<_MSN_Number.size();counterMSN_Number++){
		vReturn.getNewChild("MSN_Number").setValue(_MSN_Number.get(counterMSN_Number));
	}}
return vReturn ;
}
}
