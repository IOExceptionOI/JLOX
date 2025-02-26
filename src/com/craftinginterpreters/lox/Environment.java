package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {

    final Environment enclosing;

    private final Map<String, Object> values = new HashMap<>();    

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing){
        this.enclosing = enclosing;
    }

    

    void define(String name, Object value){
        values.put(name, value);
    }

    void assign(Token name, Object value){
        // inner first
        if(values.containsKey(name.lexeme)){
            values.put(name.lexeme, value);
            return;
        }

        // inner -> outter
        if(enclosing != null){
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, 
            "Undefined variable '" + name.lexeme + "'.");
    }

    Object get(Token name){
        // inner first -> shadow outter
        if(values.containsKey(name.lexeme)){
            return values.get(name.lexeme);
        }

       // inner -> outter 
        if(enclosing != null) return enclosing.get(name);
    

        throw new RuntimeError(name,
            "Undefined variable '"+ name.lexeme + "'.");
    }

}
