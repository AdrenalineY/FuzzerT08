package edu.nju.isefuzz.fuzzer.mutateOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class CharInsMut extends MutateKind{
    @Override
    public String mutate(ArrayList<String> sConts, int... params) {
        if (params.length != 2) {
            System.out.println("CharInsMut 参数传入错误");
            return null;
        }

        int n = params[0];
        int K = params[1];

        String sCont = sConts.get(0);
        int sCentLen = sCont.length();

        if(n <= 0 || K <= 0) {return sCont;}
        if(n > sCentLen + 1) {n = sCentLen + 1;}

        Random rand = new Random();
        String[] insStr = new String[n];
        for(int i = 0; i < K; i++){
            int pos = rand.nextInt(n);
            String alpha = (char) (rand.nextInt(26) + 'a') + "";
            if(insStr[pos] == null){
                insStr[pos] = "";
            }
            insStr[pos] = insStr[pos] + alpha;
        }

        int[] rPosList = getSomeRandomNum(sCentLen + 1, n);
        StringBuffer strBuffer = new StringBuffer(sCont);
        for(int i = 0; i < rPosList.length; i++){
            if(insStr[i] != null && insStr[i].length() != 0){ // 非空
                strBuffer.insert(rPosList[i], insStr[i]);
            }
        }

        return strBuffer.toString();
    }

}
