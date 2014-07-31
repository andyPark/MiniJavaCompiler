/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.HashMap;

import miniJava.ContextualAnalyzer.ErrorReporter;
import miniJava.ContextualAnalyzer.IdentificationTable;
import  miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassDecl extends Declaration {

  public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
	  super(cn, null, posn);
	  fieldDeclList = fdl;
	  methodDeclList = mdl;
	  type = new ClassType(new Identifier(cn, posn), posn);
	  

		for(FieldDecl fd : fieldDeclList) {
			table.enter(fd.name, fd);
			fd.heapoffset = fieldDeclList.indexOf(fd.name);
			fd.owner = this;
		}
		for (MethodDecl md : methodDeclList) {
			table.enter(md.name, md);
			md.address = methodDeclList.indexOf(md.name);
			md.owner = this;
		}
	  
  }
  
  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }   
  public FieldDeclList fieldDeclList;
  public MethodDeclList methodDeclList;
  //I added
  public HashMap<String, MemberDecl> memberVars = new HashMap<String, MemberDecl>();
  public IdentificationTable table = new IdentificationTable(new ErrorReporter());
  
  public int getNumOfMethods() {
	  return methodDeclList.size();
  }
  
  int classNumber = Integer.MIN_VALUE;
}
