//Written by:
//Andrew J. Park
package miniJava.ContextualAnalyzer;

import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.Declaration;


public class IdentificationTable {
	private int level;
	private IdEntry currentEntry;
	
	private HashMap<String, IdEntry> scopeTable;
	//static or instance field variable table for individual classes
	//method names will also be stored in fieldTable
	private HashMap<String, IdEntry> fieldTable;
	//Separate classTable since variable names can also be the name of a class
	//this prevents duplicates in the scope table.
	private HashMap<String, ClassDecl> classTable; //consider changing this to be static if I have time.
	private ErrorReporter theErrorReporter;
	
	public IdentificationTable(ErrorReporter e) {
		level = 0;
		currentEntry = null;
		classTable = new HashMap<String, ClassDecl>();
		scopeTable = new HashMap<String, IdEntry>();
		fieldTable = new HashMap<String, IdEntry>();
		theErrorReporter = e;
	}
	
	public void openScope() {
		level++;
	}
	
	public void closeScope() {
		while (currentEntry.level == this.level) {
			scopeTable.remove(currentEntry.id);
			currentEntry = currentEntry.previous;
		}
		if (level > 0)
			level--;
		else
			throw new RuntimeException("Called closeScope when scope is level 0");
	}
	
	//Done with semantic analysis of class. Remove all the field vars so that we can
	//have a clean slate for the next class.
	public void closeClassScope() {
		if (this.level != 0)
			throw new RuntimeException("Class Scope should not be closed until method scopes have been closed.");
		fieldTable = new HashMap<String, IdEntry>();
		currentEntry = null;
	}

	public void initialize(String id) {
		scopeTable.get(id).initialized = true;
	}
	
	public void enter(String id, Declaration attr, boolean initialization){
		HashMap<String, IdEntry> scope = (level > 0) ? scopeTable : fieldTable;
		attr.isDuplicate = scope.containsKey(id); //is duplicate name
	
		currentEntry = new IdEntry(id, attr, level, currentEntry, initialization);
		
	
		if (!attr.isDuplicate) scope.put(id, currentEntry);
		else
			theErrorReporter.reportError(duplicateMessage(attr.posn.start, id));
	}
	
	public void enter(String id, Declaration attr) {

		HashMap<String, IdEntry> scope = (level > 0) ? scopeTable : fieldTable;
		attr.isDuplicate = scope.containsKey(id); //is duplicate name
	
		currentEntry = new IdEntry(id, attr, level, currentEntry, false);
	
		if (!attr.isDuplicate) scope.put(id, currentEntry);
		else
			theErrorReporter.reportError(duplicateMessage(attr.posn.start, id));
	}
	
	public void enterClass(String id, ClassDecl attr) {
		attr.isDuplicate = classTable.containsKey(id);
		
		if (!attr.isDuplicate) classTable.put(id,  attr);
		else
			theErrorReporter.reportError(String.format(
					"*** Line %d: duplicate class named %s\n",
					attr.posn.start, id));
	}
	
	//put these in ErrorReporter
	private String duplicateMessage(int startPos, String id) {
		return String.format(
				"*** Line %d: variable %s is already defined in current scope.\n",
				startPos, id);
	}
	private String uninitializedMessage(int startPos, String id) {
		return String.format(
				"*** Line %d: variable %s was not initialized",
				startPos, id);
	}
	
	public boolean classDefined(String id) {
		return classTable.containsKey(id);
	}
	
	public Declaration retrieve (String id) {
		if (scopeTable.containsKey(id)) {
			IdEntry retrievedEntry = scopeTable.get(id);
			if (!retrievedEntry.initialized) {
				theErrorReporter.reportError(uninitializedMessage(retrievedEntry.attr.posn.start, id));
			}
			return retrievedEntry.attr;
		}
		else if (fieldTable.containsKey(id))
			return fieldTable.get(id).attr;
		else
			return null;
	}

	public Declaration retrieveThis(String id) {
		if (fieldTable.containsKey(id))
			return fieldTable.get(id).attr;
		else
			return null;
	}
	public ClassDecl retrieveClass(String id) {
		if (classDefined(id))
			return classTable.get(id);
		else
			return null;
	}
}
