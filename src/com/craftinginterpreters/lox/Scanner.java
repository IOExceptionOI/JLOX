package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords;

    static{
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    Scanner(String source){
        this.source = source;
    }

    List<Token> scanTokens(){
        while(!isAtEnd()){
            start = current;
            scanToken();
        }
        
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }
    
    private boolean isAtEnd(){
        return current >= source.length();
    }

    private void scanToken(){
        char c = advance();
        switch(c){
            // single-character
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case ';': addToken(SEMICOLON); break;
            case '+': addToken(PLUS); break;
            case '-': addToken(MINUS); break;
            case '*': addToken(STAR); break;
            //! '/' needs special handling
            // case '/': addToken(SLASH); break;
            //  one or two character
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=': 
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<': 
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>': 
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            // special handling for SLASH
            case '/': 
                if(match('/')){
                    // a comment goes until the end of the line
                    while(peek() != '\n' && !isAtEnd()){
                        advance();
                    }
                } else{
                    addToken(SLASH);
                }
            // other meaningless characters
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace
                break;
            case '\n':
                line++;
                break;
            
            // String literals
            case '"': string(); break;
            default:
            // Number literals
            if(isDigit(c)){
                number();
            } else if(isAlpha(c)){
                identifier();
            }else{
                Lox.error(line, "Unexpected character.");
            }
            

           
            
        }

    }

    private char advance(){
        current++;
        return source.charAt(current - 1);
    }
    private char peek(){
        if(isAtEnd()) return '\0';
        return source.charAt(current);
    }
    private boolean match(char expected){
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private void string(){
        while(peek() != '"' && !isAtEnd()){
            if(peek() == '\n') line++;
            advance();
        }

        if(isAtEnd()){
            Lox.error(line, "Unterminated string.");
            return;
        }

        // the closing ".
        advance();
        // Trim the surrouding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }
    private boolean isDigit(char c){
        return c >= '0' && c <= '9';
    }
    private void number(){
        while(isDigit(peek())){
            advance();
        }
        
        // Look for fractional part
        if(peek() == '.' && isDigit(peekNext())){
           // consume the '.' 
           advance();

           while(isDigit(peek())) advance();
        }
       
        
        // correct number : 123.456
        addToken(NUMBER, Double.valueOf(source.substring(start, current)));
    
}
    private boolean isAlpha(char c){
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c == '_'); 
    }
    private boolean isAlphaNumeric(char c){
        return isAlpha(c) || isDigit(c);
    }
    private void identifier(){
        while(isAlphaNumeric(peek())) advance();
        
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if(type == null) type = IDENTIFIER;
        addToken(type);

    }
    private char peekNext(){
        if(current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    // intermediate addToken for Token without literal
    private void addToken(TokenType type){
        addToken(type, null);
    }

    // addToken with 2 cases:
    // 1. Token with literal
    // 2. Token without literal (using previous addToken as intermediate)
    // add extra information : 1. lexeme(text) 2.line
    private void addToken(TokenType type, Object literal){
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));

    }
}
