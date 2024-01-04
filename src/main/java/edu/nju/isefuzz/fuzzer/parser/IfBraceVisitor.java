package edu.nju.isefuzz.fuzzer.parser;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;

public class IfBraceVisitor extends VoidVisitorAdapter<Void> {
    int flag = 0;
    boolean isBrace = true;

    String path =  "D:\\codes\\fuzzCodes\\fuzz-mut-demos\\fuzzer-demo\\src\\main\\java\\edu\\nju\\isefuzz\\Target1.java";

    public IfBraceVisitor(String customParameter) {
        this.path = customParameter;
    }

    @Override
    public void visit(IfStmt ifStmt, Void arg){
        super.visit(ifStmt, arg);
        isBrace = true;

        // 判断是否无花括号
        Optional<Range> rangeLine = ifStmt.getRange();
        rangeLine.ifPresent(range -> {
            Position begin = range.begin;
            printCodeLines(path, begin.line);
        });

        if (!isBrace){
            int beginLine = rangeLine.get().begin.line - 1;
            int endLine = beginLine + 1;

            // 读取源文件
            ArrayList<String> lines = new ArrayList<>();
            try(BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 插入花括号
            lines.set(beginLine, lines.get(beginLine) + "{");
            lines.set(endLine, lines.get(endLine) + "}");

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine(); // 写入换行符
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        System.out.println("if :" + flag + " " + ifStmt.getChildNodes().size());
//
//        ExpressionStmt outputStmt = new ExpressionStmt(
//                new MethodCallExpr(
//                        new FieldAccessExpr(
//                                new NameExpr("System"),
//                                "out"),
//                        "println",
//                        NodeList.nodeList(new StringLiteralExpr("Flag " + flag + ": if."))));
//
//        ((BlockStmt)ifStmt.getChildNodes().get(1)).addStatement(0,outputStmt);
//        ++flag;
    }

    // 查找需要补充花括号的代码行 根据行号打印代码
    private void printCodeLines(String filePath, int startLine) {
        try {
            Path path = Paths.get(filePath);
            Files.lines(path)
                    .skip(startLine - 1)
                    .limit(1)
                    .forEach(line -> {
                        if(line.matches(".*\\{.*")){
                            System.out.println("Line " + startLine + " 存在 { ");
                        }else {
                            System.out.println("Line " + startLine + " 不存在 { 需要补充");
                            isBrace = false;
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
