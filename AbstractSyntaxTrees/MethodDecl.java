/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class MethodDecl extends MemberDecl {
	
	public MethodDecl(MemberDecl md, ParameterDeclList pl, StatementList sl, Expression e, SourcePosition posn){
	    super(md,posn);
	    parameterDeclList = pl;
	    statementList = sl;
	    returnExp = e;
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitMethodDecl(this, o);
    }
	
	public ParameterDeclList parameterDeclList;
	public StatementList statementList;
	public Expression returnExp;
	
	//i added
	public boolean isMainMethod = false; //this gets set in the Checker class
	public int cb_offset = Integer.MIN_VALUE; //offset is where the procedure lies within the context of the class.
	//That is what number it is within the class descriptor.
	//We include the +2 in our calculations!!
	
	public int address = Integer.MIN_VALUE; //address is where the procedure starts in the code. An absolute address
	public int numOfLocalVars = 0; //added this for PA4;
}
