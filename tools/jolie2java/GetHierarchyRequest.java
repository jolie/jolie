package ebdjolieconnection.types;
import java.util.List;
import java.util.LinkedList;
import jolie.runtime.Value;

public class GetHierarchyRequest {
private CompanyReference _company_id;
private LanguageCode _language_id;
private Value v ;
private Value vReturn= Value.create() ;

public GetHierarchyRequest(Value v){

this.v=v;
if (v.hasChildren("company_id")){
_company_id=new CompanyReference( v.getFirstChild("company_id"));
}
if (v.hasChildren("language_id")){
_language_id=new LanguageCode( v.getFirstChild("language_id"));
}
}
public GetHierarchyRequest(){

}
public CompanyReference getCompany_id(){

	return _company_id;
}
public void setCompany_id(CompanyReference value ){

	_company_id=value;
}
public LanguageCode getLanguage_id(){

	return _language_id;
}
public void setLanguage_id(LanguageCode value ){

	_language_id=value;
}
public Value getValue(){
if((_company_id!=null)){
vReturn.getNewChild("company_id").deepCopy(_company_id.getValue());
}
if((_language_id!=null)){
vReturn.getNewChild("language_id").deepCopy(_language_id.getValue());
}
return vReturn ;
}
}
