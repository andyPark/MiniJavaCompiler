/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class Type extends AST {
    
    public Type(TypeKind typ, SourcePosition posn){
        super(posn);
        typeKind = typ;
    }
    
    public boolean equals(Type t) {
    		if (typeKind == TypeKind.ERROR || t.typeKind == TypeKind.ERROR)
    			return true;
    		if (typeKind.equals(TypeKind.UNSUPPORTED) || t.typeKind.equals(TypeKind.UNSUPPORTED))
    			return false;
    		if (!typeKind.equals(t.typeKind))
    			return false;
    		if (typeKind.equals(TypeKind.ARRAY) && t.typeKind.equals(TypeKind.ARRAY))
    			return ((ArrayType) this).eltType.equals(((ArrayType) t).eltType);
    		if (typeKind.equals(TypeKind.CLASS) && t.typeKind.equals(TypeKind.CLASS))
    			return ((ClassType) this).className.spelling.equals(((ClassType) t).className.spelling);
    		return true;
    }
    
    public String spelling() {
    		switch (typeKind) {
    		case ERROR:
    			return "ERROR";
    		case UNSUPPORTED:
    			return "UNSUPPORTED";
    		case ARRAY:
    			return "array";
    		case CLASS:
    			try {
    				return ((ClassType) this).className.spelling;
    			}
    			catch (Exception e) {
    				return "class (btw this shouldn't've run there was an error somewhere).";
    			}
    		case BOOLEAN:
    			return "boolean";
    		case INT:
    			return "int";
    		case VOID:
    			return "void";
    		default:
    			return "BAD ERROR";
    		}
    }
    
    public TypeKind typeKind;
    
}

        