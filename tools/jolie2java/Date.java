package ebdjolieconnection.types;
import java.util.List;
import java.util.LinkedList;
import jolie.runtime.Value;

public class Date {
private Integer _month;
private Integer _year;
private Integer _day;
private Value v ;
private Value vReturn= Value.create() ;

public Date(Value v){

this.v=v;
if (v.hasChildren("month")){
_month=new Integer(v.getFirstChild("month").intValue());
}
if (v.hasChildren("year")){
_year=new Integer(v.getFirstChild("year").intValue());
}
if (v.hasChildren("day")){
_day=new Integer(v.getFirstChild("day").intValue());
}
}
public Date(){

}
public int getMonth(){

	return _month.intValue();
}
public void setMonthValue(int value ){

	_month=new Integer(value);
}
public int getYear(){

	return _year.intValue();
}
public void setYearValue(int value ){

	_year=new Integer(value);
}
public int getDay(){

	return _day.intValue();
}
public void setDayValue(int value ){

	_day=new Integer(value);
}
public Value getValue(){
if((_month!=null)){
vReturn.getNewChild("month").setValue(_month);
}
if((_year!=null)){
vReturn.getNewChild("year").setValue(_year);
}
if((_day!=null)){
vReturn.getNewChild("day").setValue(_day);
}
return vReturn ;
}
}
