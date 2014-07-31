/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class Operator extends Terminal {

  public Operator (Token t, SourcePosition posn) {
    super (t.spelling, posn);
    token = t;
  }

  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitOperator(this, o);
  }
  
  //check if the expression is a type that matches the unary operator
  public Type accepts(Expression e) {
	  if (token.kind == Token.BANG) {
		  if (e.type.typeKind.equals(TypeKind.BOOLEAN))
			  return new BaseType(TypeKind.BOOLEAN, posn);
	  }
	  else if (token.kind == Token.MINUS) {
		  if (e.type.typeKind.equals(TypeKind.INT))
			  return new BaseType(TypeKind.INT, posn);
	  }
	  return new BaseType(TypeKind.ERROR, posn);
  }
  
  public Type accepts(Expression e1, Expression e2) {
		switch(token.kind) {
			case Token.PLUS: case Token.MINUS: case Token.DIV: case Token.MULT:
				if (e1.type.typeKind == TypeKind.INT || e1.type.typeKind == TypeKind.ERROR)
					if (e2.type.typeKind == TypeKind.INT || e2.type.typeKind == TypeKind.ERROR)
						return new BaseType(TypeKind.INT, posn);
				break;
			case Token.AND: case Token.OR:
				if (e1.type.typeKind == TypeKind.BOOLEAN || e1.type.typeKind == TypeKind.ERROR)
					if (e2.type.typeKind == TypeKind.BOOLEAN || e2.type.typeKind == TypeKind.ERROR)
						return new BaseType(TypeKind.BOOLEAN, posn);
				break;
			case Token.LT: case Token.LE: case Token.GT: case Token.GE:
				if (e1.type.typeKind == TypeKind.INT || e1.type.typeKind == TypeKind.ERROR)
					if (e2.type.typeKind == TypeKind.INT || e2.type.typeKind == TypeKind.ERROR)
						return new BaseType(TypeKind.BOOLEAN, posn);
				break;
			case Token.EQUALS: case Token.NEQ:
				if (e1.type.typeKind == TypeKind.INT || e1.type.typeKind == TypeKind.ERROR) {
					if (e2.type.typeKind == TypeKind.INT || e2.type.typeKind == TypeKind.ERROR)
						return new BaseType(TypeKind.BOOLEAN, posn);
				}
				else if (e1.type.typeKind == TypeKind.BOOLEAN || e1.type.typeKind == TypeKind.ERROR) {
					if (e2.type.typeKind == TypeKind.BOOLEAN || e2.type.typeKind == TypeKind.ERROR)
						return new BaseType(TypeKind.BOOLEAN, posn);
				}
				break;
			default:
				break;
		}
		return new BaseType(TypeKind.ERROR, posn);
  }

  public Token token;
}
