# 模糊测试工具开发日志

### T08小组成员

| 姓名   | 学号      |
| ------ | --------- |
| 严骁   | 211250239 |
| 陈浩星 | 211250237 |
| 徐立杰 | 211250238 |
| 罗晗   | 211250242 |

### 任务分配与开发流程

| 日期  | 工作内容                                                     | 负责人   |
| ----- | ------------------------------------------------------------ | -------- |
| 12.10 | 基于Demo文件搭建项目框架                                     | 严骁     |
| 12.11 | 修改`DemoMutationBlackBoxFuzzer`下的变异算子接口类`MutateKind` | 严骁     |
| 12.13 | 实现`CharIns`、`CharIns`变异算子                             | 陈浩星   |
| 12.15 | 实现`Havoc`、`Splice`、`CharFlip`变异算子                    | 陈浩星·` |
| 12.16 | 基于`jacoco`搭建插桩工具框架                                 | 严骁     |
| 12.18 | 舍弃原框架，转而使用`javaparser`搭建源代码插桩工具框架`ParserText`实现代码块插桩 | 严骁     |
| 12.18 | 实现插桩类`ForVisitor `、`WhileVisitor`                      | 徐立杰   |
| 12.20 | 实现插桩类`FunctionVisitor `、`IfElseVisitor`                | 罗晗     |
| 12.23 | 解决单行分支可能不存在花括号的问题，补充插入花括号类`IfBraceVisitor` | 徐立杰   |
| 12.23 | 添加基于新路径与轮次的能量调度策略                           | 罗晗     |
| 12.24 | 添加新机制，使用`RndMonitorMut`类基于轮次实现一种变异算子调度方法 | 罗晗     |
| 12.25 | 修改参数读取方法，项目收尾                                   | 严骁     |
|       |                                                              |          |

### 主要困难与解决流程

#### 1. 如何优雅地实现变异算子接口

使用基于`HashMap`的表驱动方法。设定抽象父类`MutateKind`,其中开放了抽象方法接口`mutate`，其中参数`sConts`为装载`String`的数组，主要应对`Splice`算子需要传入两个种子的情况，可变参数`params`满足了不同算子传入参数数量不同的条件。

```java
public abstract class MutateKind {
    public abstract String mutate(ArrayList<String> sConts, int... params);

   	...
}

```

子类实现该接口，例如`CharFlipMut`的实现代码

```java
public class CharFlipMut extends MutateKind{
    @Override
    public String mutate(ArrayList<String> sConts, int... params) {
        if (params.length != 3) {
            System.out.println("CharFlipMut 参数传入错误");
            return null;
        }

        int n = params[0];
        int L = params[1];
        int S = params[2];

        String sCont = sConts.get(0);
        int sCentLen = sCont.length();

        if(L <= 0 || S <= 0) {return sCont;}
        if(L > sCentLen) {L = sCentLen;}
        if(S > sCentLen) {S = sCentLen;}
        if(S > L){S = L;}

        Random rand = new Random();
        int startPos = rand.nextInt(sCentLen);   // 获取连续L个字符串的起始位置
//        System.out.println("字符串起始位置:" + startPos + " L:" + L + " S:" + S);
        int[] rPosList = getSomeRandomNum(L, S);    // 获取L中随机S个位置

        EasyMut easy = new EasyMut();
        String str = sCont;
        for(int i = 0; i < rPosList.length; ++i){
            int pos = startPos + rPosList[i] >= sCentLen ?
                    (startPos + rPosList[i]) % sCentLen :
                    startPos + rPosList[i];
//            System.out.println("具体变异的位置:" + pos);
            ArrayList<String> tempStr = new ArrayList<>();
            tempStr.add(str);
            str = easy.mutate(tempStr, pos, n);
        }

        return str;
    }

}
```

主类`FuzzerCore`中使用表驱动调用

```java
private static Map<String, MutateKind> map = new HashMap<String, MutateKind>() {
        {
            put("Easy", new EasyMut());
            put("CharFlip", new CharFlipMut());
            put("CharIns", new CharInsMut());
            put("CharDel", new CharDelMut());
            put("Havoc", new HavocMut());
            put("Splice", new SpliceMut());
            put("Rnd", new RndMonitorMut());
        }
    };
```

```java
case "Rnd":
	testInputs.add(map.get(chooseMutate).mutate(sConts, seeds.get(0).round));
	break;
```

#### 2. 如何实现插桩工作

最初本小组想要集成已有插桩工具`jacoco`，但在一段尝试后发现在项目代码内针对另一个项目路径下的代码进行插桩较为困难，网上大多方法是在maven配置后基于项目下的`test`文档对同项目下的代码进行测试，不符合本小组的需求。因此最终决定使用源代码插桩工具`javaparser`通过分析语法树结构对源代码插桩后，再对目标代码进行编译，最后在测试项目内运行模糊测试工具对目标项目下的代码进行模糊测试。

#### 3. 源代码插桩下的花括号问题

在面对分支语句的时候，小组成员最初的想法是直接在分支语句下的首行插桩，当运行到该分支内时就会输出语句。但当实际测试时发现java分支语句中可能出现一个分支语句内部只有一行代码的情况，此时用户可能不输入花括号，此时插桩工具运行后就会出现如下所示的错误，分支语句逻辑被破坏：

```java
 if (charArr[4] == 'o') 
 	throw new Exception("[TARGET] Hello! Find a bug!");
```

```java
 if (charArr[4] == 'o') 
	System.out.println("Flag 1: if.");
	throw new Exception("[TARGET] Hello! Find a bug!");          
```

本小组花费了大量时间，最终通过在插桩前预先对源代码进行检查，补充需要添加的花括号。同时将用户最初的源代码保存至另一个路径下，以备用户将源码与插桩内容比对。

```java
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
```

