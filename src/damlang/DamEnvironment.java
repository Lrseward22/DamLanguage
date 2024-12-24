package damlang;

import java.util.HashMap;
import java.util.Map;

public class DamEnvironment {
	
	private Map<String, String> envMap = new HashMap<>();
	private Map<String, Integer> indexMap = new HashMap<>();
	private int indexCount = 0;
	
	public void define(String name, String type) {
		envMap.put(name, type);
		indexMap.put(name, indexCount++);
	}
	
	public void assign(Token name, String type) {
		if (envMap.containsKey(name.lexeme)) {
			envMap.put(name.lexeme, type);
		} else {
			DamCompiler.error("Undefined variable " + name.lexeme);
		}
	}
	
	public String get(Token name) {
		if (envMap.containsKey(name.lexeme)) {
			return envMap.get(name.lexeme);
		} else {
			DamCompiler.error("Undefined variable " + name.lexeme);
			return null;
		}	
	}
	
	public int getIndex(String name) {
		return indexMap.get(name);
	}
		
	public int getIndex(Token name) {
		return getIndex(name.lexeme);
	}
	
	public int numVars() {
		return indexCount;
	}


}
