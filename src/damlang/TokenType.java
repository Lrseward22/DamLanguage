package damlang;

public enum TokenType {
	// Single-character tokens.
	LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
	COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

	// Boolean-related tokens.
	BANG, BANG_EQUAL,
	EQUAL, EQUAL_EQUAL,
	GREATER, GREATER_EQUAL,
	LESS, LESS_EQUAL,

	// Literals.  We do not differentiate between integers and floating point.
	// All numbers are floating point in Dam.
	IDENTIFIER, STRING, NUMBER,

	// Keywords.
	AND, ELSE, FALSE, FOR, IF, LET, NULL, OR,
	PRINT, READ, RETURN, TO, TRUE, WHILE, BREAK,
	BOOL, STR, DOUBLE,

	EOF
}