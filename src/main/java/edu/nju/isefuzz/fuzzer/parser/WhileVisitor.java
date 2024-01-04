package edu.nju.isefuzz.fuzzer.parser;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class WhileVisitor extends VoidVisitorAdapter<Void> {
    int flag = 0;

    @Override
    public void visit(WhileStmt whileStmt, Void arg){
        super.visit(whileStmt, arg);

        ExpressionStmt outputStmt = new ExpressionStmt(
                new MethodCallExpr(
                        new FieldAccessExpr(
                                new NameExpr("System"),
                                "out"),
                        "println",
                        NodeList.nodeList(new StringLiteralExpr("Flag " + flag + ": while."))));

        ((BlockStmt)whileStmt.getChildNodes().get(1)).addStatement(0,outputStmt);
        ++flag;
    }
}