package miniJava.SyntacticAnalyzer;

public class Scanner {

	private SourceFile src;
	private boolean tokenBeingScanned;
	private char currentChar;
	private StringBuffer currentSpelling;

	public Scanner(SourceFile src) {
		this.src = src;
		tokenBeingScanned = false;
		currentChar = src.getSource();
		currentSpelling = new StringBuffer("");
	}

	private int scanToken() {
		if (isLetter(currentChar)) {
			takeIt();
			while (isLetter(currentChar) || isDigit(currentChar)
					|| currentChar == '_') {
				takeIt();
			}
			//Potential Bug - if illegal ascii character typed. return error?
//			if (isIllegal(currentChar))
//				System.exit(4);
			return Token.ID;
		}
		if (isDigit(currentChar)) {
			takeIt();
			while (isDigit(currentChar))
				takeIt();
			return Token.NUM;
		}
		switch (currentChar) {
			case '=':
				takeIt();
				if (currentChar == '=') {
					takeIt();
					return Token.EQUALS;
				}
				return Token.ASSIGN;
			case '>':
				takeIt();
				if (currentChar == '=') {
					takeIt();
					return Token.GE;
				}
				return Token.GT;
			case '<':
				takeIt();
				if (currentChar == '=') {
					takeIt();
					return Token.LE;
				}
				return Token.LT;
			case '!':
				takeIt();
				if (currentChar == '=') {
					takeIt();
					return Token.NEQ;
				}
				return Token.BANG;
			case '|':
				takeIt();
				if (currentChar != '|')
					return Token.ERROR;
				takeIt();
				return Token.OR;
			case '&':
				takeIt();
				if (currentChar != '&')
					return Token.ERROR;
				takeIt();
				return Token.AND;
			case '+':
				takeIt();
				return Token.PLUS;
			case '-':
				takeIt();
				if (currentChar == '-') {
					takeIt();
					return Token.ERROR;
				}
				return Token.MINUS;
			case '*':
				takeIt();
				return Token.MULT;
			case '/':
				takeIt();
				if (currentChar == '/') { //double slash comment
					while (currentChar != '\n' && currentChar != '\r'
							&& currentChar != SourceFile.eot)
						takeIt();
					while (currentChar == ' ' || currentChar == '\n'
							|| currentChar == '\r' || currentChar == '\t')
						takeIt();
					currentSpelling = new StringBuffer("");
					return scanToken();
				}
				if (currentChar == '*') {
					takeIt();
					boolean inComment = true;
					while (inComment) {
						if (currentChar == SourceFile.eot) {
							currentSpelling = new StringBuffer("");
							return Token.ERROR;
						}
						if (currentChar == '*') {
							takeIt();
							if (currentChar == '/') {
								inComment = false;
								takeIt();
							}
						}
						else {
							takeIt();
						}
					}
					while (currentChar == ' ' || currentChar == '\n'
							|| currentChar == '\r' || currentChar == '\t')
						takeIt();
					currentSpelling = new StringBuffer("");
					return scanToken();
				}
				return Token.DIV;
			case '.':
				takeIt();
				return Token.DOT;
			case ',':
				takeIt();
				return Token.COMMA;
			case ';':
				takeIt();
				return Token.SEMICOLON;
			case '(':
				takeIt();
				return Token.LPAREN;
			case ')':
				takeIt();
				return Token.RPAREN;
			case '[':
				takeIt();
				return Token.LBRACKET;
			case ']':
				takeIt();
				return Token.RBRACKET;
			case '{':
				takeIt();
				return Token.LCURLY;
			case '}':
				takeIt();
				return Token.RCURLY;
			case SourceFile.eot:
				return Token.EOT;
			default:
				takeIt();
				return Token.ERROR;
		}
	}

	public Token scan() {

		SourcePosition thePos;
		int kind;

		tokenBeingScanned = false;

		while (isWhiteSpace(currentChar))
			takeIt();

		tokenBeingScanned = true;
		currentSpelling = new StringBuffer("");

		thePos = new SourcePosition();
		thePos.start = src.getCurrentLine();
		kind = scanToken();
		thePos.finish = src.getCurrentLine();

		return new Token(kind, currentSpelling.toString(), thePos);
	}

	private void takeIt() {
		if (tokenBeingScanned)
			currentSpelling.append(currentChar);
		currentChar = src.getSource();
	}

	private boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private boolean isWhiteSpace(char c) {
		return currentChar == ' ' || currentChar == '\n' || currentChar == '\r'
				|| currentChar == '\t';
	}
}