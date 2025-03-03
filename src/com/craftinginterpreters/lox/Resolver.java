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
        FUNCTION,
        METHOD
    }

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    //! Effective Stmts

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
        // before the initialization, the var isn't readily initialiezd; 
        // so we just declare it, and if the initializer use itself(undefined), it will raise error
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    //! Stmts for traverse

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        // declare and define the name of the class for forward reference inside the class body
        declare(stmt.name);
        define(stmt.name);

        // Before we step in and start resolving the method bodies, we push a new scope 
        // and define “this” in it as if it were a variable.
        beginScope();
        scopes.peek().put("this", true);

        // resolve each method of the class
        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            resolveFunction(method, declaration);
        }

        // Then, when we’re done, we discard that surrounding scope.
        endScope();

        // Now, whenever a this expression is encountered (at least inside a method) 
        // it will resolve to a “local variable” defined in 
        // an implicit scope just outside of the block for the method body. (resolveFunction creates a new scope)

        // The resolver has a new scope for this , 
        // so the interpreter needs to create a corresponding environment for it.
        return null;
    }
    
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt){
        // In resolution, there is no control-flow
        // So we resolve all the three: conditon, thenBranch, elseBranch
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
        // In resolution, we can also help to check whether a returnStmt is inside a function/method body
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) resolve(stmt.value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        // In resolution, there is no control-flow
        // So we resolve both the condition and while-body
        resolve(stmt.condition);
        resolve(stmt.body);

        return null;
    }
    

    
    //! Effective Exprs

    @Override
    public Void visitThisExpr(Expr.This expr) {
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        // this case can only happen when a local variable is initialized with the (undefined) itself
        // because before the varStmt resolve its initializer, the variable is just decalred rather than defined
        if(!scopes.isEmpty() &&
            scopes.peek().get(expr.name.lexeme) == Boolean.FALSE){
                Lox.error(expr.name, "Can't read local variable in its own initializer.");
            }
        
        // when we do visit a variable, we need to resolve it locally to match it to the correspoding environment. 
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        // the lhs of a AssignExpr can only be Token => variable
        // when we do visit a lhs variable, we need to resolve it locally to match it to the correspoding environment. 
        resolveLocal(expr, expr.name);
        return null;
    }

    //! Exprs for traverse

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        // Again, like Expr.Get, the property itself is dynamically evaluated, so there’s nothing to resolve there.
        resolve(expr.object);
        resolve(expr.value);
        return null;
    }
    @Override
    public Void visitGetExpr(Expr.Get expr) {
        // properties are looked up dynamically, so no need for resolution
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    
    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        // in funtion/method definiton, we don't need to resolve the formal parameters
        // but in real callExpr, we need to resolve the arguments to match the corresponding environment
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
        // Literal are just real objects without the encapsulation of a variable
        // so no need to resolve
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        // In resolution, we don't short-curcit the logicalExpr
        // Instead, we resolve both the lhs and rhs expression
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

    // expr is used to identify which syntax tree node has part to be resolved
    // name is used to find the corresponding environment
    private void resolveLocal(Expr expr, Token name){
        for (int i = scopes.size() - 1; i >= 0; i --) {
            // from the innermost scope and work outwards
            if(scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
            // if no-match, we don't use intepreter.resolve to add resolution information for the global variable
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
