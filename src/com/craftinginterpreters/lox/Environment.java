package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {

    // envionment chain
    final Environment enclosing;

    private final Map<String, Object> values = new HashMap<>();    

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing){
        this.enclosing = enclosing;
    }

    
    // Resolver

    // getAt using Resolver has no need for environment chain, instead using index directly
    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    // assignAt using Resolver has no need for environment chain, instead using index directly
    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);

    }

    // using Resolver to find the corresponding ancestor environment
    Environment ancestor(int distance) {
        Environment environment = this;
        for(int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }

        return environment;
    }

    void define(String name, Object value){
        values.put(name, value);
    }

    // Environment Chain

    // using the environment chain
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

    // using the environment chain
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
