package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import java.util.ArrayList;
import java.util.List;


public class Parser {
    private static class ParseError extends RuntimeException{}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    //! program -> declaration* EOF
    List<Stmt> parse(){
        List<Stmt> statements = new ArrayList<>();
        while(!isAtEnd()){
            statements.add(declaration());
        }
        return statements;
    }
    //! declaration -> varDecl
    //!              | statement
    private Stmt declaration(){
        try {
           if(match(VAR)) return varDeclaration(); 
           return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }
    private Stmt varDeclaration(){
        Token name = consume(IDENTIFIER, "Expected variable name.");
        
        Expr initializer = null;
        if(match(EQUAL)){
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }
    //! statement -> exprStmt
    //!            | printStmt
    //!            | block
    private Stmt statement(){
        if(match(PRINT)) return printStatement();
        if(match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    //! block -> "{" declaration* "}"
    private List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();

        while(!check(RIGHT_BRACE) && !isAtEnd()){
            statements.add(declaration());
        }

        consume(RIGHT_BRACE,"Expect '}' after block.");
        return statements;
    }

    //! exprStmt -> expression ";"
    private Stmt expressionStatement(){
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }
    //! printStmt -> "print" expression ";"
    private Stmt printStatement(){
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }
    //! expression -> assignment 
    private Expr expression(){
        return assignment();
    }

    //! assignment -> IDENTIFIER "=" assignment
    //!             | equality;
    private Expr assignment(){
        Expr expr = equality();

        if(match(EQUAL)){
            Token equals = previous();
            // right-associativity -> recursive
            Expr value = assignment();

            if(expr instanceof Expr.Variable){
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }


    //! equality -> comparison(("!=" | "==") comparison)*
    private Expr equality(){
        // current ==> left
        Expr left_expr = comparison();
        // current ==> operator
        while(match(BANG_EQUAL, EQUAL_EQUAL)){
            // if match, current++ ==> right
            // previous == current - 1 ==> matched operator
            Token operator = previous();
            // current ==> right
            Expr right_expr = comparison();
            // current ==> operator


            // left associativity
            // the current expr becomes the left part of the next BinaryExpr
            left_expr = new Expr.Binary(left_expr, operator, right_expr);
        }

        return left_expr;
    }

    private boolean match(TokenType... types){
        for(TokenType type : types){
            if(check(type)){
                // if matched, consumes the token ==> current++
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type){
        if(isAtEnd()){
            return false;
        }
        return peek().type == type;
    }


    private Token advance(){
        if(!isAtEnd()){
            current++;
        }
        return previous();
    }

    private boolean isAtEnd(){
        return peek().type == EOF;
    }

    private Token peek(){
        return tokens.get(current);
    }

    private Token previous(){
        return tokens.get(current - 1);
    }

    //! comparison -> term ( (">" | ">=" | "<" | "<=") term )*
    private Expr comparison(){
        // current ==> left term
        Expr left_expr = term();

        // current ==> operator
        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            // current ==> right term
            Expr right_expr = term(); 
            // left associativity
            left_expr = new Expr.Binary(left_expr, operator, right_expr);
        }

        return left_expr;
    }

    //! term -> factor ( ("+" | ("-") factor )*
    private Expr term(){
        // current ==> left factor
        Expr left_expr = factor();

        // cutrent ==> operator
        while(match(PLUS, MINUS)){
            Token operator = previous();
            // current ==> right factor
            Expr right_expr = factor();

            // left associativity
            left_expr = new Expr.Binary(left_expr, operator, right_expr);
        }
        return left_expr;
    }    

    //! factor -> unary ( ("*" | "/" ) unary)*
    private Expr factor(){
        // current ==> left unary
        Expr left_expr = unary();

        // current ==> operator
        while(match(STAR, SLASH)){
            // current ==> operator + 1
            Token operator = previous();
            // current ==> right unary
            Expr right_expr = unary();

            // left associativity
            left_expr = new Expr.Binary(left_expr, operator, right_expr);
        }
        return left_expr;
    }

    //! unary -> ( "!" | "-" ) unary
    //!        | primary
    private Expr unary(){
        // current ==> operator
        if(match(BANG, MINUS)){
            // current ==> opreator + 1
            Token operator = previous();
            Expr right_expr = unary();
            // right associativity
            return new Expr.Unary(operator, right_expr);
        }
        return primary();
    }

    //! primary -> NUMBER | STRING | "true" | "false" | "nil"
    //!          | IDENTIFIER
    //!          | "(" expression ")"
    private Expr primary(){
        if(match(FALSE)) return new Expr.Literal(false);
        if(match(TRUE))  return new Expr.Literal(true);
        if(match(NIL))   return new Expr.Literal(null);

        if(match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }

        if(match(IDENTIFIER)){
            return new Expr.Variable(previous());
        }
        
        if(match(LEFT_PAREN)){
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect exprssion.");
    }
    
    private Token consume(TokenType type, String message){
        if(check(type)){
            return advance();
        }

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message){
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize(){
        // panic_token ';'
        advance();

        while(!isAtEnd()){
            if(previous().type == SEMICOLON){
                return;
            }

            switch(peek().type){
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

}
