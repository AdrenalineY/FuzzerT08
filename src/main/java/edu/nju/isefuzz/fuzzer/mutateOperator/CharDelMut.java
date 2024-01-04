package edu.nju.isefuzz.fuzzer.mutateOperator;

import java.util.ArrayList;
import java.util.Random;

public class CharDelMut extends MutateKind{
    @Override
    public String mutate(ArrayList<String> sConts, int... params) {
        if (params.length != 2) {
            System.out.println("CharDelMut 参数传入错误");
            return null;
        }

        int n = params[0];
        int K = params[1];

        String sCont = sConts.get(0);
        int sContLen = sCont.length();

        if(n <= 0 || K <= 0) {return sCont;}
        if(n > sContLen) {n = sContLen;}
        if(K >= sContLen) { return "";}

        Random rand = new Random();
        StringBuilder strBuffer = new StringBuilder(sCont);
        if(K > n){
            int KNum = K;
            for(int i = 0; i < n; i++) {
                int tempCount = 0;
                if(i == n - 1){
                    tempCount = KNum;
                } else if(KNum != 0){
                    tempCount = rand.nextInt(KNum);
                }
                KNum -= tempCount;
                int tempPos = rand.nextInt(strBuffer.length());
                for(int j = 0; j < tempCount; ++j){
                    if(tempPos >= strBuffer.length()) {
                        tempPos = strBuffer.length() - 1;
                    }
                    strBuffer.deleteCharAt(tempPos);
                }
            }
        }else {
            for(int i = 0; i < K; ++i){
                int tempPos = rand.nextInt(strBuffer.length());
                strBuffer.deleteCharAt(tempPos);
            }
        }

        return strBuffer.toString();
    }
}
