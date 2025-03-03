package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable{
    private final Stmt.Function declaration;
    private final Environment closure;
    
    LoxFunction(Stmt.Function declaration, Environment closure){
        this.declaration = declaration;
        this.closure = closure;
    }
    
    LoxFunction bind(LoxInstance instance) {
        // bind a new environment
        // this environment adds "this" -> callee instance
        // and the parent of this environment is the original method's closure
        // env_func ==> env1:"this -> instance" ==> env0:closure
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment);
    }
    @Override
    public int arity(){
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments){
        
        //!! CLOSURE: the parent environment should be which declare it
        //!! NO-CLOSURE: the parent environment shouble just be which the interpreter is current at
        
        Environment environment = new Environment(closure);
        for(int i = 0; i < declaration.params.size(); i++){
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        // default return value -> nil 
        return null;
    }

    @Override
    public String toString(){
        return "<fn " + declaration.name.lexeme + ">";
    }
}
