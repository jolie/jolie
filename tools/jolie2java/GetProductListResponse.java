package testjolie2java.types;
import testjolie2java.types.ProductItem;
import java.util.List;
import java.util.LinkedList;
import jolie.runtime.Value;

public class GetProductListResponse {
private List< ProductItem> item;
private Value v ;
private Value vReturn= Value.create() ;

public GetProductListResponse(Value v){

this.v=v;
item= new LinkedList<ProductItem>();
	for(int counteritem=0;counteritem<v.getChildren("item").size();counteritem++){
		ProductItem supportitem=new ProductItem(v.getChildren("item").get(counteritem));
		item.add(supportitem);
	}
}
public GetProductListResponse(){

item= new LinkedList<ProductItem>();
}
public ProductItem getItemValue(int index){

	return item.get(index);
}
public int getItemSize(){

	return item.size();
}
public void addItemValue(ProductItem value ){

		item.add(value);
}
public void removeItemValue( int index ){
		item.remove(index);
}
public Value getValue(){
	for(int counteritem=0;counteritem<item.size();counteritem++){
		vReturn.getNewChild("item").deepCopy(item.get(counteritem).getValue());
	}
return vReturn ;
}
 }
