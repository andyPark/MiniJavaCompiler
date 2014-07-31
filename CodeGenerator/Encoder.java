package miniJava.CodeGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IndexedRef;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.QualifiedRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class Encoder implements Visitor<Object, Object> {
	
	//TODO DELETE THESE THINGS
//	private LinkedList<Patch> patchList;
	private int numberOfErrors = 0;
	private int mainAddress = Integer.MIN_VALUE; //set it to this as a failure mechanism in case i don't set it.
	private int numOfLocalVars = 0;
	private int numOfInstanceVars = 0;
	
	private HashMap<String, Integer> classDescriptors; //table with the addresses of each of the class descriptors
	private int nextClassDescriptorAddr = 0;
	private int totalNumOfMethods = 0;
	private int classCount = 0;
	private boolean isFinalAddress = false;
//	private boolean inCallExpr = false;
//	private boolean inCallStmt = false;
	private boolean localUpdate = true; //use this to decide whether or not to grab the reference or the address on the stack
	private MethodDecl currentMethod = null;
	
	private ArrayList<Patch> toBePatched;
	
	private boolean thisVisited = false;

	//	private ClassDecl currentClass;
	//private boolean needTableDetails
	
	public Encoder() {
		classDescriptors = new HashMap<String, Integer>();
		toBePatched = new ArrayList<Patch>();
	}

	@Override
	public Object visitPackage(Package prog, Object arg) {
		
		for (ClassDecl cd : prog.classDeclList) {
			classDescriptors.put(cd.name, nextClassDescriptorAddr);
			nextClassDescriptorAddr = cd.methodDeclList.size() + 2;
		}
		for (ClassDecl cd : prog.classDeclList) {
			classCount++;
			cd.visit(this, arg);
		}
		
		for (Patch p : toBePatched) {
			Machine.patch(p.fromHere, p.theMethod.address);
		}
		
		Machine.emit(Op.LOADL, Machine.nullRep);
		Machine.emit(Op.CALL, Reg.CB, mainAddress);
		Machine.emit(Machine.Op.HALT, 0, 0, 0);
		
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		//I might need to pass some values to Method and Field Decl but we'll cross that bridge when I get to it.
		
		int patchme = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0); //patchme here
		for (MethodDecl md : cd.methodDeclList) //generate the methods
			md.visit(this, arg);
		int patchingLabel = Machine.nextInstrAddr();
		Machine.patch(patchme, patchingLabel);
		Machine.emit(Op.LOADL, -1); //No superclasses... Change this if you decide to do the extra credit.
		Machine.emit(Op.LOADL, cd.methodDeclList.size());
		for (MethodDecl md : cd.methodDeclList) {//generate the references to each of the methods
			Machine.emit(Op.LOADA, Reg.CB, md.address);
			md.cb_offset = totalNumOfMethods + (classCount * 2);
			totalNumOfMethods++;
		}
	
		
//		try {
//			for (MethodDecl md : cd.methodDeclList) //generate the code addr of p_A;
//				Machine.emit(Op.LOADA, Reg.CB, md.address);
//		}
//		catch (Exception e) {
//			System.out.println("address was not assigned!!");
//			System.exit(4);
//		}
		
		int fieldNumber = 0;
		for (FieldDecl fd : cd.fieldDeclList)
			fd.heapoffset = fieldNumber++;
		
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		int returnSize = 0;
		currentMethod = md;
		
		int offset = -1;
		
		/*
		 * I'm pushing my variables onto the stack in the order such that
		 * the first item is closest to the top and the last item declared
		 * is closest to the bottom of the stack
		 */
//		for (int i = md.parameterDeclList.size() - 1; i >= 0; i--) {
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.paramOffset = offset--;
		}
		
		md.address = Machine.nextInstrAddr();
		if (md.isMainMethod)
			mainAddress = md.address;
		
		for (Statement s : md.statementList)
			s.visit(this, arg);
		
		if (md.returnExp != null) {
			md.returnExp.visit(this, arg);
			returnSize = 1;
		}

		Machine.emit(Machine.Op.RETURN, returnSize, 0, md.parameterDeclList.size());
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		int popNum = 0;
		for (Statement s: stmt.sl) {
			s.visit(this, arg);
			if (s instanceof VarDeclStmt)
				popNum++;
		}
		Machine.emit(Op.POP, popNum);
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.initExp.visit(this, arg); //this'll push the argument onto the stack
		if (stmt.varDecl.blockOwner == null)
			stmt.varDecl.localOffset = stmt.varDecl.methodOwner.numOfLocalVars++;
		else {
			stmt.varDecl.localOffset = stmt.varDecl.methodOwner.numOfLocalVars + stmt.varDecl.blockOwner.numOfVars++;
		}
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.val.visit(this, arg); //will push the value that you want to put in the address pushed by the next method
		stmt.ref.visit(this, arg); //will push the address of the reference on the top of the stack
		Machine.emit(Machine.Op.STOREI, 1, 0, 0);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		
		for (int i = stmt.argList.size()-1; i >= 0; i--) {
			stmt.argList.get(i).visit(this, arg); //push these onto the stack
		}
		
		//i'll never forgive myself for writing this expression below
		if (stmt.methodRef instanceof QualifiedRef 
			&& ((QualifiedRef) stmt.methodRef).id.spelling.equals("println")
			&& ((QualifiedRef) stmt.methodRef).ref instanceof QualifiedRef
			&& ((QualifiedRef) ((QualifiedRef) stmt.methodRef).ref).id.spelling.equals("out")
			&& ((QualifiedRef) ((QualifiedRef) stmt.methodRef).ref).ref instanceof IdRef
			&& ((IdRef) ((QualifiedRef) ((QualifiedRef) stmt.methodRef).ref).ref).id.spelling.equals("System")) {
			Machine.emit(Machine.Prim.putintnl);
			return null;
		}
		if (stmt.methodRef.isStatic) {
			toBePatched.add(new Patch(Machine.nextInstrAddr(), (MethodDecl) stmt.methodRef.decl));
			Machine.emit(Op.CALL, Reg.CB, 0);
		}
		else {
			//push the reference onto stack
			stmt.methodRef.visit(this, arg);
			toBePatched.add(new Patch(Machine.nextInstrAddr(), (MethodDecl) stmt.methodRef.decl));
			Machine.emit(Op.CALLI, Reg.CB, 0);
		}
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, arg); //this will push a 1 or a 0 onto the stack for true or false, respectively
		int startPatch = Machine.CT;
		Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, 0); 
		stmt.thenStmt.visit(this, arg); //generate a bunch of code that will be skipped if the cond expr is false
		int finishPatch = Machine.CT;
		Machine.emit(Machine.Op.JUMP, 0, Machine.Reg.CB, 0);
		Machine.patch(startPatch, Machine.CT); //a label from the beginning of the if statement to the else statement
		if (stmt.elseStmt != null)
			stmt.elseStmt.visit(this, arg); //generate the else statement code
		Machine.patch(finishPatch, Machine.CT); //label for when you finish running the if statement so you can skip the else statement
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		int condStart = Machine.CT;
		stmt.cond.visit(this, arg); //push the boolean result on to the top of the stack
		int patchme = Machine.CT;
		Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, 0); //jump to after the while loop
		stmt.body.visit(this, arg);
		Machine.emit(Machine.Op.JUMP, 0, Machine.Reg.CB, condStart);
		Machine.patch(patchme, Machine.CT);
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, arg);
		if (expr.operator.spelling.equals("!"))
			Machine.emit(Machine.Prim.not);
		else if (expr.operator.spelling.equals("-"))
			Machine.emit(Machine.Prim.neg);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		//Operator Short Circuiting
		if(expr.operator.spelling.equals("||")) {
			expr.left.visit(this, arg);
			Machine.emit(Op.LOAD, 1, Reg.ST, -1);
			int patchme = Machine.CT;
			Machine.emit(Op.JUMPIF, 1, Reg.CB, 0);
			expr.right.visit(this, arg);
			Machine.emit(Machine.Prim.or);
			Machine.patch(patchme, Machine.CT);
			return null;
		}
		if (expr.operator.spelling.equals("&&")){
			expr.left.visit(this, arg);
			Machine.emit(Op.LOAD, 1, Reg.ST, -1);
			int patchme = Machine.CT;
			Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);
			expr.right.visit(this, arg);
			Machine.emit(Machine.Prim.and);
			Machine.patch(patchme, Machine.CT);
			return null;
		}
		expr.left.visit(this, arg);
		expr.right.visit(this, arg);
		switch (expr.operator.spelling) {
		case "+":
			Machine.emit(Machine.Prim.add);
			break;
		case "-":
			Machine.emit(Machine.Prim.sub);
			break;
		case "*":
			Machine.emit(Machine.Prim.mult);
			break;
		case "/":
			Machine.emit(Machine.Prim.div);
			break;
		case "==":
			Machine.emit(Machine.Prim.eq);
			break;
		case "!=":
			Machine.emit(Machine.Prim.ne);
			break;
		case ">":
			Machine.emit(Machine.Prim.gt);
			break;
		case ">=":
			Machine.emit(Machine.Prim.ge);
			break;
		case "<":
			Machine.emit(Machine.Prim.lt);
			break;
		case "<=":
			Machine.emit(Machine.Prim.le);
			break;
		default:
			reportError("Runtime error?!?! This shouldn't've happened to be honest.");
			return null;
		}
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, arg); //this should push onto the stack the address of the reference
		
		if (!thisVisited) { //threw this in because of some dumb edge case i had to deal with
			Machine.emit(Op.LOADI, 1, 0, 0);
		}
		else {
			thisVisited = false;
		}
		//TODO might need to add an instanceof for MethodDecl
		return null;
	}
