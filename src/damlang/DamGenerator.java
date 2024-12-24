package damlang;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import damlang.Expr.Assign;
import damlang.Expr.Binary;
import damlang.Expr.Grouping;
import damlang.Expr.Literal;
import damlang.Expr.Logical;
import damlang.Expr.Unary;
import damlang.Expr.Variable;
import damlang.Stmt.Block;
import damlang.Stmt.Break;
import damlang.Stmt.Expression;
import damlang.Stmt.If;
import damlang.Stmt.Let;
import damlang.Stmt.Print;
import damlang.Stmt.Read;
import damlang.Stmt.While;

public class DamGenerator implements Expr.Visitor<String>, Stmt.Visitor<String> {
	private DamEnvironment env = new DamEnvironment();
	private List<Stmt> statements;
	private List<String> ins = new ArrayList<>();
	
	private Map<Expr, String> t = new HashMap<>();
	private Map<String, String> javat = new HashMap<>();
	private Map<Object, String> conditionLabels = new HashMap<>();
	private int labelCounter = 0;
	
	private PrintWriter writer;
	
	private String jasminFilePath;

	public DamGenerator(List<Stmt> statements) {
		this.statements = statements;
		javat.put("double", "F");
		javat.put("str", "Ljava/lang/String;");
		javat.put("bool", "Z");
		
		env.define("args", "str"); // NOTE: don't have arrays yet. Just pretend it's a string for now
	}

	/**
	 * Creates textual bytecode file and then uses Jasmin to
	 * generate the actual Java classfile.  It also removes the
	 * Jasmin file before exiting.
	 * @param absoluteStem
	 */
	public void generate(String absoluteStem) {
		// Visit the statements (and expressions) to generate the instructions in 'ins'.
		for (Stmt s : statements) {
			s.accept(this);
		}

		// Write the file, including 'ins'.
		writeClassfile(absoluteStem);
	}
	
