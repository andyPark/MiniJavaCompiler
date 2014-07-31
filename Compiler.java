/*
 * Written by: Andrew J. Park
 */

package miniJava;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.CodeGenerator.Encoder;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SourceFile;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.Checker;
import miniJava.ContextualAnalyzer.ErrorReporter;
import miniJava.ContextualAnalyzer.IdentificationTable;

//Driver class for the whole application

public class Compiler {
	
	public static void main(String[] args) {
		compile(args);
	}
	
	public static void compile(String[] args) {
		String src = args[0];
		
		Parser p = new Parser(new Scanner(new SourceFile(src)));
		Package pack = p.parse();
		ErrorReporter e = new ErrorReporter();
		Checker c = new Checker(e);
		IdentificationTable idTable = new IdentificationTable(e);
		try {
			pack.visit(c, idTable);
		}
		catch (Exception e1) {
			if (c.reporter.numberOfErrors() > 0) {
				System.out.println("Error count: "+c.reporter.numberOfErrors());
				System.exit(ExitCode.FAILURE);
			}
			System.exit(ExitCode.FAILURE);
		}
		
		if (c.reporter.numberOfErrors() > 0)
			System.exit(ExitCode.FAILURE);
		
		Encoder theEncoder = new Encoder();
		try {
			pack.visit(theEncoder, null);
		} catch (Exception anException) {
			System.exit(ExitCode.FAILURE);
		}
		
		String output = src.replaceAll(".mjava", "");
		output = output.replaceAll(".java", "");
		ObjectFile obFile = new ObjectFile(output + ".mJAM");
		obFile.write();
		System.exit(0);
	}	
}

//public class Compiler {
//	
//	public static void main(String[] args) {
//		compile(args);
//	}
//	
//	public static void compile(String[] args) {
//		String src = "/Users/andrew/Dropbox/Eclipse Workspaces/UNC Spring 2014/comp520pa4/src/pa4tests/pass423.java";
//		
//		Parser p = new Parser(new Scanner(new SourceFile(src)));
//		Package pack = p.parse();
//		ErrorReporter e = new ErrorReporter();
//		Checker c = new Checker(e);
//		IdentificationTable idTable = new IdentificationTable(e);
//		try {
//			pack.visit(c, idTable);
//		}
//		catch (Exception e1) {
//			if (c.reporter.numberOfErrors() > 0) {
//				System.out.println("Error count: "+c.reporter.numberOfErrors());
//				System.exit(ExitCode.FAILURE);
//			}
//			System.exit(ExitCode.FAILURE);
//		}
//		
//		ASTDisplay a = new ASTDisplay();
//		a.showTree(pack);
//		
//		if (c.reporter.numberOfErrors() > 0)
//			System.exit(ExitCode.FAILURE);
//		
//		Encoder theEncoder = new Encoder();
////		try {
//			pack.visit(theEncoder, null);
////		} catch (Exception anException) {
////			System.exit(ExitCode.FAILURE);
////		}
//		
//		String output = src.replaceAll(".mjava", "");
//		output = output.replaceAll(".java", "");
//		String objectCodeFileName = output + ".mJAM";
//		String asmCodeFileName = output + ".asm";
//		ObjectFile obFile = new ObjectFile(objectCodeFileName);
//		obFile.write();
//		
//		Disassembler d = new Disassembler(objectCodeFileName);
//		d.disassemble();
//		
//		Interpreter.debug(objectCodeFileName, asmCodeFileName);
//		
//		System.exit(0);
//	}	
//}