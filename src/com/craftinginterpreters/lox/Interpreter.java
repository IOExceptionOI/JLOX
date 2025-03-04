package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Interpreter implements Expr.Visitor<Object>,
                                    Stmt.Visitor<Void>{

    final Environment globals = new Environment();
    Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();


    Interpreter() {
        globals.define("clock", new LoxCallable(){
            @Override
            public int arity(){return 0;};

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments){
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString(){return "{native fn}";}
        });
    }

   

    
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        // spilt the declaration and definition for allowing reference to the class inside its own method
        environment.define(stmt.name.lexeme, null);

        // using a local map to store the methods
        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            // the current environment where the class is defined is the closure ot the methods
            // For methods, we check the name. If it equals with "init", we set the isInitializer to true to make it always return 'this'
            LoxFunction function = new LoxFunction(method, environment,
                method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt){
        // the default return value is nil
        Object value = null;
        if(stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt){
        // the visit of Function(Decl) translate it to LoxFunction
        // For actual function declarations, isInitializer is always false.
        LoxFunction function = new LoxFunction(stmt, environment, false);
        // and bind the Function(Decl) name to the invocable Loxfunction
        environment.define(stmt.name.lexeme, function);
        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt){
        while(isTruthy(evaluate(stmt.condition))){
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt){
        if(isTruthy(evaluate(stmt.condition))){
            execute(stmt.thenBranch);
        }else if(stmt.elseBranch != null){
            execute(stmt.elseBranch);
        }

        return null;
    }
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


    //! Exprs
    
    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Obly instance have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instance have properties.");
    }

    @Override
    public Object visitCallExpr(Expr.Call expr){
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for(Expr argument : expr.arguments){
            arguments.add(evaluate(argument));
        }

        if(!(callee instanceof LoxCallable)){
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;
        if(arguments.size() != function.arity()){
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size());
        }
        return function.call(this, arguments);
    }
    @Override
    public Object visitLogicalExpr(Expr.Logical expr){
        Object left = evaluate(expr.left);

        // short-circuit
        if(expr.operator.type == TokenType.OR){
            if(isTruthy(left)) return left;
        }else{
            if(!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }
    @Override
    public Object visitAssignExpr(Expr.Assign expr){
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        
        return value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr){
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr){
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else{
            return globals.get(name);
        }
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

     // Resolver Pass
     void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }
}
