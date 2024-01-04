package edu.nju.isefuzz.fuzzer.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class ParserText {
    static String tagFilePath = "";
    static String saveFilePath = "";
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.println("输入待插桩文件源码绝对路径:");
        if (scan.hasNext()) {
            String str1 = scan.next();
            System.out.println("输入的数据为：" + str1);
            tagFilePath = str1;
        }
        System.out.println("输入未被插桩的源码将被暂时保存到的路径:");
        if (scan.hasNext()) {
            String str2 = scan.next();
            System.out.println("输入的数据为：" + str2);
            saveFilePath = str2;
        }
        scan.close();
        instrument(tagFilePath);
    }

    private static void instrument(String filePath) {
        String[] parts = filePath.split("\\\\");
        // 添加花括号
        try {
            CompilationUnit cuTemp = StaticJavaParser.parse(Paths.get(filePath));
            String modifiedSource = cuTemp.toString();
            Files.write(Paths.get(saveFilePath + "\\" + parts[parts.length - 1]), modifiedSource.getBytes(StandardCharsets.UTF_8));

            cuTemp.accept(new IfBraceVisitor(filePath), null);
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            CompilationUnit cu = StaticJavaParser.parse(Paths.get(filePath));

            // 使用自定义的访问器进行插桩
            cu.accept(new IfElseVisitor(filePath), null);
            cu.accept(new WhileVisitor(), null);
            cu.accept(new ForVisitor(), null);
            cu.accept(new FunctionVisitor(), null);

            // 将修改后的 CompilationUnit 输出到控制台
            System.out.println(cu.toString());

            // 将修改后的源代码写回到目标文件
            String modifiedSource = cu.toString();
            Files.write(Paths.get(filePath), modifiedSource.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
