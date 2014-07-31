package miniJava.SyntacticAnalyzer;

public class Token {
	public int kind;
	public String spelling;
	SourcePosition position;
	
	public static final int
	ID = 0, NUM = 1, OPERATOR = 2, UNOP = 3,
	
	//reserved keywords starts here
	BOOLEAN = 4, CLASS = 5, ELSE = 6, FALSE = 7, IF = 8, INT = 9, NEW = 10,
	PRIVATE = 11, PUBLIC = 12, RETURN = 13, STATIC = 14, THIS = 15,
	TRUE = 16, VOID = 17, WHILE = 18,
	//reserved keywords end here

	DOT = 19, SEMICOLON = 20, COMMA = 21, LPAREN = 22, RPAREN = 23,
	LBRACKET = 24, RBRACKET = 25, LCURLY = 26, RCURLY = 27,
	ASSIGN = 28,

	EOT = 29,

	//operators start here
	GT = 30, GE = 31, LT = 32, LE = 33, EQUALS = 34, NEQ = 35,
	AND = 36, OR = 37, BANG = 38, PLUS = 39, MINUS = 40, MULT = 41,
	DIV = 42,
	//operators end here

	ERROR = 43;
	
	//indices where the reserve keywords start and end
	private final static int RESERVE_START = 4,
							   RESERVE_END = 18;
	
	public static String[] tokenTable = new String[] { "<identifier>", "<int>",
		"<binary operator>", "<unary operator>", "boolean", "class",
		"else", "false","if", "int", "new", "private", "public", "return",
		"static", "this", "true", "void", "while", ".", ";", ",", "(", ")",
		"[", "]", "{", "}", "=", "", ">", ">=", "<", "<=", "==", "!=",
		"&&", "||", "!", "+", "-", "*", "/", "<error>" };
	
	public Token(int kind, String spelling, SourcePosition position) {
		this.kind = kind;
		this.spelling = spelling;
		this.position = position;
		
		if (kind == ID) {
			for (int currentKind = RESERVE_START; currentKind <= RESERVE_END; currentKind++) {
				int comparisonResult = tokenTable[currentKind].compareTo(spelling);
				
				//this if block is a
				//very slight optimization here to just break out since we know that
				//if comparisonResult > 0 than there's no point in checking the rest of
				//the list because it's in alphabetical order
				if (comparisonResult > 0)
					break;
				
				if (comparisonResult == 0) {
					this.kind = currentKind;
					break;
				}
			}
		}	
	}
	
	public static String spell(int tokenKind) {
		return tokenTable[tokenKind];
	}
	
	public int startPos() {
		return position.start;
	}
	public int finishPos() {
		return position.finish;
	}
}