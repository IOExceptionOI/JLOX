package com.craftinginterpreters.lox;

import java.util.List;


public class Interpreter implements Expr.Visitor<Object>,
                                    Stmt.Visitor<Void>{

    private Environment environment = new Environment();

    @Override
    public Void visitBlockStmt(Stmt.Block stmt){
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment){
        // record the enclosing environment
        Environment previous = this.environment;
        try{
            // switch to the inner environment
            this.environment = environment;

            for(Stmt statement : statements){
                execute(statement);
            }
        // even though an error is thrown, it return to the enclosing environment
        // which is very useful in REPL mode 
        }finally{
            this.environment = previous;
        }
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt){
        Object value = null;
        if(stmt.initializer != null){
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr){
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr){
        return environment.get(expr.name);
    }
    void interpret(List<Stmt> statements){
        try {
           for(Stmt stmt : statements){
                execute(stmt);
           }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private String stringfy(Object object){
        if(object == null) return "nil";

        if(object instanceof Double){
            String text = object.toString();
            if(text.endsWith(".0")){
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt){
        evaluate(stmt.expression);
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt){
        Object value = evaluate(stmt.expression);
        System.out.println(stringfy(value));
        return null;
    }
    private void execute(Stmt stmt){
        stmt.accept(this);
    }
    @Override
    public Object visitLiteralExpr(Expr.Literal expr){
        return expr.value;
    }
    
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr){
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr){
        return expr.accept(this);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr){
        Object right_expr = evaluate(expr.right);

        switch(expr.operator.type){
            case MINUS:
            checkNumberOperand(expr.operator, right_expr);
                return - (double) right_expr;
            case BANG:
                return !isTruthy(right_expr);
        }

        // Unreachable
        return null;
    }
    private void checkNumberOperand(Token operator, Object operand){
        if(operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private boolean isTruthy(Object object){
        if(object == null) return false;
        if(object instanceof Boolean) return (boolean)object;
        return true;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr){
        // left-to-right order (which is important when evaluation has side-effect)
        Object left_expr = evaluate(expr.left);
        Object right_expr = evaluate(expr.right);

        switch(expr.operator.type){
            // arithmetic operators
            case MINUS:
                checkNumberOperands(expr.operator, left_expr, right_expr);
                return (double)left_expr - (double)right_expr;
            case SLASH:
                checkNumberOperands(expr.operator, left_expr, right_expr);
                return (double)left_expr / (double)right_expr;
            case STAR:
                checkNumberOperands(expr.operator, left_expr, right_expr);
                return (double)left_expr * (double)right_expr;
            case PLUS:

                if(left_expr instanceof Double && right_expr instanceof Double){
                    return (double)left_expr + (double)right_expr;
                }

                if(left_expr instanceof String && right_expr instanceof String){
                    return (String)left_expr + (String)right_expr;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");

            // comparison operators
            case GREATER:
                checkNumberOperands(expr.operator, left_expr, right_expr);
                return (double)left_expr > (double)right_expr;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left_expr, right_expr); 
                return (double)left_expr >= (double)right_expr;
            case LESS:
                checkNumberOperands(expr.operator, left_expr, right_expr);
                return (double)left_expr < (double)right_expr;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left_expr, right_expr);
                return (double)left_expr <= (double)right_expr;

            // no conversion ==> no type check
            case BANG_EQUAL:
                return !isEqual(left_expr, right_expr);
            case EQUAL_EQUAL:
                return isEqual(left_expr, right_expr);
        }

        // Unreachabale
        return null;
    }
    private void checkNumberOperands(Token token, Object left_expr, Object right_expr){
        if(left_expr instanceof Double && right_expr instanceof Double) return;
        throw new RuntimeError(token, "Operands must be numbers.");
    }

    private boolean isEqual(Object a, Object b){
        if(a == null && b == null) return true;
        if(a == null) return false;
        
        return a.equals(b);
    }
}
