package edu.nju.isefuzz.fuzzer.mutateOperator;

import java.util.ArrayList;
import java.util.Random;

public class RndMonitorMut extends MutateKind { // 变异算子调度

    @Override
    public String mutate(ArrayList<String> sConts, int... params) {
        if (params.length != 1) {
            System.out.println("RndMonitorMut 参数传入错误");
            return null;
        }

        int round = params[0];

        HavocMut havocMut = new HavocMut();
        EasyMut easyMut = new EasyMut();

        String sCont = sConts.get(0);
        int sCentLen = sCont.length();

        Random rand = new Random();
        int rndTag = Math.min(round, 7);
        if(rand.nextInt(10) <= rndTag){
            return easyMut.mutate(sConts, rand.nextInt(sCentLen), round);
        }else {
            return havocMut.mutate(sConts);
        }
    }
}

