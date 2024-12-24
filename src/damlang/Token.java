package damlang;

public class Token {
	public TokenType type;
	public String lexeme;
	public Object literal;
	public int line;

	Token(TokenType type, String lexeme, Object literal, int line) {
		this.type = type;
		this.lexeme = lexeme;
		this.literal = literal;
		this.line = line;
	}

	public String toString() {
		return type + " " + lexeme + " " + literal + " @ " + line;
	}
}
