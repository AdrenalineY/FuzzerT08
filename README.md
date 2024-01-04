# 模糊测试工具项目文档

### T08小组成员

| 姓名   | 学号      |
| ------ | --------- |
| 严骁   | 211250239 |
| 陈浩星 | 211250237 |
| 徐立杰 | 211250238 |
| 罗晗   | 211250242 |

### 设计方案

本小组模糊测试器主要在助教提供的**fuzzer-demo** ([fuzz-mut-demos/fuzzer-demo at main · QRXqrx/fuzz-mut-demos (github.com)](https://github.com/QRXqrx/fuzz-mut-demos/tree/main/fuzzer-demo))模糊测试简单实现代码框架上扩充搭建而成。在本项目中，模糊目标是以`public static void main(String[] args)`为执行入口、需要一个字符串作为输入的Java主类。

#### 项目架构

```
├── README.md
├── devlog.md
├── pom.xml
└── src
    ├── main            	# edu.nju.isefuzz.fuzzer目录下为模糊测试工具的详细实现
    └── test             
```

其中 **edu.nju.isefuzz.fuzzer** 内的具体结构为

```
├── FuzzerCore				# 模糊测试工具的主体
├── mutateOperator			# 主要用于实现模糊测试变异算子
	├── CharDelMut
    ├── CharFlipMut
    ├── CharInsMut
    ├── EasyMut				# 单次只替换一个字符的简单变异算子
    ├── HavocMut
    ├── RndMonitorMut		# 基于种子使用次数对变异算子进行调度的算子
    ├── SpliceMut
    └── MutateKind			# 变异算子抽象父类
└── parser					# 基于javaparser实现的源代码插桩工具
	├── ForVisitor
	├── FunctionVisitor
	├── IfBraceVisitor		# 对不含花括号的单行分支语句进行补全
	├── IfElseVisitor
	├── ParserText			# 插桩工具运行类
	└── WhileVisitor
```

#### 设计流程

依据选题要求，我们小组选择将种子调度组件与输出分析组件直接在代码主体**FuzzerCore**内实现，而将种子生成工具中调用的变异算子通过`public abstract String mutate(ArrayList<String> sConts, int... params);`接口外放至**mutateOperator**包实现。而基于**javaparser**的源代码插桩工具整体与核心测试代码关联不大，因此选择在**parser**包下实现独立的运行模块。

##### 插桩工具

插桩工具向用户获取**待插桩文件源码绝对路径**和**未被插桩的源码将被暂时保存到的路径**，然后在目标代码的原路径下进行插桩。插桩流程通过` private static void instrument(String filePath)`函数实现，其中先调用**IfBraceVisitor**对源码花括号进行维护，随后再自定义的访问器对源码进行代码块插桩。

```java
public class ParserText {
    static String tagFilePath = "";
    static String saveFilePath = "";
    public static void main(String[] args) {
       ...
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

```

##### 种子调度组件

种子调度主要实现了能量调度功能。通过将新生成的种子的覆盖路径与所有历史种子相比较，若产生了全新的覆盖路径则将该种子定义为**偏好种子**，偏好种子在此后的种子变异过程中将拥有更高的能量分配。同时赋予偏好种子**使用轮次**属性，随着偏好种子使用轮次的增加，该种子所获的能量分配将逐渐下降。并且使用轮次还被用于**变异算子调度**，在`RndMonitorMut`算子中获取该种子的使用次数，在使用轮次少时主要进行**Havoc**复杂变异，以期对较多路径进行覆盖，在使用轮次较多时则主要进行简单变异，每次只替换单个字符，以期发现可能被漏过的漏洞。

##### 输入生成组件

输入生成组件的核心是调动变异算子进行种子变异。调用变异算子的过程使用基于`HashMap`的表驱动方法。设定抽象父类`MutateKind`,其中开放了抽象方法接口`mutate`，其中参数`sConts`为装载`String`的数组，主要应对`Splice`算子需要传入两个种子的情况，可变参数`params`满足了不同算子传入参数数量不同的条件。

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

##### 输出分析组件

输出分析主要在`private static void postprocess(File outDir, List<Seed> seeds, ExecutionResult exeRes, boolean isCrash)`方法中实现，在项目的`target/classes/out`路径下输出测试信息，当发现目标程序意外终止时，将会在`crash`文件下生成`CrashReport`存储程序崩溃时的插桩输出与最终崩溃的报错信息，便于使用者与源码比对发现错误，同时输出奔溃输入。同时用户还可以在测试运行的过程中实时在命令行中查看每次运行的插桩输出以及当前种子的覆盖情况。

### 使用方法

#### 1. 插桩

在idea中运行**ParserText**类，此时用户需要在命令行中输入**待插桩文件源码绝对路径**和**未被插桩的源码将被暂时保存到的路径**，待插桩文件的源码路径下的文件将会替换为插桩后的文件，而未被插桩的源代码文件将会被保存在用户选择的暂存路径下。案例如下。

```
输入待插桩文件源码绝对路径:
D:\codes\fuzzCodes\fuzz-mut-demos\fuzz-targets\edu\nju\isefuzz\trgt\Target1.java
输入的数据为：D:\codes\fuzzCodes\fuzz-mut-demos\fuzz-targets\edu\nju\isefuzz\trgt\Target1.java
输入未被插桩的源码将被暂时保存到的路径:
D:\codes\fuzzCodes\fuzzT08\fuzzer\src\main\java\edu\nju\isefuzz\fuzzer
输入的数据为：D:\codes\fuzzCodes\fuzzT08\fuzzer\src\main\java\edu\nju\isefuzz\fuzzer
```

输入后终端会提示是否插桩成功。

#### 2. 模糊测试

进入项目路径，示例如下：

```
PS D:\codes\fuzzCodes\fuzzT08\fuzzer> cd ./target/classes
```

对待测试的源代码进行编译，若目标代码也在一个完整的idea项目内，则可以使用idea自带的项目构建功能获得目标的编译后类文件。运行模糊测试的代码语句如下：

```
java edu.nju.isefuzz.fuzzer.FuzzerCore <classpath> <target_name> <out_dir> <mutate_kind> args...
```

\<classpath> 为目标项目类路径、<target_name>为模糊目标完全限定类名、<out_dir>为模糊结果的保存路径、<mutate_kind>为该次模糊测试选用的编译算子，其中CharFlip需要依次额外传入三个参数n L S，分别表示每个字符增加的增量、翻转字符区间的总长、每次翻转的字符数量。CharIns和CharDel需要依次传入两个参数n K，分别表示在随机n个位置随机删除或插入K个字符，其他算子均不需要额外传入参数。以下提供一种实例：

```
java edu.nju.isefuzz.fuzzer.FuzzerCore D:\codes\fuzzCodes\fuzz-mut-demos\fuzz-targets\out\production\fuzz-targets edu.nju.isefuzz.trgt.Target1 ./out CharFlip 1 3 2
```

成功运行后，你可以在命令行中实时查看模糊测试状态，测试结束后你可以在模糊结果的保存路径中查看报错报告和导致崩溃的输入。
