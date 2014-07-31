package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.Declaration;

public class IdEntry {
	protected String id;
	protected Declaration attr;
	protected int level;
	protected IdEntry previous;
	protected boolean initialized;

	IdEntry(String id, Declaration attr, int level, IdEntry previous, boolean initialization) {
		this.id = id;
		this.attr = attr;
		this.level = level;
		this.previous = previous;
		initialized = initialization;
	}
	
	public void initialize() {
		initialized = true;
	}
	public void uninitialize() {
		initialized = false;
	}
}
