/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.*;

public class MethodDeclList implements Iterable<MethodDecl>
{
	public MethodDeclList() {
		methodDeclList = new ArrayList<MethodDecl>();
	}   

	public void add(MethodDecl cd){
		methodDeclList.add(cd);
	}

	public MethodDecl get(int i){
		return methodDeclList.get(i);
	}
	
	public boolean contains(String id) { //Added this.
		for (MethodDecl md : methodDeclList) {
			if (md.name.equals(id))
				return true;
		}
		return false;
	}

	public int size() {
		return methodDeclList.size();
	}

	public Iterator<MethodDecl> iterator() {
		return methodDeclList.iterator();
	}

	private List<MethodDecl> methodDeclList;
	
	//i added
	public int indexOf(String str) {
		int size = methodDeclList.size();
		for (int i=0;i<size;i++)
			if (methodDeclList.get(i).name.equals(str)) return i;
		return -1;
	}
}

