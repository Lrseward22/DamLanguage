package damlang;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * The <code>DamCompiler</code> directs the <code>DamLexer</code>, 
 * <code>DamParser</code>, and <code>DamCodeGen</code> to take
 * a Dam source file and create a class file that can be run
 * using the Java Virtual Machine.
 * 
 * It performs a bit of command-line parsing before invoking the 
 * compiler pipeline mentioned above.
 */
public class DamCompiler {
	/**
	 * The program entry point for the compiler.  It does some 
	 * command-line checking before calling <code>{@link #compile(File)}</code>.
	 * @param args
	 */
	public static void main(String[] args) {
		// Compile one .dam file.
		// Eventually think about what happens when we compile multiple.
		
		if (args.length != 1) {
			System.err.println("java damlang.DamCompiler <dam sourcefile>");
			System.exit(1);
		} else if (Files.notExists(Paths.get(args[0]))) {
			System.err.println("Cannot find file " + args[0]);
			System.exit(2);
		} else if (! args[0].endsWith(".dam")) {
			System.err.println(args[0] + " does not have a .dam extension.");
			System.exit(3);
		}
		
		compile(new File(args[0]));
	}
	
	private static String getAbsoluteStem(File f) {
		String filename = f.getAbsolutePath();
		return filename.substring(0, filename.indexOf(".dam"));
	}
	
	/**
	 * Given a Dam source <code>File</code> object, this runs the compiler
	 * pipeline to produce a Java class file in the same directory as
	 * the Dam source file. 
	 * @param f the Dam source file
	 */
	protected static void compile(File f) {
		System.out.println("Compiling "  + f.getAbsolutePath());
		List<Token> tokens = new DamLexer(f).lex();
		List<Stmt> statements = new DamParser(tokens).parse();
		/*
		for (Stmt s : statements) {
		int varIndex = env.getIndex(stmt.name);
		String varType = env.get(stmt.name);
			System.out.println(s);
		}
		*/
		new DamGenerator(statements).generate(getAbsoluteStem(f));
	}

	public static void error(String msg) {
		System.err.println("Error: " + msg);
		System.exit(1);		
	}

	public static void error(int line, String msg) {
		System.err.println("Line " + line + ": " + msg);
		System.exit(1);		
	}

	public static void error(Token token, String msg) {
		System.err.println("On Token " + token + ": " + msg);
		System.exit(1);		
	}

}
