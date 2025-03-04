package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable{
    final String name;
    final Map<String, LoxFunction> methods;
    private final LoxClass superclass;

    LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        // methods are inherited from the superclass.
        // if we don’t find it on the instance’s class, we recurse up through the superclass chain and look there.
        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return null;
    }
    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        // When a class is called, after the LoxInstance is created, 
        // we look for an “init” method. If we find one, we immediately bind and invoke it just like a normal method call.
        LoxFunction initializer = findMethod("init");

        if (initializer != null) {
            // if the initializer exists, bind the newly created instance to 'this' in the enclosing environment of the init method
            // and then call the init method
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    @Override
    public String toString() {
        return "class: " + name + (superclass != null ? " < " + superclass : "");
    }
}
