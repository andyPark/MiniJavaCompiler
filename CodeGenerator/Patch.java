package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.MethodDecl;

public class Patch {
	public int  fromHere = 0,
				toHere = 0;
	public MethodDecl theMethod;
	public Patch(int f, MethodDecl md) {
		fromHere = f;
		theMethod = md;
	}
}
