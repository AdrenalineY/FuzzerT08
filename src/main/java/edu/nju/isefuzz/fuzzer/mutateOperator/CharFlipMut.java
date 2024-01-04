package edu.nju.isefuzz.fuzzer.mutateOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

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
        int[] rPosList = getSomeRandomNum(L, S);    // 获取L中随机S个位置

        EasyMut easy = new EasyMut();
        String str = sCont;
        for(int i = 0; i < rPosList.length; ++i){
            int pos = startPos + rPosList[i] >= sCentLen ?
                    (startPos + rPosList[i]) % sCentLen :
                    startPos + rPosList[i];
            ArrayList<String> tempStr = new ArrayList<>();
            tempStr.add(str);
            str = easy.mutate(tempStr, pos, n);
        }

        return str;
    }

}
