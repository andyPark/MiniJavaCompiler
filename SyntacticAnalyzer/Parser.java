package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ExitCode;

public class Parser {
	private Token currentToken;
	private Scanner lexer;

	public Parser(Scanner lexer) {
		this.lexer = lexer;
	}
	
	private void scanError() {
		System.out.printf("Line: %d. Scan error. Error token spelling: %s\n", currentToken.position.start, currentToken.spelling);
		System.exit(ExitCode.FAILURE);
	}

	private void parseError() {
		if (currentToken.kind == Token.ERROR) 
			scanError();
		System.out.println("Line: " + currentToken.position.start
				+ ". Parse Error." + " Current Token: " + currentToken.spelling
				+ ".");
		System.exit(ExitCode.FAILURE);
	}

	private void parseError(String message) {
		if (currentToken.kind == Token.ERROR) 
			scanError();
		System.out.println(message);
		System.exit(ExitCode.FAILURE);
	}

	public void accept(int expectedToken) {
		if (expectedToken == currentToken.kind) {
			currentToken = lexer.scan();
		} else {
			parseError("Line: " + currentToken.position.start
					+ ". Expected Token " + Token.spell(expectedToken)
					+ " but received " + Token.spell(currentToken.kind)
					+ " instead.");
		}
	}

	public void acceptIt() {
		currentToken = lexer.scan();
	}

	public Package parse() {
		return parseProgram();
	}

	private Package parseProgram() {
		ClassDeclList classDeclList = new ClassDeclList();
		SourcePosition posn = new SourcePosition();
		currentToken = lexer.scan();
		posn.start = currentToken.position.start;
		while (currentToken.kind != Token.EOT) {
			classDeclList.add(parseClassDeclaration());
		}
		posn.finish = currentToken.position.finish;
		accept(Token.EOT);
		return new Package(classDeclList, posn);
	}

	private ClassDecl parseClassDeclaration() {
		String name;
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		SourcePosition posn = new SourcePosition();
		posn.start = currentToken.position.start;
		accept(Token.CLASS);
		name = currentToken.spelling;
		accept(Token.ID);
		accept(Token.LCURLY);
		MemberDecl memberDecl;
		while (currentToken.kind != Token.RCURLY) {
			memberDecl = parseDeclaration();

			// TODO this is really ugly. Find an alternative that is more type
			// safe.
			if (memberDecl instanceof FieldDecl)
				fdl.add((FieldDecl) memberDecl);
			else
				mdl.add((MethodDecl) memberDecl);
		}
		posn.finish = currentToken.position.finish;
		accept(Token.RCURLY);
		return new ClassDecl(name, fdl, mdl, posn);
	}

	private MemberDecl parseDeclaration() {
		// parse either a FieldDeclaration or a MethodDeclaration
		/*
		 * Declaration ::= FieldDeclaration | MethodDeclaration
		 * 
		 * FieldDeclaration ::= Declarators id;
		 * 
		 * MethodDeclaration ::= Declarators id ( ParameterList? ) { Statement*
		 * (return Expression; )? }
		 */
		boolean isPrivate = false;
		boolean isStatic = false;
		SourcePosition posn = new SourcePosition();
		Type theType;
		String name;
		posn.start = currentToken.position.start;
		if (currentToken.kind == Token.PUBLIC
				|| currentToken.kind == Token.PRIVATE) {
			isPrivate = currentToken.kind == Token.PRIVATE;
			acceptIt();
		}
		if (currentToken.kind == Token.STATIC) {
			isStatic = true;
			acceptIt();
		}
		theType = parseType();
		name = currentToken.spelling;
		posn.finish = currentToken.position.finish;
		MemberDecl memberDeclaration = new FieldDecl(isPrivate, isStatic,
				theType, name, new SourcePosition(posn));
		accept(Token.ID);
		if (currentToken.kind == Token.LPAREN) { // then it's a method
													// declaration
			acceptIt();
			ParameterDeclList pdl = new ParameterDeclList();
			if (isTypeStarter()) {
				pdl = parseParameterList();
			}
			accept(Token.RPAREN);
			StatementList slist = new StatementList();
			Expression returnExpression;

			accept(Token.LCURLY);
			// TODO What should we make the returnExpression if one doesn't
			// exist? Null?
			while (isStatementStarter()) {
				slist.add(parseStatement()); // TODO make parseStatement return
												// things of type Statement
			}
			if (currentToken.kind == Token.RETURN) {
				acceptIt();
				returnExpression = parseExpression();
				accept(Token.SEMICOLON);
			}
			else {
				returnExpression = null;
			}
			posn.finish = currentToken.position.finish;
			accept(Token.RCURLY);
			return new MethodDecl(memberDeclaration, pdl, slist,
					returnExpression, posn);
		} else if (currentToken.kind == Token.SEMICOLON) { // then it's a field
															// declaration
			posn.finish = currentToken.position.finish;
			acceptIt();
			return new FieldDecl(isPrivate, isStatic, theType, name, posn);
		} else {
			parseError();
			return null;
		}
	}

