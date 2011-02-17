package testjolie2java.types;
import jolie.runtime.Value;

public class ProductItem {
private int id;
private article article;
private String  name;
private Value v ;
private Value vReturn= Value.create() ;

public ProductItem(Value v){

this.v=v;
id=v.getFirstChild("id").intValue();
name=v.getFirstChild("name").strValue();
}
public ProductItem(){

}
public int getId(){

	return id;
}
public void setIdValue(int value ){

	id=value;
}
public String getName(){

	return name;
}
public void setNameValue( String value ){

		name=value;
}
public Value getValue(){
vReturn.getNewChild("id").setValue(id);
vReturn.getNewChild("name").setValue(name);
return vReturn ;
}
public class article {
private List< ArticleItem> item;
private Value v ;
private Value vReturn= Value.create() ;

public article(Value v){

this.v=v;
item= new LinkedList<ArticleItem>();
	for(int counteritem=0;counteritem<v.getChildren("item").size();counteritem++){
		ArticleItem supportitem=new ArticleItem(v.getChildren("item").get(counteritem));
		item.add(supportitem);
	}
}
public article(){

item= new LinkedList<ArticleItem>();
}
public ArticleItem getItemValue(int index){

	return item.get(index);
}
public int getItemSize(){

	return item.size();
}
public void addItemValue(ArticleItem value ){

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
