package edu.nju.isefuzz.fuzzer.mutateOperator;

import java.util.ArrayList;
import java.util.Random;

public class SpliceMut extends MutateKind{
    @Override
    public String mutate(ArrayList<String> sConts, int... params) {

        String sCont1 = sConts.get(0);
        String sCont2 = null;
        if(sConts.size() == 2){
            sCont2 = sConts.get(1);
        }else {
            sCont2 = sCont1;
        }

        Random rand = new Random();
        int flag = rand.nextInt(2);
        if(sCont1.length() < 2 || sCont2.length() < 2){
            if(flag == 0){
                return sCont1 + sCont2;
            }else {
                return sCont2 + sCont1;
            }
        }

        int splitNode1 = rand.nextInt(sCont1.length() - 1) + 1;
        int splitNode2 = rand.nextInt(sCont2.length() - 1) + 1;

        String sContSub1 = sCont1.substring(0, splitNode1) + sCont2.substring(splitNode2);
        String sContSub2 = sCont2.substring(0, splitNode2) + sCont1.substring(splitNode1);

        if (flag == 0){
            return sContSub1;
        }else {
            return sContSub2;
        }
    }
}