	private Type parseType() {
		Type theType;
		SourcePosition posn = new SourcePosition();
		posn.start = currentToken.position.start;
		switch (currentToken.kind) {
		case Token.BOOLEAN:
			posn.finish = currentToken.position.finish;
			acceptIt();
			theType = new BaseType(TypeKind.BOOLEAN, posn);
			break;
		case Token.VOID:
			posn.finish = currentToken.position.finish;
			acceptIt();
			theType = new BaseType(TypeKind.VOID, posn);
			break;
		case Token.INT:
			posn.finish = currentToken.position.finish;
			theType = new BaseType(TypeKind.INT, new SourcePosition(posn));
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) { // TODO refactor this
														// line and the one
														// below it to be a
														// single method?
				acceptIt();
				posn.finish = currentToken.position.finish;
				accept(Token.RBRACKET);
				theType = new ArrayType(theType, posn);
			}
			break;
		case Token.ID:
			posn.finish = currentToken.finishPos();
			Identifier theIdentifier = new Identifier(currentToken.spelling,
					currentToken.position.copy());
			theType = new ClassType(theIdentifier, posn.copy());
			// TODO don't I need to save the name somewhere?
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				posn.finish = currentToken.finishPos();
				accept(Token.RBRACKET);
				theType = new ArrayType(theType, posn);
			}
			break;
		default:
			theType = null;
			parseError();
		}
		return theType;
	}

	private ParameterDeclList parseParameterList() {
		ParameterDeclList pdl = new ParameterDeclList();
		Type theType;
		SourcePosition posn = new SourcePosition();
		posn.start = currentToken.startPos();
		theType = parseType();
		String name = currentToken.spelling;
		posn.finish = currentToken.finishPos();
		accept(Token.ID);
		pdl.add(new ParameterDecl(theType, name, posn));
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			posn.start = currentToken.startPos();
			theType = parseType();
			name = currentToken.spelling;
			posn.finish = currentToken.finishPos();
			accept(Token.ID);
			pdl.add(new ParameterDecl(theType, name, posn));
		}
		return pdl;
	}



	private Statement parseStatement() {
		SourcePosition posn = new SourcePosition();
		posn.start = currentToken.startPos();
		switch (currentToken.kind) {
		case Token.LCURLY:
			StatementList sl = new StatementList();
			acceptIt();
			while (isStatementStarter()) {
				sl.add(parseStatement());
			}
			posn.finish = currentToken.finishPos();
			accept(Token.RCURLY);
			return new BlockStmt(sl, posn);
		case Token.IF:
			acceptIt();
			Statement thenStatement;

			Statement elseStatement = null;
			Expression condition;
			accept(Token.LPAREN);
			condition = parseExpression();
			accept(Token.RPAREN);
			thenStatement = parseStatement();
			posn.finish = thenStatement.posn.finish;
			if (currentToken.kind == Token.ELSE) {
				posn.finish = currentToken.finishPos();
				acceptIt();
				elseStatement = parseStatement();
			}
			return new IfStmt(condition, thenStatement, elseStatement, posn);
		case Token.WHILE:
			acceptIt();
			accept(Token.LPAREN);
			Expression cond = parseExpression();
			accept(Token.RPAREN);
			Statement body = parseStatement();
			posn.finish = body.posn.finish;
			return new WhileStmt(cond, body, posn);
		case Token.BOOLEAN:
		case Token.VOID:
		case Token.INT:
			// This case is an assignment statement
			Type theType = parseType();
			String name = currentToken.spelling;
			posn.finish = currentToken.finishPos();
			VarDecl vd = new VarDecl(theType, name, posn.copy());
			accept(Token.ID);
			accept(Token.ASSIGN);
			Expression expr = parseExpression();
			posn.finish = currentToken.finishPos();
			accept(Token.SEMICOLON);
			return new VarDeclStmt(vd, expr, posn);
		case Token.THIS:
			Reference r = parseReference();
			return parseS2(r, posn);
		case Token.ID:
			Identifier iden = new Identifier(currentToken.spelling,
					new SourcePosition(currentToken.finishPos(),
							currentToken.finishPos()));

			acceptIt();
			return parseS101(iden, posn);
		default:
			parseError();
			return null;
		}
	}

	// Statement ::= Type id = Expression;

	// parseS2 ::= ( ArgsList? ); | = Expression;
	private Statement parseS2(Reference ref, SourcePosition posn) {
		if (currentToken.kind == Token.LPAREN) {
			acceptIt();
			ExprList argsList = new ExprList();
			if (isExpressionStarter()) {
				argsList = parseArgsList();
			}
			accept(Token.RPAREN);
			posn.finish = currentToken.finishPos();
			accept(Token.SEMICOLON);
			return new CallStmt(ref, argsList, posn);
		} else if (currentToken.kind == Token.ASSIGN) {
			acceptIt();
			Expression e = parseExpression();
			posn.finish = currentToken.finishPos();
			accept(Token.SEMICOLON);
			return new AssignStmt(ref, e, posn);
		} else {
			parseError();
			return null;
		}
	}

	// S101 ::= (id = Expression;) OR ( [ S102 ) OR (. | = | '(' ) ReferenceTail CallOrAssignment
	private Statement parseS101(Identifier iden, SourcePosition posn) {
		int finish_posn;
		Reference theIdRef = new IdRef(iden, iden.posn);
		switch (currentToken.kind) {
		case Token.ID:
			String name = currentToken.spelling;
			ClassType theType = new ClassType(iden, iden.posn.copy());
			SourcePosition vdPos = new SourcePosition(iden.posn.start,
					currentToken.finishPos());
			VarDecl vd = new VarDecl(theType, name, vdPos);
			acceptIt();
			accept(Token.ASSIGN);
			Expression expr = parseExpression();
			posn.finish = currentToken.finishPos();
			accept(Token.SEMICOLON);
			return new VarDeclStmt(vd, expr, posn);
		case Token.LBRACKET:
			acceptIt();
			return parseS102(iden, posn);
		case Token.DOT:
			return parseCallOrAssignment(parseRefTail(theIdRef, posn), posn);
		case Token.ASSIGN:
			acceptIt();
			Expression e = parseExpression();
			finish_posn = currentToken.finishPos();
			accept(Token.SEMICOLON);
			return new AssignStmt(theIdRef, e, new SourcePosition(posn.start, finish_posn));
		case Token.LPAREN:
			return parseCallOrAssignment(theIdRef, posn);
		default:
			parseError();
			return null;
		}
	}

	// CallOrAssignment ::= ( = Expression; ) OR ( (ArgsListE1); )
	private Statement parseCallOrAssignment(Reference ref, SourcePosition posn) {
		int finish_posn;
		ExprList el;
		if (currentToken.kind == Token.ASSIGN) {
			acceptIt();
			Expression e = parseExpression();
			finish_posn = currentToken.finishPos();
			accept(Token.SEMICOLON);
			return new AssignStmt(ref, e, new SourcePosition(posn.start, finish_posn));
		} else if (currentToken.kind == Token.LPAREN) {
			acceptIt();
			el = (isExpressionStarter()) ? parseArgsList() : new ExprList();
			accept(Token.RPAREN);
			finish_posn = currentToken.finishPos();
			accept(Token.SEMICOLON);
			return new CallStmt(ref, el, new SourcePosition(posn.start, finish_posn));
		}
		else {
			parseError();
			return null;
		}
	}

	// S102 ::= ] id = Expression; OR Expression ] ReferenceTail = Expression;
	private Statement parseS102(Identifier iden, SourcePosition posn) {
		if (currentToken.kind == Token.RBRACKET) {
			posn.finish = currentToken.finishPos();
			acceptIt();
			ClassType ct = new ClassType(iden, iden.posn);
			ArrayType arrType = new ArrayType(ct, posn.copy());
			String name = currentToken.spelling;
			SourcePosition vdPosn = new SourcePosition(posn.start,
					currentToken.finishPos());
			VarDecl vd = new VarDecl(arrType, name, vdPosn);
			accept(Token.ID);
			accept(Token.ASSIGN);
			Expression expr = parseExpression();
			posn.finish = currentToken.finishPos();
			accept(Token.SEMICOLON);
			return new VarDeclStmt(vd, expr, posn);
		} else if (isExpressionStarter()) {
			Expression anExpr = parseExpression();
			IndexedRef indexRef = new IndexedRef(new IdRef(iden, iden.posn),
					anExpr, new SourcePosition(posn.start,
							currentToken.finishPos()));
			accept(Token.RBRACKET);
			Reference ref = parseIndexRefTail(indexRef, posn);
			accept(Token.ASSIGN);
			Expression secondExpr = parseExpression();
			posn.finish = currentToken.finishPos();
			accept(Token.SEMICOLON);
			return new AssignStmt(ref, secondExpr, posn);
		} else {
			parseError();
			return null;
		}
	}

	// ReferenceTail ::= (.ID OR .ID[Expression])* //Follows an IndexReference
	private Reference parseIndexRefTail(IndexedRef indexRef, SourcePosition posn) {
		Reference returnRef = indexRef;
		Identifier id;
		Expression expr;
		while (currentToken.kind == Token.DOT) { // first go round
			acceptIt();
			id = new Identifier(currentToken.spelling, currentToken.position);
			accept(Token.ID);
			returnRef = new QualifiedRef(returnRef, id, new SourcePosition(
					indexRef.posn.start, currentToken.finishPos()));
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				expr = parseExpression();
				posn.finish = currentToken.finishPos();
				returnRef = new IndexedRef(returnRef, expr, posn.copy());
				accept(Token.RBRACKET);
			}
		}
		return returnRef;
	}

	private Expression parseExpression() {
		return parseExprOr();
	}

	private Expression parseExprOr() {
		// ExprOr ::= ExprAnd ( || ExprAnd )*
		SourcePosition posn = new SourcePosition(currentToken.position);
		Expression expr = parseExprAnd();
		Expression expr2;
		Token theOrOperator;
		while (currentToken.kind == Token.OR) {
			theOrOperator = currentToken;
			acceptIt();
			expr2 = parseExprAnd();
			posn.finish = expr2.posn.finish;
			expr = new BinaryExpr(new Operator(theOrOperator, theOrOperator.position), expr, expr2, posn);
			posn = new SourcePosition(posn);
		}
		return expr;
	}

	private Expression parseExprAnd() {
		// ExprAnd ::= ExprEquality ( && ExprEquality )*
		SourcePosition posn = new SourcePosition(currentToken.position);
		Expression expr = parseExprEquality();
		Expression expr2;
		Token theAndOperator;
		while (currentToken.kind == Token.AND) {
			theAndOperator = currentToken;
			acceptIt();
			expr2 = parseExprEquality();
			posn.finish = expr2.posn.finish;
			expr = new BinaryExpr(new Operator(theAndOperator, theAndOperator.position), expr, expr2, posn);
			posn = new SourcePosition(posn);
		}
		return expr;
	}
	
	private Expression parseExprEquality() {
		// ExprEquality ::= ExprRel ( (== | !=) ExprRel )*
		SourcePosition posn = new SourcePosition(currentToken.position);
		Expression expr = parseExprRel();
		Expression expr2;
		Token theEqualityOperator;
		while (currentToken.kind == Token.EQUALS
				   ||   currentToken.kind == Token.NEQ) {
			theEqualityOperator = currentToken;
			acceptIt();
			expr2 = parseExprRel();
			posn.finish = expr2.posn.finish;
			expr = new BinaryExpr(new Operator(theEqualityOperator, theEqualityOperator.position), expr, expr2, posn);
			posn = new SourcePosition(posn);
		}
		return expr;
	}
	
	private Expression parseExprRel() {
		// ExprRel ::= ExprAdd ( (<= | < | > | >=) ExprAdd )*
		SourcePosition posn = new SourcePosition(currentToken.position);
		Expression expr = parseExprAdd();
		Expression expr2;
		Token theRelOperator;
		while (isRelOperator()) {
			theRelOperator = currentToken;
			acceptIt();
			expr2 = parseExprAdd();
			posn.finish = expr2.posn.finish;
			expr = new BinaryExpr(new Operator(theRelOperator, theRelOperator.position), expr, expr2, posn);
			posn = new SourcePosition(posn);
		}
		return expr;
	}
	
	private Expression parseExprAdd() {
		// ExprAdd ::= ExprMult ( (+ | -) ExprMult )*
		SourcePosition posn = new SourcePosition(currentToken.position);
		Expression expr = parseExprMult();
		Expression expr2;
		Token theAdditiveOperator;
		while (currentToken.kind == Token.PLUS
				  || currentToken.kind == Token.MINUS) {
			theAdditiveOperator = currentToken;
			acceptIt();
			expr2 = parseExprMult();
			posn.finish = expr2.posn.finish;
			expr = new BinaryExpr(new Operator(theAdditiveOperator, theAdditiveOperator.position), expr, expr2, posn);
			posn = new SourcePosition(posn);
		}
		return expr;
	}
	
	private Expression parseExprMult() {
		// ExprMult ::= ExprUnary
		SourcePosition posn = new SourcePosition(currentToken.position);
		Expression expr = parseExprUnary();
		Expression expr2;
		Token theMultOperator;
		while (currentToken.kind == Token.DIV
				  || currentToken.kind == Token.MULT) {
			theMultOperator = currentToken;
			acceptIt();
			expr2 = parseExprUnary();
			posn.finish = expr2.posn.finish;
			expr = new BinaryExpr(new Operator(theMultOperator, theMultOperator.position), expr, expr2, posn);
			posn = new SourcePosition(posn);
		}
		return expr;
	}
	
	private Expression parseExprUnary() {
		// ExprUnary ::= ( - | ! )ExprUnary | Expr
		SourcePosition posn = new SourcePosition(currentToken.position);
		Token theUnaryOperator;
		Expression theExpr;
		if (currentToken.kind == Token.BANG || currentToken.kind == Token.MINUS) {
			theUnaryOperator = currentToken;
			acceptIt();
			theExpr = parseExprUnary();
			posn.finish = theExpr.posn.finish;
			return new UnaryExpr(new Operator(theUnaryOperator, theUnaryOperator.position), theExpr, posn);
		}
		return parseBaseExpression();
	}
	
	//Once you've dealt with the order of operations you can now work on parsing the normal expressions
	private Expression parseBaseExpression() {
		SourcePosition posn = new SourcePosition();
		posn.start = currentToken.startPos();
		Expression theExpr;
		switch (currentToken.kind) {
		case Token.TRUE:
		case Token.FALSE:
			Expression boolLitExpr = new LiteralExpr(new BooleanLiteral(currentToken.spelling, currentToken.position), currentToken.position);
			acceptIt();
			return boolLitExpr;
		case Token.NUM:
			Expression IntLitExpr = new LiteralExpr(new IntLiteral(currentToken.spelling, currentToken.position), currentToken.position);
			acceptIt();
			return IntLitExpr;
		case Token.THIS:
		case Token.ID:
			Reference r = parseReference();
			return parseE1(r); //use the start of reference posn
		case Token.LPAREN:
			int paren_start = currentToken.startPos();
			acceptIt();
			theExpr = parseExpression();
			theExpr.posn.start = paren_start;
			theExpr.posn.finish = currentToken.finishPos();
			accept(Token.RPAREN);
			return theExpr;
		// Expression ::= new newObjectExpr
		case Token.NEW:
			int new_start = currentToken.startPos();
			acceptIt();
			return parseNewObjectExpr(new_start);
		default:
			parseError();
			return null;
		}
	}

	private Expression parseE1(Reference ref) {
		if (currentToken.kind == Token.LPAREN) {
			acceptIt();
			Expression theCallExpr = parseArgsListE1(ref);
			theCallExpr.posn.finish = currentToken.finishPos();
			accept(Token.RPAREN);
			return theCallExpr;
		}
		else {
			return new RefExpr(ref, ref.posn);
		}
	}
	
	private Expression parseArgsListE1(Reference ref) {
		if (isExpressionStarter()) {
			ExprList el = parseArgsList();
			return new CallExpr(ref, el, ref.posn.copy());
		}
		else {
			return new CallExpr(ref, new ExprList(), ref.posn.copy());
		}
	}
	
	private ExprList parseArgsList() {
		ExprList el = new ExprList();
		el.add(parseExpression());
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			el.add(parseExpression());
		}
		return el;
	}

	
	//TODO Start back up here
	// NewObjectExpr ::= int BracketExpression | id LeftE2
	private Expression parseNewObjectExpr(int startPosn) { //startPosn of the new keyword
		Type theType;
		ClassType theClassType;
		switch (currentToken.kind) {
		case Token.INT:
			theType = new BaseType(TypeKind.INT, currentToken.position);
			acceptIt();
			return parseBracketExpression(startPosn, theType);
		case Token.ID:
			theClassType = new ClassType(new Identifier(currentToken.spelling, currentToken.position), currentToken.position);
			acceptIt();
			return parseLeftE2(startPosn, theClassType);
		default:
			parseError();
			return null;
		}
	}

	private Expression parseBracketExpression(int startPosn, Type theType) {
		accept(Token.LBRACKET);
		Expression e = parseExpression();
		int finishPosn = currentToken.position.finish;
		accept(Token.RBRACKET);
		return new NewArrayExpr(theType, e, new SourcePosition(startPosn, finishPosn));
	}

	//LeftE2 ::= () | [ Expression ]
	//Will return either a new Object Expression or new Array Expression (an array of objects)
	private Expression parseLeftE2(int newStartPosn, ClassType theType) { 
		int theFinishPosn;
		switch (currentToken.kind) {
		case Token.LPAREN:
			acceptIt();
			theFinishPosn = currentToken.finishPos();
			accept(Token.RPAREN);
			return new NewObjectExpr(theType, new SourcePosition(newStartPosn, theFinishPosn));
		case Token.LBRACKET:
			acceptIt();
			Expression e = parseExpression();
			theFinishPosn = currentToken.finishPos();
			accept(Token.RBRACKET);
			return new NewArrayExpr(theType, e, new SourcePosition(newStartPosn, theFinishPosn));
		default: // DEBUG LINE
			parseError();
			return null; //this shouldn't ever run
		}
	}

	private Reference parseReference() {
		Reference r = parseBaseRef();
		return parseRefTail(r, r.posn.copy());
	}

	private Reference parseBaseRef() {
		SourcePosition posn = currentToken.position;
		if (currentToken.kind == Token.THIS) {
			acceptIt();
			return new ThisRef(posn);
		} else if (currentToken.kind == Token.ID) {
			Identifier iden = new Identifier(currentToken.spelling, posn);
			IdRef theIdRef = new IdRef(iden, iden.posn);
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				Expression e = parseExpression();
				int theFinish = currentToken.finishPos();
				accept(Token.RBRACKET);
				return new IndexedRef(theIdRef, e, new SourcePosition(iden.posn.start, theFinish));
			}
			else {
				return theIdRef;
			}
		} else {
			parseError();
			return null;
		}
	}
	
	private Reference parseRefTail(Reference theRef, SourcePosition posn) {
		Reference returnRef = theRef;
		Identifier id;
		Expression expr;
		while (currentToken.kind == Token.DOT) {
			acceptIt();
			id = new Identifier(currentToken.spelling, currentToken.position);
			accept(Token.ID);
			returnRef = new QualifiedRef(returnRef, id, new SourcePosition(
					theRef.posn.start, currentToken.finishPos()));
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				expr = parseExpression();
				posn.finish = currentToken.finishPos();
				returnRef = new IndexedRef(returnRef, expr, posn.copy());
				accept(Token.RBRACKET);
			}
		}
		return returnRef;
	}

	// checks if the currentToken.kind is a starter(expression)
	private boolean isExpressionStarter() {
		switch (currentToken.kind) {
		case Token.THIS:
		case Token.ID:
		case Token.UNOP:
		case Token.LPAREN:
		case Token.NUM:
		case Token.TRUE:
		case Token.FALSE:
		case Token.NEW:
		case Token.MINUS:
			return true;
		default:
			return false;
		}
	}

	// if | while | boolean | void | int | id | this | {
	private boolean isStatementStarter() {
		switch (currentToken.kind) {
		case Token.IF:
		case Token.WHILE:
		case Token.BOOLEAN:
		case Token.VOID:
		case Token.INT:
		case Token.ID:
		case Token.THIS:
		case Token.LCURLY:
			return true;
		default:
			return false;
		}
	}

	private boolean isTypeStarter() {
		switch (currentToken.kind) {
		case Token.BOOLEAN:
		case Token.VOID:
		case Token.INT:
		case Token.ID:
			return true;
		default:
			return false;
		}
	}
	
	private boolean isRelOperator() {
		switch(currentToken.kind) {
		case Token.GT: case Token.LT:
		case Token.LE: case Token.GE:
			return true;
		default:
			return false;
		}
	}

	// Obsolete Method - Used in PA1 and no longer used.
	// private void parseDeclarators() {
	// if (currentToken.kind == Token.PUBLIC || currentToken.kind ==
	// Token.PRIVATE) {
	// acceptIt();
	// }
	// if (currentToken.kind == Token.STATIC) {
	// acceptIt();
	// }
	// parseType();
	// }
//	private boolean isBinOp() {
//		switch (currentToken.kind) {
//		case Token.PLUS:
//		case Token.MINUS:
//		case Token.MULT:
//		case Token.DIV:
//		case Token.GT:
//		case Token.LT:
//		case Token.GE:
//		case Token.LE:
//		case Token.EQUALS:
//		case Token.NEQ:
//		case Token.AND:
//		case Token.OR:
//			return true;
//		default:
//			return false;
//		}
//	}
//
//	private boolean isUnaryOp() {
//		switch (currentToken.kind) {
//		case Token.MINUS:
//		case Token.BANG:
//			return true;
//		default:
//			return false;
//		}
//	}
}