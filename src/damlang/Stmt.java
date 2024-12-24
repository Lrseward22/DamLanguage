package damlang;

import java.util.List;

abstract class Stmt {
    interface Visitor<T> {
        T visitBlockStmt(Block stmt);
        T visitExpressionStmt(Expression stmt);
        T visitIfStmt(If stmt);
        T visitPrintStmt(Print stmt);
        T visitReadStmt(Read stmt);
        T visitLetStmt(Let stmt);
        T visitWhileStmt(While stmt);
        T visitBreakStmt(Break stmt);
    }

    abstract <T> T accept(Visitor<T> visitor);


    static class Block extends Stmt {
        Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitBlockStmt(this);
        }

        List<Stmt> statements;
    }

    static class Expression extends Stmt {
        Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitExpressionStmt(this);
        }

        Expr expression;
    }

    static class If extends Stmt {
        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitIfStmt(this);
        }

        Expr condition;
        Stmt thenBranch;
        Stmt elseBranch;
    }

    static class Print extends Stmt {
        Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitPrintStmt(this);
        }

        Expr expression;
    }

    static class Read extends Stmt {
        Read(Token name) {
            this.name = name;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitReadStmt(this);
        }

        Token name;
    }

    static class Let extends Stmt {
        Let(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitLetStmt(this);
        }

        Token name;
        Expr initializer;
    }

    static class While extends Stmt {
        While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        <T> T accept(Visitor<T> visitor) {
            return visitor.visitWhileStmt(this);
        }

        Expr condition;
        Stmt body;
    }
    
    static class Break extends Stmt {
    	Break() {
    	}
    	
    	@Override
    	<T> T accept(Visitor<T> visitor) {
    		return visitor.visitBreakStmt(this);
    	}
    }
}