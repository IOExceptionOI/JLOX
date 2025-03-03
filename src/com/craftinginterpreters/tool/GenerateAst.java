package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
public class GenerateAst {
    static String indents(int times){
        return "    ".repeat(times);
    }
    public static void main(String[] args) throws IOException {
      if(args.length != 1){
        System.err.println("Usage: generate_ast <output directory>");
        System.exit(64);
      } 
      String outputDir = args[0];
      defineAst(outputDir, "Expr", Arrays.asList(
        "Set : Expr object, Token name, Expr value",
        "Get : Expr object, Token name",
        "Call : Expr callee, Token paren, List<Expr> arguments",
        "Assign : Token name, Expr value",
        "Logical : Expr left, Token operator, Expr right",
        "Binary : Expr left, Token operator, Expr right",
        "Grouping : Expr expression",
        "Literal : Object value",
        "Unary : Token operator, Expr right",
        "Variable : Token name" 
      ));

      defineAst(outputDir, "Stmt", Arrays.asList(
        "Class : Token name, List<Stmt.Function> methods",
        "Return : Token keyword, Expr value",
        "Function : Token name, List<Token> params, List<Stmt> body",
        "Block : List<Stmt> statements",
        "Expression : Expr expression",
        "If : Expr condition, Stmt thenBranch, Stmt elseBranch",
            "Print : Expr expression",
            "Var : Token name, Expr initializer:",
            "While : Expr condition, Stmt body"
      ));
   } 

   private static void defineAst(
    String outputDir, String baseName, List<String> types
   ) throws IOException{
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println(indents(0) + "abstract class " + baseName + "{");
    writer.println();
    // The base accept() method
    writer.println(indents(1) + "abstract <R> R accept(Visitor<R> visitor);");

    writer.println();
    // The Visitor classes
    defineVisitor(writer, baseName, types);

    // The AST classes
    for(String type : types){
        String className = type.split(":")[0].trim();
        String fields = type.split(":")[1].trim();
        defineType(writer, baseName, className, fields);
    }


    writer.println(indents(0) + "}");
    writer.close();

   }
   private static void defineVisitor(PrintWriter writer, String baseName, List<String> types){
    /* 
        interface Visitor<R>{
            R visitBinaryExpr(Binary expr);
            R visitLiteralExpr(Literal expr);
            ...
        } 
    */ 
    
    writer.println(indents(1) + "interface Visitor<R>{");

    for(String type : types){
        String typeName = type.split(":")[0].trim();
        writer.println(indents(2) + "R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() +");");
    }

    writer.println(indents(1) + "}");
    writer.println();
   }
   private static void defineType(
    PrintWriter writer, String baseName,
    String className, String fieldList){
        //?  baseName : Expr
        //?  fieldList: "Binary : Expr left, Token operator, Expr right" ...
        

        //? abstract class Expr{
        //?    
        //?    abstrct <R> R accept(Visitor<R> visitor);
        //?
        //?    interface Visitor<R>{
        //?        R visitBinaryExpr(Binary expr);
        //?        R visitLiteralExpr(Literal expr);
        //?        ...
        //?    } 
        //?
        //?     static class Binary extends Expr{
        //?         Binary(Expr left, Token operator, Expr right){
        //?             this.left = left;
        //?             this.operator = operator;
        //?             this.right = right;
        //?         }
        //?         
        //?         final Expr left;
        //?         final Token operator;
        //?         final Expr right;
        //? 
        //?         @Override
        //?         <R> R accept(Visitor<R> visitor){
        //?             return visitor.visitBinaryExpr(this);
        //?         }
        //?     }
        //? }
        writer.println(indents(1)+ "static class " + className + " extends " + baseName + "{" );
        
        // Constructor
        writer.println(indents(2) + className + "(" + fieldList + ")" + "{");

        // Store parameters in fields
        String[] fields = fieldList.split(", ");
        for (String field : fields){
            String name = field.split(" ")[1];
            writer.println(indents(3)+ "this." + name + " = " + name + ";");
        }

        writer.println(indents(2)+ "}");
        writer.println();

        // Fields.
        for (String field : fields){
            writer.println(indents(2) + "final " + field + ";");
        }
        writer.println();

        // Visitor pattern
        writer.println(indents(2) + "@Override");
        writer.println(indents(2) + "<R> R accept(Visitor<R> visitor){");
        writer.println(indents(3) + "return visitor.visit" + className + baseName + "(this);");
        writer.println(indents(2) + "}");
        writer.println();
        
        

        writer.println(indents(1) + "}");
    }
   
}
