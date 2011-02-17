package testjolie2java.types;
import jolie.runtime.Value;

public class typeTwo {
private int pippo;
private double pluto;
private Value v ;
private Value vReturn ;

public typeTwo(Value v){

this.v=v;
pippo=v.getFirstChild("pippo").intValue();
pluto=v.getFirstChild("pluto").doubleValue();
}
public typeTwo(){

}
public int getPippo(){

	return pippo;
}
public void setPippoValue(int value ){

	pippo=value;
}
public double getPluto(){

	return pluto;
}
public void setPlutoValue( double value ){

		pluto=value;
}
public Value getValue(){
vReturn.getNewChild("pippo").setValue(pippo);
vReturn.getNewChild("pluto").setValue(pluto);
return vReturn ;
}
 }
