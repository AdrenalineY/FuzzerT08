package edu.nju.isefuzz.fuzzer.parser;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class FunctionVisitor extends VoidVisitorAdapter<Void> {
    int flag = 0;

    @Override
    public void visit(MethodDeclaration method, Void arg){
        super.visit(method, arg);

        ExpressionStmt outputStmt = new ExpressionStmt(
                new MethodCallExpr(
                        new FieldAccessExpr(
                                new NameExpr("System"),
                                "out"),
                        "println",
                        NodeList.nodeList(new StringLiteralExpr("Flag " + flag + ": function."))));

        BlockStmt body = method.getBody().orElse(null);
        if (body != null) {
            body.addStatement(0,outputStmt);
            ++flag;
        }
    }
}
