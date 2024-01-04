package edu.nju.isefuzz.fuzzer;

import edu.nju.isefuzz.fuzzer.mutateOperator.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;

public class FuzzerCore {

    static String firstSeed = "abcde";
    static String chooseMutate = "Easy";
    static int maxRound = 2000; // 最大运行轮数

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


    /**
     * The entry point of fuzzing.
     */
    public static void main(String[] args) throws Exception {

        // Initialize. Parse args and prepare seed queue
//        if (args.length < 4) {
//            System.out.println("DemoMutationBlackBoxFuzzer: <classpath> <target_name> <out_dir> <mutate_kind>");
//            System.exit(0);
//        }
        ArrayList<Integer> mutArgs = judgeMutateArgs(args);

        String cp = args[0];
        String tn = args[1];
        File outDir = new File(args[2]);
        chooseMutate = args[3];

        System.out.println("[FUZZER] cp: " + cp);
        System.out.println("[FUZZER] tn: " + tn);
        System.out.println("[FUZZER] outDir: " + outDir.getAbsolutePath());
        System.out.println("[FUZZER] mutKind: " + chooseMutate);
        List<Seed> seedQueue = prepare();
//        List<Seed> crashingInputs = new ArrayList<>();

        // Dry-run phase. Run all the given seeds once.
//        List<Seed> seedQueue = prepare();

        // Main fuzzing loop.q
        int fuzzRnd = 0;
        boolean findCrash = false;
        Set<ExecutionResult> observedRes = new HashSet<>();
        ExecutionResult execRes = null;
        while (true) {
            // Seed scheduling: no seed prioritication and pick next seed. Update round number.
            Seed nextSeed = pickSeed(seedQueue, ++fuzzRnd);
            System.out.printf("[FUZZER] Pick seed `%s`, queue_size `%d`\n",
                    nextSeed, seedQueue.size());

            Seed tempSeed = null;
            if (chooseMutate.equals("Splice")) {
                tempSeed = pickSeed(seedQueue, fuzzRnd + 1);
                System.out.printf("[FUZZER] SpliceMut pick seed `%s`, queue_size `%d`\n",
                        tempSeed, seedQueue.size());
            }

            ArrayList<Seed> seeds = new ArrayList<>();
            seeds.add(nextSeed);
            seeds.add(tempSeed);

            // Generate offspring inputs. 基于种子通过变异生成子代测试样例集合 包括能量分配
            Set<String> testInputs = generate(seeds, mutArgs);

            // Execute each test input. 执行每个测试输入
            for (String ti : testInputs) {  // No. 表示种子轮数
                System.out.printf("[FUZZER] FuzzRnd No.%d, execute the target with input `%s`",
                        fuzzRnd, ti);
                execRes = execute(cp, tn, ti);
                System.out.println(execRes.info);   // 测试目标输出以及崩溃信息均会打印

                // Output analysis.
                // Update queue.
                Seed potentialSeed = new Seed(ti);
                if (seedQueue.contains(potentialSeed))
                    continue;
                // Identify crash
                if (execRes.isCrash()) {    // 识别崩溃
                    // Exit directly once find a crash.
//                    System.out.printf("[FUZZER] Find a crashing input `%s`\n", ti);
//                    System.exit(0);
                    // Try to record these seeds.
                    findCrash = true;
                    potentialSeed.markCrashed();
                }
                // Identify favored seeds.
                if (!observedRes.contains(execRes)) {   // 新路径 获得更多能量
                    potentialSeed.markFavored();
                    observedRes.add(execRes);
                    System.out.printf("[FUZZER] Find a favored seed `%s`\n", potentialSeed);
                }
                seedQueue.add(potentialSeed);
                if (findCrash) {
                    break;
                }
            }

            // 首轮终止限制
//            if(fuzzRnd == 1){
//                break;
//            }

            // Seed scheduling: seed retirement.
            if (seedQueue.size() > 500 || findCrash) {
                int oriSize = seedQueue.size();

                // Remove previously unfavored seeds.
                List<Seed> unfavoredSeeds = new ArrayList<>();
                seedQueue.stream()
                        .filter(s -> !s.isFavored)
                        .forEach(unfavoredSeeds::add);
                seedQueue.removeAll(unfavoredSeeds);
                System.out.printf("[FUZZER] Shrink queue, size: %d -> %d\n",
                        oriSize, seedQueue.size());

            }

            // Break to reach postprocess
            if (findCrash || fuzzRnd > maxRound)
                break;

        } /* End of the main fuzzing loop */

        // Postprocess. Seed preservation (favored & crashed). 在后处理程序中实现输出分析组件 将所有output装入日志文件
        postprocess(outDir, seedQueue, execRes, findCrash);

    }

