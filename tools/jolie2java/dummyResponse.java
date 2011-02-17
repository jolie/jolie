package testjolie2java.types;
import testjolie2java.types.typeTwo;
import testjolie2java.types.typeTwo;
import java.util.List;
import java.util.LinkedList;
import jolie.runtime.Value;

public class dummyResponse {
private b b;
private int c;
private double a;
private List< typeTwo> j;
private String  z;
private typeTwo y;
private List<Integer> x;
private Value v ;
private Value vReturn ;

public dummyResponse(Value v){

this.v=v;
c=v.getFirstChild("c").intValue();
a=v.getFirstChild("a").doubleValue();
j= new LinkedList<typeTwo>();
z=v.getFirstChild("z").strValue();
y=new typeTwo( v.getFirstChild("y"));
x= new LinkedList<Integer>();
	for(int counterj=0;counterj<v.getChildren("j").size();counterj++){
		typeTwo supportj=new Double(v.getChildren("j").get(counterj);
		j.add(supportj);
	}

	for(int counterx=0;counterx<v.getChildren("x").size();counterx++){
		Integer supportx=new Integer(v.getChildren("x").get(counterx).intValue());
		x.add(supportx);
	}
}
public dummyResponse(){

j= new LinkedList<typeTwo>();
x= new LinkedList<Integer>();
}
public int getC(){

	return c;
}
public void setCValue(int value ){

	c=value;
}
public double getA(){

	return a;
}
public void setAValue( double value ){

		a=value;
}
public typeTwo setJValue(int index){

	return j.get(index);
}
public void addJValue(typeTwovalue ){

		j.add(value);
}
public void removeJValue( int index ){
		j.remove(index);
}
public String getZ(){

	return z;
}
public void setZValue( String value ){

		z=value;
}
public typeTwo getY(){

	return y;
}
public void setY(typeTwo value ){

	y=value;
}
public int getXValue(int index){
	return x.get(index).intValue();
}
public void addXValue(int value ){
		Integer supportx=new Integer(value);
		x.add(supportx );
}
public void removeXValue( int index ){
		x.remove(index);
}
public Value getValue(){
vReturn.getNewChild("c").setValue(c);
vReturn.getNewChild("a").setValue(a);
	for(int counterj=0;counterj<j.size();counterj++){
		vReturn.getNewChild("j").deepCopy(j.get(counterj).getValue()));
	}
}
vReturn.getNewChild("z").setValue(z);
vReturn.getNewChild("y").deepCopy(y.getValue());
	for(int counterx=0;counterx<x.size();counterx++){
		vReturn.getNewChild("x").setValue(x.get(counterx));
	}return vReturn ;
}
public class b {
private List<Integer> ba;
private Value v ;
private Value vReturn ;

public b(Value v){

this.v=v;
ba= new LinkedList<Integer>();

	for(int counterba=0;counterba<v.getChildren("ba").size();counterba++){
		Integer supportba=new Integer(v.getChildren("ba").get(counterba).intValue());
		ba.add(supportba);
	}
}
public b(){

ba= new LinkedList<Integer>();
}
public int getBaValue(int index){
	return ba.get(index).intValue();
}
public void addBaValue(int value ){
		Integer supportba=new Integer(value);
		ba.add(supportba );
}
public void removeBaValue( int index ){
		ba.remove(index);
}
public Value getValue(){
	for(int counterba=0;counterba<ba.size();counterba++){
		vReturn.getNewChild("ba").setValue(ba.get(counterba));
	}return vReturn ;
}
 }