//	@Override
//	public Object visitCallExpr(CallExpr expr, Object arg) {
//		for (int i = expr.argList.size()-1; i >= 0; i--) {
//			expr.argList.get(i).visit(this, arg); //this will push each expressions result onto the top of the stack
//		}
//		if (expr.functionRef.isStatic) {
//			MethodDecl meth = (MethodDecl) expr.functionRef.decl; //hopefully contextual analysis makes this correct
//			toBePatched.add(new Patch(Machine.nextInstrAddr(), (MethodDecl) expr.functionRef.decl));
//			Machine.emit(Op.CALL, Reg.CB, 0);
//		}
//		else {
//			MethodDecl meth = (MethodDecl) expr.functionRef.decl;
//			expr.functionRef.visit(this, arg); //push the location of the reference onto the stack
//			Machine.emit(Op.LOADI, 1, 0, 0);
//			toBePatched.add(new Patch(Machine.nextInstrAddr(), (MethodDecl) expr.functionRef.decl));
//			Machine.emit(Op.CALLI, Reg.CB, 0);
//		}
//		return null;
//	}
	
	public Object visitCallExpr(CallExpr expr, Object arg) {

		for (int i = expr.argList.size()-1; i >= 0; i--) {
			expr.argList.get(i).visit(this, arg); //push these onto the stack
		}
		
		if (expr.functionRef.isStatic) {
			toBePatched.add(new Patch(Machine.nextInstrAddr(), (MethodDecl) expr.functionRef.decl));
			Machine.emit(Op.CALL, Reg.CB, 0);
		}
		else {
			//push the reference onto stack
			expr.functionRef.visit(this, arg);
			toBePatched.add(new Patch(Machine.nextInstrAddr(), (MethodDecl) expr.functionRef.decl));
			Machine.emit(Op.CALLI, Reg.CB, 0);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		if (expr.type.typeKind == TypeKind.INT)	
			Machine.emit(Op.LOADL, Integer.parseInt(expr.literal.spelling));
		else { //typeKind is boolean
			if (expr.literal.spelling.equals("true"))
				Machine.emit(Op.LOADL, 1);
			else
				Machine.emit(Op.LOADL, 0);
		}
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		
		Machine.emit(Op.LOADA, Reg.SB, classDescriptors.get(expr.classtype.className.spelling));
		Machine.emit(Op.LOADL, expr.classtype.classDecl.fieldDeclList.size());
		Machine.emit(Prim.newobj);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.sizeExpr.visit(this, arg);
		Machine.emit(Prim.newarr);
		return null;
	}

//	@Override
//	public Object visitQualifiedRef(QualifiedRef ref, Object arg) {
//		ref.ref.visit(this, arg); //push the address of the controlling reference onto the stack
//		
//		MemberDecl memb = null;
//		if (ref.ref.type instanceof ClassType) {
//			memb = ((ClassType) ref.ref.type).classDecl.memberVars.get(ref.id.spelling);
//		}
//		else if (ref.ref.type instanceof ArrayType && ref.id.spelling.equals("length")) {
//			Machine.emit(Op.LOADL, -1);
//			Machine.emit(Machine.Prim.add);
//			return null;
//		}
//		else { //wait what? what does this mean?
//			throw new RuntimeException("Something bad happened");
//		}
//		
//		if (memb instanceof FieldDecl) {
//			FieldDecl field = (FieldDecl) memb;
////			if (field.type.typeKind == TypeKind.CLASS || field.type.typeKind == TypeKind.ARRAY) { //some magic which ends up with the address of the id in the heap appearing at the top of the stack
//			Machine.emit(Op.LOADL, field.heapoffset);
//			Machine.emit(Machine.Prim.add);
////			}
//		}
//		else if (memb instanceof MethodDecl) {
//			Machine.emit(Op.LOADI,1,0,0);
//			//TODO do we even need to do anything here?
//		}
//		return null;
//	}
	
	@Override
	public Object visitQualifiedRef(QualifiedRef ref, Object arg) {
		ref.ref.visit(this, arg); //push the address of the controlling reference onto the stack
		//this by the way doesn't mean that its the final address in heap. It could just be the address which holds
		//the value which points to the next address that we're going to follow
		
		if (!thisVisited) {
			Machine.emit(Op.LOADI,1,0,0); //I've concluded its just best to dereference everything before you continue when you enter here.
		}
		else {
			thisVisited = false;
		}
		
		
		if (ref.ref.type instanceof ArrayType && ref.id.spelling.equals("length")) {
			Machine.emit(Op.LOADL, -1);
			Machine.emit(Machine.Prim.add);
			return null;
		}

		if (ref.id.decl instanceof FieldDecl) {
			Machine.emit(Op.LOADL, ((FieldDecl) ref.id.decl).heapoffset);
			Machine.emit(Prim.add);
		}
		return null;
	}


	@Override
	public Object visitIndexedRef(IndexedRef ref, Object arg) {
		ref.ref.visit(this, arg); //push the local address to the top of the stack
		Machine.emit(Op.LOADI, 1, 0, 0); //get the address that that local address contained
		ref.indexExpr.visit(this, arg);
		Machine.emit(Machine.Prim.add);
		return null;
	}


	
	@Override
	//this part will either just return the local address or the heap address if you're grabbing your variable from a field declaration
	public Object visitIdRef(IdRef ref, Object arg) {
		//TODO this doesn't cover the case for static references using the class
		//TODO figure out how to do the offsets for differing scope levels
		if (ref.decl instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl) ref.decl;
			Machine.emit(Op.LOADA, Reg.OB, fd.heapoffset);
		}
		else if (ref.decl instanceof ParameterDecl) {
			Machine.emit(Op.LOADA, Reg.LB, ((ParameterDecl) ref.decl).paramOffset); 
		}
		else if (ref.decl instanceof VarDecl) {
			Machine.emit(Op.LOADA, Reg.LB, ref.decl.localOffset + 3); //+3 because of OB RA and DL
		}
		else { 
			Machine.emit(Op.LOADA, Reg.OB, 0);
		}
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		thisVisited = true;
		Machine.emit(Op.LOADA, Reg.OB, 0);
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return null;
	}
	
	private void reportError(String msg) {
		numberOfErrors++;
		System.out.println(msg);
	}
}