    private static ArrayList<Integer> judgeMutateArgs(String[] args) {
        if (args.length < 4) {
            System.out.println("FuzzerCore: <classpath> <target_name> <out_dir> <mutate_kind>");
            System.exit(0);
        }
        String mutKind = args[3];
        if (!map.containsKey(mutKind)) {
            System.out.println("FuzzerCore: Wrong <mutate_kind>. Please choose the kind from Readme.md");
            System.exit(0);
        }
        ArrayList<Integer> mutArgs = new ArrayList<>();
        switch (mutKind) {
            case "CharFlip":
                if(args.length == 7){
                    mutArgs.add(Integer.parseInt(args[4]));
                    mutArgs.add(Integer.parseInt(args[5]));
                    mutArgs.add(Integer.parseInt(args[6]));
                }else {
                    System.out.println("FuzzerCore: Wrong args of CharFlip.");
                    System.exit(0);
                }
                break;
            case "CharIns":
                if(args.length == 6){
                    mutArgs.add(Integer.parseInt(args[4]));
                    mutArgs.add(Integer.parseInt(args[5]));
                }else {
                    System.out.println("FuzzerCore: Wrong args of CharIns.");
                    System.out.println(0);
                }
                break;
            case "CharDel":
                if(args.length == 6){
                    mutArgs.add(Integer.parseInt(args[4]));
                    mutArgs.add(Integer.parseInt(args[5]));
                }else {
                    System.out.println("FuzzerCore: Wrong args of CharDel.");
                    System.out.println(0);
                }
                break;
            default:
                break;
        }
        return mutArgs;
    }

    /**
     * A simple ADT for seed inputs.
     */
    private static class Seed {

        String content;
        boolean isFavored;
        int round;
        boolean isCrash;

        Seed(String content, boolean isFavored) {
            this.content = content;
            this.isFavored = isFavored;
            round = 0;
            this.isCrash = false;
        }

        Seed(String content) {
            this(content, false);
        }

        public void markFavored() {
            this.isFavored = true;
        }

        public void markCrashed() {
            this.isCrash = true;
        }

        public void addRound() {
            ++this.round;
        }

        @Override
        public boolean equals(Object that) {
            if (that instanceof Seed)
                return ((Seed) that).content.equals(this.content);
            return false;
        }

        @Override
        public String toString() {
            String suffix = this.isFavored ? "@favored" : "@unfavored";
            return this.content + suffix + " Round:" + this.round;
        }
    }

    /**
     * An exemplified seed.
     */
    static Seed initSeed = new Seed(firstSeed, true);

    /**
     * The preparation stage for fuzzing. At this stage, we tend to
     * collect seeds to build and corpus and minimize the corpus to
     * produce a selective seed queue for fuzzing
     */
    private static List<Seed> prepare() {
        return new ArrayList<>(Collections.singletonList(initSeed));
    }

    /**
     * Pick the next seed. Avoid out of bound.
     */
    private static Seed pickSeed(List<Seed> seeds, int rnd) {
        int pos = (rnd - 1) % seeds.size();
        seeds.get(pos).addRound();
        return seeds.get(pos);
    }
    /**
     * The essential component of a mutation-based fuzzer. This method
     * mutates the given seed once to produce an offspring test input.
     * Here the method implements a simple mutator by adding the character
     * at the given position by step. Besides, this method ensures the
     * mutated character is in [a-z];
     *
     * @param sCont the content of the parent seed input.
     * @param pos   the position of the character to be mutated
     * @param step  the step of character flipping.
     * @return an offspring test input
     */
//    private static String mutate(String sCont, MutateKind kind,int... params) {
//        // TODO: 此处实现5种变异算子
//            kind.mutate(sCont,params);
//    }

    /**
     * Call (different flavors of) mutation methods/mutators several times
     * to produce a set of test inputs for subsequent test executions. This
     * method also showcases a simple power scheduling. The power, i.e., the
     * number of mutations, is affected by the flag {@link Seed#isFavored}.
     * A favored seed is mutated 10 times as an unfavored seed.
     *
     * @param seeds the parent seed input
     * @return a set of offspring test inputs.
     */
    private static Set<String> generate(ArrayList<Seed> seeds, ArrayList<Integer> mutArgs) {

        Seed seed = seeds.get(0);
        String sCont = seed.content;
        Seed tempSeed;
        String sContTemp = null;
        if (chooseMutate.equals("Splice")) {
            tempSeed = seeds.get(1);
            sContTemp = tempSeed.content;
        }
        // 装填Splice的一对种子
        ArrayList<String> sConts = new ArrayList<String>();
        sConts.add(sCont);
        if (sContTemp != null) {
            sConts.add(sContTemp);
        }

        //Power scheduling. 结合运行轮次的能量分配 结合afl-fast 部分方法
        int basePower = 5;
        int powerToken = Math.min(seed.round, 5);
        int power = seed.isFavored ? basePower * (10 - powerToken) : basePower;

        // Test generation.
        Set<String> testInputs = new HashSet<>(power);
        for (int i = 0; i < power; i++) {
            switch (chooseMutate) {
                case "CharFlip":
                    testInputs.add(map.get(chooseMutate).mutate(sConts, mutArgs.get(0), mutArgs.get(1), mutArgs.get(2)));
                    break;
                case "CharDel":
                case "CharIns":
                    testInputs.add(map.get(chooseMutate).mutate(sConts, mutArgs.get(0), mutArgs.get(1)));
                    break;
                case "Rnd":
                    testInputs.add(map.get(chooseMutate).mutate(sConts, seeds.get(0).round));
                    break;
                case "Easy":
                    testInputs.add(map.get(chooseMutate).mutate(sConts, i % sCont.length(), i / sCont.length() + 1));
                    break;
                default:
                    testInputs.add(map.get(chooseMutate).mutate(sConts));
                    break;
            }
        }

        // 返回包含所有该种子生成测试输入的集合
        return testInputs;
    }


