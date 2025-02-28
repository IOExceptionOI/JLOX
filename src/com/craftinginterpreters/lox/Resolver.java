package com.craftinginterpreters.lox;



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;



public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>{
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    
    private enum FunctionType {
        NONE,
        FUNCTION
    }

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    // Effective Stmts

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType type){
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }

        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    // Stmts for traverse

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt){
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if(stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) resolve(stmt.value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);

        return null;
    }
    

    
    // Effective Exprs

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        // Stack.peek() ==> the used expr is the currently being initialized variable
        if(!scopes.isEmpty() &&
            scopes.peek().get(expr.name.lexeme) == Boolean.FALSE){
                Lox.error(expr.name, "Can't read local variable in its own initializer.");
            }

        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    // Exprs for traverse
    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    
    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }
    
    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }
    
    // Helper Functions

    private void resolveLocal(Expr expr, Token name){
        for (int i = scopes.size() - 1; i >= 0; i --) {
            // from the innermost scope and work outwards
            if(scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    // start point of the whole program
    void resolve(List<Stmt> statements){
        for(Stmt statement : statements){
            resolve(statement);
        }
    }

    private void declare(Token name) {
        // global scope doesn't need to resolve
        if(scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();
        // local scope doesn't allow multiple varDecl
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name,
                "Already variable with this name inthis scope.");
        }
        // bind the name to false to mark it as not initialiezd
        scope.put(name.lexeme, false); 
    }

    private void define(Token name) {
        if(scopes.isEmpty()) return;

        // bind the name to true to mark it as fully initialized
        scopes.peek().put(name.lexeme, true);
    }

    private void resolve(Stmt stmt){
        stmt.accept(this);
    }
    private void resolve(Expr expr){
        expr.accept(this);
    }

    private void beginScope(){
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope(){
        scopes.pop();
    }
}
