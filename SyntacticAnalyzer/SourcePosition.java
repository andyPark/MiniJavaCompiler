package miniJava.SyntacticAnalyzer;

public class SourcePosition {

	public int start, finish;

	public SourcePosition() {
		start = 0;
		finish = 0;
	}

	public SourcePosition(int s, int f) {
		start = s;
		finish = f;
	}
	
	public SourcePosition(SourcePosition posn) {
		this(posn.start, posn.finish);
	}

	public String toString() {
		return "(" + start + ", " + finish + ")";
	}
	
	public SourcePosition copy() {
		return new SourcePosition(this);
	}
	
}