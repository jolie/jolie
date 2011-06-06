package ebdjolieconnection.types;
import java.util.List;
import java.util.LinkedList;
import jolie.runtime.Value;

public class GetProductListResponse {
private ProductList _product_list;
private Value v ;
private Value vReturn= Value.create() ;

public GetProductListResponse(Value v){

this.v=v;
if (v.hasChildren("product_list")){
_product_list=new ProductList( v.getFirstChild("product_list"));
}
}
public GetProductListResponse(){

}
public ProductList getProduct_list(){

	return _product_list;
}
public void setProduct_list(ProductList value ){

	_product_list=value;
}
public Value getValue(){
if((_product_list!=null)){
vReturn.getNewChild("product_list").deepCopy(_product_list.getValue());
}
return vReturn ;
}
}
