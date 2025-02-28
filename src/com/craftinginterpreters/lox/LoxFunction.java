package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable{
    private final Stmt.Function declaration;
    
    LoxFunction(Stmt.Function declaration){
        this.declaration = declaration;
    }
    
    @Override
    public int arity(){
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments){
        //? WHY NOT (new Environment(interpreter.environment)) ?
        Environment environment = new Environment(interpreter.environment);
        for(int i = 0; i < declaration.params.size(); i++){
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        interpreter.executeBlock(declaration.body, environment);
        return null;
    }

    @Override
    public String toString(){
        return "<fn " + declaration.name.lexeme + ">";
    }
}
