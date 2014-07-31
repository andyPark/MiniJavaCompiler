package miniJava.ContextualAnalyzer;

public class ErrorReporter {
	private int numberOfErrors;
	public ErrorReporter() {
		numberOfErrors = 0;
	}
	public int numberOfErrors() {
		if (numberOfErrors < 0)
			throw new RuntimeException("Something bad (overflow?) occurred in class ErrorReporter's variable");
		return numberOfErrors;
	}
	public void reportError(String errorMessage) {
		numberOfErrors++;
		System.out.println(errorMessage);
	}
}