    /**
     * A simple wrapper for execution result
     */
    private static class ExecutionResult {
        String info;
        int exitVal;

        ExecutionResult(String info, int exitVal) {
            this.info = info;
            this.exitVal = exitVal;
        }

        public boolean isCrash() {
            return exitVal != 0;
        }

        private String getInfo() {
            return this.info;
        }

        @Override
        public boolean equals(Object that) {
            if (that instanceof ExecutionResult)
                return ((ExecutionResult) that).info.equals(this.info);
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(info);
        }
    }

//    static enum MutateKind{
//        Easy,
//        CharFlip,
//        CharIns,
//        CharDel,
//        Havoc,
//        Splice,
//    }

    /**
     * An execution method for Java-main fuzz targets/drivers. The method
     * execute the given fuzz target once and return the output of the
     * fuzz target.
     *
     * @param cp classpath to the fuzz target
     * @param tn target name, essentially the fully qualified name of a
     *           java class
     * @param ti (the content of) the test input
     * @return the output of the fuzz target.
     * @throws IOException if the executor starts wrongly.
     */
    private static ExecutionResult execute(
            String cp, String tn, String ti) throws IOException, InterruptedException {
        // Construct executor
        ProcessBuilder pb = new ProcessBuilder("java", "-cp", cp, tn, ti);

        // Redirect execution result to here and execute.
        pb.redirectErrorStream(true);   // 将执行结果的错误流重定向到标准输出流，以便统一处理
        Process p = pb.start(); // 执行目标代码
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));  // 获取Process进程的输出数据
        // Wait for execution to finish, or we cannot get exit value.
        p.waitFor();

        // Read execution info
        StringBuilder infoBuilder = new StringBuilder();    // 读取进程的标准输出流，收集执行信息
        String line;
        while (true) {
            line = br.readLine();
            if (line == null)
                break;
            else
                infoBuilder.append('\n');
            infoBuilder.append(line);
        }

        // Wrap and return execution result 包含process退出值 0正常退出 非0非正常 正常崩溃代码为1
//        if(p.exitValue()!=0) {
//            System.out.println(infoBuilder + " --- " + p.exitValue());
//        }
        return new ExecutionResult(infoBuilder.toString(), p.exitValue());
    }

    private static void postprocess(File outDir, List<Seed> seeds, ExecutionResult exeRes, boolean isCrash) throws IOException {
        if (!isCrash) {
            System.out.println("在既定轮数内未发现目标代码崩溃");
            return;
        }
        // Delete old outDir
        if (outDir.exists()) {
            FileUtils.forceDelete(outDir);
            System.out.println("[FUZZER] Delete old output directory.");
        }
        boolean res = outDir.mkdirs();
        if (res)
            System.out.println("[FUZZER] Create output directory.");
        File queueDir = new File(outDir, "queue");
        File crashDir = new File(outDir, "crash");
        res = queueDir.mkdir();
        if (res)
            System.out.println("[FUZZER] Create queue directory: " + queueDir.getAbsolutePath());
        res = crashDir.mkdir();
        if (res)
            System.out.println("[FUZZER] Create crash directory: " + crashDir.getAbsolutePath());
        // Record seeds.
        for (Seed s : seeds) {
            File seedFile;
            if (s.isCrash)
                seedFile = new File(crashDir, s.content);
            else
                seedFile = new File(queueDir, s.content);
            FileWriter fw = new FileWriter(seedFile);
            fw.write(s.content);
            fw.close();
            System.out.println("[FUZZER] Write test input to: " + seedFile.getAbsolutePath());
        }
        // 装填目标崩溃 或 运行超时 时的运行信息
        File report = new File(crashDir, "CrashReport");
        FileWriter fw = new FileWriter(report);
        fw.write(exeRes.getInfo());
        fw.close();
    }

}