	private void writeClassfile(String absoluteStem) {		
		jasminFilePath = absoluteStem + ".j";
		
		String javaClassName = absoluteStem;
		int slash = absoluteStem.lastIndexOf(File.separator);
		if (slash >= 0) {
			javaClassName = absoluteStem.substring(slash + 1);
		}
		
		try {
			writer = new PrintWriter(jasminFilePath);
			writeHeader(javaClassName);
			writeCtor();
			writeMainStart();
			for (String inst : ins) {
				System.out.println(inst);
				writer.println("\t" + inst);
			}
			writeMainEnd();
		} catch (IOException ioe) {
			DamCompiler.error("Error generating bytecode. " + ioe.getMessage());
		} finally {
			writer.close();
		}
		
		// Run jasmin on our .j file to create the .class file.
		jasmin.Main jasminMain = new jasmin.Main();
		jasminMain.run(new String[]{jasminFilePath});
		
		// Remove the .j and then move the .class to the same location as the .dam file.
		//new File(jasminFilePath).delete();
		Path sourceClass = Paths.get(javaClassName + ".class");
		Path targetClass = Paths.get(absoluteStem + ".class");
		try {
			Files.move(sourceClass, targetClass, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
            DamCompiler.error("Fatal error: " + e.getMessage());
        } 
	}
	
	private void writeHeader(String javaClassName) {
		writer.println(".class public " + javaClassName + "\n"
				+ ".super java/lang/Object");
	}
	
	private void writeCtor() {
		writer.println(".method public <init>()V\n"
				+ "    aload_0\n"
				+ "    invokenonvirtual java/lang/Object/<init>()V\n"
				+ "    return\n"
				+ ".end method");
	}
	
	// FIXME We will need to know how many local variables we have
	// and approximately how large the stack should be.
	private void writeMainStart() {
		writer.println(".method public static main([Ljava/lang/String;)V\n"
				+ "    .limit stack " + (ins.size()/2 + 1) + "\n"
				+ "    .limit locals " + (env.numVars() + 1));
	}

	private void writeMainEnd() {
		writer.println("    return\n"
				+ ".end method");
	}

	@Override
	public String visitBlockStmt(Block stmt) {
		for (Stmt st : stmt.statements) {
			st.accept(this);
		}
		return null;
	}

	@Override
	public String visitExpressionStmt(Expression stmt) {
		stmt.expression.accept(this);
		return null;
	}

	@Override
	public String visitIfStmt(If stmt) {
		String thenLabel = "THEN" + labelCounter;
		String elseLabel = "ELSE" + labelCounter;
		String endLabel = "END" + labelCounter++;
		
		if (stmt.elseBranch == null) {
			conditionLabels.put(stmt.condition, endLabel);
		} else {
			conditionLabels.put(stmt.condition, elseLabel);
		}
		String conditionType = stmt.condition.accept(this);
		determineTruthy(conditionType, stmt.elseBranch, elseLabel, endLabel, stmt.condition);
		ins.add(thenLabel + ":");
		stmt.thenBranch.accept(this);

		if (stmt.elseBranch != null) {
			ins.add("goto " + endLabel);
			ins.add(elseLabel + ":");
			stmt.elseBranch.accept(this);
		}
		
		ins.add(endLabel + ":");
		return null;
	}

	@Override
	public String visitPrintStmt(Print stmt) {
		ins.add("getstatic java/lang/System/out Ljava/io/PrintStream;\n");
		stmt.expression.accept(this);
		
		String exprType = t.get(stmt.expression);
		String javaType = javat.get(exprType);
		ins.add("invokevirtual java/io/PrintStream/println(" + javaType + ")V");
		return null;
	}

	@Override
	public String visitReadStmt(Read stmt) {
		int varIndex = env.getIndex(stmt.name);
		ins.add("new java/util/Scanner");
		ins.add("dup");
		ins.add("getstatic java/lang/System/in Ljava/io/InputStream;");
		ins.add("invokespecial java/util/Scanner/<init>(Ljava/io/InputStream;)V");
		ins.add("invokevirtual java/util/Scanner/nextLine()Ljava/lang/String;");
		env.assign(stmt.name, "str");
		ins.add("astore " + varIndex);
		return null;
	}

	@Override
	public String visitLetStmt(Let stmt) {
		if (stmt.initializer == null) {
			Literal defaultValue = new Literal("");
			stmt.initializer = defaultValue;
		}
		stmt.initializer.accept(this);
		String rhsType = t.get(stmt.initializer);
		env.define(stmt.name.lexeme, rhsType);
		int varIndex = env.getIndex(stmt.name);
		
		if (rhsType.equals("double")) {
			ins.add("fstore " + varIndex);
		} else if (rhsType.equals("str")) {
			ins.add("astore " + varIndex);
		}else if (rhsType.equals("bool")) {
			ins.add("istore " + varIndex);
		}
		return null;
	}

	@Override
	public String visitWhileStmt(While stmt) {
		String loopLabel = "THEN" + labelCounter;
		String endLabel = "END" + labelCounter++;
		conditionLabels.put(stmt.condition, loopLabel);
		conditionLabels.put(stmt.condition, endLabel);
		ins.add(loopLabel + ":");
		String conditionType = stmt.condition.accept(this);
		determineTruthy(conditionType, endLabel, stmt.condition);
		
		stmt.body.accept(this);
		ins.add("goto " + loopLabel);
		ins.add(endLabel + ":");
		
		return null;
	}
	
	@Override
	public String visitBreakStmt(Break stmt) {
		return null;
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		expr.left.accept(this);
		expr.right.accept(this);
		String ltype = t.get(expr.left);
		String rtype = t.get(expr.right);
		if (! ltype.equals(rtype)) {
			DamCompiler.error("Type mismatch on line " + expr.operator.line
					+ ". Cannot apply " + expr.operator.lexeme + " to '"
					+ ltype + "' and '" + rtype + "'.");
		}
		
		switch (expr.operator.type) {
		case TokenType.PLUS:
		case TokenType.MINUS:
		case TokenType.STAR:
		case TokenType.SLASH:
			if (ltype.equals("double")) {
				if (expr.operator.type == TokenType.PLUS) 		ins.add("fadd");
				else if (expr.operator.type == TokenType.MINUS) ins.add("fsub");
				else if (expr.operator.type == TokenType.STAR)	ins.add("fmul");
				else if (expr.operator.type == TokenType.SLASH) ins.add("fdiv");
			} else if (ltype.equals("str")) {
				if (expr.operator.type == TokenType.PLUS) {
					ins.add("invokevirtual java/lang/String/concat(" +
							"Ljava/lang/String;)Ljava/lang/String;");
				} else {
					DamCompiler.error("Cannot apply " + expr.operator.lexeme + " to str.");
				}
			}
			
			t.put(expr, rtype);
			break;
			
		case TokenType.BANG_EQUAL:
		case TokenType.EQUAL_EQUAL:
		case TokenType.GREATER:
		case TokenType.GREATER_EQUAL:
		case TokenType.LESS:
		case TokenType.LESS_EQUAL:
			t.put(expr, "bool");
			
			if (ltype.equals("double")) {
				ins.add("fcmpl");
				String jumpLabel = conditionLabels.get(expr);
				if (expr.operator.type == TokenType.BANG_EQUAL)		ins.add("ifeq " + jumpLabel);
				if (expr.operator.type == TokenType.EQUAL_EQUAL)	ins.add("ifne " + jumpLabel);
				if (expr.operator.type == TokenType.GREATER)		ins.add("ifle " + jumpLabel);
				if (expr.operator.type == TokenType.GREATER_EQUAL)	ins.add("iflt " + jumpLabel);
				if (expr.operator.type == TokenType.LESS)			ins.add("ifge " + jumpLabel);
				if (expr.operator.type == TokenType.LESS_EQUAL)		ins.add("ifgt " + jumpLabel);
			} else if (ltype.equals("str")) {
				ins.add("invokevirtual java/lang/String/compareTo(Ljava/lang/String;)I");
				String jumpLabel = conditionLabels.get(expr);
				if (expr.operator.type == TokenType.BANG_EQUAL)		ins.add("ifeq " + jumpLabel);
				if (expr.operator.type == TokenType.EQUAL_EQUAL)	ins.add("ifne " + jumpLabel);
				if (expr.operator.type == TokenType.GREATER)		ins.add("ifle " + jumpLabel);
				if (expr.operator.type == TokenType.GREATER_EQUAL)	ins.add("iflt " + jumpLabel);
				if (expr.operator.type == TokenType.LESS)			ins.add("ifge " + jumpLabel);
				if (expr.operator.type == TokenType.LESS_EQUAL)		ins.add("ifgt " + jumpLabel);
			} else if (ltype.equals("bool")) {
				String jumpLabel = conditionLabels.get(expr);
				if (expr.operator.type == TokenType.BANG_EQUAL)		ins.add("if_icmpeq " + jumpLabel);
				if (expr.operator.type == TokenType.EQUAL_EQUAL)	ins.add("if_icmpne " + jumpLabel);
				if (expr.operator.type == TokenType.GREATER)		ins.add("if_icmple " + jumpLabel);
				if (expr.operator.type == TokenType.GREATER_EQUAL)	ins.add("if_icmplt " + jumpLabel);
				if (expr.operator.type == TokenType.LESS)			ins.add("if_icmpge " + jumpLabel);
				if (expr.operator.type == TokenType.LESS_EQUAL)		ins.add("if_icmpgt " + jumpLabel);
			}
			
		default:
		}
		
		return t.get(expr);
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		System.out.println("GROUPING");
		expr.expression.accept(this);
		t.put(expr, t.get(expr.expression));
		return null;
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		if (expr.value instanceof Double) {
			t.put(expr,  "double");
			ins.add("ldc " + expr.value);
		} else if (expr.value instanceof String) {
			t.put(expr, "str");
			ins.add("ldc \"" + expr.value + "\"");
		} else if (expr.value instanceof Boolean) {
			t.put(expr, "bool");
			if (expr.value.equals(true))	ins.add("iconst_1");
			if (expr.value.equals(false))	ins.add("iconst_0");
		}
		return t.get(expr);
	}

	@Override
	public String visitLogicalExpr(Logical expr) {
		String jumpLabel = conditionLabels.get(expr);
		switch (expr.operator.type) {
		case TokenType.AND:
		case TokenType.OR:
			t.put(expr, "bool");
			conditionLabels.put(expr.right, jumpLabel);	
			
			if (expr.operator.type == TokenType.AND) {
				conditionLabels.put(expr.left, jumpLabel);	
				String ltype = expr.left.accept(this);
				determineTruthy(ltype, jumpLabel, expr.left);
				String rtype = expr.right.accept(this);
				determineTruthy(rtype, jumpLabel, expr.right);
			} else if (expr.operator.type == TokenType.OR) {
				String otherCondition = "OTHERCONDITION" + labelCounter++;
				String afterCondition = "AFTER" + jumpLabel.substring(jumpLabel.length()-1);
				conditionLabels.put(expr.left, otherCondition);
				String ltype = expr.left.accept(this);
				determineTruthy(ltype, otherCondition, expr.left);
				ins.add("goto " + afterCondition);
				ins.add(otherCondition + ":");
				String rtype = expr.right.accept(this);
				determineTruthy(rtype, jumpLabel, expr.right);
				ins.add(afterCondition + ":");
			}
			
		default:
		}
		return null;
	}

	@Override
	public String visitVariableExpr(Variable expr) {
		String type = env.get(expr.name);
		t.put(expr, type);
		
		if (type.equals("double")) {
			ins.add("fload " + env.getIndex(expr.name));
		} else if (type.equals("str")) {
			ins.add("aload " + env.getIndex(expr.name));
		} else if (type.equals("bool")) {
			ins.add("iload " + env.getIndex(expr.name));
		}
		return t.get(expr);
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		expr.right.accept(this);
		String rtype = t.get(expr.right);
		
		switch (expr.operator.type) {
		case TokenType.BANG:
			if (rtype != "bool") {
				DamCompiler.error("Cannot apply " + expr.operator.lexeme +" operator to " + rtype + ".");
			}
			t.put(expr, "bool");
			String thenLabel = "THEN" + labelCounter;
			String endLabel = "END" + labelCounter++;
			
			ins.add("iconst_0");
			ins.add("if_icmpeq " + thenLabel);
			ins.add("iconst_0");
			ins.add("goto " + endLabel);
			ins.add(thenLabel + ":");
			ins.add("iconst_1");
			ins.add(endLabel + ":");
			break;

		case TokenType.MINUS:
			if (rtype != "double") {
				DamCompiler.error("Cannot apply " + expr.operator.lexeme +" operator to " + rtype + ".");
			}
			t.put(expr, "double");
			
			ins.add("ldc -1.0");
			ins.add("fmul");
			break;
		case TokenType.BOOL:
			t.put(expr, "bool");
			String truthyLabel = "THEN" + labelCounter;
			String afterLabel = "END" + labelCounter++;
			determineTruthy(rtype, truthyLabel, expr.right);
			ins.add("iconst_1");
			ins.add("goto " + afterLabel);
			ins.add(truthyLabel + ":");
			ins.add("iconst_0");
			ins.add(afterLabel + ":");
			break;
		case TokenType.STR:
			t.put(expr, "str");
			if (rtype == "bool") 	ins.add("invokestatic java/lang/Boolean/toString(Z)Ljava/lang/String;");
			if (rtype == "double")	ins.add("invokestatic java/lang/Float/toString(F)Ljava/lang/String;");
			break;
		case TokenType.DOUBLE:
			t.put(expr, "double");
			String trueLabel = "THEN" + labelCounter;
			String aftLabel = "END" + labelCounter++;
			if (rtype == "str")		ins.add("invokestatic java/lang/Float/parseFloat(Ljava/lang/String;)F");
			if (rtype == "bool") {
				ins.add("iconst_0");
				ins.add("if_icmpne " + trueLabel);
				ins.add("fconst_0");
				ins.add("goto " + aftLabel);
				ins.add(trueLabel + ":");
				ins.add("fconst_1");
				ins.add(aftLabel + ":");
			}
			break;
		
		default:
		}
		return t.get(expr);
	}

	@Override
	public String visitAssignExpr(Assign expr) {
		// This line checks if the variable exists
		env.get(expr.name);
		// Reassign it if it gets past
		int varIndex = env.getIndex(expr.name);
		expr.right.accept(this);
		String rhsType = t.get(expr.right);
		env.assign(expr.name, rhsType);
		
		if (rhsType.equals("double")) {
			ins.add("fstore " + varIndex);
		} else if (rhsType.equals("str")) {
			ins.add("astore " + varIndex);
		}else if (rhsType.equals("bool")) {
			ins.add("istore " + varIndex);
		}
		return null;
	}
	
	private void determineTruthy(String conditionType, Stmt elseBranch, String elseLabel, String endLabel, Expr condition) {
		if (conditionType == "str") {
			ins.add("ldc \"\"");
			ins.add("invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z");
			if (elseBranch != null) {
				ins.add("ifne " + elseLabel);
			} else {
				ins.add("ifne " + endLabel);
			}
		} else if (conditionType == "double") {
			ins.add("fconst_0");
			ins.add("fcmpg");
			if (elseBranch != null) {
				ins.add("ifeq " + elseLabel);
			} else {
				ins.add("ifeq " + endLabel);
			}
		} else if (conditionType == "bool" && !(condition instanceof Binary)) {
			ins.add("iconst_0");
			if (elseBranch != null) {
				ins.add("if_icmpeq " + elseLabel);
			} else {
				ins.add("if_icmpeq " + endLabel);
			}
		}
	}
	private void determineTruthy(String conditionType, String endLabel, Expr condition) {
		determineTruthy(conditionType, null, null, endLabel, condition);
	}

}
