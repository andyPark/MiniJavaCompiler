/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.*;

public class ParameterDeclList implements Iterable<ParameterDecl>
{
    public ParameterDeclList() {
    	parameterDeclList = new ArrayList<ParameterDecl>();
    }
    
    public void add(ParameterDecl s){
    	parameterDeclList.add(s);
    }
    
    public ParameterDecl get(int i){
        return parameterDeclList.get(i);
    }
    
    public int size() {
        return parameterDeclList.size();
    }
    
    public Iterator<ParameterDecl> iterator() {
    	return parameterDeclList.iterator();
    }
    
    public boolean accepts(ExprList argsList) {
    		if (argsList.size() != size())
    			return false;
    		for (int i = 0; i < size(); i++) {
    			if (!get(i).type.equals(argsList.get(i).type))
    				return false;
    		}
    		return true;
    }
    
    private List<ParameterDecl> parameterDeclList;
}
