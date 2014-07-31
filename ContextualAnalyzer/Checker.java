//Written by:
//Andrew J. Park
package miniJava.ContextualAnalyzer;

import miniJava.ExitCode;
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
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IndexedRef;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualifiedRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.AbstractSyntaxTrees.Type;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class Checker implements Visitor<IdentificationTable, Type> {
	public ErrorReporter reporter;
	
	// miniJava Standard Environment Class Declarations
	private ClassDecl systemDecl;
	private ClassDecl stringDecl;
	private ClassDecl printStreamDecl;

	// since we have to check all the classes for a single main method
	// we should keep a variable that tells us if a main method has been found
	private boolean mainFound;
	@SuppressWarnings("unused")
	private MethodDecl mainMethodDecl;
	
	//
	ClassDecl currentClass;
	
	//in static
	private boolean inStaticMethod;

	public Checker(ErrorReporter e) {
		reporter = e;
	}

	@Override
	public Type visitPackage(Package prog, IdentificationTable idTable) {
		SourcePosition posn = new SourcePosition(); // default source position
		for (ClassDecl cd : prog.classDeclList) {
			idTable.enterClass(cd.name, cd);
			//beta test code lets see if this ends up working...
			for (FieldDecl fd : cd.fieldDeclList) {
				if (cd.memberVars.containsKey(fd.name))
					reporter.reportError("*** multiply defined member variable names.");
				else
					cd.memberVars.put(fd.name, fd);
			}
			for (MethodDecl md : cd.methodDeclList) {
				if (cd.memberVars.containsKey(md.name))
					reporter.reportError("*** multiply defined member variable names.");
				else
					cd.memberVars.put(md.name, md);
					
			} //end
		}

		/* Setup the miniJava Standard Environment Classes */
		// Setup String Class Decl
		stringDecl = new ClassDecl("String", new FieldDeclList(),
				new MethodDeclList(), posn);

		stringDecl.type = new ClassType(new Identifier("String", new SourcePosition()), stringDecl.posn);
		idTable.enterClass("String", stringDecl);
		// Setup _PrintStream Class Decl
		MethodDeclList psMDL = new MethodDeclList();
		MemberDecl psMDLMember = new FieldDecl(false, false, new BaseType(
				TypeKind.VOID, posn), "println", posn);
		ParameterDeclList psPDL = new ParameterDeclList();
		psPDL.add(new ParameterDecl(new BaseType(TypeKind.INT, posn), "n", posn));
		MethodDecl psMDLMethod = new MethodDecl(psMDLMember, psPDL,
				new StatementList(), null, // return expression is null when
											// void?
				posn);
		psMDL.add(psMDLMethod);
		printStreamDecl = new ClassDecl("_PrintStream", new FieldDeclList(),
				psMDL, posn);
		printStreamDecl.type = new ClassType(new Identifier("_PrintStream", new SourcePosition()), printStreamDecl.posn);
		MemberDecl printlnMember = new FieldDecl(false, false, new BaseType(TypeKind.VOID, new SourcePosition()), "println", new SourcePosition());
		ParameterDeclList printlnPDL = new ParameterDeclList();
		printlnPDL.add(new ParameterDecl(new BaseType(TypeKind.INT, new SourcePosition()), "n", new SourcePosition()));
		MethodDecl printlnMethod = new MethodDecl(printlnMember, printlnPDL, null, null, printStreamDecl.posn);
		printStreamDecl.memberVars.put("println", printlnMethod);

		idTable.enterClass("_PrintStream", printStreamDecl);
		// Setup System Class Decl
		FieldDeclList systemFDL = new FieldDeclList();
		ClassType systemFDLClassType = new ClassType(new Identifier(
				"_PrintStream", posn), posn);
		systemFDL.add(new FieldDecl(false, true, systemFDLClassType, "out",
				posn));
		MethodDeclList systemMDL = new MethodDeclList();
		systemDecl = new ClassDecl("System", systemFDL, systemMDL, posn);
		systemDecl.type = new ClassType(new Identifier("System", new SourcePosition()), systemDecl.posn);
		systemDecl.memberVars.put("out", new FieldDecl(false, true, new ClassType(new Identifier("_PrintStream", new SourcePosition()), new SourcePosition()), "out", new SourcePosition()));
		idTable.enterClass("System", systemDecl);

		for (ClassDecl cd : prog.classDeclList)
			cd.visit(this, idTable);

		if (!mainFound) {
			reporter.reportError("*** no main method found in package");
		}
		return null;
	}

	@Override
	public Type visitClassDecl(ClassDecl cd, IdentificationTable idTable) {
		currentClass = cd;

		// fd.visit will enter the names of each field var into the idTable
		for (FieldDecl fd : cd.fieldDeclList)
			fd.visit(this, idTable);
		// Enter the names of the methods before you visit each method
		// declaration
		for (MethodDecl md : cd.methodDeclList)
			idTable.enter(md.name, md, true);
		for (MethodDecl md : cd.methodDeclList)
			md.visit(this, idTable);
		
		//I need to do this for some reason. Probably because I have so much duplicate code that I need to make sure all my
		//variables are initialized and this is a good place to do it.
		cd.type = new ClassType(new Identifier(cd.name, cd.posn), cd.posn);
		((ClassType) cd.type).classDecl = cd;
		((ClassType) cd.type).className = new Identifier(cd.name, cd.posn);
		
		idTable.closeClassScope();
		return null;
		// return new BaseType(TypeKind.CLASS, theIdTable.currentClass().posn);
	}

	@Override
	public Type visitFieldDecl(FieldDecl fd, IdentificationTable idTable) {
		idTable.enter(fd.name, fd);

		// Check to see if the user entered a void type for a field variable
		if (fd.type.typeKind == TypeKind.VOID) {
			reporter.reportError(String.format(
					"*** Line %d: Field variable cannot be of type 'void'",
					fd.type.posn.start));
			fd.type = new BaseType(TypeKind.UNSUPPORTED, fd.type.posn);
			return null;
		}
		
		// Check here to see if the class type that the field variable has
		// exists in the package scope
		if (fd.type.typeKind == TypeKind.CLASS) {
			ClassType fdClassType = (ClassType) fd.type;
			if (!idTable.classDefined(fdClassType.className.spelling)) {
				fd.type = new BaseType(TypeKind.UNSUPPORTED, fd.type.posn);
				reporter.reportError(String.format(
						"*** Line %d: cannot find symbol %s",
						fd.type.posn.start, fdClassType.className.spelling));
				return null;
			}
		}
		
		fd.type.visit(this, idTable);
		return null;
	}

	@Override
	public Type visitMethodDecl(MethodDecl md, IdentificationTable idTable) {
		idTable.openScope();
		
		md.owner = currentClass; //added for pa4
		
		inStaticMethod = md.isStatic;

		// Check to see if you've found the main method yet
		if (isMainMethod(md)) {
			if (mainFound)
				reporter.reportError("*** Line %d: main method was already defined.");
			mainFound = true;
			mainMethodDecl = md;
			md.isMainMethod = true;
		}

		for (ParameterDecl pd : md.parameterDeclList)
			pd.visit(this, idTable);
		for (Statement s : md.statementList) {
			if (s instanceof BlockStmt) {
				BlockStmt b = (BlockStmt) s;
				b.methodOwner = md;
			}
			if (s instanceof VarDeclStmt) {
				VarDeclStmt v = (VarDeclStmt) s;
				v.varDecl.methodOwner = md;
			}
			s.visit(this, idTable);
		}

		if (md.returnExp == null) {
			if (!md.type.typeKind.equals(TypeKind.VOID))
				reporter.reportError(String.format(
						"*** Line %d: missing return statement", md.posn.start));
			idTable.closeScope();
			return null;
		}
		// if the returnExp is a returnExp
		md.returnExp.visit(this, idTable);
		// if the methods actual return type doesn't match the declared return
		// type
		if (!md.type.typeKind.equals(md.returnExp.type.typeKind)) {
			reporter.reportError(String.format(
					"*** Line %d: incompatible return type",
					md.returnExp.posn.start));
		}
		idTable.closeScope();
		return null;
	}

	@Override
	public Type visitParameterDecl(ParameterDecl pd, IdentificationTable idTable) {
		idTable.enter(pd.name, pd);
		idTable.initialize(pd.name);
		if (invalidClassType(pd, idTable))
			reporter.reportError(String.format(
				"*** Line %d: cannot find symbol %s", pd.type.posn.start,
				((ClassType) pd.type).className.spelling));
		if (pd.type.typeKind == TypeKind.CLASS) {
			pd.type.visit(this, idTable); // here we'll check to see if the Type
													  // if its a class is valid
			((ClassType) pd.type).classDecl = idTable
					.retrieveClass(((ClassType) pd.type).className.spelling);
		} else if (pd.type.typeKind == TypeKind.VOID)
			reporter.reportError(String.format(
					"*** Line %d: Field variable cannot be of type 'void'",
					pd.type.posn.start));
		return null;
	}

	@Override
	public Type visitVarDecl(VarDecl decl, IdentificationTable idTable) {
		idTable.enter(decl.name, decl, false); // don't set it to initialized
												// yet because it
												// potentially be referenced in
												// the initialization expression
		if (isTypeKindVoid(decl.type))
			reporter.reportError(String.format(
					"*** Line %d: Field variable cannot be of type 'void'",
					decl.type.posn.start));
		
		decl.type.visit(this, idTable);
		return null;
	}

	@Override
	public Type visitBaseType(BaseType type, IdentificationTable idTable) {
		return type;
	}

	@Override
	public Type visitClassType(ClassType type, IdentificationTable idTable) {
		type.classDecl = idTable.retrieveClass(type.className.spelling);
		if (type.classDecl == null) {
			reporter.reportError(String.format(
					"*** Line %d: cannot find symbol %s", type.posn.start,
					type.className.spelling));
			type.typeKind = TypeKind.UNSUPPORTED;
		}
		return type;
	}

	@Override
	public Type visitArrayType(ArrayType type, IdentificationTable idTable) {
		type.eltType.visit(this, idTable);
		return type;
	}

	@Override
	public Type visitBlockStmt(BlockStmt stmt, IdentificationTable idTable) {
		idTable.openScope();
		for (Statement s : stmt.sl) {
			if (s instanceof VarDeclStmt) {
				((VarDeclStmt) s).varDecl.blockOwner = stmt;
				((VarDeclStmt) s).varDecl.methodOwner = stmt.methodOwner;
			}
			if (s instanceof BlockStmt) {
				((BlockStmt) s).methodOwner = stmt.methodOwner;
			}
			s.visit(this, idTable);
		}
		idTable.closeScope();
		return null;
	}

	@Override
	public Type visitVardeclStmt(VarDeclStmt stmt, IdentificationTable idTable) {
		stmt.varDecl.visit(this, idTable);
		stmt.initExp.visit(this, idTable);
		idTable.initialize(stmt.varDecl.name);

		checkTypeMismatch(stmt, stmt.varDecl.type, stmt.initExp.type);

		return null;
	}

	@Override
	public Type visitAssignStmt(AssignStmt stmt, IdentificationTable idTable) {
		stmt.ref.visit(this, idTable);
		stmt.val.visit(this, idTable);

		// since method names and field names share the same idTable you need to
		// make sure
		// that your assignments don't try to assign expressions to method
		// references
		if (stmt.ref.isMethod)
			reporter.reportError(String.format("*** Line %d: Can't assign a method reference to an expression.",
							stmt.posn.start));

		checkTypeMismatch(stmt, stmt.ref.type, stmt.val.type);
		return null;
	}

	@Override
	public Type visitCallStmt(CallStmt stmt, IdentificationTable idTable) {
		stmt.methodRef.visit(this, idTable);
		for (Expression a : stmt.argList)
			a.visit(this, idTable);
		if (isTypeKindError(stmt.methodRef.type))
			return null;
		else if (!stmt.methodRef.isMethod) {
			reporter.reportError(String.format(
					"*** Line %d: Method not defined.", stmt.posn.start));
			return null;
		} else if (!((MethodDecl) stmt.methodRef.decl).parameterDeclList
				.accepts(stmt.argList))
			reporter.reportError(String
					.format("*** Line %d: Actual and formal argument lists differ in length",
							stmt.posn.start));
		return null;
	}

	@Override
	public Type visitIfStmt(IfStmt stmt, IdentificationTable idTable) {
		stmt.cond.visit(this, idTable);
		
		checkConditionalIsBool(stmt.cond.type);
		
		stmt.thenStmt.visit(this, idTable);
		checkIfStmtVarDecl(stmt.thenStmt);
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, idTable);
			checkIfStmtVarDecl(stmt.elseStmt);
		}
		return null;
	}

	@Override
	public Type visitWhileStmt(WhileStmt stmt, IdentificationTable idTable) {
		stmt.cond.visit(this, idTable);
		
		checkConditionalIsBool(stmt.cond.type);
		
		stmt.body.visit(this, idTable);
		checkIfStmtVarDecl(stmt.body);
		return null;
	}



	@Override
	public Type visitUnaryExpr(UnaryExpr expr, IdentificationTable idTable) {
		expr.expr.visit(this, idTable);
		expr.operator.visit(this, idTable);
		expr.type = expr.operator.accepts(expr.expr);
		return expr.type;
	}

	@Override
	public Type visitBinaryExpr(BinaryExpr expr, IdentificationTable idTable) {
		expr.left.visit(this, idTable);
		expr.right.visit(this, idTable);
		expr.operator.visit(this, idTable);
		expr.type = expr.operator.accepts(expr.left, expr.right);
		if (expr.type.typeKind == TypeKind.ERROR) {
			reporter.reportError(String.format("*** Line %d: type error in binary expression.", expr.posn.start));
		}
		return expr.type;
	}

	@Override
	public Type visitRefExpr(RefExpr expr, IdentificationTable idTable) {
		expr.ref.visit(this, idTable);
		expr.type = expr.ref.type;
		if (expr.ref == null) {
			reporter.reportError(String.format("*** Line %d: cannot find reference symbol.", expr.ref.posn));
			expr.type = new BaseType(TypeKind.ERROR,expr.posn);
			return null;
		}
		/* PA4 ADDED THIS */
		if (expr.ref instanceof QualifiedRef && expr.ref.type.typeKind == TypeKind.ARRAY && ((QualifiedRef) expr.ref).id.spelling.equals("length")) {
			expr.type = new BaseType(TypeKind.INT, expr.posn);
		}
		else if ( !(expr.ref instanceof IndexedRef) && expr.ref.decl.isClassName) {
			reporter.reportError(String.format("*** Line %d: class name '%s' cannot be a reference expression", expr.ref.posn.start, expr.ref.decl.name));
			return null;
		}
			
		return expr.type;
	}

	@Override
	public Type visitCallExpr(CallExpr expr, IdentificationTable idTable) {
		expr.functionRef.visit(this, idTable);
		if (!expr.functionRef.isMethod) {
			reporter.reportError(String.format("*** Line %d: cannot find method symbol",
					 expr.posn.start));
		}
		for (Expression e : expr.argList)
			e.visit(this, idTable);
		if (!((MethodDecl) expr.functionRef.decl).parameterDeclList.accepts(expr.argList))
			reporter.reportError(String.format("*** Line %d: Actual and formal argument lists differ in length",
							                						 expr.posn.start));
		expr.type = expr.functionRef.type;
		return expr.type;
	}

	@Override
	public Type visitLiteralExpr(LiteralExpr expr, IdentificationTable idTable) {
		expr.literal.visit(this, idTable);
		expr.type = expr.literal.type;
		return expr.type;
	}

	@Override
	public Type visitNewObjectExpr(NewObjectExpr expr,
			IdentificationTable idTable) {
		expr.classtype.visit(this, idTable);
		expr.type = expr.classtype;
		if (expr.type == null)
			expr.type = new BaseType(TypeKind.ERROR, expr.posn);
		return expr.type;
	}

	@Override
	public Type visitNewArrayExpr(NewArrayExpr expr, IdentificationTable idTable) {
		expr.eltType.visit(this, idTable);
		expr.sizeExpr.visit(this, idTable);
		if (!isTypeKindInt(expr.sizeExpr.type)) {
			reporter.reportError(String.format("*** Line %d: type error. array size should be of type int.",
					 expr.posn.start));
			expr.sizeExpr.type = new BaseType(TypeKind.ERROR, expr.posn);
		}
		expr.type = new ArrayType(expr.eltType, expr.posn);
		return expr.type;
	}
	
	@Override
	public Type visitQualifiedRef(QualifiedRef qref, IdentificationTable idTable) {
		qref.ref.visit(this, idTable);
		if (qref.ref instanceof ThisRef) { //since at parse time we determined this starts the qref we don't have to check the errors that follows that
			ClassType id_decl_classtype = null;
			MemberDecl id_decl = currentClass.memberVars.get(qref.id.spelling);
			qref.id.decl = id_decl; //added for pa4
			if (id_decl == null) {
				reporter.reportError(String.format("*** Line %d: id error. couldn't find id %s within current class context." , qref.posn, qref.id.spelling));
			}
			qref.type = id_decl.type;
			qref.decl = id_decl;
			qref.isStatic = id_decl.isStatic;
			qref.isMethod = id_decl.isMethod;
		} //we're done dealing with ThisRef
		
		if (qref.ref.type.typeKind == TypeKind.ARRAY && qref.id.spelling.equals("length")) {
			qref.type = qref.ref.type;
			qref.decl = qref.ref.decl;
			qref.isStatic = false;
			qref.isMethod = false;
			return null;
		}
		if (qref.ref.type.typeKind != TypeKind.CLASS) {
			reporter.reportError(String.format("*** Line %d: type error. qualified reference ref must be of type class.",
			 qref.posn.start));
			return null;
		} //now we're guaranateed that qref is a typekind.class

		if (qref.ref.decl instanceof MethodDecl) {
			reporter.reportError(String.format("*** Line %d: id error. no access through a method in qualified reference", qref.posn.start));
		}
		
		ClassType id_decl_classtype = null;
		
		//if the controlling reference is a classtype then we can save 
		//the classdecl to ref_cd
		if (qref.ref.decl.type instanceof ClassType) {
			//this gets the classDecl of the controlling decl
			qref.ref.type.visit(this, idTable);
			ClassDecl ref_cd = ((ClassType) qref.ref.type).classDecl;
			MemberDecl id_decl = ref_cd.memberVars.get(qref.id.spelling);
			qref.id.decl = id_decl; //added for PA4
			if (id_decl == null) {
				reporter.reportError(String.format("*** Line %d: id error. cannot find member variable %s in class %s.",
																	qref.posn.start, qref.id.spelling, qref.ref.decl.name));
				return new BaseType(TypeKind.ERROR, qref.posn);
			} //this now guarantees that we got something when we checked the memberVars of this class
			
			if (id_decl.type instanceof ClassType) {
				id_decl_classtype = ((ClassType) id_decl.type);
				id_decl_classtype.classDecl = idTable.retrieveClass(id_decl.type.spelling());
			}
			

			if (!id_decl.isStatic) {
				if (qref.ref instanceof IdRef) {
					if (qref.ref.decl.isClassName) {
						reporter.reportError(String.format("*** no static member named %s", qref.id.spelling));
					}
				}
			}
			
			if (id_decl.isPrivate) {
				if (!currentClass.memberVars.containsKey(id_decl.name)) {
					if (!(qref.ref instanceof ThisRef) || !(qref.ref.decl == currentClass))
						reporter.reportError(String.format("*** cannot access private variable in current context"));
				}
			}
			
			if (id_decl.type instanceof BaseType) {
				qref.decl = id_decl;
				qref.type = id_decl.type;
				qref.isMethod = id_decl instanceof MethodDecl;
				qref.isStatic = id_decl.isStatic;
				return null;
			}
			
			qref.decl = id_decl;
			qref.type = id_decl.type;
			qref.isMethod = id_decl instanceof MethodDecl;
			qref.isStatic = id_decl.isStatic;
		}
		return null;
	}

	@Override
	public Type visitIndexedRef(IndexedRef ref, IdentificationTable idTable) {
		ref.ref.visit(this, idTable);
		ref.indexExpr.visit(this, idTable);
		
		if (!isTypeKindInt(ref.indexExpr.type)) {
			reporter.reportError(String.format("*** Line %d: type error. index expression should be of type int.",
					 ref.posn.start));
			ref.indexExpr.type = new BaseType(TypeKind.ERROR, ref.indexExpr.posn);
		}
		if (isTypeKindError(ref.ref.type)) {
			ref.type = new BaseType(TypeKind.ERROR, ref.posn);
			return ref.type;
		}
		else if (!isTypeKindArray(ref.ref.type)) {
			ref.type = new BaseType(TypeKind.ERROR, ref.posn);
			reporter.reportError(String.format("*** Line %d: type error. array required but %s found.",
					 ref.posn.start, ref.ref.type.spelling()));
			return ref.type;
		}
		ref.type = ((ArrayType) ref.ref.type).eltType;

//		if (ref.type instanceof ClassType) {
//			ref.decl = idTable.retrieveClass(ref.decl.name);
//		}
//		else {
//			ref.decl = 
//		}
		return ref.type;
	}

	@Override
	public Type visitThisRef(ThisRef ref, IdentificationTable idTable) {
		if (inStaticMethod) {
			reporter.reportError(String.format("*** Line %d: cannot reference 'this' inside a static context.",
					 ref.posn.start));
		}
		ref.decl = currentClass;
		ref.type = new ClassType(new Identifier(currentClass.name, ref.posn), ref.posn);
		return ref.type;
	}
	
	@Override
	public Type visitIdRef(IdRef ref, IdentificationTable idTable) {
		ref.decl = idTable.retrieve(ref.id.spelling);
		
		if (ref.decl == null) {
			ref.decl = idTable.retrieveClass(ref.id.spelling);
			if (ref.decl == null) {
				reporter.reportError(String.format("*** Line %d: cannot find reference symbol '%s'.",
						 ref.posn.start, ref.id.spelling));
			}
			else {
				ref.decl.isClassName = true;
			}
		}
		ref.type = ref.decl.type; //just make sure we initialize everything
		if (ref.decl instanceof MemberDecl) {
			ref.isStatic = ((MemberDecl) ref.decl).isStatic;
			
			//if you're in a static method and you try to access a non-static member
			if (inStaticMethod && !ref.isStatic) {
				reporter.reportError("*** tried to access a non-static member inside static context");
			}
		}
		ref.isMethod = ref.decl instanceof MethodDecl;
		ref.type = ref.decl.type;
		return null;
	}
	
	@Override
	public Type visitIdentifier(Identifier id, IdentificationTable idTable) {
		return null;
	}

	@Override
	public Type visitOperator(Operator op, IdentificationTable idTable) {
		return null;
	}

	@Override
	public Type visitIntLiteral(IntLiteral num, IdentificationTable idTable) {
		return num.type;
	}

	@Override
	public Type visitBooleanLiteral(BooleanLiteral bool,
			IdentificationTable idTable) {
		return bool.type;
	}

	/* PRIVATE HELPER METHODS BELOW */
	// checks to see if this MethodDecl has the correct form of a main method
	private boolean isMainMethod(MethodDecl md) {
		Type paramType;
		if (md.parameterDeclList.size() == 1) {
			paramType = md.parameterDeclList.get(0).type;
			// this line just tells you if the current method being inspected is
			// of the form
			// "public static void main(String[] someVarName)"
			if (!md.isPrivate && md.isStatic
					&& md.type.typeKind == TypeKind.VOID
					&& md.name.equals("main")
					&& paramType.typeKind == TypeKind.ARRAY
					&& isTypeKindClass(((ArrayType) paramType).eltType)
					&& ((ArrayType) md.parameterDeclList.get(0).type).eltType.spelling().equals("String"))
				return true;
		}
		return false;
	}

	private void checkTypeMismatch(Statement s, Type e, Type r) {
		if (!e.equals(r))
			reporter.reportError(String.format("*** Line %d: Type mismatch.",
					s.posn.start));
	}

	private boolean isTypeKindVoid(Type t) {
		return t.typeKind == TypeKind.VOID;
	}

	private boolean isTypeKindClass(Type t) {
		return t.typeKind == TypeKind.CLASS;
	}

	private boolean isTypeKindError(Type t) {
		return t.typeKind == TypeKind.ERROR;
	}
	
	private boolean isTypeKindBool(Type t) {
		return t.typeKind == TypeKind.BOOLEAN;
	}
	private boolean isTypeKindInt(Type t) {
		return t.typeKind == TypeKind.INT;
	}
	private boolean isTypeKindArray(Type t) {
		return t.typeKind == TypeKind.ARRAY;
	}

	private boolean invalidClassType(Declaration decl,
			IdentificationTable idTable) {
		return decl.type.typeKind == TypeKind.CLASS
				&& !idTable
						.classDefined(((ClassType) decl.type).className.spelling);
	}

	//check to see that the stmt isn't an instance of VarDeclStmt otherwise throw an error
	private void checkIfStmtVarDecl(Statement stmt) {
		if (stmt instanceof VarDeclStmt)
			reporter.reportError(String
					.format("*** Line %d: Variable declaration cannot be the solitary statement in a branch of a conditional statement.",
							stmt.posn.start));
	}
	
	//check that the conditional is of type bool. if its not report an error.
	private void checkConditionalIsBool(Type type) {
		if (!isTypeKindBool(type))
			reporter.reportError(String
					.format("*** Line %d: condition expression must be of type boolean.",
							type.posn.start));
	}
}
