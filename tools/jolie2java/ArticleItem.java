package testjolie2java.types;
import jolie.runtime.Value;

public class ArticleItem {
private int id;
private String  name;
private Value v ;
private Value vReturn= Value.create() ;

public ArticleItem(Value v){

this.v=v;
id=v.getFirstChild("id").intValue();
name=v.getFirstChild("name").strValue();
}
public ArticleItem(){

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
 }
